package mz.org.fgh.hl7.web.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import mz.org.fgh.hl7.web.service.Hl7ApiService;
import mz.org.fgh.hl7.web.service.SchedulerConfigService;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.extras.java8time.util.TemporalFormattingUtils;

import mz.org.fgh.hl7.web.model.HL7File;
import mz.org.fgh.hl7.web.model.Location;
import mz.org.fgh.hl7.web.model.ProcessingResult;
import mz.org.fgh.hl7.web.service.Hl7Service;

@RestController
@RequestMapping("/api")
public class ApiController {

    public enum ProcessingStatus {
        PROCESSING, FAILED, DONE;
    }
    private final Hl7ApiService hl7ApiService;

    public ApiController(Hl7ApiService hl7ApiService) {
        this.hl7ApiService = hl7ApiService;
    }

    @GetMapping
    public Map<String, Object> getModifiedTime(Locale locale) throws InterruptedException, ExecutionException {
        return hl7ApiService.getProcessingStatus(locale);
    }
}
