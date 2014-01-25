package fi.seweb.client.xmpp;


interface IXMPPChatCallback {
	void newChatMessageReceived(in String chat, in String message);
}
