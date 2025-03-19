package mz.org.fgh.hl7.web.env;

import java.io.IOException;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import mz.org.fgh.hl7.lib.service.HL7EncryptionServiceImpl;
import mz.org.fgh.hl7.lib.service.HL7KeyStoreService;

public class EncryptedEnvironmentLoader
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {

        ConfigurableEnvironment environment = event.getEnvironment();

        String keyStorePath = environment.getProperty("app.keyStore");
        if (keyStorePath == null) {
            throw new IllegalArgumentException("app.keyStore is missing.");
        }

        String keyStorePassword = environment.getProperty("app.keyStore.password");
        if (keyStorePassword == null) {
            throw new IllegalArgumentException("app.keyStore.password is missing.");
        }

        HL7KeyStoreService hl7KeyStoreService = new HL7KeyStoreService(keyStorePath, keyStorePassword);
        HL7EncryptionServiceImpl encryptionService = new HL7EncryptionServiceImpl();
        EncryptedPropertySourceLoader loader = new EncryptedPropertySourceLoader(hl7KeyStoreService, encryptionService);


        Resource path = new ClassPathResource("application.properties.enc");


        PropertySource<?> propertySource = loadEncryptedProperties(loader, path);

        environment.getPropertySources().addFirst(propertySource);
    }

    private PropertySource<?> loadEncryptedProperties(EncryptedPropertySourceLoader loader, Resource path) {
        Assert.isTrue(path.exists(), () -> "Resource " + path + " does not exist");
        try {
            return loader.load("custom-resource", path).get(0);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load configuration from " + path, ex);
        }
    }
}
