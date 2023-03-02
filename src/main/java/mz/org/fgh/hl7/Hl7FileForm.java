package mz.org.fgh.hl7;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class Hl7FileForm {
    @NotBlank(message = "{hl7.validation.fileform.email.NotBlank.message}")
    @Email(message = "{hl7.validation.fileform.email.Email.message}")
    private String email;

    @NotBlank(message = "{hl7.validation.fileform.filename.NotBlank.message}")
    @Pattern(regexp= "^[a-zA-Z0-9 _]*$", message="{hl7.validation.fileform.filename.Pattern.message}")
    private String filename;

    private Location province;
    private Location district;

    @NotNull(message = "{hl7.validation.fileform.healthFacilities.Size.message}")
    @Size(min = 1, message = "{hl7.validation.fileform.healthFacilities.Size.message}")
    private List<Location> healthFacilities;
}
