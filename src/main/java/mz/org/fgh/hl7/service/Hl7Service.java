package mz.org.fgh.hl7.service;

import java.util.List;

import ca.uhn.hl7v2.HL7Exception;
import mz.org.fgh.hl7.model.HL7File;
import mz.org.fgh.hl7.model.Location;

public interface Hl7Service {
	public void validateCreate(String filename);
	public void create(String fileName, List<Location> locations) throws HL7Exception;
    public byte[] read(String filename);
    public void delete(String filename);
    public List<HL7File> findAll();
}
