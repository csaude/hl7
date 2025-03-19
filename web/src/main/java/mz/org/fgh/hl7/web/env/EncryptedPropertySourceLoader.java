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

            // Get the Partner_Id from environment variable
            String partnerId = System.getenv("Partner_Id");
            String hl7Url = System.getenv("HL7_URL");
            if (partnerId == null) {
                partnerId = ""; // Or set a default value, or throw an exception
            }

            // Set the properties with the Partner_Id inserted
            String baseUrl = hl7Url + partnerId;
            props.setProperty("hl7.generate.api", baseUrl + "/api/demographics/generate");
            props.setProperty("hl7.generatedHl7Files.api", baseUrl + "/api/demographics/getGeneratedHL7Files/");
            props.setProperty("hl7.downloadFile.api", baseUrl + "/api/demographics/download/");
            props.setProperty("hl7.fileStatus.api", baseUrl + "/api/demographics/status/");


            props.setProperty("app.disa.secretKey", new String(entries.get(DISA_SECRET_KEY_ALIAS)));

            return Collections.singletonList(new PropertiesPropertySource("decrypted-props", props));
        }
    }
}
