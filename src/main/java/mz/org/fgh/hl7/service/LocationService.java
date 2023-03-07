package mz.org.fgh.hl7.service;

import java.util.List;

import mz.org.fgh.hl7.model.Location;

public interface LocationService {

    Location findByUuid(String uuid);

    List<Location> findAllProvinces();
}
