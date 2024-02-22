package mz.org.fgh.hl7.web.model;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class Location implements Serializable {

    private static final long serialVersionUID = 1L;

    public static String joinLocations(List<Location> locations) {
        return locations.stream()
                .map(Location::getName)
                .collect(Collectors.joining(" • "));
    }

    private String uuid;

    @EqualsAndHashCode.Exclude
    private String name;

    @EqualsAndHashCode.Exclude
    private Location parentLocation;

    @EqualsAndHashCode.Exclude
    private List<Location> childLocations;
}
