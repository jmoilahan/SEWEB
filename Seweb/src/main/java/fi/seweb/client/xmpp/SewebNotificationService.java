package fi.seweb.client.xmpp;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.AndroidConnectionConfiguration;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import fi.seweb.R;
import fi.seweb.client.app.BuddyListView;
import fi.seweb.client.common.PresenceStatus;
import fi.seweb.client.common.SewebPreferences;
import fi.seweb.client.core.Buddy;
import fi.seweb.client.core.MessageStorage;
import fi.seweb.client.core.RosterCache;

public class SewebNotificationService extends Service {
	private static final String TAG = "SewebNotificationService";
	
	/* worker thread's queue where tasks are submitted to */
	private ExecutorService taskQueue = Executors.newSingleThreadExecutor();
	
	/* databases */
	private MessageStorage mMessageDatabase;
	private RosterCache mRosterDatabase;
	
	/* String = full JID (RemoteUserID), e.g. "joni_pc@seweb.p1.im" Chat = a chat's instance */
	private final HashMap<String, Chat> mChats = new HashMap<String, Chat>();
	
	private final RemoteCallbackList<IXMPPRosterCallback> mRosterCallbacks = new RemoteCallbackList<IXMPPRosterCallback>();
	private final RemoteCallbackList<IXMPPChatCallback> mChatCallbacks = new RemoteCallbackList<IXMPPChatCallback>();
	
	private boolean mBuddyViewBound = false;
	private boolean mChatViewBound = false;

	private NotificationManager mNM;
	
	private XMPPConnection mXmppConnection;
	
	private SewebPreferences mConfig;
	
	// a workaround to keep the connection alive
	// http://stackoverflow.com/questions/13080535/how-to-keep-a-xmpp-connection-stable-on-android-with-asmack
	static {
		    try {
		        Class.forName("org.jivesoftware.smack.ReconnectionManager");
		    } catch (ClassNotFoundException ex) {
		        Log.e(TAG, "Failed to load ReconnectionManager");
		    }
	}
	
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

			@Override
			public void connect() throws RemoteException {
				Log.i(TAG, "connecting manually");
				//mHandler.post(new Runnable() {
				taskQueue.execute(new Runnable() {
					@Override
					public void run() {
						try {
							connect();
						} catch (RemoteException e) {
							Log.e(TAG, e.getMessage());
						}
					}
				});
			}

			@Override
			public void updatePresence(final int presenceCode, final String statusMsg)
					throws RemoteException {
				Log.i(TAG, "Setting own presence: " + presenceCode + " : " + statusMsg );
				taskQueue.execute(new Runnable() {
					@Override
					public void run() {
						Presence presence = PresenceStatus.toPresence(presenceCode, statusMsg);
						
						if (presence.getType() == Presence.Type.unavailable) {
							mRosterDatabase.setPresenceAllOffline();
						}

						if (mXmppConnection != null && mXmppConnection.isConnected() && mXmppConnection.isAuthenticated()) {
								mXmppConnection.sendPacket(presence);
								Log.i(TAG, "Own presence updated: " + presence.getType() + " : " + statusMsg );
						}	
					}
				});
			}

			@Override
			public boolean isConnected() throws RemoteException {
				if (mXmppConnection != null && mXmppConnection.isConnected()) {
					return true;
				}
				return false;
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
		
		@Override
		public void sendMessage(final String toUser, final String message) throws RemoteException {
			Log.i(TAG, "sendMessage() called");
			
			taskQueue.execute(new Runnable () {
				@Override
				public void run() {
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

						String myJID = mConfig.getMyFullJid(); 
						Message xmppMessage = new Message();
						xmppMessage.setBody(message);
						xmppMessage.setFrom(myJID); // from us
						xmppMessage.setTo(toUser);
						xmppMessage.setThread(chat.getThreadID());
						
						try {
							chat.sendMessage(xmppMessage);
							Log.i(TAG, "message sent: " + message);
						} catch (XMPPException e) {
							Log.e(TAG, "Failed to send the message " + e.getMessage());
						}
						mMessageDatabase.addMessage(chat.getThreadID(), xmppMessage);
					} else { // connection is not available
						// TODO:
						// register an offline message to send it later
					}
				}
			});
		}
		
		@Override
		public boolean isAuthenticated() throws RemoteException {
			if (mXmppConnection != null && mXmppConnection.isConnected()) {
				return mXmppConnection.isAuthenticated();
			}
			return false;
		}
		
		@Override
		public void clearNotifications(final String jid) throws RemoteException {
			Log.i(TAG, "clearNotifications: " + jid);
			if (jid == null || jid.length() == 0 || !fi.seweb.client.common.StringUtils.isValidJid(jid))
				return;
			
			taskQueue.execute(new Runnable () {
				@Override
				public void run() {
					// update the roster cache
					mRosterDatabase.setPresence(jid, false);
				}
			});
		}
	};
	
	private void connect() {
		taskQueue.execute(new Runnable () {
			@Override
			public void run() {
				if (mXmppConnection == null || mXmppConnection.isConnected() || mXmppConnection.isAuthenticated()) {
					return; // ignore if the connection object does not exist or if already connected
				}
				try {
					mXmppConnection.connect();
					mXmppConnection.login(mConfig.getUsername(), mConfig.getPassword());
				} catch (XMPPException e) {
					Log.e(TAG, "Could not connect to " + mConfig.getDomain(), e);
				}

				if (!mXmppConnection.isConnected()) {
			 		Log.e(TAG, "Could not connect to " + mConfig.getDomain() + " server");
			 	} 
			}
		});
	}
	
	//terminates the connection and all listeners
	private void disconnect() {
		if (mXmppConnection != null && mXmppConnection.isConnected()) {
			mXmppConnection.disconnect();
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "onBind() called");
		
		// Check the intent and return the correct interface
        if (IXMPPRosterService.class.getName().equals(intent.getAction())) {
        	Log.i(TAG, "Bound as " + intent.getAction());
        	//Toast.makeText(this, "Bound as " + intent.getAction(), Toast.LENGTH_SHORT).show();
        	mBuddyViewBound = true;
            return mRoster2ServiceBinder;
            
        } else if (IXMPPChatService.class.getName().equals(intent.getAction())) {
        	Log.i(TAG, "Bound as " + intent.getAction());
        	//Toast.makeText(this, "Bound as " + intent.getAction(), Toast.LENGTH_SHORT).show();
        	mChatViewBound = true;
            return mChat2ServiceBinder;
        }
        return null; // do not allow binding for other services
	}
	
	@Override
	public void onRebind(Intent intent) {
		Log.i(TAG, "onRebind() called");
		super.onRebind(intent);
		
		// Check the intent and return the correct interface
        if (IXMPPRosterService.class.getName().equals(intent.getAction())) {
        	Log.i(TAG, "Rebound as " + intent.getAction());
        	//Toast.makeText(this, "ReBound as " + intent.getAction(), Toast.LENGTH_SHORT).show();
        	mBuddyViewBound = true;

        } else if (IXMPPChatService.class.getName().equals(intent.getAction())) {
        	Log.i(TAG, "Rebound as " + intent.getAction());
        	//Toast.makeText(this, "ReBound as " + intent.getAction(), Toast.LENGTH_SHORT).show();
        	mChatViewBound = true;
        }
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(TAG, "onUnbind() called");
		
		// Check the intent and return the correct interface
        if (IXMPPRosterService.class.getName().equals(intent.getAction())) {
        	Log.i(TAG, "Unbinding" + intent.getAction());
        	//Toast.makeText(this, "Unbinding" + intent.getAction(), Toast.LENGTH_SHORT).show();
        	mBuddyViewBound = false; // all buddy view clients were detached
        }
        
        if (IXMPPChatService.class.getName().equals(intent.getAction())) {
        	Log.i(TAG, "Unbinding: " + intent.getAction());
        	//Toast.makeText(this, "Unbinding: " + intent.getAction(), Toast.LENGTH_SHORT).show();
        	mChatViewBound = false; // all chat view clients were detached
        }
        return true; 
	}
	
	@Override
	public void onCreate() {
		Log.i(TAG, "onCreate() called");
		super.onCreate();
		SmackAndroid.init(this.getApplicationContext());
		
		mConfig = new SewebPreferences(PreferenceManager
				.getDefaultSharedPreferences(this)); 
		
		mRosterDatabase = new RosterCache(getApplicationContext());
		mMessageDatabase =  new MessageStorage(getApplicationContext(), mConfig.getMyFullJid());
		
		// Display a global notification
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        showNotification();
	}
	
	@Override 
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		Log.i(TAG, "onStartCommand() called");
		
		//mHandler.post(new Runnable() {
		new Thread (new Runnable() {
		 	@Override public void run() {
		 		AndroidConnectionConfiguration config = null;
		 		
		 		try {
					config = new AndroidConnectionConfiguration(
					        mConfig.getDomain(), mConfig.getPort());
					config.setReconnectionAllowed(true); // changed to false based on this problem:
					// http://stackoverflow.com/questions/19117218/reconnectionmanager-in-asmack
					// also check the code in Yaxim
			 		config.setSASLAuthenticationEnabled(true);
			 		config.setRosterLoadedAtLogin(true);
			 		
			 		if (Build.VERSION.SDK_INT >= 14 /*Build.VERSION_CODES.ICE_CREAM_SANDWICH*/) {
			 	        config.setTruststoreType("AndroidCAStore");
			 	        config.setTruststorePassword(null);
			 	        config.setTruststorePath(null);
			 	        System.out.println("ICE_CREAM_SANDWICH");
			 	    } else {
			 	        config.setTruststoreType("BKS");
			 	        String path = System.getProperty("javax.net.ssl.trustStore");
			 	        path = System.getProperty("java.home") + File.separator + "etc"
			 	                + File.separator + "security" + File.separator
			 	                + "cacerts.bks";
			 	        config.setTruststorePath(path);
			 	        System.out.println("NO ICE_CREAM_SANDWICH");
			 	    }
			 		
				} catch (XMPPException e) {
					Log.e(TAG, "Error while creating an android configuration " + e.getMessage());
					return;
				} catch (NullPointerException e) {
					Log.e(TAG, "Error while creating an android configuration " + e.getMessage());
					return;
				}
		 		
		 		SASLAuthentication.supportSASLMechanism("PLAIN");
		 		SmackConfiguration.setDefaultPingInterval(5000);
		 		mXmppConnection = new XMPPConnection(config);
		 		mXmppConnection.getRoster().addRosterListener(new SewebRosterListener());
		 		mXmppConnection.addConnectionListener(new SewebConnectionListener());
		 		mXmppConnection.getChatManager().addChatListener(new SewebChatManagerListener());
		 		PingManager.getInstanceFor(mXmppConnection);
		 		
		 		//need to populate the buddy list here
		 		connect();
		 		bulkUpdateRosterCache();
		 	}
		 //});
		}).start();
		return Service.START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy() called");
		super.onDestroy();
		// cleanup: kill threads, registered listeners, receivers
		
		new Thread(new Runnable() {
			@Override public void run() {
				disconnect();
			}
		}).start();
		
		// Tell the user we stopped.
        //Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
	    
		// Cancel the persistent notification.
        mNM.cancel(R.string.new_message_ticker);

        // Unregister all callbacks.
        mRosterCallbacks.kill();
        mChatCallbacks.kill();
        
        shutdownAndAwaitTermination(taskQueue);
	}
	
	// gracefully shutting down the executor (as recommended by Google)
	void shutdownAndAwaitTermination(ExecutorService executor) {
	   executor.shutdown(); // Disable new tasks from being submitted
	   try {
	     // Wait a while for existing tasks to terminate
	     if (!executor.awaitTermination(50, TimeUnit.MILLISECONDS)) {
	       executor.shutdownNow(); // Cancel currently executing tasks
	       // Wait a while for tasks to respond to being cancelled
	       if (!executor.awaitTermination(50, TimeUnit.MILLISECONDS)) {
	           Log.e(TAG, "Pool did not terminate");
	       }
	     }
	   } catch (InterruptedException ie) {
	     // (Re-)Cancel if current thread also interrupted
	     executor.shutdownNow();
	     // Preserve interrupt status
	     Thread.currentThread().interrupt();
	   }
	}
	
	private final Handler mHandler = new Handler(Looper.getMainLooper()) {
		@Override public void handleMessage(android.os.Message msg) {
			Log.i(TAG, "handleMessage() called");
		}
	};
	
	/**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        CharSequence ticker = getText(R.string.remote_service_started);
        CharSequence title = getText(R.string.app_name);
        CharSequence content_text = mConfig.getMyFullJid();
        
        Intent toLaunch = new Intent(getApplicationContext(), BuddyListView.class);
        toLaunch.setAction(Intent.ACTION_MAIN);
        toLaunch.addCategory(Intent.CATEGORY_LAUNCHER);
        toLaunch.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        
        //also need to specify android:launchMode="singleTask" in the manifest so that duplicates are not running.
        
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, toLaunch, 0);
       
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this);
   
        // Set the info for the views that show in the notification panel.
        Notification notification = (nBuilder.setContentIntent(contentIntent))
        								.setSmallIcon(R.drawable.seweb_launcher_small)
        								.setTicker(ticker)
        								.setWhen(System.currentTimeMillis())
        								.setContentTitle(title)
        								.setContentText(content_text)
        								.build();
        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.new_message_ticker, notification);
    }
    
    /* Fetch all entries from the roster on startup */
   private void bulkUpdateRosterCache() {
	   taskQueue.execute(new Runnable() {
		   @Override
		   public void run() {
			   if (mXmppConnection == null || !mXmppConnection.isConnected() || 
					   !mXmppConnection.isAuthenticated() || mXmppConnection.getRoster() == null || 
					   mXmppConnection.getRoster().getEntryCount() == 0) {
				   return;
			   }
	   			
			   //kill all previous records
			   mRosterDatabase.clear();
			   
			   Collection <RosterEntry> entries = mXmppConnection.getRoster().getEntries();
			   if (!entries.isEmpty()) {
				   for (RosterEntry e : entries) {
					   String jid =  StringUtils.parseBareAddress(e.getUser());
					   String name = e.getName();
					   if (name == null || name.length() == 0) {
						   name = StringUtils.parseName(jid);
					   }
					   Buddy buddy = new Buddy(jid);
					   buddy.setName(name);
			 		
					   // fetch the presence: this is the way recommended by smack 
					   Presence p = mXmppConnection.getRoster().getPresence(jid);
					   Presence.Type type = p.getType();
					   if (type == Presence.Type.unavailable) {
						   buddy.setPresenceOffline();
					   } else {
						   Presence.Mode mode = p.getMode();
						   int presenceCode = PresenceStatus.getPresenceCode(type, mode); 
						   String status = p.getStatus();
						   if (status == null) { status = " "; }				
						   buddy.setPresence(status, presenceCode, false);
					   }
					
					   Collection<RosterGroup> groups = e.getGroups();
					   if (!groups.isEmpty()) {
						   for (RosterGroup g : groups) {
							   buddy.addToGroup((String) g.getName());
						   }
					   }
					   mRosterDatabase.addEntry(buddy);
				   }
			   }
		   }
	   });
	}
    
    private class SewebConnectionListener implements ConnectionListener {
    	@Override public void connectionClosed() {
    		Log.e(TAG, "CollectionClosed");
    		//Toast.makeText(SewebNotificationService.this, "Connection closed", Toast.LENGTH_SHORT).show();
    	}
		@Override public void connectionClosedOnError(Exception exception) {
			Log.e(TAG, "CollectionClosedOnError: " + exception.getMessage());
    		//Toast.makeText(SewebNotificationService.this, "Connection closed on error", Toast.LENGTH_SHORT).show();
		}
		@Override public void reconnectingIn(int time) {
			Log.e(TAG, "Reconnecting in: " + time);
    		//Toast.makeText(SewebNotificationService.this, "Reconnecting in: " + time, Toast.LENGTH_SHORT).show();
		}
		@Override public void reconnectionFailed(Exception exception) {
			Log.e(TAG, "ReconnectionFailed: " + exception.getMessage());
    		//Toast.makeText(SewebNotificationService.this, "ReconnectionFailed", Toast.LENGTH_SHORT).show();
		}
		@Override public void reconnectionSuccessful() {
			Log.e(TAG, "Reconnection Succesfull");
    		//Toast.makeText(SewebNotificationService.this, "Reconnection Succesfull", Toast.LENGTH_SHORT).show();
		}
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
			final String user = StringUtils.parseBareAddress(presence.getFrom());
			taskQueue.execute(new Runnable () {
				@Override 
				public void run() {
					// fetch the presence: this is the way recommended by smack 
					Presence newPresence = mXmppConnection.getRoster().getPresence(user);
					Presence.Type type = newPresence.getType();
					Presence.Mode mode = newPresence.getMode();
					int presenceCode = PresenceStatus.getPresenceCode(type, mode); 
					String status = newPresence.getStatus();
					if (status == null) { status = " "; }
					
					// cache the change locally
					mRosterDatabase.setPresence(user, status, type, mode);
					Log.i(TAG, "Presense changed: " + user + ": " + getResources().getString(PresenceStatus.getTextId(presenceCode)));
				}
			});
		}
    }
    
    /* Listens for new chats */
    private class SewebChatManagerListener implements ChatManagerListener {
		@Override
		public void chatCreated(final Chat chat, final boolean createdLocally) {
			taskQueue.execute(new Runnable () {
				@Override
				public void run() {
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
			});
		}
    }
    
   /* 
    *  Listens for all messages in all chats
    */
    private class SewebChatMessageListener implements MessageListener {
    	@Override 
		public void processMessage(final Chat chat, Message message) {
			if (message.getBody() == null) 
				return;
			
			final Message msg = message;
			
			taskQueue.execute(new Runnable () {
				@Override
				public void run() {
					final String from = StringUtils.parseBareAddress(chat.getParticipant());
					Log.i(TAG, "New message from: '" + from);
					
					// store the message for future retrieval
					final String chatID = chat.getThreadID();
					mMessageDatabase.addMessage(chatID, msg);
					Log.i(TAG, "Message stored: " + msg.getBody());
					
					// the chat is currently opened in the view
					boolean isCurrentChat = false;
	
					// we ignore notifications if the chat is opened in a view
					if (!isCurrentChat) {
						// update the roster cache
						mRosterDatabase.setPresence(from, true);
					}
				}
			});
		} // processMessage
	} // inner class
}

