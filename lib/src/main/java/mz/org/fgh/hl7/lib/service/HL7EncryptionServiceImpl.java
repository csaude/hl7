package mz.org.fgh.hl7.lib.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HL7EncryptionServiceImpl implements HL7EncryptionService {

    private static final Logger logger = Logger.getLogger(HL7EncryptionServiceImpl.class.getName());

    public void encrypt(ByteArrayOutputStream outputStream, String passPhrase, Path donePath) {

        // Convert the ByteArrayOutputStream to a ByteArrayInputStream
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {

            String[] command = {
                    "openssl",
                    "enc",
                    "-aes-256-cbc",
                    "-md", "sha256",
                    "-salt",
                    "-out", donePath.toString(),
                    "-k", passPhrase
            };

            // Create ProcessBuilder instance with the command and its arguments
            ProcessBuilder processBuilder = new ProcessBuilder(command);

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

        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "A cifra do arquivo hl7 falhou. Por favor, "
                    + "verifique se o aplicativo OpenSSL está instalado corretamente." + e.getMessage());
            logger.log(Level.SEVERE, "Stack trace:", e);
        }
    }

    public InputStream decrypt(Path encryptedFilePath, String passPhrase) {
        try {
            String[] command = {
                    "openssl",
                    "enc",
                    "-aes-256-cbc",
                    "-d",
                    "-in", encryptedFilePath.toString(),
                    "-k", passPhrase
            };

            // Create ProcessBuilder instance with the command
            ProcessBuilder processBuilder = new ProcessBuilder(command);

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
            logger.log(Level.SEVERE, "A decifra do arquivo hl7 falhou" + e.getMessage());
            logger.log(Level.SEVERE, "Stack trace:", e);
            return null;
        }
    }
}
