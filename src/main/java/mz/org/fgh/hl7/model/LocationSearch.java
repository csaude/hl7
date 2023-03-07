package mz.org.fgh.hl7.model;

import java.util.List;

import lombok.Data;

@Data
public class LocationSearch {
    private List<Location> results;
}
