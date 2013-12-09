package fi.seweb.client.common;

public class SewebPreferences {
	
	public static final String DOMAIN = "seweb.p1.im";
	public static final String USERNAME = "testuser1";
	public static final String PASSWORD = "1";
	
	
	/* global system constants: *DO NOT CHANGE* */
	public static final int PRESENSE_CHANGED = 1;
	public static final int SERVER_DISCONNECTED = 2;
	
	
	private SewebPreferences() {
		throw new AssertionError("This class isn't supposed to be instantialized");
	}
}
