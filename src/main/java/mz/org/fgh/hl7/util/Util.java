package mz.org.fgh.hl7.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.message.ADT_A24;
import ca.uhn.hl7v2.parser.Parser;
import mz.org.fgh.hl7.Location;

public class Util {
	
	static Logger log = Logger.getLogger(Util.class.getName());
	
	private static String footers;

	public static String getCurrentTimeStamp() {
		return new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
	}
	
	public static String listToString(List<String> locationsBySite) {
		String locations = StringUtils.join(locationsBySite, "','");
		locations = "'" + locations + "'";
		return locations;
	}
	
	public static List<String> extractUuid(List<Location> locations) {
		List<String> locationUuid = new ArrayList<String>();
		for (Location location : locations) {
			locationUuid.add(location.getUuid());
		}
		return locationUuid;
	}
	
	public static void writeMessageToFile(Parser parser, List<ADT_A24> adtMessages, String outputFilename, String headers, String hl7LocationFolder)
			throws IOException, FileNotFoundException, HL7Exception {
		log.info("writeMessageToFile called...");

		OutputStream outputStream = null;
		try {

			// Remember that the file may not show special delimiter characters when using
			// plain text editor
			File file = new File(Paths.get(hl7LocationFolder, outputFilename).toString());   

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
