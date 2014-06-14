package fi.seweb.client.core;

import java.util.ArrayList;

import org.jivesoftware.smack.packet.Presence;

import fi.seweb.client.common.StringUtils;

/*
 * Data holder class which represents a RosterEntry
 * */
public class Buddy {
	
	public final String mJid;
	
	private String mName;
	private LocalPresence mPresence;
	
	private final ArrayList<String> mGroups;
	
	/* default constructor */
	public Buddy (String jid) {
		if (jid == null || jid.length() == 0)
			throw new IllegalArgumentException("jid is empty or null: " + jid);
		if (!StringUtils.isValidJid(jid))
			throw new IllegalArgumentException("Invalid jid: " + jid);
		
		this.mJid = jid;
		
		/* setting default values */ 
		this.mName = jid;
		this.mGroups = new ArrayList<String> (); // empty list
		this.mPresence = new LocalPresence(); // type = Presence.Type.offline, status = "", mode = null
	}
	
	/* 
	 * Resets to default values 
	 * 
	 * Type: Presence.Type.offline 
	 * Mode: null 
	 * String: ""
	 *  
	 * */
	public void setPresenceOffline() {
		this.mPresence = new LocalPresence();
	}
	
	public void setPresence(String status, Presence.Type type, Presence.Mode mode) {
		if (status == null)
			throw new IllegalArgumentException("Status message should not be null");
		if (type == null)
			throw new IllegalArgumentException("Presence type should not be null");
		
		boolean unreadMessages = getPresence().mUnreadMessages;
		mPresence = new LocalPresence(status, type, mode, unreadMessages);
	}
	
	public void setPresence(String status, int presenceCode, boolean unreadMessages) {
		if (status == null)
			throw new IllegalArgumentException("Status message should not be null");
		if (presenceCode < 0)
			throw new IllegalArgumentException("Presence type should be a positive int: " + presenceCode);
		
		mPresence = new LocalPresence(status, presenceCode, unreadMessages);
	}
	
	public void setPresence(boolean unreadMessages) {
		String status = getPresence().mStatus;
		int code = getPresence().mPresenceCode;
		mPresence = new LocalPresence(status, code, unreadMessages);
	}
	
	
	public LocalPresence getPresence() {
		return mPresence;
	}
	
	public String getName() {
		return mName;
	}
	
	public void setName(String name) {
		if (name == null)
			throw new IllegalArgumentException("Name is null");

		mName = name;
	}
	
	public ArrayList<String> getGroups() {
		ArrayList<String> groups = new ArrayList<String> (); 
		
		if (!mGroups.isEmpty()) {
			for (String s : mGroups) {
				groups.add(s);
			}
		}
				
		return groups;
	}
	
	public boolean addToGroup(String group) {
		if (group == null || group.length() == 0)
			throw new IllegalArgumentException("Group name is null / empty");
		
		if (!mGroups.contains(group)) {
			mGroups.add(group);
			return true;
		}
		return false;
	}
	
	public boolean removeFromGroup(String group) {
		if (group == null || group.length() == 0)
			throw new IllegalArgumentException("Group name is null / empty");
		
		if (mGroups.contains(group)) {
			mGroups.remove(group);
			return true;
		}
		return false;
	}
	
}
