package mz.org.fgh.hl7.dao.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import ca.uhn.hl7v2.HL7Exception;
import mz.org.fgh.hl7.dao.Hl7FileGeneratorDao;
import mz.org.fgh.hl7.dao.dto.Hl7Form;
import mz.org.fgh.hl7.generator.Hl7FileGenerator;
import mz.org.fgh.hl7.model.PatientDemographic;

@Controller
@RequestMapping("showForm")
public class Hl7Controller {
	
	@Autowired 
	private Hl7FileGeneratorDao hl7FileGeneratorDao;
	
	@GetMapping
	public String getForm(Hl7Form hl7Form) {
		return "form";
	}
	
	@PostMapping("generateFile") 
	public String newForm(@Valid Hl7Form hl7Form, BindingResult bindingResult) throws HL7Exception, IOException {
		if (bindingResult.hasErrors()) { 
			return "form"; 
		}
		
		List<PatientDemographic> patientDemographics = hl7FileGeneratorDao.
				getPatientDemographicData(new ArrayList<>(Arrays.asList(hl7Form.getLocationUuid())));
		
		Hl7FileGenerator.createHl7File(patientDemographics);
		
		return "form";
	}
}
