
import java.util.Collection;
import java.util.UUID;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;

import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

/* Maintained by agrrh */
public class ChatAppManager {
	
	private final ChatView parent;
	
	private static final String uuid = UUID.randomUUID().toString().substring(0, 6);
	private static String username;
	private static String password;
	
	private static final int packetReplyTimeout = 500; // millis
	private String server;
	private int port;
	private ConnectionConfiguration config;
	private XMPPConnection connection;
	private ChatManager chatManager;
	private MessageListener messageListener;
	private Roster roster;
		
	public ChatAppManager(ChatView parent, String server, int port) {
		this.server = server;
		this.port = port;
		this.parent = (ChatView) parent;
	}
	
	public ChatAppManager(ChatView parent) {
		this.parent = (ChatView) parent;
		this.port = 5269;
		this.server = "seweb.p1.im";
	}
	
	public void init() {
		ChatAppManager.username = "testuser1"+ "@seweb.p1.im";
		ChatAppManager.password = "testuser1";

		// Create a connection to the xmpp server.
		System.out.println(String.format("Initializing connection to server %1$s port %2$d", server, port));
		SmackConfiguration.setPacketReplyTimeout(packetReplyTimeout);
		config = new ConnectionConfiguration(server, port);
		config.setSASLAuthenticationEnabled(false);
		config.setSecurityMode(SecurityMode.disabled);
		 	         
		connection = new XMPPConnection(config);
		
		new Thread(new Runnable() {
		 	@Override public void run() {
		 			try {
		 				connection.connect();
		 				Thread.sleep(50);
		 				connection.login(username, password);
		 				chatManager = connection.getChatManager();
		 				setStatus(true, "Hello everyone");
		 				createEntry("testuser2", "testuser2");
		 				System.out.println("Connected: " + connection.isConnected());
		 			} catch (XMPPException e) {
		 				Log.e("ERROR", "Failed to connect to XMPP server"); 
		 				e.printStackTrace();
		 			} catch (Exception e) {
		 				Log.e("ERROR", e.getMessage());
		 				e.printStackTrace();
		 			}
	        }
	    }).start();
		messageListener = new MyMessageListener();
		
		new Thread(new Runnable() {
			@Override public void run() {
				roster = connection.getRoster();
				roster.addRosterListener(new RosterListener() {
					public void entriesAdded(Collection<String> addresses) {}
				    public void entriesDeleted(Collection<String> addresses) {}
				    public void entriesUpdated(Collection<String> addresses) {}
				    public void presenceChanged(final Presence presence) {
				    	parent.runOnUiThread(new Runnable() {
				    	    public void run() {
				    	    	String text = "Presence changed: " + presence.getFrom() + " " + presence;
				    	        Toast.makeText(parent, text, Toast.LENGTH_LONG).show();
				    	    }
				    	});
				    }
				});
				System.out.println("Got roster");
			}
		}).start();
	} //init()
	
	class MyMessageListener implements MessageListener {
		@Override
		public void processMessage(Chat chat, Message message) {
			final String from = message.getFrom();
			final String body = message.getBody();
			Log.i("DEBUG", String.format("Received message '%1$s' from %2$s", body, from));
			Log.i("DEBUG", "Constructing a chat message...");
			parent.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					String prefix = " " + from + ": ";
					parent.addNewChatMessage(body, prefix, Color.GREEN);
				}
			});
		}
	}
	
	public void performLogin(String username, String password) throws XMPPException {
		if (connection!=null && connection.isConnected()) {
		   connection.login(username, password);
		}
	}
	
	public void setStatus(boolean available, String status) {
		Presence.Type type = available? Type.available: Type.unavailable;
		Presence presence = new Presence(type);
		presence.setStatus(status);
		connection.sendPacket(presence);
	}
	
	public void destroy() {
		if (connection!=null && connection.isConnected()) {
			connection.disconnect();
		}
	}
	
	public void sendMessage(String message, String buddyJID) throws XMPPException {
		System.out.println(String.format("Sending mesage '%1$s' to user %2$s", message, buddyJID));
		Chat chat = chatManager.createChat(buddyJID, messageListener);
		chat.sendMessage(message);
	}
	
	public void createEntry(String user, String name) throws Exception {
		System.out.println(String.format("Creating entry for buddy '%1$s' with name %2$s", user, name));
		Roster roster = connection.getRoster();
		roster.createEntry(user, name, null);
	}
	
	public Collection<RosterEntry> getRosterEntries() {
		return roster.getEntries();
	}
	
	public void sendChatMessage(final CharSequence msg) {
		if (msg == null || msg.length() == 0)
			return;
		
		try {
			sendMessage((String) msg, username + server);
		    Log.i("DEBUG", "Message " + msg + " is sent!");
		}
		catch (XMPPException ex) {
		    System.out.println("Error Delivering block");
		}
	}

}
