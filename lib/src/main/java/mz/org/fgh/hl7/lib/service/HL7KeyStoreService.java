package mz.org.fgh.hl7.lib.service;

import static mz.org.fgh.hl7.lib.Constants.C_SAUDE_SECRET_KEY_ALIAS;
import static mz.org.fgh.hl7.lib.Constants.DISA_SECRET_KEY_ALIAS;
import static mz.org.fgh.hl7.lib.Constants.KEY_STORE_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.SecretKeyEntry;
import java.security.UnrecoverableKeyException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HL7KeyStoreService {

    private static final Logger log = LoggerFactory.getLogger(HL7KeyStoreService.class);

    private String keyStorePath;
    private String keyStorePassword;

    public HL7KeyStoreService(
            String keyStorePath,
            String keystorePassword) {
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keystorePassword;
    }

    public Map<String, byte[]> getEntries() throws IOException {

        Path path = Paths.get(keyStorePath);

        try (InputStream is = Files.newInputStream(path)) {

            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);

            keyStore.load(is, keyStorePassword.toCharArray());
            KeyStore.ProtectionParameter protectionParam = new KeyStore.PasswordProtection(
                    keyStorePassword.toCharArray());

            KeyStore.SecretKeyEntry disaSecretKey = (KeyStore.SecretKeyEntry) keyStore
                    .getEntry(DISA_SECRET_KEY_ALIAS, protectionParam);
            KeyStore.SecretKeyEntry csaudeSecretKey = (SecretKeyEntry) keyStore
                    .getEntry(C_SAUDE_SECRET_KEY_ALIAS, protectionParam);

            if (disaSecretKey == null) {
                throw new HL7KeyStoreException("disaSecretKey is missing from keyStore");
            }

            if (csaudeSecretKey == null) {
                throw new HL7KeyStoreException("csaudeSecretKey is missing from keyStore");
            }

            Map<String, byte[]> map = new HashMap<>();
            map.put(DISA_SECRET_KEY_ALIAS, disaSecretKey.getSecretKey().getEncoded());
            map.put(C_SAUDE_SECRET_KEY_ALIAS, csaudeSecretKey.getSecretKey().getEncoded());
            return map;

        } catch (GeneralSecurityException e) {
            throw new HL7KeyStoreException(e);
        } catch (NoSuchFileException e) {
            log.info("Could not find keystore {}", path, e);
            throw e;
        } catch (AccessDeniedException e) {
            log.info("Could read keystore {}", path, e);
            throw e;
        } catch (IOException e) {
            if (e.getCause() instanceof UnrecoverableKeyException) {
                log.info("Wrong password for keystore {}", path, e);
            }
            throw e;
        }
    }
}
