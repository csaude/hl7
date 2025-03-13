package mz.org.fgh.hl7.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import mz.org.fgh.hl7.web.controller.FileController;
import mz.org.fgh.hl7.web.model.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import mz.org.fgh.hl7.web.Hl7FileForm;
import mz.org.fgh.hl7.web.model.HL7File;
import mz.org.fgh.hl7.web.model.ProcessingResult;
import mz.org.fgh.hl7.web.model.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import sun.rmi.runtime.Log;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SchedulerConfigServiceImpl implements SchedulerConfigService {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Logger LOG = LoggerFactory.getLogger(SchedulerConfigServiceImpl.class);
    private Scheduler config;
    private final TaskScheduler taskScheduler;
    private ScheduledFuture<?> scheduledFuture;
    private Hl7Service hl7Service;
    private WebClient webClient;
    private String hl7GenerateAPI;
    private Hl7FileService fileService;

    public SchedulerConfigServiceImpl(@Lazy Hl7FileService fileService, TaskScheduler taskScheduler, Hl7Service hl7Service, WebClient webClient, @Value("${hl7.generate.api}") String hl7GenerateAPI) {
        this.taskScheduler = taskScheduler;
        this.hl7Service = hl7Service;
        this.webClient = webClient;
        this.hl7GenerateAPI = hl7GenerateAPI;
        this.fileService = fileService;
        loadConfig(); // Load on startup
    }

    public void loadConfig() {
        File configFile = new File("config.json");

        if (!configFile.exists()) {
            createDefaultConfig(configFile); // Create default config if file doesn't exist
        }

        try {
            config = objectMapper.readValue(configFile, Scheduler.class);
        } catch (IOException e) {
            LOG.error("Error reading config file", e);
            config = getDefaultConfig(); // Ensure config is never null
        }
        rescheduleTask();
    }

    private void createDefaultConfig(File configFile) {
        try {
            Scheduler defaultConfig = getDefaultConfig();
            objectMapper.writeValue(configFile, defaultConfig);
            LOG.info("Default config.json created successfully!");
        } catch (IOException e) {
            LOG.error("Failed to create default config.json", e);
        }
    }

    private Scheduler getDefaultConfig() {
        Scheduler defaultConfig = new Scheduler();
        defaultConfig.setFrequency(10);
        defaultConfig.setGenerationTime(LocalTime.parse("13:00:00"));
        defaultConfig.setLastRunTime(hl7Service.getFileLastModifiedTime() != null ? hl7Service.getFileLastModifiedTime() : null);
        defaultConfig.setJobId(null);
        return defaultConfig;
    }



    public int getFrequency() {
        return config.getFrequency();
    }

    public LocalTime getGenerationTime() {
        return config.getGenerationTime();
    }

    public LocalDateTime getLastRunTime() {
        return config.getLastRunTime();
    }
    public String getJobId() { return config.getJobId(); }

    // Reload config if the file changes
    public void reloadConfig() {
        loadConfig();
        System.out.println("Config reloaded!");
    }

    public void updateConfig(int frequency, LocalTime generationTime) {
        config.setFrequency(frequency);
        config.setGenerationTime(generationTime);

        try {
            objectMapper.writeValue(new File("config.json"), config);
            LOG.info("Config updated!");
            rescheduleTask(); // Reschedule after updating config
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void rescheduleTask() {
        // Cancel any existing scheduled task
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(false);
        }

        // Calculate the delay until the next task execution
        long delay = calculateDelay();

        LOG.info("Next scheduled execution: " + new Date(System.currentTimeMillis() + delay));

        // If the calculated delay is negative (task overdue), execute the task immediately
        if (delay <= 0) {
            try {
                scheduledTask(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            delay = calculateDelay(); // Recalculate delay after immediate execution
        }

        // Schedule the new task at the calculated time in the future
        scheduledFuture = taskScheduler.schedule(() -> {
            try {
                scheduledTask(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, new Date(System.currentTimeMillis() + delay));
    }

    private long calculateDelay() {
        // Calculate the delay in milliseconds based on the frequency and the time
        LocalTime now = LocalTime.now();
        long delay = 0;

        // If lastExecutionTime is available, calculate delay from that moment
        if (config.getLastRunTime() != null) {
            LocalDateTime lastExecutionTime = config.getLastRunTime();
            LocalDateTime nextExecutionTime = lastExecutionTime.plusMinutes(getFrequency());
//                    .withHour(config.getGenerationTime().getHour())
//                    .withMinute(config.getGenerationTime().getMinute());

            if (nextExecutionTime.isAfter(LocalDateTime.now())) {
                delay = Duration.between(LocalDateTime.now(), nextExecutionTime).toMillis();
            }
        } else {
            // Fallback to normally scheduling if no last execution time is found
            if (now.isBefore(getGenerationTime())) {
                delay = Duration.between(now, getGenerationTime()).toMillis();
            } else {
                delay = Duration.between(now, getGenerationTime().plusHours(24)).toMillis();
            }

            delay += Duration.ofDays(getFrequency()).toMillis();
        }

        LOG.info("We have the following delay: "+delay);

        return delay;
    }

    public String scheduledTask(Hl7FileForm hl7FileForm) throws Exception {
        // Call getJobStatus method
        ResponseEntity<Map<String, Object>> statusResponse = fileService.checkJobStatus();
        Map<String, Object> statusMap = statusResponse.getBody();

        // Check if there's an ongoing job
        if (statusMap != null && "PROCESSING".equals(statusMap.get("status"))) {
            return "PROCESSING";
        }

        // Get current values from config
        int frequency = getFrequency();
        LocalTime generationTime = getGenerationTime();

        LOG.info("Scheduled task currently running at frequency: " + frequency + " minutes");
        System.out.println(hl7FileForm);

        if(hl7FileForm != null){
        LOG.info("From HL7 Form");
        // Get new values from form
        int newFrequency = hl7FileForm.getFrequency();
        LocalTime newGenerationTime = LocalTime.parse(hl7FileForm.getGenerationTime());
        // Check if values changed
        if (newFrequency != frequency || !newGenerationTime.equals(generationTime))
            updateConfig(newFrequency, newGenerationTime);
        }

        LOG.info("Updated scheduled task running. Frequency: " + getFrequency() + " days, Time: " + getGenerationTime());

        // Generate HL7 File
        HL7File hl7File = hl7Service.getHl7File();

        if (hl7FileForm == null) {
            hl7FileForm = new Hl7FileForm();
        }

        // Assign values with fallback to `hl7File`
        hl7FileForm.setProvince(hl7FileForm.getProvince() != null ? hl7FileForm.getProvince() : hl7File.getProvince());
        hl7FileForm.setDistrict(hl7FileForm.getDistrict() != null ? hl7FileForm.getDistrict() : hl7File.getDistrict());
        hl7FileForm.setHealthFacilities(hl7FileForm.getHealthFacilities() != null ? hl7FileForm.getHealthFacilities() : hl7File.getHealthFacilities());


        // Create a CountDownLatch to wait for the response
        CountDownLatch latch = new CountDownLatch(1);
        final String[] jobIdResult = new String[1]; // Array to hold the result

        List<String> locationsByUuid = hl7FileForm.getHealthFacilities().stream().map(Location::getUuid)
                .collect(Collectors.toList());

        LOG.info("Here are the locations:"+locationsByUuid);
        LOG.info("Here is the province UUID:"+ hl7FileForm.getProvince().getUuid());
        LOG.info("Sending request with body: " + hl7FileForm);

        webClient.post()
                .uri(hl7GenerateAPI)
                .bodyValue(hl7FileForm)
                .retrieve()
                .onStatus(HttpStatus::isError, response ->
                        response.bodyToMono(String.class).flatMap(body -> {
                            LOG.error("❌ API returned error: " + body);
                            return Mono.error(new RuntimeException("API error: " + body)); // Throw exception with details
                        })
                )
                .bodyToMono(String.class)  // Mono<String>
                .doOnSubscribe(sub -> LOG.info("📡 Sending request to " + hl7GenerateAPI))
                .doOnSuccess(response -> {
                    if (response == null || response.isEmpty()) {
                        LOG.error("⚠️ Response was null or empty!");
                        return;
                    }
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode rootNode = mapper.readTree(response);
                        jobIdResult[0] = rootNode.get("JobId").asText();
                        LOG.info("Extracted JobId: " + jobIdResult[0]);
                        config.setJobId(jobIdResult[0]);
                        saveConfig();  // Save the updated config to JSON
                    } catch (Exception e) {
                        LOG.error("Failed to parse response: " + e.getMessage());
                    }
                })
                .doOnError(error -> {
                    LOG.error("❌ Error: " + error.getMessage());
                    latch.countDown();
                })
                .subscribe();

        LOG.info("HL7 file is being generated");
        // Save last execution time
        config.setLastRunTime(LocalDateTime.now());

        // Reschedule the task after the specified frequency
        rescheduleTask();

        try {
            // Wait for the response with a timeout
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            if (!completed) {
                LOG.info("Retrieved JobId");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Interrupted while waiting for JobId", e);
        }

        return jobIdResult[0];
    }

    private void saveConfig() {
        try {
            objectMapper.writeValue(new File("config.json"), config);
            LOG.info("Config updated with last execution time: " + config.getLastRunTime());
        } catch (IOException e) {
            LOG.error("Failed to save config", e);
        }
    }

    @Override
    public ScheduledFuture <?> getProcessingResult() {
        return scheduledFuture;
    }

}
