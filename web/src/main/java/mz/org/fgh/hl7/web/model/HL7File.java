package mz.org.fgh.hl7.web.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class HL7File extends HL7FileRequest {

    private LocalDateTime lastModifiedTime;
}
