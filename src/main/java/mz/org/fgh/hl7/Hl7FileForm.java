package mz.org.fgh.hl7;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class Hl7FileForm {
    @NotBlank(message = "{hl7.validation.fileform.email.NotBlank.message}")
    @Email(message = "{hl7.validation.fileform.email.Email.message}")
    private String email;
    private Location province;
    private Location district;

    @NotNull(message = "{hl7.validation.fileform.healthFacilities.Size.message}")
    @Size(min = 1, message = "{hl7.validation.fileform.healthFacilities.Size.message}")
    private List<Location> healthFacilities;
}
