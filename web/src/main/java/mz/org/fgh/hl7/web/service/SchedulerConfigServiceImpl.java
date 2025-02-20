package mz.org.fgh.hl7.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import mz.org.fgh.hl7.web.Hl7FileForm;
import mz.org.fgh.hl7.web.controller.ConfigController;
import mz.org.fgh.hl7.web.model.HL7File;
import mz.org.fgh.hl7.web.model.HL7FileRequest;
import mz.org.fgh.hl7.web.model.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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

    public SchedulerConfigServiceImpl(TaskScheduler taskScheduler, Hl7Service hl7Service) {
        this.taskScheduler = taskScheduler;
        this.hl7Service = hl7Service;
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
        LOG.info(String.valueOf("Next scheduled execution:" + new Date(System.currentTimeMillis() + delay)));
//        long delay = 120 * 1000;

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
        int frequency = getFrequency();
        LocalTime generationTime = getGenerationTime();
        LOG.info("Scheduled task running. Frequency: " + frequency + " days, Time: " + generationTime);

        // Generate HL7 File
        HL7File hl7File = hl7Service.getHl7File();
        HL7FileRequest req = new HL7FileRequest();
        req.setProvince(hl7FileForm != null ? hl7FileForm.getProvince() : hl7File.getProvince());
        req.setDistrict(hl7FileForm != null ? hl7FileForm.getDistrict() : hl7File.getDistrict());
        req.setHealthFacilities(hl7FileForm!= null ? hl7FileForm.getHealthFacilities() : hl7File.getHealthFacilities());

        hl7Service.generateHl7File(req);

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
