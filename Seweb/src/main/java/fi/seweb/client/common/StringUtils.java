package fi.seweb.client.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
	
	private StringUtils() {
		throw new AssertionError("Not supposed to be instantiated");
	}
	
	public static boolean isValidJid(String jid) {
		if (jid == null)
			throw new IllegalArgumentException("Jid param is null");
		
		// jid = [ node "@" ] domain [ "/" resource ]
				
		Pattern p = Pattern
				.compile("(?i)[a-z0-9\\-_\\.]++@[a-z0-9\\-_]++(\\.[a-z0-9\\-_]++)++");
		Matcher m = p.matcher(jid);

		return m.matches();
	}
}
