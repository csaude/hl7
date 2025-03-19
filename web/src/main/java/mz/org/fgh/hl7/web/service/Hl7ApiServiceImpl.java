package mz.org.fgh.hl7.web.service;

import mz.org.fgh.hl7.web.controller.ApiController;
import mz.org.fgh.hl7.web.model.HL7File;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import mz.org.fgh.hl7.web.model.Location;
import mz.org.fgh.hl7.web.model.ProcessingResult;
import org.thymeleaf.extras.java8time.util.TemporalFormattingUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@Service
public class Hl7ApiServiceImpl implements Hl7ApiService {

    private final Hl7Service hl7Service;
    private final MessageSource messageSource;

    public Hl7ApiServiceImpl(Hl7Service hl7Service, MessageSource messageSource) {
        this.hl7Service = hl7Service;
        this.messageSource = messageSource;
    }

    @Override
    public Map<String, Object> getProcessingStatus(Locale locale) throws InterruptedException, ExecutionException {
        Map<String, Object> hl7 = new HashMap<>();
        CompletableFuture<ProcessingResult> processingResult = hl7Service.getProcessingResult();
        HL7File file = hl7Service.getHl7File();
        String healthFacilities = file != null ? Location.joinLocations(file.getHealthFacilities()) : "";

        TemporalFormattingUtils fmt = new TemporalFormattingUtils(locale, ZoneId.systemDefault());

        if (processingResult.isDone() && !processingResult.isCompletedExceptionally()) {
            LocalDateTime modifiedAt = file.getLastModifiedTime();
            Object[] args = new Object[]{fmt.format(modifiedAt)};
            hl7.put("processingStatus", ApiController.ProcessingStatus.DONE);
            hl7.put("message", messageSource.getMessage("hl7.file.updated.at", args, locale));
            hl7.put("healthFacilities", healthFacilities);
            hl7.put("logs", processingResult.get().getErrorLogs());
        } else if (processingResult.isCompletedExceptionally()) {
            hl7.put("processingStatus", ApiController.ProcessingStatus.FAILED);
            hl7.put("message", getFailedMessage(file, locale));
            hl7.put("healthFacilities", healthFacilities);
        } else {
            hl7.put("processingStatus", ApiController.ProcessingStatus.PROCESSING);
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
                    new Object[]{hl7File.getLastModifiedTime()}, locale);
        }
    }
}
