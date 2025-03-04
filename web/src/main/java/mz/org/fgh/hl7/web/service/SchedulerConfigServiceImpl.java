package mz.org.fgh.hl7.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import mz.org.fgh.hl7.web.Hl7FileForm;
import mz.org.fgh.hl7.web.model.HL7File;
import mz.org.fgh.hl7.web.model.ProcessingResult;
import mz.org.fgh.hl7.web.model.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    public String scheduledTask(Hl7FileForm hl7FileForm) {
        // Most of your existing scheduledTask code stays the same
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
        if (newFrequency != frequency || !newGenerationTime.equals(generationTime)) {
            updateConfig(newFrequency, newGenerationTime);

        }

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

        LOG.info("Sending request with body: " + hl7FileForm);

        webClient.post()
                .uri(TARGET_API_URL)
                .bodyValue(hl7FileForm)
                .retrieve()
                .onStatus(HttpStatus::isError, response ->
                        response.bodyToMono(String.class).doOnNext(body ->
                                LOG.info("Received error response: " + body)
                        ).then(Mono.empty()) // Don't throw an error, just log it
                )
                .bodyToMono(String.class)  // Mono<String>
                .doOnSubscribe(sub -> LOG.info("📡 Sending request to " + TARGET_API_URL))
                .doOnSuccess(response -> {
                    LOG.info("✅ Response: " + response);
                    try {
                        // Parse the response to get JobId
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode rootNode = mapper.readTree(response);
                        jobIdResult[0] = rootNode.get("JobId").asText();
                        LOG.info("Extracted JobId: " + jobIdResult[0]);
                    } catch (Exception e) {
                        LOG.error("Failed to parse response: " + e.getMessage());
                    }
                    latch.countDown();
                })
                .doOnError(error -> {
                    LOG.error("❌ Error: " + error.getMessage());
                    latch.countDown();
                })
                .subscribe();



        LOG.info("HL7 file is being generated");

        // Save last execution time
        config.setLastRunTime(LocalDateTime.now());
        saveConfig();  // Save the updated config to JSON

        // Reschedule the task after the specified frequency
        rescheduleTask();

        try {
            // Wait for the response with a timeout
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                LOG.warn("Request timed out waiting for JobId");
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
