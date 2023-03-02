package mz.org.fgh.hl7.generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Logger;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.message.ADT_A24;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.parser.PipeParser;
import mz.org.fgh.hl7.model.PatientDemographic;
import mz.org.fgh.hl7.util.Util;

public class Hl7FileGenerator {

	static Logger log = Logger.getLogger(Hl7FileGenerator.class.getName());

	private static String headers;
	
	private static String footers;

	public static void createHl7File(List<PatientDemographic> patientDemographics) throws HL7Exception, IOException {
		log.info("createHl7File called...");

		String currentTimeStamp = Util.getCurrentTimeStamp();

		// prepare the headers
		headers = "FHS|^~\\&|XYZSYS|XYZ " + "DEFAULT_LOCATION_NAME" + "|DISA*LAB|SGP|" + currentTimeStamp
				+ "||Patient_Demographic_Data.hl7|" + "WEEKLY HL7 UPLOAD|00009972|\rBHS|^~\\&|XYZSYS|XYZ "
				+ "DEFAULT_LOCATION_NAME" + "|DISA*LAB|SGP|" + currentTimeStamp + "||||00010223\r";

		// create the HL7 message
		List<ADT_A24> adtMessages = AdtMessageFactory.createMessage("A24", patientDemographics);

		PipeParser pipeParser = new PipeParser();
		pipeParser.getParserConfiguration();

		// serialize the message to pipe delimited output file
		writeMessageToFile(pipeParser, adtMessages, "TEST" + "_Patient_Demographic_Data.hl7");
	}

	public static void writeMessageToFile(Parser parser, List<ADT_A24> adtMessages, String outputFilename)
			throws IOException, FileNotFoundException, HL7Exception {
		log.info("writeMessageToFile called...");

		OutputStream outputStream = null;
		try {

			// Remember that the file may not show special delimiter characters when using
			// plain text editor
			File file = new File("/home/machabane/hl7/" + outputFilename); 

			file.createNewFile();

			log.info("Serializing message to file...");
			outputStream = new FileOutputStream(file);

			outputStream.write(headers.getBytes());

			for (ADT_A24 adt_A24 : adtMessages) {
				outputStream.write(parser.encode(adt_A24).getBytes());
				outputStream.write(System.getProperty("line.separator").getBytes());
				outputStream.flush();
			}

			footers = "BTS|" + String.valueOf(adtMessages.size()) + "\rFTS|1";

			outputStream.write(footers.getBytes());

			System.out.printf("Message serialized to file '%s' successfully", file);
			System.out.println("\n");

			// send the hl7 file to disa
			// Util.sendHl7File(file.getName());
		} finally {
			if (outputStream != null) {
				outputStream.close();
			}
			adtMessages = null;
		}
	}
}
