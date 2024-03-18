package mz.org.fgh.hl7.lib;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Properties;

public class Base64EncoderDecoder {

    public static String encodePropertiesToBase64(Properties properties) throws IOException {
        StringWriter writer = new StringWriter();
        properties.store(writer, null);
        byte[] bytes = writer.toString().getBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static InputStreamReader decodeBase64ToInputStreamReader(Path encodedFilePath) throws IOException {
        byte[] encodedFileBytes = Files.readAllBytes(encodedFilePath);
        byte[] decodedBytes = Base64.getDecoder().decode(encodedFileBytes);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decodedBytes);
        return new InputStreamReader(byteArrayInputStream);
    }
}
