package fi.seweb.client.common;

import org.jivesoftware.smack.packet.Presence;

public class SewebPreferences {
	
	public static final String DOMAIN = "seweb.p1.im";
	public static final String USERNAME = "android";
	public static final String PASSWORD = "1";
	/* Default port for normal connections is 5222, for SSL connections is 5223 */
	public static final int PORT = 5222; 
	
	/* Global system constants: *DO NOT CHANGE* */
	/* Reserve 1-10 for update events sent from the background service to the UI */
	public static final int PRESENCE_CHANGED = 0;
	public static final int CONNECTION_CHANGED = 2;
	public static final int NEW_CHAT_MESSAGE = 3;
	
	/* Presence status codes */
	public static final int PRESENCE_AVAILABLE = 11;
	public static final int PRESENCE_OFFLINE = 12;
	public static final int PRESENCE_AWAY = 13;
	public static final int PRESENCE_DND = 14;
	public static final int PRESENCE_XA = 15;
	public static final int PRESENCE_CHAT = 16;
	
	/* Connection status codes */
	public static final int CONNECTION_ONLINE = 1;
	public static final int CONNECTION_OFFLINE = 2;

	/* Chat event codes */
	
	
	/* ERROR */
	public static final int ERROR = 99999;
	
	private SewebPreferences() {
		throw new AssertionError("This class isn't supposed to be instantialized");
	}
	
	public static String presenceAsString(int presenceCode) {
		String presenceStr = "";
		switch (presenceCode) {
        case SewebPreferences.PRESENCE_AVAILABLE:
            presenceStr = "Available";
            break;
        case SewebPreferences.PRESENCE_AWAY:
        	presenceStr = "Away";
            break;
        case SewebPreferences.PRESENCE_DND:
        	presenceStr = "Do not disturb";
            break;
        case SewebPreferences.PRESENCE_OFFLINE:
        	presenceStr = "Offline";
            break;
        case SewebPreferences.PRESENCE_XA:
        	presenceStr = "Extended away";
            break;
        case SewebPreferences.PRESENCE_CHAT:
        	presenceStr = "Chat";
            break;
        default:
            presenceStr = "Error";
    }
		return presenceStr;
	}
	
	public static int presenceModeAsInt (Presence.Mode mode) {
		int code;
		switch (mode) {
        case available:
            code = SewebPreferences.PRESENCE_AVAILABLE;
            break;
        case away: 
        	code = SewebPreferences.PRESENCE_AWAY;
            break;
        case dnd: 
        	code = SewebPreferences.PRESENCE_DND;
            break;
        case chat:
        	code = SewebPreferences.PRESENCE_CHAT;
            break;
        case xa: 
        	code = SewebPreferences.PRESENCE_XA;
            break;
        default:
            code = SewebPreferences.ERROR;
		}
		return code;
	}
}
