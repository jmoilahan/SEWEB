package fi.seweb.client.core;

import java.util.ArrayList;
import java.util.HashMap;

import org.jivesoftware.smack.packet.Message;

/* a mockup of a database to store and retrieve "old/existing" messages between 
 * different UI views 
 * the mockup will maintain the order in which the messages are added
 * 
 * !not threadsafe! 
 * */


public class MessageStorage {

	// String = chatID
	private final HashMap <String, ArrayList <Message>> mData = new HashMap <String, ArrayList <Message>>();
	
	/* ChatID = the id set by the XMPP protocol when the chat is created
	 * Message = the message object */
	public void addMessage(String chatID, Message message) {
		if (chatID == null || chatID.length() == 0) 
			throw new IllegalArgumentException("Chat ID is null / empty!");
		if (message == null)
			throw new IllegalArgumentException("Message param is null!");
		
		ArrayList <Message> messageArray = null;
		
		//adding the timestamp
		message.setProperty("timestamp", System.currentTimeMillis());
		
		if (mData.containsKey(chatID)) {
			messageArray =  mData.get(chatID);
			if (messageArray != null) {
				messageArray.add(message);
				System.out.println("Message added: " + message.getBody());
			}
		} else {
			messageArray = new ArrayList <Message>();
			messageArray.add(message);
			mData.put(chatID, messageArray);
			System.out.println("Message added: " + message.getBody());
		}
	}
	
	/* retrieves all messages by a chatID */
	/* returns null if the message array not found */
	public ArrayList<Message> retrieveMessage(String chatID) {
		if (chatID == null || chatID.length() == 0) 
			throw new IllegalArgumentException("Chat ID is null / empty!");
		
		ArrayList <Message> result = null;
		if (mData.containsKey(chatID)) {
			result = MessageStorage.deepCopy(mData.get(chatID));
		}
		
		return result;
	}
	
	/* returns empty array if the source array is empty or
	 * copies deeply the contents of the source array to the dest array */
	public static ArrayList <Message> deepCopy (ArrayList <Message> source) {
		if (source == null) {
			throw new IllegalArgumentException("Source array is null");
		}
		ArrayList <Message> destination = new ArrayList<Message> (source.size());
		
		if (!source.isEmpty()) {
			for (Message m : source) {
				destination.add(m);
			}
		}
		return destination;
	}
	
	/*
	 * Convenience method to check if the the storage has messages for a concrete chat id
	 */
	public boolean hasMessagesFor(String chatID) {
		if (chatID == null || chatID.length() == 0) 
			throw new IllegalArgumentException("Chat ID is null / empty!");
		
		boolean result = false;
		
		if (mData.containsKey(chatID)) {
			if (!mData.get(chatID).isEmpty()) {
				result = true;
			}
		}
		return result;
	}
	
	public Message getLast(String chatID) {
		if (chatID == null || chatID.length() == 0) 
			throw new IllegalArgumentException("Chat ID is null / empty!");
		
		if (!hasMessagesFor(chatID))
			return null;
		
		ArrayList<Message> list = retrieveMessage(chatID);
		if (list != null && list.size() >= 0) {
			int size = list.size();
			if (size == 0) {
				return list.get(size);
			} else { 
				return list.get(size - 1);
			}
		}
		return null;
	}
}
