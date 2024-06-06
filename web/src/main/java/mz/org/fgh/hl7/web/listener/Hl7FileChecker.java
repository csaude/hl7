package mz.org.fgh.hl7.web.listener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class Hl7FileChecker {
	
	private final Path filePath;
	
	public Hl7FileChecker(@Value("${app.hl7.folder}") String hl7Folder, 
						  @Value("${app.hl7.filename}") String hl7FileName) {
		if(hl7Folder == null || hl7FileName == null) {
			throw new IllegalArgumentException("HL7 folder and filename must not be null");
		}
		this.filePath = Paths.get(hl7Folder, hl7FileName + ".hl7");
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationEvent() {
		try {
			if (Files.deleteIfExists(filePath)) {
				log.info("File {} deleted successfully.", filePath);
			}
		} catch (IOException e) {
			log.error("An error occurred while deleting the file {}.hl7", filePath, e); 
		}
	}
}
