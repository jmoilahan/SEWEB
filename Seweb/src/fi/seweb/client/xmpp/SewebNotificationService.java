package fi.seweb.client.xmpp;

import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import fi.seweb.R;
import fi.seweb.client.app.TotalKillView;
import fi.seweb.client.common.SewebPreferences;

public class SewebNotificationService extends Service {
	private final String TAG = "SewebNotificationService";
	
	private RemoteCallbackList<IXMPPRosterCallback> mRosterCallbacks = new RemoteCallbackList<IXMPPRosterCallback>();
	private NotificationManager mNM;
	private XMPPConnection mXmppConnection;
		
	private final IXMPPRosterService.Stub mRoster2ServiceBinder = new IXMPPRosterService.Stub() {
			@Override
			public void registerRosterCallback(IXMPPRosterCallback callback)
					throws RemoteException {
				if (callback != null)
					mRosterCallbacks.register(callback);
			}
			
			@Override
			public void unregisterRosterCallback(IXMPPRosterCallback callback)
					throws RemoteException {
				if (callback != null)
					mRosterCallbacks.unregister(callback);
			}
			
			@Override
			public void disconnect() throws RemoteException {
				//check the connection's status and disconnect
			}
			
			@Override
			public void connect() throws RemoteException {
				//connect and login to the XMPP server in a new thread
				new Thread(new Runnable() {
				 	@Override public void run() {
				 		mXmppConnection = new XMPPConnection(SewebPreferences.DOMAIN);
				 		try {
				 			mXmppConnection.connect();
				 			mXmppConnection.login(SewebPreferences.USERNAME, SewebPreferences.PASSWORD);
				 			Log.i(TAG, "Connected to" + SewebPreferences.DOMAIN + " server" + " as " + SewebPreferences.USERNAME);
				 			Toast.makeText(SewebNotificationService.this, "Connected to XMPP Server", Toast.LENGTH_SHORT).show();
				 		}
				 		catch (final XMPPException e) {
				 			Log.e(TAG, "Could not connect to " + SewebPreferences.DOMAIN + " server", e);
				 			Toast.makeText(SewebNotificationService.this, "Connected error", Toast.LENGTH_SHORT).show();
				 			return;
				 		}
				 		if (!mXmppConnection.isConnected()) {
				 			Log.e(TAG, "Could not connect to " + SewebPreferences.DOMAIN + " server");
				 			Toast.makeText(SewebNotificationService.this, "Connected error", Toast.LENGTH_SHORT).show();
				 			return;
				 		}
				 	}
				 }).start();
			}
	};

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "onBind() called");

		// Select the interface to return.  If your service only implements
        // a single interface, you can just return it here without checking
        // the Intent.
        if (IXMPPRosterService.class.getName().equals(intent.getAction())) {
        	Toast.makeText(this, "BOUND!", Toast.LENGTH_SHORT).show();
            return mRoster2ServiceBinder;
        }
        if (IXMPPChatService.class.getName().equals(intent.getAction())) {
        	//TODO: implement the secondary interface
            return null;
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
		return false;
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
		
		/*
		// start our own service to keep it in the background when unbound
		Intent intent = new Intent(this, GenericXMPPService.class);
		intent.setAction("Starting the XMPPSerivce from itself");
		startService(intent);
		*/
	}
	
	@Override 
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		Log.i(TAG, "onStartCommand() called");
		
		if (intent != null) {
			Log.i(TAG, intent.getAction());
		}
		return Service.START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy() called");
		super.onDestroy();
		// cleanup: kill threads, registered listeners, receivers
		// this is the last method call 
		
		if (mXmppConnection != null && mXmppConnection.isConnected()) {
			mXmppConnection.disconnect();
			Toast.makeText(SewebNotificationService.this, "Connected to XMPP Server", Toast.LENGTH_SHORT).show();
		}
		
		// Tell the user we stopped.
        Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
		
		// Cancel the persistent notification.
        mNM.cancel(R.string.remote_service_started);

        // Unregister all callbacks.
        mRosterCallbacks.kill();
	}
	
	/**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        CharSequence ticker = getText(R.string.remote_service_started);
        CharSequence title = getText(R.string.new_message_ticker);
        CharSequence content_text = "Bla-bla-bla";
       
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
        mNM.notify(R.string.remote_service_started, notification);
    }
}
