package mz.org.fgh.hl7.web.env;

import static mz.org.fgh.hl7.lib.Constants.DISA_SECRET_KEY_ALIAS;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.crypto.SecretKey;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

import lombok.extern.log4j.Log4j2;
import mz.org.fgh.hl7.lib.Base64EncoderDecoder;
import mz.org.fgh.hl7.lib.Constants;

@Log4j2
public class EncryptedPropertySourceLoader implements PropertySourceLoader {

    private String keyStorePassword;

    public EncryptedPropertySourceLoader(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    @Override
    public String[] getFileExtensions() {
        return new String[] { "enc" };
    }

    @Override
    public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
        try (InputStreamReader input = Base64EncoderDecoder
                .decodeBase64ToInputStreamReader(resource.getFile().toPath())) {
            Properties props = new Properties();
            props.load(input);
            String passPhrase = loadPassPhraseFromKeyStore(props.getProperty("app.keyStore"), keyStorePassword);
            props.setProperty("app.hl7.passPhrase", passPhrase);
            return Collections.singletonList(new PropertiesPropertySource("decrypted-props", props));
        }

    }

    private String loadPassPhraseFromKeyStore(String keyStorePath, String keyStorePassword) throws IOException {
        Path path = Paths.get(keyStorePath);
        try (InputStream is = Files.newInputStream(path)) {
            KeyStore keyStore = KeyStore.getInstance(Constants.KEY_STORE_TYPE);

            keyStore.load(is, keyStorePassword.toCharArray());
            KeyStore.ProtectionParameter protectionParam = new KeyStore.PasswordProtection(
                    keyStorePassword.toCharArray());
            KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore
                    .getEntry(DISA_SECRET_KEY_ALIAS, protectionParam);

            if (secretKeyEntry == null) {
                throw new IOException("disaSecretKey alias missing from keystore");
            }

            SecretKey secretKey = secretKeyEntry.getSecretKey();
            return new String(secretKey.getEncoded());

        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableEntryException e) {
            log.error("Unable to load secret key from keystore", e);
        }
        return null;
    }
}
