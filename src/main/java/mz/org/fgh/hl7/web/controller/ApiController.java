package mz.org.fgh.hl7.web.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.extras.java8time.util.TemporalFormattingUtils;

import mz.org.fgh.hl7.model.HL7File;
import mz.org.fgh.hl7.model.Location;
import mz.org.fgh.hl7.service.Hl7Service;

@RestController
@RequestMapping("/api")
public class ApiController {

    public enum ProcessingStatus {
        PROCESSING, FAILED, DONE;
    }

    private Hl7Service hl7Service;

    private MessageSource messageSource;

    public ApiController(Hl7Service hl7Service, MessageSource messageSource) {
        this.hl7Service = hl7Service;
        this.messageSource = messageSource;
    }

    @GetMapping
    public Map<String, Object> getModifiedTime(Locale locale) throws InterruptedException, ExecutionException {

        Map<String, Object> hl7 = new HashMap<>();
        CompletableFuture<HL7File> hl7File = hl7Service.getHl7FileFuture();
        HL7File file = hl7Service.getHl7File();

        String healthFacilities = file != null ? Location.joinLocations(file.getHealthFacilities()) : "";

        // Format the same as in thymeleaf
        TemporalFormattingUtils fmt = new TemporalFormattingUtils(locale, ZoneId.systemDefault());

        if (hl7File.isDone() && !hl7File.isCompletedExceptionally()) {
            LocalDateTime modifiedAt = file.getLastModifiedTime();
            Object[] args = new Object[] { fmt.format(modifiedAt) };
            hl7.put("processingStatus", ProcessingStatus.DONE);
            hl7.put("message", messageSource.getMessage("hl7.file.updated.at", args, locale));
            hl7.put("healthFacilities", healthFacilities);
        } else if (hl7File.isCompletedExceptionally()) {
            hl7.put("processingStatus", ProcessingStatus.FAILED);
            hl7.put("message", getFailedMessage(file, locale));
            hl7.put("healthFacilities", healthFacilities);
        } else {
            hl7.put("processingStatus", ProcessingStatus.PROCESSING);
            hl7.put("message", messageSource.getMessage("hl7.files.processing", null, locale));
            hl7.put("healthFacilities", healthFacilities);
        }

        return hl7;
    }

    private String getFailedMessage(HL7File hl7File, Locale locale) {
        if (hl7File == null) {
            return messageSource.getMessage("hl7.files.processing.error", null, locale);
        } else {
            return messageSource.getMessage("hl7.files.processing.error.previous.at",
                    new Object[] { hl7File.getLastModifiedTime() }, locale);
        }
    }
}
