package mz.org.fgh.hl7.configurer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import mz.org.fgh.hl7.lib.service.HL7EncryptionService;
import mz.org.fgh.hl7.lib.service.HL7EncryptionServiceImpl;
import mz.org.fgh.hl7.lib.service.HL7KeyStoreService;

@SpringBootApplication
public class ConfigurerApp {

    @Bean
    public HL7EncryptionService encryptionService() {
        return new HL7EncryptionServiceImpl();
    }

    @Bean
    public HL7KeyStoreService hl7KeyStoreService(
            @Value("${app.keyStore}") String keyStorePath,
            @Value("${app.keyStore.password}") String keystorePassword) {
        return new HL7KeyStoreService(keyStorePath, keystorePassword);
    }

    public static void main(String[] args) {
        SpringApplication.run(ConfigurerApp.class, args);
    }
}
