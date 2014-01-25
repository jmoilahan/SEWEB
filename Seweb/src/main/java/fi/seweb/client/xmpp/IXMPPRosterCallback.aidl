package fi.seweb.client.xmpp;

/*
	interface to send notifications to UI
*/

interface IXMPPRosterCallback {
	void presenceChanged(String user, int presenceCode, String statusMsg);
	void connectionStatusChanged(int connectionStatusCode);
	void newChatMessageReceived(String fromJID, String chatID);
}
