package mz.org.fgh.hl7.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import mz.org.fgh.hl7.AppException;
import mz.org.fgh.hl7.HL7File;
import mz.org.fgh.hl7.HL7File.ProcessingStatus;

@Service
public class HL7FileServiceImpl implements HL7FileService {

    private static final String PROCESSING_PREFIX = ".";

    private static String HL7_EXTENSION = ".hl7";

    @Value("${app.hl7.folder}")
    private String hl7FolderName;

    public List<HL7File> findAll() {
        try {
            Path path = Paths.get(hl7FolderName);
            try (Stream<Path> paths = Files.list(path)) {
                return paths
                        .filter(p -> p.toString().endsWith(HL7_EXTENSION))
                        .map(this::createHL7File)
                        .sorted((a , b) -> b.getLastModifiedTime().compareTo(a.getLastModifiedTime()))
                        .toList();
            }
        } catch (IOException e) {
            throw new AppException("While listing hl7 file directory", e);
        }
    }

    public byte[] read(String filename) {

        Path path = Paths.get(hl7FolderName).resolve(filename);

        // Should not read file being processed
        if(Files.exists(path.resolveSibling(PROCESSING_PREFIX + filename))) {
            throw new AppException("hl7.read.error.processing");
        }

        // Should not read non hl7 files
        HL7File hl7 = createHL7File(path);
        int dotIndex = hl7.getFileName().lastIndexOf(".");
        if (dotIndex > 0 && !hl7.getFileName().substring(dotIndex).equals(HL7_EXTENSION)) {
            throw new AppException("hl7.read.error.notHL7");
        }

        // Should not read files outside defined hl7 folder
        checkIfInHL7Folder(path);

        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new AppException("hl7.read.error", e);
        }
    }

    @Override
    @Async
    public void create(String filename) {

        Path processing = Paths.get(hl7FolderName)
                .resolve(PROCESSING_PREFIX + filename + HL7_EXTENSION);

        Path processed = processing.resolveSibling(filename + HL7_EXTENSION);

        // Should not create existing file
        if (Files.exists(processing) || Files.exists(processed)) {
            throw new AppException("hl7.create.error.exists");
        }

        // Should not create files outside defined hl7 folder
        checkIfInHL7Folder(processing);

        try {

            Files.createFile(processing);

            Thread.sleep(5000);

            Files.move(processing, processed);

        } catch (IOException | InterruptedException e) {
            throw new AppException("hl7.create.error", e);
        }
    }

    public void delete(String filename) {

        Path path = Paths.get(hl7FolderName).resolve(filename);

        // Should not delete processing file
        if(Files.exists(path.resolveSibling(PROCESSING_PREFIX + filename))) {
            throw new AppException("hl7.delete.error.processing");
        }

        // Should not delete file outside defined hl7 folder
        checkIfInHL7Folder(path);

        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new AppException("hl7.delete.error", e);
        }
    }

    private HL7File createHL7File(Path path) {
        try {

            HL7File hl7 = new HL7File();

            // If processing remove '.' from start of filename
            if (path.getFileName().toString().startsWith(PROCESSING_PREFIX)) {
                hl7.setProcessingStatus(ProcessingStatus.PROCESSING);
                hl7.setFileName(path.getFileName().toString().substring(1));
            } else {
                hl7.setProcessingStatus(ProcessingStatus.DONE);
                hl7.setFileName(path.getFileName().toString());
            }

            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            LocalDateTime lastModified = attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            hl7.setLastModifiedTime(lastModified);
            return hl7;
        } catch (IOException e) {
            throw new AppException("hl7.create.error", e);
        }
    }

    private void checkIfInHL7Folder(Path path) {
        if(!path.normalize().startsWith(Paths.get(hl7FolderName))) {
            throw new AppException("hl7.error.folder");
        }
    }
}
