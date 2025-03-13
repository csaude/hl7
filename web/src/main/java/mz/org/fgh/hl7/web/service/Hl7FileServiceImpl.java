package mz.org.fgh.hl7.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.org.fgh.hl7.web.model.HL7File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class Hl7FileServiceImpl implements Hl7FileService{
    private static final Logger log = LoggerFactory.getLogger(Hl7FileServiceImpl.class.getName());

    private final Hl7Service hl7Service;
    private final SchedulerConfigService config;
    private final WebClient webClient;
    private final String hl7GeneratedFilesAPI;
    private final String hl7DownloadFileAPI;
    private final String hl7FileStatusAPI;
    private final String hl7DefaultDownloadFolder;
    private final String hl7StandardName;
    private final String hl7MetadataName;
    private final String hl7HiddenFileName;


    public Hl7FileServiceImpl(
            Hl7Service hl7Service,
            WebClient webClient,
            SchedulerConfigService config,
            @Value("${hl7.fileStatus.api}") String hl7FileStatusAPI,
            @Value("${hl7.hidden.file.name}") String hl7HiddenFileName,
            @Value("${hl7.default.download.folder}") String hl7DefaultDownloadFolder,
            @Value("${hl7.generatedHl7Files.api}") String hl7GeneratedFilesAPI,
            @Value("${hl7.downloadFile.api}") String hl7DownloadFileAPI,
            @Value("${hl7.standard.file.name}") String hl7StandardName,
            @Value("${hl7.metadata.name}") String hl7MetadataName) {

        this.hl7Service = hl7Service;
        this.config = config;
        this.webClient = webClient;
        this.hl7GeneratedFilesAPI = hl7GeneratedFilesAPI;
        this.hl7DownloadFileAPI = hl7DownloadFileAPI;
        this.hl7FileStatusAPI = hl7FileStatusAPI;
        this.hl7DefaultDownloadFolder = hl7DefaultDownloadFolder;
        this.hl7StandardName = hl7StandardName;
        this.hl7MetadataName = hl7MetadataName;
        this.hl7HiddenFileName = hl7HiddenFileName;
    }

    @Override
    public HL7File getHl7File() {
        return hl7Service.getHl7File();
    }

    @Override
    public List<Map<String, String>> getGeneratedFiles(String locationUUID) {
        String apiUrl = hl7GeneratedFilesAPI + locationUUID;
        log.info("API URL: {}", apiUrl);

        List<Map<String, String>> files = new ArrayList<>();

        // Fetch data from the API
        List<Map<String, Object>> jobs = webClient.get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .block();

        // Process the response
        if (jobs != null && !jobs.isEmpty()) {
            for (Map<String, Object> job : jobs) {
                Map<String, String> fileMap = new HashMap<>();

                // Extract filename from downloadURL
                String downloadURL = (String) job.get("downloadURL");
                String fileName = downloadURL.substring(downloadURL.lastIndexOf("/") + 1);

                // Format the date from createdAt
                String createdAt = (String) job.get("createdAt");
                LocalDateTime dateTime = LocalDateTime.parse(createdAt);

                // Format it to "dd/MM/yyyy HH:mm"
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String formattedDate = dateTime.format(formatter);

                // Build the file map
                fileMap.put("name", fileName);
                fileMap.put("id", job.get("jobId").toString());
                fileMap.put("date", formattedDate);
                fileMap.put("link", hl7DownloadFileAPI + job.get("jobId").toString());
                fileMap.put("hospitals", job.get("healthFacilities").toString());

                files.add(fileMap);
            }
        }

        Collections.reverse(files);
        return files;
    }

    @Override
    public void downloadAndSaveFile(String fileUrl) throws IOException {
        // Create the save directory if it does not exist
        Path saveDirectory = Paths.get(hl7DefaultDownloadFolder);
        if (!Files.exists(saveDirectory)) {
            Files.createDirectories(saveDirectory);
        }

        // Define the save paths for both files
        Path hl7SavePath = saveDirectory.resolve(hl7StandardName);
        Path metadataPath = saveDirectory.resolve(hl7MetadataName);

        // Temporary ZIP file path
        Path zipPath = Files.createTempFile("download-", ".zip");

        try {
            // Download the ZIP file
            URL url = new URL(fileUrl);
            try (InputStream in = url.openStream()) {
                Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Extract files from the ZIP
            extractFilesFromZip(zipPath, saveDirectory, hl7SavePath, metadataPath);

        } finally {
            // Clean up the temporary ZIP file
            Files.deleteIfExists(zipPath);
        }
    }

    private void extractFilesFromZip(Path zipPath, Path saveDirectory, Path hl7SavePath, Path metadataPath) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                String entryName = entry.getName();

                // Determine which file we're extracting and save to appropriate path
                if (entryName.equals("Patient_Demographic_Data.hl7.enc")) {
                    Files.copy(zipIn, hl7SavePath, StandardCopyOption.REPLACE_EXISTING);

                    // Create a clone of the HL7 file with the hidden filename
                    Path hiddenFilePath = saveDirectory.resolve(hl7HiddenFileName);
                    Files.copy(hl7SavePath, hiddenFilePath, StandardCopyOption.REPLACE_EXISTING);

                    // Make the hidden file hidden for Windows
                    if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        Files.setAttribute(hiddenFilePath, "dos:hidden", true);
                    }
                } else if (entryName.equals(".metadata.json")) {
                    Files.copy(zipIn, metadataPath, StandardCopyOption.REPLACE_EXISTING);
                }

                zipIn.closeEntry();
            }
        }
    }

//    @Override
//    public String getConfigJobId() {
//        // This is a placeholder. In the original code, it was using a comment referencing "config.getJobId()"
//        // You'd likely need to implement this with the SchedulerConfigService
//        return schedulerConfigService.getJobId();
//    }

    @Override
    public ResponseEntity<Map<String, Object>> checkJobStatus() throws Exception {

        String jobStatusUrl = hl7FileStatusAPI + config.getJobId();
        Map<String, Object> response = new HashMap<>();

        // Make API call to check job status
        String resp = webClient.get()
                .uri(jobStatusUrl)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // Parse JSON response
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(resp);
        // Extract the status attribute
        String status = rootNode.get("status").asText();

        // Download the file and save once finished
        if (Objects.equals(status, "COMPLETED")) {
            String downloadURL = hl7DownloadFileAPI + config.getJobId();
            downloadAndSaveFile(downloadURL);
            log.info("File successfully saved!");
        }

        response.put("status", status);
        response.put("jobId", config.getJobId());
        return ResponseEntity.ok(response);
    }
}
