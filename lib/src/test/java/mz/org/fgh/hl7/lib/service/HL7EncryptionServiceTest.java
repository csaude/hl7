package mz.org.fgh.hl7.lib.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HL7EncryptionServiceTest {

	private HL7EncryptionService hl7EncryptionService;

	private String hl7FolderName;

	private String hl7FileName;

	private Path hl7FilePath;

	private String passPhrase;

	public HL7EncryptionServiceTest() {
		this.hl7EncryptionService = new HL7EncryptionServiceImpl();
		this.hl7FolderName = "/tmp/";
		this.hl7FileName = "Patient_Demographic_Data";
		byte[] bytes = new byte[10];
		new Random().nextBytes(bytes);
		this.passPhrase = new String(bytes);
	}

	@BeforeEach
	public void beforeEach() {
		hl7FilePath = Paths.get(hl7FolderName, hl7FileName + ".hl7.enc");
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

		assertThat(Files.exists(hl7FilePath)).isTrue();
	}

	@Test
	public void testDecrypt() throws Exception {
		InputStream decryptedInputStream = hl7EncryptionService.decrypt(hl7FilePath, passPhrase);
		assertThat(decryptedInputStream != null).isTrue();
	}
}
