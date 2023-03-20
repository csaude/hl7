package mz.org.fgh.hl7.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ContextRefreshedEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(ContextRefreshedEventListener.class);

    @Value("${app.hl7.folder}")
    private String hl7FolderName;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        createHL7Folder();
    }

    public void createHL7Folder() {
        Path path = Paths.get(hl7FolderName);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                LOG.info("Created folder {}", hl7FolderName);
            } catch (IOException e) {
                LOG.error(String.format("Could not create folder %s", hl7FolderName), e);
            }
        }
    }
}
