package mz.org.fgh.hl7.lib.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface HL7EncryptionService {

	/**
	 * @param outputStream the file to encrypt
	 * @param passPhrase   key used to encrypt the file
	 * @param donePath     where to save the encrypted file
	 * @throws IOException
	 * @throws Exception
	 */
	public void encrypt(ByteArrayOutputStream outputStream, String passPhrase, Path donePath) throws IOException;

	/**
	 * @param encryptedFilePath path of the encrypted file
	 * @param passPhrase        key used to decrypt the file
	 * @return The decrypted file
	 */
	public InputStream decrypt(Path encryptedFilePath, String passPhrase);
}
