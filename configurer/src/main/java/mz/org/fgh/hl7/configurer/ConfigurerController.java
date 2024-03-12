package mz.org.fgh.hl7.configurer;

import static mz.org.fgh.hl7.lib.Constants.DISA_SECRET_KEY_ALIAS;
import static mz.org.fgh.hl7.lib.Constants.KEY_STORE_TYPE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mz.org.fgh.hl7.lib.Base64EncoderDecoder;

@RestController
public class ConfigurerController {

    private static final String DISA_SECRET_KEY_CYPHER = "AES";

    private static final Path[] TOMCAT_WEBAPPS_DIR = new Path[] {
            Paths.get("/", "usr", "local", "tomcat", "webapps"),
            Paths.get("C:\\Program Files\\Apache Software Foundation\\Tomcat 8.5\\webapps")
    };

    private Logger log = LoggerFactory.getLogger(getClass());

    @GetMapping("/folder")
    public String getWebAppFolder() {
        for (Path path : TOMCAT_WEBAPPS_DIR) {
            Path hl7WebApp = path.resolve("hl7");
            if (Files.exists(hl7WebApp) && Files.isDirectory(hl7WebApp)) {
                return hl7WebApp.toString();
            }
        }
        return "";
    }

    @GetMapping("/key-store")
    public ResponseEntity<Map<String, String>> isValidKeyStore(@RequestParam String keyStorePath,
            @RequestParam String keyStorePassword)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
            UnrecoverableEntryException {
        Path path = Paths.get(keyStorePath);

        if (!Files.exists(path)) {
            log.info("Could not find keystore {}", path);
            return ResponseEntity.notFound().build();
        }

        KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);

        try (InputStream is = Files.newInputStream(path)) {

            keyStore.load(is, keyStorePassword.toCharArray());
            KeyStore.ProtectionParameter protectionParam = new KeyStore.PasswordProtection(
                    keyStorePassword.toCharArray());
            KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore
                    .getEntry(DISA_SECRET_KEY_ALIAS, protectionParam);

            // If configuring the app for the first time, the secret key might not be
            // available, so we'll return an empty string.
            if (secretKeyEntry == null) {
                return ResponseEntity.ok(Collections.singletonMap(DISA_SECRET_KEY_ALIAS, ""));
            }

            SecretKey secretKey = secretKeyEntry.getSecretKey();
            Map<String, String> map = Collections.singletonMap(DISA_SECRET_KEY_ALIAS,
                    new String(secretKey.getEncoded()));
            return ResponseEntity.ok(map);

        } catch (NoSuchFileException e) {
            log.info("Could not find keystore {}", path, e);
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            log.info("Could read keystore {}", path, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IOException e) {
            if (e.getCause() instanceof UnrecoverableKeyException) {
                log.info("Wrong password for keystore {}", path, e);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            } else {
                throw e;
            }
        }
    }

    @GetMapping("/configuration")
    public ResponseEntity<Configuration> getConfiguration(@RequestParam String folder) throws IOException {
        Configuration configuration = new Configuration();
        try {
            configuration.load(loadProperties(folder));
            return ResponseEntity.ok().body(configuration);
        } catch (NoSuchFileException e) {
            log.info("Could not find configuration in {}", folder, e);
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            log.info("Could read configuration in {}", folder, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/configuration")
    public ResponseEntity<Object> configure(@RequestParam String folder, @RequestBody @Valid Configuration config,
            BindingResult binding)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {

        if (binding.hasErrors()) {
            Map<String, String> errors = binding.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            return ResponseEntity.badRequest().body(errors);
        }

        storeSecretKey(config);
        storeConfiguration(folder, config);

        return ResponseEntity.ok(config);
    }

    /**
     * Tries to find the HL7 app folder and load application.properties values into
     * the Configuration.
     * 
     * @param folder the tomcat webapps HL7 app folder.
     * @return The Configuration
     * @throws IOException
     */
    private Properties loadProperties(String folder) throws IOException {
        Path applicationProperties = Paths.get(folder)
                .resolve(Paths.get("WEB-INF", "classes", "application.properties"));

        Properties props = new Properties();
        // First try to load the unencrypted file
        try (BufferedReader reader = Files.newBufferedReader(applicationProperties)) {
            props.load(reader);
            return props;
        } catch (IOException e) {
            // No problem, we'll look for the encrypted file
        }

        Path encrypted = applicationProperties.resolveSibling("application.properties.enc");
        try (InputStreamReader reader = Base64EncoderDecoder.decodeBase64ToInputStreamReader(encrypted)) {
            props.load(reader);
        }
        return props;
    }

    /**
     * Store disa secret key inside keystore.
     * 
     * @param config
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    private void storeSecretKey(Configuration config)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
        Path path = Paths.get(config.getKeyStorePath());
        char[] password = config.getKeyStorePassword().toCharArray();

        if (Files.exists(path)) {
            try (InputStream inStream = Files.newInputStream(path)) {
                keyStore.load(inStream, password);
            }
        } else {
            keyStore.load(null, password);
        }

        try (OutputStream outStream = Files.newOutputStream(path)) {
            SecretKey secretKey = new SecretKeySpec(config.getAppHL7PassPhrase().getBytes(), DISA_SECRET_KEY_CYPHER);
            KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
            KeyStore.ProtectionParameter protectionParam = new KeyStore.PasswordProtection(password);
            keyStore.setEntry(DISA_SECRET_KEY_ALIAS, secretKeyEntry, protectionParam);
            keyStore.store(outStream, password);
        }
    }

    /**
     * Saves the configuration making sure that existing properties that are not
     * available in the given config are kept.
     * The existing unencrypted configuration file will be deleted.
     * 
     * @param folder the tomcat webapps HL7 app folder.
     * @param config the configuration to save.
     * @throws IOException
     */
    private void storeConfiguration(String folder, Configuration config) throws IOException {
        Properties props = loadProperties(folder);
        Path applicationProperties = Paths.get(folder, "WEB-INF", "classes", "application.properties");
        try (BufferedWriter writer = Files
                .newBufferedWriter(applicationProperties.resolveSibling("application.properties.enc"))) {
            props.setProperty("app.keyStore", Paths.get(config.getKeyStorePath()).toRealPath().toString());
            props.setProperty("app.username", config.getAppUsername());
            props.setProperty("app.password", config.getAppPassword());
            props.setProperty("openmrs.url", config.getOpenmrsUrl());
            props.setProperty("openmrs.username", config.getOpenmrsUsername());
            props.setProperty("openmrs.password", config.getOpenmrsPassword());
            props.setProperty("spring.datasource.url", config.getDataSourceUrl());
            props.setProperty("spring.datasource.username", config.getDataSourceUsername());
            props.setProperty("spring.datasource.password", config.getDataSourcePassword());
            String encryptedProps = Base64EncoderDecoder.encodePropertiesToBase64(props);
            writer.write(encryptedProps);
        }
        Files.deleteIfExists(applicationProperties);
    }
}
