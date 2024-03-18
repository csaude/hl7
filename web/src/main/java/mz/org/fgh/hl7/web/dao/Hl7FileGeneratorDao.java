package mz.org.fgh.hl7.web.dao;

import java.util.List;

import mz.org.fgh.hl7.web.model.PatientDemographic;

public interface Hl7FileGeneratorDao {

	public List<PatientDemographic> getPatientDemographicData(List<String> locationsByUuid); 
}
