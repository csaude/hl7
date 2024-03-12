package mz.org.fgh.hl7.test.unit.service;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import mz.org.fgh.hl7.service.HL7EncryptionService;

@SpringBootTest
public class HL7EncryptionServiceTest {

	@Autowired
	private HL7EncryptionService hl7EncryptionService;

	@Value("${app.hl7.folder}")
	String hl7FolderName;

	@Value("${app.hl7.filename}")
	private String hl7FileName;

	private Path hl7FilePath;

	@Value("${hl7.passPhrase}")
	private String passPhrase;

	@BeforeEach
	public void beforeEach() {
		hl7FilePath = Paths.get(hl7FolderName, hl7FileName + ".hl7.enc");

		try {
			if (!Files.exists(hl7FilePath)) {
				Files.createFile(hl7FilePath);
			} else {
				System.out.println("File already exists: " + hl7FilePath);
			}
		} catch (IOException e) {
			System.err.println("Failed to create file: " + e.getMessage());
		}
	}

	@AfterEach
	public void afterEach() {
		// Delete all .hl7 files
		File hl7Folder = Paths.get(hl7FolderName).toFile();
		for (File f : hl7Folder.listFiles()) {
			if (f.getName().endsWith(".hl7.enc")) {
				f.delete();
			}
		}
	}

	@Test
	public void testEncrypt() {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		hl7EncryptionService.encrypt(outputStream, passPhrase, hl7FilePath);

		assertTrue(Files.exists(hl7FilePath));
	}

	@Test
	public void testDecrypt() throws Exception {
		InputStream decryptedInputStream = hl7EncryptionService.desincrypt(hl7FilePath, passPhrase);
		assertTrue(decryptedInputStream != null);
	}
}
