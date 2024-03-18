package mz.org.fgh.hl7.web.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class HL7FileRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Location province;

    private Location district;

    private List<Location> healthFacilities;
}
