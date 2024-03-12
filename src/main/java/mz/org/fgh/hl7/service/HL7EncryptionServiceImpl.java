package mz.org.fgh.hl7.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Service;

@Service
public class HL7EncryptionServiceImpl implements HL7EncryptionService {

	public void encrypt(ByteArrayOutputStream outputStream, String passPhrase, Path donePath) { 
        try {
            // Convert the ByteArrayOutputStream to a ByteArrayInputStream
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

            // Construct the OpenSSL command
            String command = String.format("openssl enc -aes-256-cbc -md sha256 -salt -out %s -k %s", donePath, passPhrase); 

            // Create ProcessBuilder instance with the command
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command); // For Unix-like systems, change to "cmd", "/c", command for Windows

            // Start the process
            Process process = processBuilder.start();

            // Pass the input stream to the process
            try (OutputStream stdin = process.getOutputStream()) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    stdin.write(buffer, 0, length);
                }
            }
            
            // Wait for the process to finish
            int exitCode = process.waitFor();

            // Print the exit code
            System.out.println("Command executed with exit code: " + exitCode);
            
            // Create a copy of the encrypted file with a hidden filename
            Path destinationPath = donePath.resolveSibling(".Hidden." + donePath.getFileName().toString());
            Files.copy(donePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);


            
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
	
	public InputStream desincrypt(Path encryptedFilePath, String passPhrase) {
        try {
            // Construct the OpenSSL command
            String command = String.format("openssl enc -aes-256-cbc -d -in %s -k %s", encryptedFilePath, passPhrase);

            // Create ProcessBuilder instance with the command
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);

            // Start the process
            Process process = processBuilder.start();

            // Read the output of the command into a ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = process.getInputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            // Wait for the process to finish
            int exitCode = process.waitFor();

            // Print the exit code
            System.out.println("Command executed with exit code: " + exitCode);

            // Convert the output to an InputStream and return
            return new ByteArrayInputStream(outputStream.toByteArray());

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
