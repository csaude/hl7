package mz.org.fgh.hl7.web;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import mz.org.fgh.hl7.web.env.EncryptedEnvironmentLoader;

@SpringBootApplication
public class Hl7Application extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(Hl7Application.class)
				.listeners(new EncryptedEnvironmentLoader());
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(Hl7Application.class)
				.listeners(new EncryptedEnvironmentLoader())
				.run(args);
	}
}
