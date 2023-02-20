package mz.org.fgh.hl7.service;

import mz.org.fgh.hl7.Location;

public interface LocationService {
    Location findByUuid(String uuid);

    Location findById(long id);
}
