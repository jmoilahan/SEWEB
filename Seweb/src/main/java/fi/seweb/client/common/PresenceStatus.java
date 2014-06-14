package fi.seweb.client.common;

import org.jivesoftware.smack.packet.Presence;

import fi.seweb.R;

public enum PresenceStatus {
	available(R.string.status_available, R.drawable.presence_online, R.drawable.user_online),
	chat(R.string.status_chat, R.drawable.presence_online, R.drawable.user_online),
	away(R.string.status_away, R.drawable.presence_away, R.drawable.user_away),
	dnd(R.string.status_dnd, R.drawable.presence_dnd, R.drawable.user_dnd),
	xa(R.string.status_xa, R.drawable.presence_na, R.drawable.user_na),
	offline(R.string.status_offline, R.drawable.presence_offline, R.drawable.user_offline);

	private final int textId;
	private final int rosterIconId;
	private final int userStatusIconId;

	PresenceStatus(int textId, int rosterIconId, int userStatusIconId) {
		this.textId = textId;
		this.rosterIconId = rosterIconId;
		this.userStatusIconId = userStatusIconId;
	}

	public int getTextId() {
		return textId;
	}

	public int getRosterIconId() {
		return rosterIconId;
	}

	public int getUserStatusIconId() {
		return userStatusIconId;
	}
	
	public String toString() {
		return name();
	}

	public static PresenceStatus fromString(String status) {
		return PresenceStatus.valueOf(status);
	}
	
	public static int getUserStatusIconId(int presenceCode) {
		if (presenceCode == PresenceStatus.offline.ordinal()) {
			return PresenceStatus.offline.userStatusIconId;
		} else if (presenceCode == PresenceStatus.available.ordinal()) {
			return PresenceStatus.available.userStatusIconId;
		} else if (presenceCode == PresenceStatus.away.ordinal()) {
			return PresenceStatus.away.userStatusIconId;
		} else if (presenceCode == PresenceStatus.dnd.ordinal()) {
			return PresenceStatus.dnd.userStatusIconId;
		} else if (presenceCode == PresenceStatus.chat.ordinal()) {	
			return PresenceStatus.chat.userStatusIconId;
		} else if (presenceCode == PresenceStatus.xa.ordinal()) {
			return PresenceStatus.xa.userStatusIconId;
		}
		throw new IllegalArgumentException("Unknown presence code: " + presenceCode);
	}
	
	/* corrected */
	public static int getRosterIconId(int presenceCode) {
		if (presenceCode == PresenceStatus.offline.ordinal()) {
			return PresenceStatus.offline.rosterIconId;
		} else if (presenceCode == PresenceStatus.available.ordinal()) {
			return PresenceStatus.available.rosterIconId;
		} else if (presenceCode == PresenceStatus.away.ordinal()) {
			return PresenceStatus.away.rosterIconId;
		} else if (presenceCode == PresenceStatus.dnd.ordinal()) {
			return PresenceStatus.dnd.rosterIconId;
		} else if (presenceCode == PresenceStatus.chat.ordinal()) {	
			return PresenceStatus.chat.rosterIconId;
		} else if (presenceCode == PresenceStatus.xa.ordinal()) {
			return PresenceStatus.xa.rosterIconId;	
		}

		throw new IllegalArgumentException("Unknown presence code: " + presenceCode);
	}
	
	/* corrected */
	public static int getTextId(int presenceCode) {
		if (presenceCode == PresenceStatus.offline.ordinal()) {
			return PresenceStatus.offline.textId;
		} else if (presenceCode == PresenceStatus.available.ordinal()) {
			return PresenceStatus.available.textId;
		} else if (presenceCode == PresenceStatus.away.ordinal()) {
			return PresenceStatus.away.textId;
		} else if (presenceCode == PresenceStatus.dnd.ordinal()) {
			return PresenceStatus.dnd.textId;
		} else if (presenceCode == PresenceStatus.chat.ordinal()) {	
			return PresenceStatus.chat.textId;
		} else if (presenceCode == PresenceStatus.xa.ordinal()) {
			return PresenceStatus.xa.textId;			
		}

		throw new IllegalArgumentException("Unknown presence code: " + presenceCode);
	}
	
	/* corrected */
	public static int getPresenceCode(Presence.Type type, Presence.Mode mode) {
		if (type == null)
			throw new IllegalArgumentException("Presence type is null");
		
		if (type == Presence.Type.unavailable) {
			return PresenceStatus.offline.ordinal();
		} else if (type == Presence.Type.available) {
			if (mode == null) { // default mode's value
				return PresenceStatus.available.ordinal();
			} else if (mode == Presence.Mode.available) {
				return PresenceStatus.available.ordinal();
			} else if (mode == Presence.Mode.away) {
				return PresenceStatus.away.ordinal();
			} else if (mode == Presence.Mode.chat) {
				return PresenceStatus.chat.ordinal();
			} else if (mode == Presence.Mode.dnd) {
				return PresenceStatus.dnd.ordinal();
			} else if (mode == Presence.Mode.xa) {
				return PresenceStatus.xa.ordinal();
			}
		}
		throw new IllegalArgumentException("Unknown Presence.Mode's value: " + mode);
	}
	
	/* corrected */
	public static Presence toPresence(int code, String status) {
		if (status == null)
			throw new IllegalArgumentException("Status is null");
		
		Presence.Type type = null;
		Presence.Mode mode = null;
		
		if (code == PresenceStatus.offline.ordinal()) {
			type = Presence.Type.unavailable;
			mode = null;
		} else if (code == PresenceStatus.available.ordinal()) {
			type = Presence.Type.available;
			mode = Presence.Mode.available;
		} else if (code == PresenceStatus.away.ordinal()) {
			type = Presence.Type.available;
			mode = Presence.Mode.away;
		} else if (code == PresenceStatus.dnd.ordinal()) {
			type = Presence.Type.available;
			mode = Presence.Mode.dnd;
		} else if (code == PresenceStatus.chat.ordinal()) {	
			type = Presence.Type.available;
			mode = Presence.Mode.chat;
		} else if (code == PresenceStatus.xa.ordinal()) {
			type = Presence.Type.available;
			mode = Presence.Mode.xa;
		} else {
			throw new IllegalArgumentException("Unknown presence code: " + code);
		}
		
		return new Presence(type, status, 128, mode);
	}
	
	public static boolean isCorrectStatusCode(int code) {
		if (code == PresenceStatus.offline.ordinal()) {
			return true;
		} else if (code == PresenceStatus.available.ordinal()) {
			return true;
		} else if (code == PresenceStatus.away.ordinal()) {
			return true;
		} else if (code == PresenceStatus.dnd.ordinal()) {
			return true;
		} else if (code == PresenceStatus.chat.ordinal()) {	
			return true;
		} else if (code == PresenceStatus.xa.ordinal()) {
			return true; 			
		}
		return false;
	}
}
