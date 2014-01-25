package fi.seweb.client.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestStringUtils {
	
	@Test
	public void testIsValidJID() {
		String validJID = "joni_pc@seweb.p1.im";
		
		String invalidJID = "something";
		String anotherInvalidJID = "joni67676376_pc@sewe{]}£b.p1.im";
		String emptyJID = "";
		
		assertTrue(StringUtils.isValidJid(validJID));
		assertFalse(StringUtils.isValidJid(invalidJID));
		assertFalse(StringUtils.isValidJid(anotherInvalidJID));
		assertFalse(StringUtils.isValidJid(emptyJID));
	}
}
