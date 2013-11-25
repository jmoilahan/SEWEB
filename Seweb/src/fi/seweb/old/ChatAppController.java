package fi.seweb.old;
import java.util.UUID;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import fi.seweb.app.ChatAppView;
import fi.seweb.util.Channel;
import fi.seweb.util.Event;


public class ChatAppController implements EventListener {
	
	//private final PubSubEngine engine = PubSubEngineImpl.getInstance();
	private PubSubEngine engine = null;
	private static ChatAppView parent;
	private static ChatAppController self;
	
	@SuppressWarnings("deprecation")
	private final JSONParser parser = new JSONParser();
	
	private static final String uuid = UUID.randomUUID().toString().substring(0, 6);
	private static String username;
	private static String password;
	private final Channel channel = new Channel("All"); 
	
	public ChatAppController() {
				
		ChatAppController.self = this;
		//init();
	}
		
	public void setParent(Activity parent) {
		ChatAppController.parent = (ChatAppView) parent;	
	}
	
	public void sendChatMessage(final CharSequence msg) {
		Log.i("DEBUG", "sendChatMessage...");
		
		if (msg == null || msg.length() == 0)
			return;
		
		net.minidev.json.JSONObject obj = new JSONObject();
		
		obj.put("name", uuid);
		obj.put("msg", msg.toString());
		
		
		Event e = new Event.Builder(obj.toJSONString(), channel).build();;
		
		engine.publish(e);
		
		obj.clear();
	}
	
	
	public void reconnectAll(){

	}
	
	public void init() {
 		
		Context ctx = parent.getApplicationContext();
		ChatAppController.username = "Something@ibicoop.org";
		ChatAppController.password = "SomePassword";
		String term_id = uuid;
		
		Log.i("DEBUG", "starting the PubSubEngine");
		
		engine = EngineFactory.getInstance().build(ctx, username, password, term_id);
		
		//engine.init(ctx, username, password , term_id);
		
		engine.subscribe(this, channel);
		engine.startNotification();
		
	}
	
	
	public ChatAppController getChatManager() {
		return ChatAppController.self;
	}
	
	
	public void terminate() {	
		
	}
	
	@Override
	public void onEventReceived(Event e) {
		Log.i("DEBUG", "The event is delivered");
		
		net.minidev.json.JSONObject obj = null;
		
		try {
			obj = (JSONObject) parser.parse(e.getPayload());
			Log.i("DEBUG", "Parsed the event object, Event->JSONObject");
		} catch (net.minidev.json.parser.ParseException e1) {
			throw new AssertionError("Failed to JSON parse an incoming event");
		}
		
		if (obj == null)
			return;
				
		final String name = (String) obj.get("name");
		final String message = (String) obj.get("msg");
		
		Log.i("DEBUG", "Constructing a chat message...");
		
		parent.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				 
				String prefix = " " + name + ": ";
				parent.addNewChatMessage(message, prefix, Color.GREEN);
				
			}
		});
		
	}
}
