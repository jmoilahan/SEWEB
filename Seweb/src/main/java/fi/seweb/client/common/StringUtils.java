package fi.seweb.client.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

	// Public Constants
	// Max timestamp in the system: //23:59:59,999
	public  final static long MAXTIME = 86399999;
	// Min timestamp in the system: //00:00:00,000
	public  final static long MINTIME = 0; 
		
	// Private constants:
 	private final static String DATE = "HH:mm";
	private final static SimpleDateFormat FORMAT = new SimpleDateFormat(DATE);
	
	public static boolean isValidJid(String jid) {
		if (jid == null)
			throw new IllegalArgumentException("Jid param is null");
		
		// jid = [ node "@" ] domain [ "/" resource ]
				
		Pattern p = Pattern
				.compile("(?i)[a-z0-9\\-_\\.]++@[a-z0-9\\-_]++(\\.[a-z0-9\\-_]++)++");
		Matcher m = p.matcher(jid);

		return m.matches();
	}
	
	/*
	 * Parses time from a String to a long value. The string param should not be empty or null 
	 * - otherwise throws IllegalArgumentException. If parsing fails, throws 
	 *  IllegalArgumentException as well. 
	 */
	public static long parseTime (String timeStr) {
		if (timeStr == null || timeStr.length() == 0)
			throw new IllegalArgumentException("timeStr should not be null or empty");
		
		//FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
		FORMAT.setTimeZone(TimeZone.getDefault());
		long timestamp = 0;
		try {
			Date d = FORMAT.parse(timeStr);
			timestamp = d.getTime();
		} catch (ParseException e) {
			throw new IllegalArgumentException("Failed to parse the timeStr argument: " + e.getMessage());
		}
		return timestamp;
	}
	
	/*
	 * Converts time from a long value (milliseconds) to a String
	 * the long param (timestamp) has to have a positive value, be smaller 
	 * than the system's MAXTIME constant, and be greater than the system's MINTIME constant.
	 * An exception (IllegalArgumentException) is thrown otherwise.
	 */
	public static String parseTime(long timestamp) {
		if (timestamp <= MINTIME)
			throw new IllegalArgumentException("timestamp must be a positive long: " + timestamp);
		/*if (timestamp > MAXTIME) 
			throw new IllegalArgumentException("timestamp is too large");
			*/
		
		//FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
		FORMAT.setTimeZone(TimeZone.getDefault());
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestamp);
		return FORMAT.format(cal.getTime());
	}
	
	private StringUtils() {
		throw new AssertionError("Not supposed to be instantiated");
	}
}
