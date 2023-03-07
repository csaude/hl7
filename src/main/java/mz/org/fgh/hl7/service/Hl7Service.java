package mz.org.fgh.hl7.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import ca.uhn.hl7v2.HL7Exception;

public interface Hl7Service {
	
	public void createHl7File(List<String> locationsByUuid, String fileName, String hl7LocationFolder) throws FileNotFoundException, HL7Exception, IOException;
}
