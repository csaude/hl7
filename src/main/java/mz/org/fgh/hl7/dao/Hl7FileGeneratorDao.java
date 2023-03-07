package mz.org.fgh.hl7.dao;

import java.util.List;

import mz.org.fgh.hl7.model.PatientDemographic;

public interface Hl7FileGeneratorDao {

	public List<PatientDemographic> getPatientDemographicData(List<String> locationsByUuid); 
}
