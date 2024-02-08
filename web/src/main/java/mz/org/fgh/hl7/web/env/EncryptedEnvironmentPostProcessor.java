package mz.org.fgh.hl7.web.env;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Loads an encrypted application.properties file and add the decrypted values
 * to the spring environment.
 */
public class EncryptedEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private final EncryptedPropertySourceLoader loader = new EncryptedPropertySourceLoader();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Resource path = new ClassPathResource("application.properties.enc");
        PropertySource<?> propertySource = loadEncryptedProperties(path);
        environment.getPropertySources().addFirst(propertySource);
    }

    private PropertySource<?> loadEncryptedProperties(Resource path) {
        Assert.isTrue(path.exists(), () -> "Resource " + path + " does not exist");
        try {
            return this.loader.load("custom-resource", path).get(0);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load yaml configuration from " + path, ex);
        }
    }
}
