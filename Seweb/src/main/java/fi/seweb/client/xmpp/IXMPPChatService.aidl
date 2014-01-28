package fi.seweb.client.xmpp;

import fi.seweb.client.xmpp.IXMPPChatCallback;

interface IXMPPChatService {
	void sendMessage(String toUser, String message, long timestamp);
	boolean isAuthenticated();
	void clearNotifications(String jid);
	String obtainLastMessage(String jid);
	
	void registerChatCallback(IXMPPChatCallback callback); 
	void unregisterChatCallback(IXMPPChatCallback callback);
}