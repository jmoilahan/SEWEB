package fi.seweb.client.core;

import org.jivesoftware.smack.packet.Presence;

import fi.seweb.client.common.PresenceStatus;
import fi.seweb.client.common.SewebPreferences;

/* represents a user's presence */
public class LocalPresence {
	
	public final String mStatus;
	public final int mPresenceCode;
	public final boolean mUnreadMessages;
	
	/* default constructor */
	public LocalPresence() {
		this.mStatus = "";
		//this.mPresenceCode = SewebPreferences.getPresenceCode(Presence.Type.unavailable, null);
		this.mPresenceCode = PresenceStatus.getPresenceCode(Presence.Type.unavailable, null);
		this.mUnreadMessages = false;
	}
	
	/* default value for mode is null */
	public LocalPresence(String status, Presence.Type type, Presence.Mode mode, boolean unreadMessages) {
		if (status == null)
			throw new IllegalArgumentException("Status message should not be null");
		if (type == null)
			throw new IllegalArgumentException("Presence type should not be null");
		
		this.mStatus = status;
		//this.mPresenceCode = SewebPreferences.getPresenceCode(Presence.Type.unavailable, null);
		this.mPresenceCode = PresenceStatus.getPresenceCode(type, mode);
		this.mUnreadMessages = unreadMessages;
	}
	
	public LocalPresence(String status, int presenceCode, boolean unreadMessages) {
		if (status == null)
			throw new IllegalArgumentException("Status message should not be null");
		if (presenceCode < 0)
			throw new IllegalArgumentException("Illegal presence code: " + presenceCode);
		
		this.mStatus = status;
		this.mPresenceCode = presenceCode;
		this.mUnreadMessages = unreadMessages;
	}	
}
