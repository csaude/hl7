package mz.org.fgh.hl7.web.env;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

import mz.org.fgh.hl7.lib.Base64EncoderDecoder;

public class EncryptedPropertySourceLoader implements PropertySourceLoader {

    @Override
    public String[] getFileExtensions() {
        return new String[] { "enc" };
    }

    @Override
    public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
        InputStreamReader input = Base64EncoderDecoder.decodeBase64ToInputStreamReader(resource.getFile().toPath());
        Properties props = new Properties();
        props.load(input);
        return Collections.singletonList(new PropertiesPropertySource("decrypted-props", props));
    }
}
