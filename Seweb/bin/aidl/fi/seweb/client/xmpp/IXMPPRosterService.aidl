package fi.seweb.client.xmpp;

import fi.seweb.client.xmpp.IXMPPRosterCallback;

interface IXMPPRosterService {
	
	/* xmpp methods */ 
	void updatePresence(int presenceCode, String statusMsg);
	void connect();
	boolean isConnected();
	
	/*void addRosterItem(String user, String alias, String group);
	void addRosterGroup(String group);
	void renameRosterGroup(String group, String newGroup);
	void removeRosterItem(String user);
	void requestAuthorizationForRosterItem(String user);
	void renameRosterItem(String user, String newName);
	void moveRosterItemToGroup(String user, String group);*/
	
	/* fetch the entire roster */
 	// String getRosterEntries();
	
	/* callback methods */
	void registerRosterCallback(IXMPPRosterCallback callback);
	void unregisterRosterCallback(IXMPPRosterCallback callback);
	
}
