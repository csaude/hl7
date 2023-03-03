package mz.org.fgh.hl7.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.message.ADT_A24;
import ca.uhn.hl7v2.parser.PipeParser;
import mz.org.fgh.hl7.dao.Hl7FileGeneratorDao;
import mz.org.fgh.hl7.generator.AdtMessageFactory;
import mz.org.fgh.hl7.util.Util;

@Service
public class Hl7ServiceImpl implements Hl7Service {
	
	static Logger log = Logger.getLogger(Hl7ServiceImpl.class.getName());
	
	private String headers;
	
    @Autowired
    private Hl7FileGeneratorDao hl7FileGeneratorDao;

	@Async
	public void createHl7File(List<String> locationsByUuid, String fileName, String hl7LocationFolder) throws FileNotFoundException, HL7Exception, IOException {

		log.info("createHl7File called...");

		String currentTimeStamp = Util.getCurrentTimeStamp();

		// prepare the headers
		headers = "FHS|^~\\&|XYZSYS|XYZ " + "DEFAULT_LOCATION_NAME" + "|DISA*LAB|SGP|" + currentTimeStamp
				+ "||Patient_Demographic_Data.hl7|" + "WEEKLY HL7 UPLOAD|00009972|\rBHS|^~\\&|XYZSYS|XYZ "
				+ "DEFAULT_LOCATION_NAME" + "|DISA*LAB|SGP|" + currentTimeStamp + "||||00010223\r";

		// create the HL7 message
		List<ADT_A24> adtMessages = AdtMessageFactory.createMessage("A24", hl7FileGeneratorDao.getPatientDemographicData(locationsByUuid));

		PipeParser pipeParser = new PipeParser();
		pipeParser.getParserConfiguration();

		// serialize the message to pipe delimited output file
		Util.writeMessageToFile(pipeParser, adtMessages, fileName + "_Patient_Demographic_Data.hl7", headers, hl7LocationFolder); 
	}
}
