package mz.org.fgh.hl7.web;

import java.time.LocalTime;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;
import mz.org.fgh.hl7.web.model.Location;

@Data
public class Hl7FileForm {

    private Location province;

    private Location district;

    @NotNull(message = "{hl7.validation.fileform.healthFacilities.Size.message}")
    @Size(min = 1, message = "{hl7.validation.fileform.healthFacilities.Size.message}")
    private List<Location> healthFacilities;

    private int frequency;
    private String generationTime;
}
