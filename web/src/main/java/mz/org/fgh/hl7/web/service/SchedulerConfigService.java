package mz.org.fgh.hl7.web.service;

import mz.org.fgh.hl7.web.Hl7FileForm;
import mz.org.fgh.hl7.web.model.Scheduler;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.ScheduledFuture;

public interface SchedulerConfigService {
    public void loadConfig();

    public int getFrequency();

    public LocalTime getGenerationTime();
    public LocalDateTime getLastRunTime();
    public String getJobId();
    public void reloadConfig();

    public void updateConfig(int frequency, LocalTime generationTime);

    public String scheduledTask(Hl7FileForm hl7FileForm);

    public ScheduledFuture<?> getProcessingResult();


}
