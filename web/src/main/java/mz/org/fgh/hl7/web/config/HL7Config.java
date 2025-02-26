package mz.org.fgh.hl7.web.config;

import java.util.Locale;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import mz.org.fgh.hl7.lib.service.HL7EncryptionService;
import mz.org.fgh.hl7.lib.service.HL7EncryptionServiceImpl;

@Configuration
@EnableAsync
@EnableCaching
@EnableScheduling
public class HL7Config {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource());
        return bean;
    }

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        slr.setDefaultLocale(new Locale("pt"));
        return slr;
    }
    @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler(); // Or any other TaskScheduler implementation
    }

    @Bean
    public HL7EncryptionService encryptionService() {
        return new HL7EncryptionServiceImpl();
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }
}
