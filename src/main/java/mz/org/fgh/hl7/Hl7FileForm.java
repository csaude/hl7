package mz.org.fgh.hl7;

import java.util.List;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class Hl7FileForm {
    private Location province;
    private Location district;
    @Size(min = 1)
    private List<Location> healthFacilities;
}
