package mz.org.fgh.hl7.model;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class Location {
    private String uuid;

    @EqualsAndHashCode.Exclude
    private String name;

    @EqualsAndHashCode.Exclude
    private Location parentLocation;

    @EqualsAndHashCode.Exclude
    private List<Location> childLocations;
}
