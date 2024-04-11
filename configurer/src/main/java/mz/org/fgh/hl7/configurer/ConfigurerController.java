package mz.org.fgh.hl7.configurer;

import static mz.org.fgh.hl7.lib.Constants.C_SAUDE_SECRET_KEY_ALIAS;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

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

import mz.org.fgh.hl7.lib.service.HL7EncryptionService;
import mz.org.fgh.hl7.lib.service.HL7KeyStoreException;
import mz.org.fgh.hl7.lib.service.HL7KeyStoreService;

@RestController
public class ConfigurerController {

    private static final Path[] TOMCAT_WEBAPPS_DIR = new Path[] {
            Paths.get("/", "usr", "local", "tomcat", "webapps"),
            Paths.get("C:\\Program Files\\Apache Software Foundation\\Tomcat 8.5\\webapps")
    };

    private static final Logger log = LoggerFactory.getLogger(ConfigurerController.class);

    private HL7EncryptionService encryptionService;

    private HL7KeyStoreService keyStoreService;

    public ConfigurerController(
            HL7EncryptionService encryptionService,
            HL7KeyStoreService keyStoreService) {
        this.encryptionService = encryptionService;
        this.keyStoreService = keyStoreService;
    }

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
            BindingResult binding) throws IOException {

        if (binding.hasErrors()) {
            Map<String, String> errors = binding.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            return ResponseEntity.badRequest().body(errors);
        }

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

        String cSaudeSecretKey = new String(keyStoreService.getEntries().get(C_SAUDE_SECRET_KEY_ALIAS));
        Path encrypted = applicationProperties.resolveSibling("application.properties.enc");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(encryptionService.decrypt(encrypted, cSaudeSecretKey)))) {
            props.load(reader);
        } catch (NoSuchFileException e) {
            log.info("Could not find configuration in {}", folder, e);
        } catch (AccessDeniedException e) {
            log.info("Could read configuration in {}", folder, e);
        }
        return props;
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
        Path encryptedPath = applicationProperties.resolveSibling("application.properties.enc");
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            props.setProperty("app.username", config.getAppUsername());
            props.setProperty("app.password", config.getAppPassword());
            props.setProperty("app.openmrs.login", String.valueOf(config.getAppOpenmrsLogin()));
            props.setProperty("openmrs.url", config.getOpenmrsUrl());
            props.setProperty("openmrs.username", config.getOpenmrsUsername());
            props.setProperty("openmrs.password", config.getOpenmrsPassword());
            props.setProperty("spring.datasource.url", config.getDataSourceUrl());
            props.setProperty("spring.datasource.username", config.getDataSourceUsername());
            props.setProperty("spring.datasource.password", config.getDataSourcePassword());
            props.store(out, null);
            String cSaudeSecretKey = new String(keyStoreService.getEntries().get(C_SAUDE_SECRET_KEY_ALIAS));
            encryptionService.encrypt(out, cSaudeSecretKey, encryptedPath);
            Files.deleteIfExists(applicationProperties);
        } catch (HL7KeyStoreException e) {
            log.error("Erro ao carregar a keyStore.", e);
            throw e;
        }
    }
}
