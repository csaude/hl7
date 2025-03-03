package mz.org.fgh.hl7.web.service;

import mz.org.fgh.hl7.web.Hl7FileForm;
import mz.org.fgh.hl7.web.model.Scheduler;

import java.time.LocalDateTime;
import java.time.LocalTime;

public interface SchedulerConfigService {
    public void loadConfig();

    public int getFrequency();

    public LocalTime getGenerationTime();
    public LocalDateTime getLastRunTime();
    public void reloadConfig();

    public void updateConfig(int frequency, LocalTime generationTime);

    public void scheduledTask(Hl7FileForm hl7FileForm);


}
