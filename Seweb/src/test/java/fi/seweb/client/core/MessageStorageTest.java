package fi.seweb.client.core;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.jivesoftware.smack.packet.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MessageStorageTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	/*
	@Test
	public void testAddGetMessage() {
		MessageStorage data = new MessageStorage();
		
		Message first = new Message();
		first.setBody("body");
		String chatID = "someid";
		data.addMessage(chatID, first);
		
		assertNotNull(data.retrieveMessage(chatID));
		
		ArrayList<Message> list = data.retrieveMessage(chatID);
		
		assertFalse(list.isEmpty());
		
		Message second = list.get(0);
		
		assertEquals(first, second);
	}
	
	@Test
	public void testAddOrder() {
		String keyword = "Body";
		String chatID = "anotherId";
		int size = 100;
		MessageStorage storage = new MessageStorage();
		
		ArrayList<Message> dataset = new ArrayList<Message> ();
		
		for (int i = 0; i < size; i++) {
			Message m = new Message();
			m.setBody(keyword + i);
			dataset.add(m);
		}
		
		//adding the messages to the storage
		for (int j = 0; j < size; j++) {
			storage.addMessage(chatID, dataset.get(j));
		}
		
		//ok, the storage now has $size$ messages in the order from 0 to $size$
		ArrayList <Message> result = storage.retrieveMessage(chatID);
		assertNotNull(result);
		
		//check that the messages will be retrieved in the same order as added.
		for (int i = 0; i < size; i++) {
			Message original = dataset.get(i);
			Message retrieved = result.get(i);
			assertTrue(original.getBody().equalsIgnoreCase(retrieved.getBody()));
		}
		
		// let's add one more message
		String body = "LastMessage";
		Message someMessage = new Message();
		someMessage.setBody(body);
		
		storage.addMessage(chatID, someMessage);
		Message lastMessage = storage.getLast(chatID);
		
		assertNotNull(lastMessage);
		
		assertTrue(body.equalsIgnoreCase(lastMessage.getBody()));
	}
	*/
}
