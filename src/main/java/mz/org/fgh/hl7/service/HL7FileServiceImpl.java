package mz.org.fgh.hl7.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
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

    private final ConcurrentHashMap<String, ProcessingStatus> processingStatus = new ConcurrentHashMap<>();

    @Value("${app.hl7.folder}")
    private String hl7FolderName;

    public List<HL7File> findAll() {
        try {
            Path path = Paths.get(hl7FolderName);
            try (Stream<Path> paths = Files.list(path)) {
                return paths
                        .filter(p -> p.toString().endsWith(HL7_EXTENSION))
                        .map(this::buildHL7File)
                        .sorted((a, b) -> b.getLastModifiedTime().compareTo(a.getLastModifiedTime()))
                        .toList();
            }
        } catch (IOException e) {
            throw new AppException("While listing hl7 file directory", e);
        }
    }

    public byte[] read(String filename) {

        Path path = Paths.get(hl7FolderName).resolve(filename);

        // Should not read file being processed
        if (Files.exists(path.resolveSibling(PROCESSING_PREFIX + filename))) {
            throw new AppException("hl7.read.error.processing");
        }

        // Should not read non hl7 files
        HL7File hl7 = buildHL7File(path);
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

    public void validateCreate(String filename) {

        Path processing = Paths.get(hl7FolderName)
                .resolve(PROCESSING_PREFIX + filename + HL7_EXTENSION);

        Path processed = processing.resolveSibling(filename + HL7_EXTENSION);

        // Should not create existing file
        String key = processed.getFileName().toString();
        if (processingStatus.containsKey(key)) {
            throw new AppException("hl7.create.error.exists");
        }

        // Should not create files outside defined hl7 folder
        checkIfInHL7Folder(processing);
    }

    @Override
    @Async
    public void create(String filename) {

        Path processing = Paths.get(hl7FolderName)
                .resolve(PROCESSING_PREFIX + filename + HL7_EXTENSION);

        Path processed = processing.resolveSibling(filename + HL7_EXTENSION);

        // Should not create existing file
        String key = processed.getFileName().toString();
        if (processingStatus.containsKey(key)) {
            throw new AppException("hl7.create.error.exists");
        }

        // Should not create files outside defined hl7 folder
        checkIfInHL7Folder(processing);

        try {

            Files.createFile(processing);

            processingStatus.put(key, ProcessingStatus.PROCESSING);

            Thread.sleep(5000);

            Random random = new Random();
            if (random.nextBoolean()) {
                throw new IOException("Random error");
            }

            Files.move(processing, processed);

            processingStatus.put(key, ProcessingStatus.DONE);

        } catch (IOException | InterruptedException e) {

            processingStatus.put(key, ProcessingStatus.FAILED);

            throw new AppException("hl7.create.error", e);
        }
    }

    public void delete(String filename) {

        Path path = Paths.get(hl7FolderName).resolve(filename);

        // Should not delete processing file
        if (processingStatus.get(filename) == ProcessingStatus.PROCESSING) {
            throw new AppException("hl7.delete.error.processing");
        }

        // Should not delete file outside defined hl7 folder
        checkIfInHL7Folder(path);

        try {

            Path deletePath = path;
            // If it failed it should still have the PROCESSING_PREFIX, so
            // we need to make sure to delete the correct filename.
            if (processingStatus.get(filename) == ProcessingStatus.FAILED) {
                deletePath = path.resolveSibling(PROCESSING_PREFIX + path.getFileName().toString());
            }

            Files.delete(deletePath);

            processingStatus.remove(filename);

        } catch (IOException e) {
            throw new AppException("hl7.delete.error", e);
        }
    }

    private HL7File buildHL7File(Path path) {
        try {

            HL7File hl7 = new HL7File();

            String filename = path.getFileName().toString();

            // If processing remove '.' from start of filename
            if (filename.startsWith(PROCESSING_PREFIX)) {
                hl7.setFileName(filename.substring(1));
            } else {
                hl7.setFileName(filename);
            }

            // Get the processing status map key, which should be the
            // filename without PROCESSING_PREFIX.
            String key = filename;
            if (key.startsWith(PROCESSING_PREFIX)) {
                key = key.substring(1);
            }
            ProcessingStatus status = processingStatus.get(key);

            // If not in the processing status map, we need to add it to the map.
            if (status == null) {
                // If prefix is present it means that it either failed, or it is processing.
                // We'll consider the processing as failed because if it is not yet in the
                // processing status map, the app probably crashed.
                if (filename.startsWith(PROCESSING_PREFIX)) {
                    hl7.setProcessingStatus(ProcessingStatus.FAILED);
                    processingStatus.put(key, ProcessingStatus.FAILED);
                } else {
                    hl7.setProcessingStatus(ProcessingStatus.DONE);
                    processingStatus.put(key, ProcessingStatus.DONE);
                }
            } else {
                hl7.setProcessingStatus(status);
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
        if (!path.normalize().startsWith(Paths.get(hl7FolderName))) {
            throw new AppException("hl7.error.folder");
        }
    }
}
