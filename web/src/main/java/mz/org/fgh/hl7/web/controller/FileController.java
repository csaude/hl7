package mz.org.fgh.hl7.web.controller;

import mz.org.fgh.hl7.web.Alert;
import mz.org.fgh.hl7.web.model.HL7File;
import mz.org.fgh.hl7.web.service.Hl7FileService;
import mz.org.fgh.hl7.web.service.SchedulerConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/file")
public class FileController {
    private static final Logger log = LoggerFactory.getLogger(FileController.class.getName());
    private final Hl7FileService hl7FileService;
    private final SchedulerConfigService config;

    public FileController(Hl7FileService hl7FileService, SchedulerConfigService config) {
        this.hl7FileService = hl7FileService;
        this.config = config;
    }

    @GetMapping
    public String showFiles(Model model) {
        try {
            HL7File hl7File = hl7FileService.getHl7File();

            if (hl7File == null || hl7File.getDistrict() == null || hl7File.getDistrict().getUuid() == null) {
                model.addAttribute("error", "Location information is missing.");
                return "file";
            }

            String locationUUID = hl7File.getDistrict().getUuid();
            log.info("Location UUID: {}", locationUUID);

            List<Map<String, String>> files = hl7FileService.getGeneratedFiles(locationUUID);
            model.addAttribute("files", files);

        } catch (Exception e) {
            log.error("Error fetching HL7 files: ", e);
            model.addAttribute("error", "Failed to fetch HL7 files. Please try again later.");
        }

        return "file";
    }

    @GetMapping("/download-and-save")
    public String downloadAndSave(@RequestParam("url") String fileUrl, RedirectAttributes redirectAttrs) {
        try {
            hl7FileService.downloadAndSaveFile(fileUrl);
            redirectAttrs.addFlashAttribute(Alert.success("hl7.download.success"));
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute(Alert.danger(e.getMessage()));
        }
        return "redirect:/file";
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getJobStatus() {
        try {
            String jobId = config.getJobId();

            if (jobId == null || jobId.isEmpty()) {
                return ResponseEntity.ok(Collections.singletonMap("status", "NO_JOB"));
            }

            ResponseEntity<Map<String, Object>> status = hl7FileService.checkJobStatus();
            return ResponseEntity.ok(status.getBody());

        } catch (Exception e) {
            log.error("Error checking job status: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to fetch job status"));
        }
    }
}

