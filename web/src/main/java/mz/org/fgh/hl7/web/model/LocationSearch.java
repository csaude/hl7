package mz.org.fgh.hl7.web.model;

import java.util.List;

import lombok.Data;

@Data
public class LocationSearch {
    private List<Location> results;
}
