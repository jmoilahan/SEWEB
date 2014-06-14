package fi.seweb.client.core;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RosterCacheConverter {
	
	private static final String TAG = "RosterCacheConverter";
	private static final String TAG_NAME = "name";
	private static final String TAG_JID = "jid";
	private static final String TAG_GROUPS = "groups";
	private static final String TAG_GROUP = "group";
	private static final String TAG_STATUS = "status";
	private static final String TAG_PRESENCE_CODE = "code";
	private static final String TAG_ARRAY = "rosterentries";
	private static final String TAG_MESSAGES = "messages";

	public static Buddy toBuddy(String json) {
		if (json == null || json.length() == 0)
			throw new IllegalArgumentException("json object is null");
		if (json.equalsIgnoreCase("{}"))
			throw new IllegalArgumentException("json object is empty");
		if (!isValidJSON(json))
			throw new IllegalArgumentException("invalid json string: " + json);
		
		Buddy buddy = null;
		
		try {
			JSONObject obj = new JSONObject(json);
			buddy = toBuddy(obj);
		} catch (JSONException e) {
			System.out.println(TAG + " Failed to parse the json string: " + json);
		}
		return buddy;
	}
	
	public static Buddy toBuddy(JSONObject jsonObj) {
		if (jsonObj == null)
			throw new IllegalArgumentException("json object is null");
		if (jsonObj.toString().equalsIgnoreCase("{}"))
			throw new IllegalArgumentException("json object is empty");
		
		Buddy buddy = null;
		
		try {
			String jid = jsonObj.getString(TAG_JID);
			String name = jsonObj.getString(TAG_NAME);
			int code = jsonObj.getInt(TAG_PRESENCE_CODE);
			String status = jsonObj.getString(TAG_STATUS);
			boolean hasMessages = jsonObj.getBoolean(TAG_MESSAGES);
			
            JSONArray jsonArr = jsonObj.getJSONArray(TAG_GROUPS);
            
            ArrayList<String> groups = new ArrayList<String>();
    		
            // parsing all child nodes
            for (int i = 0; i < jsonArr.length(); i++) {
              	JSONObject c = jsonArr.getJSONObject(i);
               	String group = c.getString(TAG_GROUP);
               	groups.add(group);
            }
            
            if (jid != null && jid.length() != 0 && !groups.isEmpty() &&
            		status != null && code >= 0 ) {
				buddy = new Buddy(jid);
				buddy.setName(name);
				buddy.setPresence(status, code, hasMessages);
				for (String group : groups) {
	               buddy.addToGroup(group);
	            }
				
			}
		} catch (JSONException e) {
			System.out.println(TAG + " Failed to covert a json object to a buddy: " + jsonObj.toString());
			throw new RuntimeException(e);
		}
		
		return buddy;
	}

	public static String toJson(Buddy buddy) {
		JSONObject object = toJSONObject(buddy);
		return object.toString();
	}
	
	public static String toJSONArray (ArrayList<Buddy> array) {
		if (array == null)
			throw new IllegalArgumentException("Array is null");
		if (array.isEmpty()) {
			return "{}";
		}
		
		JSONArray jsonArray = new JSONArray();
		
		for (int i = 0; i < array.size(); i++) {
			Buddy buddy = array.get(i);
			JSONObject jsonObj = toJSONObject(buddy);
			jsonArray.put(jsonObj);
		}
		
		JSONObject buddiesObj = new JSONObject();
		try {
		    buddiesObj.put(TAG_ARRAY, jsonArray);
		} catch (JSONException e) {
			System.out.println(TAG + " Failed to construct a json array");
			throw new RuntimeException(e);
		}
		
		return buddiesObj.toString();
	}
	
	
	public static ArrayList<Buddy> toArrayList(String jsonArray) {
		if (jsonArray.toString().equalsIgnoreCase("{}"))
			return new ArrayList<Buddy>();
		if (!isValidJSONArray(jsonArray))
			throw new IllegalArgumentException("Failed to parse the json array: " + jsonArray);
		
		ArrayList<Buddy> array = new ArrayList<Buddy>();
		
		try {
			// Getting JSON object and the array node
            JSONObject jsonObj = new JSONObject(jsonArray);
            JSONArray jsonArr = jsonObj.getJSONArray(TAG_ARRAY);
            
            // parsing all child nodes
            for (int i = 0; i < jsonArr.length(); i++) {
            	JSONObject c = jsonArr.getJSONObject(i);
            	Buddy buddy = toBuddy(c);
            	array.add(buddy);
            }
		} catch (JSONException e) {
			System.out.println(TAG + " Failed to parse the json array: " + jsonArray);
			throw new RuntimeException(e);
		}
		return array;
	}
		
	private static JSONObject toJSONObject (Buddy buddy) {
		if (buddy == null)
			throw new IllegalArgumentException("Buddy is null");
		
		String name = buddy.getName();
		String jid = buddy.mJid;
		ArrayList<String> groups = buddy.getGroups();
		LocalPresence presence = buddy.getPresence();
		
		//are all message's fields are non empty / non null?
		if (name == null || name.length() == 0 ||
			jid == null || jid.length() == 0 ||
			presence == null || groups.isEmpty()) {
			return new JSONObject();
		}
	
		JSONArray groupsArray = groupsToJSONArray(groups);
		
		JSONObject jsonObj = new JSONObject();
		try {
		    jsonObj.put(TAG_JID, jid);
		    jsonObj.put(TAG_NAME, name);
		    jsonObj.put(TAG_STATUS, buddy.getPresence().mStatus);
		    jsonObj.put(TAG_PRESENCE_CODE, buddy.getPresence().mPresenceCode);
		    jsonObj.put(TAG_MESSAGES, buddy.getPresence().mUnreadMessages);
		    jsonObj.put(TAG_GROUPS, groupsArray);
		} catch (Exception e) {
			System.out.println(TAG + " Error while converting a buddy to a jsonString: " + e.getMessage());
			throw new RuntimeException(e);
		}
		return jsonObj;
	}
	
	private static JSONArray groupsToJSONArray (ArrayList<String> groups) {
		if (groups == null)
			throw new IllegalArgumentException("Groups array is null");
		
		JSONArray jsonArray = new JSONArray();
		
		for (int i = 0; i < groups.size(); i++) {
			String group = groups.get(i);
			JSONObject jsonObj = new JSONObject();
			try {
				jsonObj.put(TAG_GROUP, group);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			jsonArray.put(jsonObj);
		}
		return jsonArray;
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
	
	private RosterCacheConverter() {
		throw new AssertionError("Not supposed to be instantiated");
	}
}
