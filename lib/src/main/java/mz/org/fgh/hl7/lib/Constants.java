package mz.org.fgh.hl7.lib;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface Constants {
    public static final Path APP_CONFIG_LOCATION = Paths
            .get("C:\\Program Files\\Apache Software Foundation\\Tomcat 8.5\\conf\\hl7");
    public static final String APPLICATION_PROPERTIES_ENC = "application.properties.enc";
    public static final String KEY_STORE_TYPE = "JCEKS";
    public static final String DISA_SECRET_KEY_ALIAS = "disaSecretKeyAlias";
    public static final String C_SAUDE_SECRET_KEY_ALIAS = "csaudeSecretKeyAlias";
}
