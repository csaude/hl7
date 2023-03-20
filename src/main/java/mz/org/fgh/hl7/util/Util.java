package mz.org.fgh.hl7.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class Util {

	public static String getCurrentTimeStamp() {
		return new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
	}

	public static String listToString(List<String> locationsBySite) {
		String locations = StringUtils.join(locationsBySite, "','");
		locations = "'" + locations + "'";
		return locations;
	}
}
