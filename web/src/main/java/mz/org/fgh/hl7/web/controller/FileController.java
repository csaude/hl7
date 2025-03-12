package mz.org.fgh.hl7.web.controller;

import ca.uhn.hl7v2.model.v251.segment.LOC;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.org.fgh.hl7.web.Alert;
import mz.org.fgh.hl7.web.model.HL7File;
import mz.org.fgh.hl7.web.service.Hl7Service;
import mz.org.fgh.hl7.web.service.Hl7ServiceImpl;
import mz.org.fgh.hl7.web.service.SchedulerConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


@Controller
@RequestMapping("/file")
public class FileController {
    private Hl7Service hl7Service;
    private WebClient webClient;
    private String hl7GeneratedFilesAPI;
    private String hl7DownloadFileAPI;
    private String hl7FileStatusAPI;
    private String hl7DefaultDownloadFolder;
    private String hl7StandardName;
    private String hl7MetadataName;
    private String hl7HiddenFileName;

    private static final Logger log = LoggerFactory.getLogger(FileController.class.getName());

    public FileController(Hl7Service hl7Service, WebClient webClient, @Value("${hl7.fileStatus.api}") String hl7FileStatusAPI, @Value("${hl7.hidden.file.name}") String hl7HiddenFileName, @Value("${hl7.default.download.folder}") String hl7DefaultDownloadFolder,  @Value("${hl7.generatedHl7Files.api}") String hl7GeneratedFilesAPI, @Value("${hl7.downloadFile.api}") String hl7DownloadFileAPI, @Value("${hl7.standard.file.name}") String hl7StandardName, @Value("${hl7.metadata.name}") String hl7MetadataName) {
        this.hl7Service = hl7Service;
        this.webClient = webClient;
        this.hl7GeneratedFilesAPI = hl7GeneratedFilesAPI;
        this.hl7DownloadFileAPI = hl7DownloadFileAPI;
        this.hl7FileStatusAPI = hl7FileStatusAPI;
        this.hl7DefaultDownloadFolder = hl7DefaultDownloadFolder;
        this.hl7StandardName = hl7StandardName;
        this.hl7MetadataName = hl7MetadataName;
        this.hl7HiddenFileName = hl7HiddenFileName;
    }

    @GetMapping
    public String showFiles(Model model) {
        HL7File hl7File = hl7Service.getHl7File();
        System.out.println(hl7File);

        if (hl7File == null || hl7File.getDistrict() == null || hl7File.getDistrict().getUuid() == null) {
            System.out.println(hl7File.getDistrict());

            model.addAttribute("error", "Location information is missing.");
            return "file"; // Return the view with an error message
        }

        String locationUUID = hl7File.getDistrict().getUuid();
        log.info("Location UUID: " + locationUUID);

        String apiUrl = hl7GeneratedFilesAPI + locationUUID;
        log.info("API URL: " + apiUrl);

        List<Map<String, String>> files = new ArrayList<>();

        try {
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
        } catch (Exception e) {
            log.error("Error fetching HL7 files: ", e);
            model.addAttribute("error", "Failed to fetch HL7 files. Please try again later.");
        }

        Collections.reverse(files);
        model.addAttribute("files", files);
        return "file";
    }


    @GetMapping("/download-and-save")
    public String downloadAndSave(@RequestParam("url") String fileUrl, RedirectAttributes redirectAttrs) {
        return processFileDownload(fileUrl, redirectAttrs);
    }

    private String processFileDownload(String fileUrl, RedirectAttributes redirectAttrs) {
        try {
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

            // Download the ZIP file
            URL url = new URL(fileUrl);
            try (InputStream in = url.openStream()) {
                Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Extract files from the ZIP
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

            // Clean up the temporary ZIP file
            Files.delete(zipPath);

            // Add a flash attribute with a success message
            redirectAttrs.addFlashAttribute(Alert.success("hl7.download.success"));
            // Redirect to the /file page
            return "redirect:/file";
        } catch (IOException e) {
            // Add a flash attribute with an error message
            redirectAttrs.addFlashAttribute(Alert.danger(e.getMessage()));

            // Redirect to the /file page with the error message
            return "redirect:/file";
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getJobStatus() {
        // Check if this job was already processed
//        Boolean alreadyProcessed = (Boolean) session.getAttribute("job_" + jobId + "_processed");
        String jobId = config.getJobId();
        if (jobId == null || jobId.isEmpty()) {
            return ResponseEntity.ok(Collections.singletonMap("status", "NO_JOB"));
        }

        // Make API call to App Y to check job status
        String jobStatusUrl = hl7FileStatusAPI + jobId;

        try {
            String resp = webClient.get()
                    .uri(jobStatusUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // Blocking here because it's a REST call from the backend

            Map<String, Object> response = new HashMap<>();

            // Parse JSON response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(resp);

            // Extract the status attribute
            String status = rootNode.get("status").asText();

            //Download the file and save once finished
            if(Objects.equals(status, "COMPLETED")){
                // Add already processed flag
//                response.put("alreadyProcessed", alreadyProcessed == Boolean.TRUE);

                // Only process if not already done
//                if (alreadyProcessed != Boolean.TRUE) {
                    String downloadURL = hl7DownloadFileAPI + jobId;
                    RedirectAttributesModelMap redirectAttrs = new RedirectAttributesModelMap();
                    processFileDownload(downloadURL, redirectAttrs);
                    // Mark as processed
//                    session.setAttribute("job_" + jobId + "_processed", Boolean.TRUE);
                    log.info("File successfully saved!");
//                }


            }

            response.put("status", status);
            response.put("jobId", jobId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to fetch job status"));
        }
    }
}

