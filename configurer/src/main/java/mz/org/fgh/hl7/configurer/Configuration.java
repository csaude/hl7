package mz.org.fgh.hl7.configurer;

import java.util.Properties;

import javax.validation.constraints.NotBlank;

public class Configuration {

    @NotBlank
    private String appUsername = "";

    @NotBlank
    private String appPassword = "";

    @NotBlank
    private String openmrsUrl = "";

    @NotBlank
    private String openmrsUsername = "";

    @NotBlank
    private String openmrsPassword = "";

    @NotBlank
    private String dataSourceUrl = "";

    @NotBlank
    private String dataSourceUsername = "";

    @NotBlank
    private String dataSourcePassword = "";

    @NotBlank
    private String keyStorePath = "";

    @NotBlank
    private String keyStorePassword = "";

    @NotBlank
    private String appHL7PassPhrase = "";

    public void load(Properties properties) {
        keyStorePath = properties.getProperty("app.keyStore");
        appUsername = properties.getProperty("app.username");
        appPassword = properties.getProperty("app.password");
        openmrsUrl = properties.getProperty("openmrs.url");
        openmrsUsername = properties.getProperty("openmrs.username");
        openmrsPassword = properties.getProperty("openmrs.password");
        dataSourceUrl = properties.getProperty("spring.datasource.url");
        dataSourceUsername = properties.getProperty("spring.datasource.username");
        dataSourcePassword = properties.getProperty("spring.datasource.password");
    }

    public String getAppUsername() {
        return appUsername;
    }

    public void setAppUsername(String appUsername) {
        this.appUsername = appUsername;
    }

    public String getAppPassword() {
        return appPassword;
    }

    public void setAppPassword(String appPassword) {
        this.appPassword = appPassword;
    }

    public String getOpenmrsUrl() {
        return openmrsUrl;
    }

    public void setOpenmrsUrl(String openmrsUrl) {
        this.openmrsUrl = openmrsUrl;
    }

    public String getOpenmrsPassword() {
        return openmrsPassword;
    }

    public void setOpenmrsPassword(String openmrsPassword) {
        this.openmrsPassword = openmrsPassword;
    }

    public String getDataSourceUrl() {
        return dataSourceUrl;
    }

    public void setDataSourceUrl(String dataSourceUrl) {
        this.dataSourceUrl = dataSourceUrl;
    }

    public String getDataSourceUsername() {
        return dataSourceUsername;
    }

    public void setDataSourceUsername(String dataSourceUsername) {
        this.dataSourceUsername = dataSourceUsername;
    }

    public String getDataSourcePassword() {
        return dataSourcePassword;
    }

    public void setDataSourcePassword(String dataSourcePassword) {
        this.dataSourcePassword = dataSourcePassword;
    }

    public String getOpenmrsUsername() {
        return openmrsUsername;
    }

    public void setOpenmrsUsername(String openmrsUsername) {
        this.openmrsUsername = openmrsUsername;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getAppHL7PassPhrase() {
        return appHL7PassPhrase;
    }

    public void setAppHL7PassPhrase(String disaSecretKey) {
        this.appHL7PassPhrase = disaSecretKey;
    }
}
