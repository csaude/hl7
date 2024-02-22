package mz.org.fgh.hl7.web.service;

import java.util.List;

import mz.org.fgh.hl7.web.model.Location;

public interface LocationService {

    Location findByUuid(String uuid);

    List<Location> findAllProvinces();
}
