package mz.org.fgh.hl7.web.env;

import static mz.org.fgh.hl7.lib.Constants.APPLICATION_PROPERTIES_ENC;
import static mz.org.fgh.hl7.lib.Constants.APP_CONFIG_LOCATION;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
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

        Path path = APP_CONFIG_LOCATION.resolve(APPLICATION_PROPERTIES_ENC);
        Resource resource = new FileSystemResource(path);
        PropertySource<?> propertySource = loadEncryptedProperties(loader, resource);

        environment.getPropertySources().addFirst(propertySource);
    }

    private PropertySource<?> loadEncryptedProperties(EncryptedPropertySourceLoader loader, Resource resource) {
        Assert.isTrue(resource.exists(), () -> "Resource " + resource + " does not exist");
        try {
            return loader.load("custom-resource", resource).get(0);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load configuration from " + resource, ex);
        }
    }
}
