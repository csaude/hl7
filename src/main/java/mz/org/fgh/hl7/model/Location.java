package mz.org.fgh.hl7.model;

import java.util.List;

import lombok.Data;

@Data
public class Location {
    private long id;
    private String uuid;
    private String name;
    private Location parentLocation;
    private List<Location> childLocations;
}
