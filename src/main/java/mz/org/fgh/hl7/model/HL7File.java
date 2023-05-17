package mz.org.fgh.hl7.model;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class HL7File {

    private Location province;

    private Location district;

    private List<Location> healthFacilities;

    private LocalDateTime lastModifiedTime;
}
