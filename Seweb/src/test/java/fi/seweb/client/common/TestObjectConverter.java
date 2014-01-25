package fi.seweb.client.common;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fi.seweb.client.core.ObjectConverter;

public class TestObjectConverter {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testToArray() {
		
	}
	
	
	@Test
	public void testToJSONArray() {
		ArrayList<Message> array = new ArrayList<Message> ();
		
		int arraySize = 50;
		
		String to = "to";
		String from = "from";
		String threadID = "someid";
		Type type = Type.chat;
		String timestamp = "timestamp";
		String body = "body";
		
		//fill up a message array
		for (int i = 0; i <= arraySize; i++) {
			Message message = new Message();
			message.setBody(body + "i");
			message.setFrom(from);
			message.setTo(to);
			message.setThread(threadID);
			message.setType(type);
			message.setProperty("timestamp", timestamp);
			array.add(message);
		}
		
		String jsonArray = ObjectConverter.toJSONArray(array);
		assertNotNull(jsonArray);
		
	}
	

}
