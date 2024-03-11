package mz.org.fgh.hl7.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;

public interface HL7EncryptionService {

	/**
	 * @param the demographic patient information in memory to the encrypted
	 * @param the secret key used to encrypt the hl7 file
	 * @param the location to save the encrypted hl7 file
	 */
	public void encrypt(ByteArrayOutputStream outputStream, String passPhrase, Path donePath);
	
	/**
	 * @param the location of the encrypted hl7 file
	 * @param the secret key used to desIncrypt the hl7 file
	 * @return desIncrypt hl7 file
	 */
	public InputStream desincrypt(Path encryptedFilePath, String passPhrase);
}
