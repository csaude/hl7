package mz.org.fgh.hl7.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import mz.org.fgh.hl7.web.model.Location;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import mz.org.fgh.hl7.web.Hl7FileForm;
import mz.org.fgh.hl7.web.model.HL7File;
import mz.org.fgh.hl7.web.model.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
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

    }

    @EventListener(ApplicationReadyEvent.class)
    public void init(){
        loadConfig(); // Load on startup
        rescheduleTask();
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
        defaultConfig.setLastStatus(null);
        defaultConfig.setHealthFacilities("");
        defaultConfig.setProvince(null);
        defaultConfig.setDistrict(null);
        defaultConfig.setHealthFacilitiesList(Collections.emptyList());
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
    public String getLastStatus() { return config.getLastStatus(); }
    public void setLastStatus(String status) {
        config.setLastStatus(status);
        // We should save to disk so it survives restarts
        saveConfig();
    }
    public String getHealthFacilities(){ return  config.getHealthFacilities();};

    @Override
    public Location getProvince() {
        return config.getProvince();
    }
    @Override
    public Location getDistrict() {
        return config.getDistrict();
    }
    @Override
    public List<Location> getHealthFacilitiesList() {
        return config.getHealthFacilitiesList();
    }

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

        LOG.info("Calculating delay");
        // Calculate the delay until the next task execution
        long delay = calculateDelay();

        LOG.info("Next scheduled execution: " + new Date(System.currentTimeMillis() + delay));

        // If the calculated delay is negative (task overdue), execute the task immediately
        try {
            if (delay <= 0) {
                LOG.info("Executing task now");
                scheduledTask(null);
                delay = calculateDelay(); // Recalculate for the next run
            }
        } catch (Exception e) {
            // Log the error but DO NOT re-throw. Let the app start.
            LOG.error("Failed to execute overdue scheduled task on startup: {}", e.getMessage());
        }

        // Schedule the new task at the calculated time in the future
        scheduledFuture = taskScheduler.schedule(() -> {
            try {
                LOG.info("Scheduling next task");
                scheduledTask(null);
            } catch (Exception e) {
                LOG.error("❌ Automatic scheduled task failed: {}", e.getMessage());
            }
        }, new Date(System.currentTimeMillis() + delay));
    }

    private long calculateDelay() {
        // Calculate the delay in milliseconds based on the frequency and the time
        LocalTime now = LocalTime.now();
        long delay = 5000;

        // If lastExecutionTime is available, calculate delay from that moment
        if (config.getLastRunTime() != null) {
            LocalDateTime lastExecutionTime = config.getLastRunTime();
            LocalDateTime nextExecutionTime = lastExecutionTime.plusDays(getFrequency())
                    .withHour(config.getGenerationTime().getHour())
                    .withMinute(config.getGenerationTime().getMinute());

            if (nextExecutionTime.isAfter(LocalDateTime.now())) {
                delay = Duration.between(LocalDateTime.now(), nextExecutionTime).toMillis();
            }
        } else {
            if (now.isBefore(getGenerationTime())) {
                delay = Duration.between(now, getGenerationTime()).toMillis();
            } else {
                delay = Duration.between(now, getGenerationTime().plusHours(24)).toMillis();
            }
            // Add the frequency days to the next run time
            delay += Duration.ofDays(getFrequency()).toMillis();
        }

        LOG.info("We have the delay: {}", delay);

        return delay;
    }

    public String scheduledTask(Hl7FileForm hl7FileForm) throws Exception {
        LOG.info("Starting scheduled task");
        boolean isManualRun = hl7FileForm != null;

        try {

            ResponseEntity<Map<String, Object>> statusResponse = fileService.checkJobStatus();
            Map<String, Object> statusMap = statusResponse.getBody();

            if (statusMap != null && "PROCESSING".equals(statusMap.get("status"))) {
                return "PROCESSING";
            }

            // Get current values from config
            int frequency = getFrequency();
            LocalTime generationTime = getGenerationTime();

            LOG.info("Scheduled task currently running at frequency: " + frequency + " minutes");
            System.out.println(hl7FileForm);

            if (hl7FileForm == null) {
                hl7FileForm = new Hl7FileForm();
            } else {
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
            System.out.println(hl7File);

            if (!isManualRun && hl7File == null) {
                LOG.warn("Automatic scheduler run skipped: No HL7 configuration has been saved yet.");
                return "SKIPPED_NO_CONFIG";
            }

            // Assign values with fallback to `hl7File`
            if (hl7FileForm.getProvince() == null) {
                hl7FileForm.setProvince(hl7File.getProvince());
            }
            if (hl7FileForm.getDistrict() == null) {
                hl7FileForm.setDistrict(hl7File.getDistrict());
            }
            if (hl7FileForm.getHealthFacilities() == null) {
                hl7FileForm.setHealthFacilities(hl7File.getHealthFacilities());
            }

            List<String> locationsByUuid = hl7FileForm.getHealthFacilities().stream().map(Location::getUuid)
                    .collect(Collectors.toList());

            LOG.info("Here are the locations:" + locationsByUuid);
            LOG.info("Here is the province UUID:" + hl7FileForm.getProvince().getUuid());
            LOG.info("Sending request with body: " + hl7FileForm);

            // This is the call to the middleware
            String response = webClient.post()
                    .uri(hl7GenerateAPI)
                    .bodyValue(hl7FileForm)
                    .retrieve()
                    .onStatus(HttpStatus::isError, clientResponse -> // Handle 4xx/5xx errors
                            clientResponse.bodyToMono(String.class).flatMap(body -> {
                                LOG.error("❌ API returned error: " + body);
                                return Mono.error(new RuntimeException("API error: " + body)); // Re-throw
                            })
                    )
                    .bodyToMono(String.class)
                    .doOnSubscribe(sub -> LOG.info("📡 Sending request to " + hl7GenerateAPI))
                    .block(Duration.ofSeconds(20)); // Wait a max of 20 seconds

            // If we get here, the call was successful (2xx)
            if (response == null || response.isEmpty()) {
                LOG.error("⚠️ Response was null or empty!");
                throw new RuntimeException("Middleware returned an empty response.");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);
            String jobId = rootNode.get("JobId").asText();
            LOG.info("Extracted JobId: " + jobId);

            // Save config
            config.setJobId(jobId);
            config.setLastStatus("PROCESSING");
            config.setLastRunTime(LocalDateTime.now());
            if (isManualRun) {
                config.setProvince(hl7FileForm.getProvince());
                config.setDistrict(hl7FileForm.getDistrict());
                config.setHealthFacilitiesList(hl7FileForm.getHealthFacilities());
                if (hl7FileForm.getHealthFacilities() != null) {
                    String facilities = Location.joinLocations(hl7FileForm.getHealthFacilities());
                    config.setHealthFacilities(facilities);
                }
            }
            saveConfig();
            // If this was a MANUAL run, reschedule immediately
            if (isManualRun) {
                rescheduleTask();
            }

            return jobId;

        } catch (Exception e) {

            LOG.error("❌ Failed to generate HL7 file: " + e.getMessage());
            throw e;
        } finally {
            if (!isManualRun) {

                LOG.info("Updating lastRunTime on automatic run failure...");
                config.setLastRunTime(LocalDateTime.now());
                saveConfig();
                LOG.info("Rescheduling next automatic run...");
                rescheduleTask();
            }
        }
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
