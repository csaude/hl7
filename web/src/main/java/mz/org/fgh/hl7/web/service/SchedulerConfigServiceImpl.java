package mz.org.fgh.hl7.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import mz.org.fgh.hl7.web.Hl7FileForm;
import mz.org.fgh.hl7.web.model.HL7File;
import mz.org.fgh.hl7.web.model.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

@Service
public class SchedulerConfigServiceImpl implements SchedulerConfigService {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Logger LOG = LoggerFactory.getLogger(SchedulerConfigServiceImpl.class);
    private Scheduler config;
    private final TaskScheduler taskScheduler;
    private ScheduledFuture<?> scheduledFuture;

    private Hl7Service hl7Service;

    private WebClient webClient;

    private static final String TARGET_API_URL = "http://localhost:8081/api/demographics/generate";

    public SchedulerConfigServiceImpl(TaskScheduler taskScheduler, Hl7Service hl7Service, WebClient webClient) {
        this.taskScheduler = taskScheduler;
        this.hl7Service = hl7Service;
        this.webClient = webClient;
        loadConfig(); // Load on startup
    }

    public void loadConfig() {
        try {
            config = objectMapper.readValue(new File("config.json"), Scheduler.class);
        } catch (IOException e) {
            e.printStackTrace();
            config = new Scheduler(); // Ensure config is never null
        }
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
            System.out.println("Config updated!");
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
//        long delay = 1600;
        LOG.info(String.valueOf("Next scheduled execution:" + new Date(System.currentTimeMillis() + delay)));

        // Schedule the new task
        scheduledFuture = taskScheduler.schedule(  () -> scheduledTask(null), new Date(System.currentTimeMillis() + delay));
    }

    private long calculateDelay() {
        // Calculate the delay in milliseconds based on the frequency and the time
        LocalTime now = LocalTime.now();
        long delay = 0;

        // If lastExecutionTime is available, calculate delay from that moment
        if (config.getLastRunTime() != null) {
            LocalDateTime lastExecutionTime = config.getLastRunTime();
            LocalDateTime nextExecutionTime = lastExecutionTime.plusDays(config.getFrequency())
                    .withHour(config.getGenerationTime().getHour())
                    .withMinute(config.getGenerationTime().getMinute());

            if (nextExecutionTime.isAfter(LocalDateTime.now())) {
                delay = Duration.between(LocalDateTime.now(), nextExecutionTime).toMillis();
            }
        } else {
            // Fallback to normally scheduling if no last execution time is found
            if (now.isBefore(config.getGenerationTime())) {
                delay = Duration.between(now, config.getGenerationTime()).toMillis();
            } else {
                delay = Duration.between(now, config.getGenerationTime().plusHours(24)).toMillis();
            }

            delay += Duration.ofDays(config.getFrequency()).toMillis();
        }

        return delay;
    }

    public void scheduledTask(Hl7FileForm hl7FileForm) {
        // Get current values from config
        int frequency = getFrequency();
        LocalTime generationTime = getGenerationTime();

        // Get new values from form
        int newFrequency = hl7FileForm.getFrequency();
        LocalTime newGenerationTime = LocalTime.parse(hl7FileForm.getGenerationTime());

        // Check if values changed
        if (newFrequency != frequency || !newGenerationTime.equals(generationTime)) {
            updateConfig(newFrequency, newGenerationTime);
        }

        LOG.info("Scheduled task running. Frequency: " + frequency + " days, Time: " + generationTime);

        // Generate HL7 File
        HL7File hl7File = hl7Service.getHl7File();

        if (hl7FileForm == null) {
            hl7FileForm = new Hl7FileForm();
        }

        // Assign values with fallback to `hl7File`
        hl7FileForm.setProvince(hl7FileForm.getProvince() != null ? hl7FileForm.getProvince() : hl7File.getProvince());
        hl7FileForm.setDistrict(hl7FileForm.getDistrict() != null ? hl7FileForm.getDistrict() : hl7File.getDistrict());
        hl7FileForm.setHealthFacilities(hl7FileForm.getHealthFacilities() != null ? hl7FileForm.getHealthFacilities() : hl7File.getHealthFacilities());

        webClient.post()
                .uri(TARGET_API_URL)
                .bodyValue(hl7FileForm)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSubscribe(sub -> LOG.info("📡 Sending request to " + TARGET_API_URL))
                .doOnSuccess(response -> LOG.info("✅ Response: " + response))
                .doOnError(error -> LOG.error("❌ Error: " + error.getMessage()))
                .subscribe();

        LOG.info("HL7 file is being generated");

        // Save last execution time
        config.setLastRunTime(LocalDateTime.now());
        saveConfig();  // Save the updated config to JSON

        // Reschedule the task after the specified frequency
        rescheduleTask();
    }

    private void saveConfig() {
        try {
            objectMapper.writeValue(new File("config.json"), config);
            LOG.info("Config updated with last execution time: " + config.getLastRunTime());
        } catch (IOException e) {
            LOG.error("Failed to save config", e);
        }
    }

}
