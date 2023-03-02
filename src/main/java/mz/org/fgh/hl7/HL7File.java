package mz.org.fgh.hl7;

import java.time.LocalDateTime;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class HL7File {

    public enum ProcessingStatus {
        PROCESSING, DONE;
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
}
