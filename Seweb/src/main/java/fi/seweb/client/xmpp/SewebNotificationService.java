package fi.seweb.client.xmpp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.jivesoftware.smack.AndroidConnectionConfiguration;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ping.PingManager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import fi.seweb.R;
import fi.seweb.client.app.TotalKillView;
import fi.seweb.client.common.SewebPreferences;
import fi.seweb.client.core.MessageStorage;
import fi.seweb.client.core.ObjectConverter;
import fi.seweb.client.xmpp.ping.PingPacketFilter;
import fi.seweb.client.xmpp.ping.PingPacketListener;

public class SewebNotificationService extends Service {
	private static final String TAG = "SewebNotificationService";

	private RemoteCallbackList<IXMPPRosterCallback> mRosterCallbacks = new RemoteCallbackList<IXMPPRosterCallback>();
	private RemoteCallbackList<IXMPPChatCallback> mChatCallbacks = new RemoteCallbackList<IXMPPChatCallback>();
	
	private boolean mBuddyViewBound = false;
	private boolean mChatViewBound = false;
	
	private final Handler mHandler = new Handler();

	private NotificationManager mNM;
	
	private XMPPConnection mXmppConnection;
	
	/* Mockup of a message database */
	// String = chat id
	//private HashMap<String, Queue<Message>> mMessages = new HashMap<String, Queue<Message>>();
	MessageStorage mMessages = new MessageStorage();
	
	/* String = full JID (RemoteUserID), e.g. "joni_pc@seweb.p1.im" 
	 * Chat = object*/
	private HashMap<String, Chat> mChats = new HashMap<String, Chat>();
		
	private final IXMPPRosterService.Stub mRoster2ServiceBinder = new IXMPPRosterService.Stub() {
			@Override
			public void registerRosterCallback(IXMPPRosterCallback callback)
					throws RemoteException {
				if (callback != null) {
					mRosterCallbacks.register(callback);
				}
			}
			
			@Override
			public void unregisterRosterCallback(IXMPPRosterCallback callback)
					throws RemoteException {
				if (callback != null) {
					mRosterCallbacks.unregister(callback);
				}
			}
	};

	private final IXMPPChatService.Stub mChat2ServiceBinder = new IXMPPChatService.Stub() {
		
		@Override
		public void registerChatCallback(IXMPPChatCallback callback)
				throws RemoteException {
			Log.i(TAG, "registerChatCallback() called");
			if (callback != null) {
				mChatCallbacks.register(callback);
			}
		}
		
		@Override
		public void unregisterChatCallback(IXMPPChatCallback callback)
				throws RemoteException {
			Log.i(TAG, "unRegisterChatCallback() called");
			if (callback != null) {
				mChatCallbacks.unregister(callback);
			}
		}
		
		/* this method is called when we send our message out */
		/* @param toUser = full JID (e.g. "joni_pc@seweb.p1.im")
		 */
		@Override
		public void sendMessage(String toUser, String message) throws RemoteException {
			Log.i(TAG, "sendMessage() called");
			if (mXmppConnection != null && mXmppConnection.isConnected() && mXmppConnection.isAuthenticated()) {
				Chat chat = null;
				if (!mChats.isEmpty() && mChats.containsKey(toUser)) {
					chat = mChats.get(toUser);
					Log.i(TAG, "got an exising chat object");
				} else { // need to create a new chat
					chat = mXmppConnection.getChatManager().createChat(toUser, new SewebChatMessageListener());
					if(!mChats.containsKey(toUser)) {
						mChats.put(toUser, chat);
					}
					Log.i(TAG, "created a new chat object");
				}
				
				String myJID = SewebPreferences.USERNAME + "@" + SewebPreferences.DOMAIN;
				if (!fi.seweb.client.common.StringUtils.isValidJid(myJID)) {
					Log.e(TAG, "Not able to build a valid JID: " + myJID);
					//throw new IllegalStateException("Error while preparing a message to send");
				}
				
				Message xmppMessage = new Message();
				xmppMessage.setBody(message);
				xmppMessage.setFrom(myJID); // from us
				xmppMessage.setTo(toUser);
				xmppMessage.setThread(chat.getThreadID());
				
				try {
					//chat.sendMessage(message);
					chat.sendMessage(xmppMessage);
				} catch (XMPPException e) {
					Log.e(TAG, "Failed to send the message " + e.getMessage());
				}
				
				//storing the message for future retrieval
				mMessages.addMessage(chat.getThreadID(), xmppMessage);
				
			} else { // connection is not available
				// TODO:
				// register an offline message
				// it will be sent later once the connection is available
			}
		}
		
		@Override
		public boolean isAuthenticated() throws RemoteException {

			if (mXmppConnection != null && mXmppConnection.isConnected()) {
				return mXmppConnection.isAuthenticated();
			}
			return false;
		}
		
		@Override
		public void clearNotifications(String jid) throws RemoteException {}

		@Override
		public String obtainLastMessage(String jid) throws RemoteException {
			Log.i(TAG, "obtainLastMessage called: " + jid);
			if (jid == null || jid.length() == 0 || !fi.seweb.client.common.StringUtils.isValidJid(jid))
				return "";
			
			//Message lastMessage = null;
			
			ArrayList<Message> messageArray = new ArrayList<Message>();
			
			// are there existing chats with this user?
			if (mChats.containsKey(jid)) {
				Log.i(TAG, "obtainLastMessage: " + "Chat exits");
				String chatID = mChats.get(jid).getThreadID();
				
				if (mMessages.hasMessagesFor(chatID)) {
					//retrieve messages
					messageArray = mMessages.retrieveMessage(chatID);
					//lastMessage = mMessages.getLast(chatID);
				}
			}
			
			if (messageArray.isEmpty()) {
				Log.i(TAG, "obtainLastMessage: no messages for " + jid);
				return "{}";
			}
			/*if (lastMessage == null) {
				Log.i(TAG, "obtainLastMessage: " + "lastMessage is null");
				return "";
			}*/
			return ObjectConverter.toJSONArray(messageArray);
		}
	};
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "onBind() called");

		// Check the intent and return the correct interface
        if (IXMPPRosterService.class.getName().equals(intent.getAction())) {
        	Log.i(TAG, "Bound as " + intent.getAction());
        	Toast.makeText(this, "Bound as " + intent.getAction(), Toast.LENGTH_SHORT).show();
        	mBuddyViewBound = true;
            return mRoster2ServiceBinder;
            
        } else if (IXMPPChatService.class.getName().equals(intent.getAction())) {
        	Log.i(TAG, "Bound as " + intent.getAction());
        	Toast.makeText(this, "Bound as " + intent.getAction(), Toast.LENGTH_SHORT).show();
        	mChatViewBound = true;
            return mChat2ServiceBinder;
        }
        return null; // do not allow binding for other services
	}
	
	@Override
	public void onRebind(Intent intent) {
		Log.i(TAG, "onRebind() called");
		super.onRebind(intent);
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(TAG, "onUnbind() called");
		
		// Check the intent and return the correct interface
        if (IXMPPRosterService.class.getName().equals(intent.getAction())) {
        	Log.i(TAG, "Unbinding" + intent.getAction());
        	Toast.makeText(this, "Unbinding" + intent.getAction(), Toast.LENGTH_SHORT).show();
        	mBuddyViewBound = false; // all buddy view clients were detached
        }
        
        if (IXMPPChatService.class.getName().equals(intent.getAction())) {
        	Log.i(TAG, "Bound as " + intent.getAction());
        	Toast.makeText(this, "Bound as " + intent.getAction(), Toast.LENGTH_SHORT).show();
        	mChatViewBound = false; // all chat view clients were detached
        }

        return false; // we don't want the onRebind() to be called.
	}
	
	@Override
	public void onCreate() {
		// performs one-time setup procedures, before the onStartCommand (or onBind) is called
		Log.i(TAG, "onCreate() called");
		super.onCreate();
		SmackAndroid.init(this.getApplicationContext());
		
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		// Display a notification about us starting.
        showNotification();
	}
	
	@Override 
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		Log.i(TAG, "onStartCommand() called");
		
		new Thread(new Runnable() {
		 	@Override public void run() {

		 		AndroidConnectionConfiguration config = null;
				
		 		try {
					config = new AndroidConnectionConfiguration(
					        SewebPreferences.DOMAIN, SewebPreferences.PORT);
					config.setReconnectionAllowed(true);
			 		config.setSASLAuthenticationEnabled(true);
			 		config.setRosterLoadedAtLogin(true);
				} catch (XMPPException e) {
					Log.e(TAG, "Error while creating an android configuration " + e.getMessage());
				} catch (NullPointerException e) {
					Log.e(TAG, "Error while creating an android configuration " + e.getMessage());
				}
		 		
		 		SASLAuthentication.supportSASLMechanism("PLAIN");
		 		SmackConfiguration.setDefaultPingInterval(5000);
		 		mXmppConnection = new XMPPConnection(config);
		 		try {
		 			Log.i(TAG, "Attempting to connect to XMPP Server " + SewebPreferences.DOMAIN);
		 			mXmppConnection.connect();
		 			PingManager.getInstanceFor(mXmppConnection);
		 			mXmppConnection.getRoster().addRosterListener(new SewebRosterListener());
		 			mXmppConnection.addPacketListener(new PingPacketListener(mXmppConnection), new PingPacketFilter());
		 			mXmppConnection.addConnectionListener(new SewebConnectionListener());
		 			mXmppConnection.login(SewebPreferences.USERNAME, SewebPreferences.PASSWORD);

		 			Log.i(TAG, "Connected to " + SewebPreferences.DOMAIN + " server as " + SewebPreferences.USERNAME);
		 			//Toast.makeText(SewebNotificationService.this, "XMPP Server connection established", Toast.LENGTH_SHORT).show();
		 			
		 			mXmppConnection.getChatManager().addChatListener(new SewebChatManagerListener());
		 		}
		 		catch (final XMPPException e) {
		 			Log.e(TAG, "Could not connect to " + SewebPreferences.DOMAIN + " server", e);
		 			//Toast.makeText(SewebNotificationService.this, "Connection error", Toast.LENGTH_SHORT).show();
		 			return;
		 		}
		 		
		 		if (!mXmppConnection.isConnected()) {
		 			Log.e(TAG, "Could not connect to " + SewebPreferences.DOMAIN + " server");
		 			//Toast.makeText(SewebNotificationService.this, "Connection error", Toast.LENGTH_SHORT).show();
		 			return;
		 		}
		 	}
		 }).start();

		return Service.START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy() called");
		super.onDestroy();
		// cleanup: kill threads, registered listeners, receivers
		// this is the last method call 
		
		new Thread(new Runnable() {
			@Override public void run() {
				if (mXmppConnection != null && mXmppConnection.isConnected()) {
					mXmppConnection.disconnect();
				}
			}
		}).start();
		
		// Tell the user we stopped.
        Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
		
		// Cancel the persistent notification.
        mNM.cancel(R.string.new_message_ticker);

        // Unregister all callbacks.
        mRosterCallbacks.kill();
	}
	
	/**
     * Show a notification while this service is running.
     */
	// TODO:
	// should point to a chat with a concrete user.
	// parameters: chat_id?
    private void showNotification() {
        CharSequence ticker = getText(R.string.remote_service_started);
        CharSequence title = getText(R.string.new_message_ticker); // "new messages"
        CharSequence content_text = "Seweb App";
       
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, TotalKillView.class), 0);
       
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this);
        
        // Set the icon, scrolling text and timestamp
        // Set the info for the views that show in the notification panel.
        Notification notification = nBuilder.setContentIntent(contentIntent)
        								.setSmallIcon(R.drawable.stat_sample)
        								.setTicker(ticker)
        								.setWhen(System.currentTimeMillis())
        								.setAutoCancel(true)
        								.setContentTitle(title)
        								.setContentText(content_text)
        								.build();
        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.new_message_ticker, notification);
    }
    
    private class SewebConnectionListener implements ConnectionListener {
    	@Override public void connectionClosed() {
			//CONNECT Manually
		}
		@Override
		public void connectionClosedOnError(Exception exception) {
			//CONNECT Manually
		}
		@Override public void reconnectingIn(int arg0) {}
		@Override public void reconnectionFailed(Exception arg0) {}
		@Override public void reconnectionSuccessful() {}
    }
    
    private class SewebRosterListener implements RosterListener {
		@Override
		public void entriesAdded(Collection<String> arg0) {}
		@Override
		public void entriesDeleted(Collection<String> arg0) {}
		@Override
		public void entriesUpdated(Collection<String> arg0) {}
		@Override
		public void presenceChanged(Presence presence) {
			Log.i(TAG, "Presense changed: " + StringUtils.parseBareAddress(presence.getFrom()));
			Log.i(TAG, "Type: " + presence.getType() + " Mode: " + presence.getMode());
			//do not broadcast if the buddy view is not bound
			if (!mBuddyViewBound) {
				return;
			}
			
			final String user = StringUtils.parseBareAddress(presence.getFrom());
			int presenceCode = SewebPreferences.ERROR;
			if (presence.getType() == Presence.Type.available) {//online
				Presence.Mode mode = presence.getMode();
				if (mode == null) { // custom mode, set by the remote user
					presenceCode = SewebPreferences.PRESENCE_AVAILABLE;
				} else { // standard mode
					presenceCode = SewebPreferences.presenceModeAsInt(presence.getMode());
					Log.i(TAG, "PresenceCode: " + presenceCode);
				}
			} else if (presence.getType() == Presence.Type.unavailable) { //offline
				presenceCode = SewebPreferences.PRESENCE_OFFLINE;
			}
			
			String statusTmp = presence.getStatus();
			if (statusTmp == null) { statusTmp = ""; }
			
			final int code = presenceCode;
			final String status = statusTmp;
			
			/* Use handler to process the callbacks */
			mHandler.post(new Runnable() {
				@Override public void run() {
					synchronized (this) { 
						final int broadCastItems = mRosterCallbacks.beginBroadcast();
						for (int i = 0; i < broadCastItems; i++) {
							try {
								mRosterCallbacks.getBroadcastItem(i).presenceChanged(user, code, status);
							} catch (RemoteException e) {
								Log.e(TAG, "RemoteException: " + e.getMessage());
							}
						}
						mRosterCallbacks.finishBroadcast();
					}
				}
			});
		}
    }
    
    /* Listens for new chats */
    private class SewebChatManagerListener implements ChatManagerListener {
		@Override
		public void chatCreated(Chat chat, boolean createdLocally) {
			String user = StringUtils.parseBareAddress(chat.getParticipant());
			
			if (createdLocally) {
				Log.i(TAG, "New local chat created with: " + user );
			} else {
				Log.i(TAG, "New remote chat created with: " + user );
			}
				
			if (!createdLocally) {
					chat.addMessageListener(new SewebChatMessageListener());
					if (!mChats.containsKey(chat.getThreadID())) {
						mChats.put(user, chat);
					}
			}
		}
    }
    
   /* 
    *  Listens for all messages sent in a chat
    *  regardless if the chat was created by us 
    *  or by a remote user
    */
    private class SewebChatMessageListener implements MessageListener {
    	
		@Override 
		public void processMessage(Chat chat, Message message) {
			if (message.getBody() == null) 
				return;
			
			Log.i(TAG, "New incoming chat message from: '" + StringUtils.parseBareAddress(chat.getParticipant()));
			Log.i(TAG, "message:  '" + message.getBody());
			Log.i(TAG, "Chat ID: '" + chat.getThreadID());
			
			// store the message for future retrieval
			final String chatID = chat.getThreadID();
			mMessages.addMessage(chatID, message);
			Log.i(TAG, "Message stored");

			// if a chat view is bound
			// send the message through the chatcallback
			// use the handler
			if (mChatViewBound) {
				final String body = message.getBody();
				mHandler.post(new Runnable () {
					@Override public void run() {
						final int broadcastItems = mChatCallbacks.beginBroadcast();
						for (int i = 0; i < broadcastItems; i++) {
							try {
								mChatCallbacks.getBroadcastItem(i).newChatMessageReceived(chatID, body);
								Log.i(TAG, "Chat Callback Broadcast " + "newChatMessageReceived()");
							} catch (RemoteException e) {
								Log.e(TAG, "RemoteException: " + e.getMessage());
							} 
						}
						mChatCallbacks.finishBroadcast();
				}});
			}
			
			// else, if the chat window is not opened -> 
			// add a new notification to the 
			// (1) tray
				
			// (2) Roster list
			if (mBuddyViewBound) {
				final String user = StringUtils.parseBareAddress(message.getFrom());
				mHandler.post( new Runnable() {
					@Override public void run() {
						final int broadcastItems = mRosterCallbacks.beginBroadcast();
						for (int i = 0; i < broadcastItems; i++) {
							try {
								mRosterCallbacks.getBroadcastItem(i).newChatMessageReceived(user, chatID);
								Log.i(TAG, "Rooster Callback BROADCAST! " + "newChatMessageReceived()");
							} catch (RemoteException e) {
								Log.e(TAG, "RemoteException: " + e.getMessage());
							}
						}
						mRosterCallbacks.finishBroadcast();
					} // run
				}); // post
			} // if
		} // processMessage
	} // inner class
}

