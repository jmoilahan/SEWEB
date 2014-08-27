package fi.seweb.client.xmpp;

interface IXMPPChatCallback {
	// called when a new incoming message arrives
	void newChatMessageReceived(in String chat, in String message, in long timestamp);
	// called to identify the remote user the
	String getRemoteUserJID();
}
