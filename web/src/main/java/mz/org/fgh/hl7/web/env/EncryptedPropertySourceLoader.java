package mz.org.fgh.hl7.web.env;

import static mz.org.fgh.hl7.lib.Constants.C_SAUDE_SECRET_KEY_ALIAS;
import static mz.org.fgh.hl7.lib.Constants.DISA_SECRET_KEY_ALIAS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

import mz.org.fgh.hl7.lib.service.HL7EncryptionService;
import mz.org.fgh.hl7.lib.service.HL7KeyStoreService;

public class EncryptedPropertySourceLoader implements PropertySourceLoader {

    private HL7KeyStoreService hl7KeyStoreService;

    private HL7EncryptionService encryptionService;

    public EncryptedPropertySourceLoader(HL7KeyStoreService keyStoreService, HL7EncryptionService encryptionService) {
        this.hl7KeyStoreService = keyStoreService;
        this.encryptionService = encryptionService;
    }

    @Override
    public String[] getFileExtensions() {
        return new String[] { "enc" };
    }

    @Override
    public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
        Map<String, byte[]> entries = hl7KeyStoreService.getEntries();
        String csaudeSecretKey = new String(entries.get(C_SAUDE_SECRET_KEY_ALIAS));
        InputStream decryptedProperties = encryptionService.decrypt(resource.getFile().toPath(), csaudeSecretKey);
        try (BufferedReader input = new BufferedReader(new InputStreamReader(decryptedProperties))) {
            Properties props = new Properties();
            props.load(input);
            props.setProperty("app.disa.secretKey", new String(entries.get(DISA_SECRET_KEY_ALIAS)));
            return Collections.singletonList(new PropertiesPropertySource("decrypted-props", props));
        }
    }
}
