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
	public void testJSONToArray() {
		ArrayList<Message> originalArr = new ArrayList<Message> ();
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
			message.setBody(body + i);
			message.setFrom(from);
			message.setTo(to);
			message.setThread(threadID);
			message.setType(type);
			message.setProperty(timestamp, System.currentTimeMillis());
			originalArr.add(message);
		}
		
		String jsonArray = ObjectConverter.toJSONArray(originalArr);
		assertNotNull(jsonArray);
		
		ArrayList <Message> resultArr = ObjectConverter.toArrayList(jsonArray);
		assertEquals(originalArr.size(), resultArr.size());
		
		for (int i = 0; i < originalArr.size(); i++) {
			Message m1 = originalArr.get(i);
			Message m2 = resultArr.get(i);
			assertEquals(m1.getBody(), m2.getBody());
			assertEquals(m1.getFrom(), m2.getFrom());
			assertEquals(m1.getTo(), m2.getTo());
			assertEquals(m1.getThread(), m2.getThread());
			assertEquals(m1.getType(), m2.getType());
		}
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
			message.setBody(body + i);
			message.setFrom(from);
			message.setTo(to);
			message.setThread(threadID);
			message.setType(type);
			message.setProperty(timestamp, System.currentTimeMillis());
			array.add(message);
		}
		
		String jsonArray = ObjectConverter.toJSONArray(array);
		assertNotNull(jsonArray);
		
		System.out.println(jsonArray);
	}
	

}
