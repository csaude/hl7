package mz.org.fgh.hl7;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import mz.org.fgh.hl7.service.LocationService;

@Component
public class StringToLocationConverter implements Converter<String, Location> {

    private LocationService locationService;

    public StringToLocationConverter(LocationService locationService) {
        this.locationService = locationService;
    }

    public Location convert(String id) {
        return locationService.findById(Long.valueOf(id));
    }
}
