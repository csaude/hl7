package mz.org.fgh.hl7.model;

import java.time.LocalDateTime;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class HL7File {

    public enum ProcessingStatus {
        PROCESSING, FAILED, DONE;
    }

    @Getter
    @Setter
    @EqualsAndHashCode.Exclude
    private ProcessingStatus processingStatus;

    @Getter
    @Setter
    private String fileName;

    @Getter
    @Setter
    private LocalDateTime lastModifiedTime;

    public boolean isProcessing() {
        return processingStatus == ProcessingStatus.PROCESSING;
    }

    public boolean isFailed() {
        return processingStatus == ProcessingStatus.FAILED;
    }

    public boolean isDone() {
        return processingStatus == ProcessingStatus.DONE;
    }
}
