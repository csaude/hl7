package mz.org.fgh.hl7.configurer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigurerController {

    private static final Path[] TOMCAT_WEBAPPS_DIR = new Path[] {
            Paths.get("/", "usr", "local", "tomcat", "webapps"),
    };

    private Logger log = LoggerFactory.getLogger(getClass());

    @GetMapping("/folder")
    public String getWebAppFolder() {
        for (Path path : TOMCAT_WEBAPPS_DIR) {
            Path hl7WebApp = path.resolve("hl7");
            if (Files.exists(hl7WebApp) && Files.isDirectory(hl7WebApp)) {
                return hl7WebApp.toString();
            }
        }
        return "";
    }

    @GetMapping("/configuration")
    public ResponseEntity<Configuration> getConfiguration(@RequestParam String folder) throws IOException {
        Configuration configuration;
        try {
            configuration = loadConfiguration(folder);
            return ResponseEntity.ok().body(configuration);
        } catch (NoSuchFileException e) {
            log.info("Could not find configuration in {}", folder, e);
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            log.info("Could read configuration in {}", folder, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/configuration")
    public ResponseEntity<Object> install(@RequestParam String folder, @RequestBody @Valid Configuration config,
            BindingResult binding)
            throws IOException {

        if (binding.hasErrors()) {
            Map<String, String> errors = binding.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            return ResponseEntity.badRequest().body(errors);
        }

        storeConfiguration(folder, config);

        return ResponseEntity.ok(config);
    }

    /**
     * Tries to find the HL7 app folder and load application.properties values into
     * the Configuration.
     * 
     * @param folder the tomcat webapps HL7 app folder.
     * @return The Configuration
     * @throws IOException
     */
    private Configuration loadConfiguration(String folder) throws IOException {
        Configuration config = new Configuration();
        Path applicationProperties = Paths.get(folder)
                .resolve(Paths.get("WEB-INF", "classes", "application.properties"));
        try (BufferedReader reader = Files.newBufferedReader(applicationProperties)) {
            Properties props = new Properties();
            props.load(reader);
            config.load(props);
            return config;
        }
    }

    /**
     * Saves the configuration making sure that existing properties that are not
     * referenced are kept.
     * 
     * @param folder the tomcat webapps HL7 app folder.
     * @param config the configuration to save.
     * @throws IOException
     */
    private void storeConfiguration(String folder, Configuration config) throws IOException {
        Path webappFolder = Paths.get(folder);
        Path applicationProperties = webappFolder.resolve(Paths.get("WEB-INF", "classes", "application.properties"));
        Properties props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(applicationProperties);) {
            props.load(reader);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(applicationProperties)) {
            props.setProperty("app.username", config.getAppUsername());
            props.setProperty("app.password", config.getAppPassword());
            props.setProperty("openmrs.url", config.getOpenmrsUrl());
            props.setProperty("openmrs.username", config.getOpenmrsUsername());
            props.setProperty("openmrs.password", config.getOpenmrsPassword());
            props.setProperty("spring.datasource.url", config.getDataSourceUrl());
            props.setProperty("spring.datasource.username", config.getDataSourceUsername());
            props.setProperty("spring.datasource.password", config.getDataSourcePassword());
            props.store(writer, null);
        }
    }
}
