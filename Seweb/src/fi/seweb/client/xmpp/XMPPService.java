package fi.seweb.client.xmpp;

import java.util.ArrayList;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class XMPPService extends Service {
	private static final String TAG = "XMPPService";
	private static final String DOMAIN = "seweb.p1.im";
	private static final String USERNAME = "testuser1";
	private static final String PASSWORD = "1";
	private ArrayList<Chat> chats = new ArrayList<Chat>();
	private Handler handler;
	private XMPPConnection mXmppConnection;

	@Override public IBinder onBind(final Intent intent) {
		return new LocalBinder<XMPPService>(this);
	}

	@Override public void onCreate() {
		super.onCreate();
		SmackAndroid.init(this.getApplicationContext());
		
		//connect and login to the XMPP server in a new thread
		new Thread(new Runnable() {
		 	@Override public void run() {
		 		mXmppConnection = new XMPPConnection(DOMAIN);
		 		try {
		 			mXmppConnection.connect();
		 			mXmppConnection.login(USERNAME, PASSWORD);
		 			Log.i(TAG, "Connected to" + DOMAIN + " server" + " as " + USERNAME);
		 		}
		 		catch (final XMPPException e) {
		 			Log.e(TAG, "Could not connect to " + DOMAIN + " server", e);
		 			return;
		 		}
		 		if (!mXmppConnection.isConnected()) {
		 			Log.e(TAG, "Could not connect to " + DOMAIN + " server");
		 			return;
		 		}
		 		
		 		mXmppConnection.getChatManager().addChatListener(new ChatManagerListener() {
		 			@Override
		 			public void chatCreated(final Chat chat, final boolean createdLocally) {
		 				if (!createdLocally) {
		 					chat.addMessageListener(new XMPPMessageListener());
		 					if (!chats.contains(chat))
		 						chats.add(chat);
		 				}
		 			}
		 		});
		 	}
		 }).start();
	}

	@Override public int onStartCommand(final Intent intent, final int flags, final int startId) {
		return Service.START_NOT_STICKY;
	}

	@Override public boolean onUnbind(final Intent intent) {
		return super.onUnbind(intent);
	}

	@Override public void onDestroy() {
		super.onDestroy();
		mXmppConnection.disconnect();
	}
	
	public void registerHandler(Handler serviceHandler) {
	    handler = serviceHandler;
	}
	
	public boolean sendMessage(String msg) {
		if (!mXmppConnection.isConnected() || !mXmppConnection.isAuthenticated())
			return false;
		if (chats.isEmpty())
			return false;

		//TODO: which chat?
		Chat chat = chats.get(0);
		try {
			chat.sendMessage(msg);
			return true;
		} catch (XMPPException e) {
			Log.e(TAG, "Failed to send the message " + e.getMessage());
			return false;
		}
	}
	
	public static String getThreadSignature() {
		final Thread t = Thread.currentThread();
		return new StringBuilder(t.getName()).append("[id=").append(t.getId()).append(", priority=")
				.append(t.getPriority()).append("]").toString();
	}
	
	private class XMPPMessageListener implements MessageListener {
		@Override public void processMessage(final Chat chat, final Message message) {
			if (message.getBody() != null) {
				Log.i(TAG, "Xmpp message received: '" + message.getBody());
				if (handler != null) {
					android.os.Message msg = handler.obtainMessage(0, message);
					handler.sendMessage(msg);
				}
			}
		}
	}
	
}
