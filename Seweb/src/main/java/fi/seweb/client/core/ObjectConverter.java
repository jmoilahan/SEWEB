package fi.seweb.client.core;

import java.util.ArrayList;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class ObjectConverter {
	
	private static final String TAG = "ObjectConverter";
	private static final String TAG_BODY = "body";
	private static final String TAG_FROM = "from";
	private static final String TAG_TO = "to";
	private static final String TAG_THREADID = "threadid";
	private static final String TAG_TYPE = "type";
	private static final String TAG_TIMESTAMP = "timestamp";
	private static final String TAG_ARRAY = "messages";
	
	public static Message toMessage(String jsonString) {
		if (jsonString == null || jsonString.length() == 0)
			throw new IllegalArgumentException("jsonString is null or empty: ");
		if (jsonString.equalsIgnoreCase("{}"))
			return new Message();
		
		Message message = new Message();
		
		try {
		
			JSONObject jsonObject = new JSONObject(jsonString);
			message = toMessage(jsonObject);

		} catch (Exception e) {
			System.out.println(TAG + " Error parsing json string: " + e.getMessage());
			throw new RuntimeException(e);
		}
		
		System.out.println(TAG + " Parsed message: " + message.toXML());
		
		return message;
	}
	
	public static String toJson(Message message) {
		
		JSONObject object = toJSONObject(message);
		Log.i(TAG, object.toString());

		return object.toString();
	}
	
	public static String toJSONArray (ArrayList<Message> array) {
		if (array == null)
			throw new IllegalArgumentException("Array is null");
		if (array.isEmpty()) {
			return "{}";
		}
		
		JSONArray jsonArray = new JSONArray();
		
		for (int i = 0; i < array.size(); i++) {
			Message msg = array.get(i);
			JSONObject jsonObj = toJSONObject(msg);
			jsonArray.put(jsonObj);
		}
		
		JSONObject messagesObj = new JSONObject();
		try {
		    messagesObj.put(TAG_ARRAY, jsonArray);
		} catch (JSONException e) {
			System.out.println(TAG + " Failed to construct a json array");
			throw new RuntimeException(e);
		}
		
		return messagesObj.toString();
	}
	
	public static ArrayList<Message> toArrayList (String jsonArray) {
		if (!isValidJSONArray(jsonArray))
			throw new IllegalArgumentException("Failed to parse the json array: " + jsonArray);
		if (jsonArray.toString().equalsIgnoreCase("{}"))
			return new ArrayList<Message>();
		
		ArrayList <Message> array = new ArrayList<Message>();
		
		try {
            // Getting JSON object and the array node
            JSONObject jsonObj = new JSONObject(jsonArray);
            JSONArray jsonArr = jsonObj.getJSONArray(TAG_ARRAY);
            
            // parsing all child nodes
            for (int i = 0; i < jsonArr.length(); i++) {
            	JSONObject c = jsonArr.getJSONObject(i);
            	Message message = toMessage(c);
            	array.add(message);
            }
		} catch (Exception e) {
			System.out.println(TAG + " Failed to parse the json array: " + jsonArray);
			throw new RuntimeException(e);
		}

		return array;
	}
	
	private static JSONObject toJSONObject (Message message) {
		if (message == null)
			throw new IllegalArgumentException("Message is null");
		
		String body = message.getBody();
		String from = message.getFrom();
		String to = message.getTo();
		String threadID = message.getThread();
		String type = message.getType().name();
		long time = (Long) message.getProperty(TAG_TIMESTAMP);
		
		//are all message's fields are non empty / non null?
		if (body == null      || body.length() == 0     ||
			from == null      || from.length() == 0     ||
			to == null        || to.length() == 0       ||
			threadID == null  || threadID.length() == 0 ||
			type == null      || type.length() == 0     ||
			time == 0      								  ) {
			return new JSONObject();
		}
		
		JSONObject jsonObj = new JSONObject();
		
		try {
		    jsonObj.put(TAG_BODY, body);
		    jsonObj.put(TAG_FROM, from);
		    jsonObj.put(TAG_TO, to);
		    jsonObj.put(TAG_THREADID, threadID);
		    jsonObj.put(TAG_TYPE, type);
		    jsonObj.put(TAG_TIMESTAMP, time);
		} catch (Exception e) {
			System.out.println(TAG + " Error while converting a message to a jsonString: " + e.getMessage());
			throw new RuntimeException(e);
		}
		
		return jsonObj;
	}
	
	private static Message toMessage(JSONObject jsonObj) {
		if (jsonObj == null)
			throw new IllegalArgumentException("json object is null");
		if (jsonObj.toString().equalsIgnoreCase("{}"))
			return new Message();
		
		Message msg = new Message();
		
		try {
			String body = jsonObj.getString(TAG_BODY);
			String from = jsonObj.getString(TAG_FROM);
			String to = jsonObj.getString(TAG_TO);
			String threadID = jsonObj.getString(TAG_THREADID);
			String typeStr = jsonObj.getString(TAG_TYPE);
			long time = (Long) jsonObj.get(TAG_TIMESTAMP);
		
			msg.setBody(body);
			msg.setFrom(from);
			msg.setTo(to);
			msg.setThread(threadID);
			msg.setProperty(TAG_TIMESTAMP, time);
		
			if (typeStr.equalsIgnoreCase(Type.chat.name())) {
				msg.setType(Type.chat);
			} else if (typeStr.equalsIgnoreCase(Type.error.name())) {
				msg.setType(Type.error);
			} else if (typeStr.equalsIgnoreCase(Type.groupchat.name())) {
				msg.setType(Type.groupchat);
			} else if (typeStr.equalsIgnoreCase(Type.headline.name())) {
				msg.setType(Type.headline);
			} else if (typeStr.equalsIgnoreCase(Type.normal.name())) {
				msg.setType(Type.normal);
			}
		} catch (Exception e) {
			System.out.println(TAG + " Failed to covert a json object to message: " + jsonObj.toString());
			throw new RuntimeException(e);
		}
		
		return msg;
	}
	
	
	/* a helper method to determine if the string is a parsable json string */
	private static boolean isValidJSON(String json) {
		try {
	        new JSONObject(json);
	        return true;
	    } catch(JSONException ex) { 
	        return false;
	    }
	}
	
	private static boolean isValidJSONArray(String jsonArray) {
		try {
			JSONObject object = new JSONObject(jsonArray);
			object.getJSONArray(TAG_ARRAY);
			return true;
		} catch (JSONException e) {
			return false;
		}
	}
	
	private ObjectConverter() {
		throw new AssertionError("Not supposed to be instantiated");
	}

}
