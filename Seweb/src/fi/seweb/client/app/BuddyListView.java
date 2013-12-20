package fi.seweb.client.app;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;
import fi.seweb.R;
import fi.seweb.client.common.SewebPreferences;
import fi.seweb.client.common.UserPresence;
import fi.seweb.client.xmpp.IXMPPChatService;
import fi.seweb.client.xmpp.IXMPPRosterCallback;
import fi.seweb.client.xmpp.IXMPPRosterService;

public class BuddyListView extends Activity {
	
	private ListView mainListView;  
	private UserPresenceAdapter presenceAdapter;
	private IXMPPRosterService mRosterService = null;
	//private IXMPPChatService mChatService = null;
	private static final String TAG = "BuddyListView";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_buddy_list);
		
		bindService(new Intent(IXMPPRosterService.class.getName()),
                mRosterConnection, Context.BIND_AUTO_CREATE);
		
		/*
		 * bindService(new Intent(IXMPPChatService.class.getName()),
                mChatConnection, Context.BIND_AUTO_CREATE);
		*/
		
		// Create and populate a list of roster entries 
	    UserPresence[] buddies = new UserPresence[] {};    
	    ArrayList<UserPresence> buddyList = new ArrayList<UserPresence>();  
	    buddyList.addAll(Arrays.asList(buddies));  
	      
	    // Create ArrayAdapter using the buddy list.  
	    presenceAdapter = new UserPresenceAdapter(this, R.layout.row_buddy_list, buddyList);
	    
	    // Find the ListView resource.   
	    mainListView = (ListView) findViewById(R.id.lvBuddyList);
	    mainListView.setAdapter(presenceAdapter);
	    mainListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {
				UserPresence presence = presenceAdapter.getItem(position);
				
				if (presence.hasUnreadMessages()) {
					//chat has been created alread
				}
				
				//clear the bold text and "new message" notification from the buddy view
				presenceAdapter.clearNotification(presence.user);
				//TODO: 
				//START THE CHAT HERE!!!
				
				final String item = presence.user;
				runOnUiThread(new Runnable() {
			        @Override
			        public void run() {
			            Toast.makeText(parent.getContext(), item, Toast.LENGTH_SHORT).show();
			        }
			    });
				
				Bundle b = new Bundle();
				b.putString("remoteJID", presence.user);
				
				Intent chatViewIntent = new Intent();
        		chatViewIntent.setClass(getApplicationContext(), ChatView.class);
				chatViewIntent.putExtras(b);
        		startActivity(chatViewIntent);
        		//finish();
			}
	    });
	    // Show this view if the list adapter is empty 
	    mainListView.setEmptyView(findViewById(R.id.tvEmptyView));
	}
	
	@Override
	public void onStart() {
		super.onStart();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.buddy_list, menu);
		return true;
	}
	
	
	/*
	 Class for interacting with the chat interface of the SewebNotificationService 
	
	private ServiceConnection mChatConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mChatService = IXMPPChatService.Stub.asInterface(service);
			
			Toast.makeText(BuddyListView.this, R.string.remote_service_connected,
                    Toast.LENGTH_SHORT).show();
		}
		
		public void onServiceDisconnected(ComponentName className) {
			mChatService = null;

			Toast.makeText(BuddyListView.this, R.string.remote_service_connected,
                    Toast.LENGTH_SHORT).show();
		}
	};
	*/
	
    /**
     * Class for interacting with the roster interface of the SewebNotificationService.
     */
    private ServiceConnection mRosterConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
        	// get a client-side representation from the raw service object.
            mRosterService = IXMPPRosterService.Stub.asInterface(service);

            try {
                mRosterService.registerRosterCallback(mCallback);
            } catch (RemoteException e) {
                // the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            	Log.e(TAG, e.getMessage());
            }
            
            // tell the user what happened.
            Toast.makeText(BuddyListView.this, R.string.remote_service_connected,
                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed (or stopped).
            mRosterService = null;
            
            // tell the user what happened.
            Toast.makeText(BuddyListView.this, R.string.remote_service_disconnected,
                    Toast.LENGTH_SHORT).show();
        }
    };
    
    /**
     * This implementation is used to receive callbacks from the SewebNotificationService.
     */
    private IXMPPRosterCallback mCallback = new IXMPPRosterCallback.Stub() {
        
    	/* Called when a roster item changes its status. The change has to be propagated to the 
    	 * corresponding UI component. This method is called dynamically by the SewebNotificationService
    	 * to update the UI.  
    	 * 
    	 * */
    	@Override
		public void presenceChanged(String user, int code, String status) throws RemoteException {
    		Log.i(BuddyListView.TAG, "presenceChanged() called");
    		Log.i(BuddyListView.TAG, user + " " + code + " " + status);
    		
    		UserPresence presence = new UserPresence(user, code, status);
    		Message msg = mHandler.obtainMessage();
    		msg.what = SewebPreferences.PRESENCE_CHANGED;
    		msg.obj = presence;
			mHandler.sendMessage(msg);
		}
    	
    	/*
    	 * This is called when the connection goes offline / online, hence the UI has to be updated.
    	 * @see fi.seweb.client.xmpp.IXMPPRosterCallback#connectionStatusChanged(int)
    	 */
		@Override
		public void connectionStatusChanged(int connectionStatusCode)
				throws RemoteException {
			Log.i(BuddyListView.TAG, "connectionStatusChanged() called");
    		Message msg = mHandler.obtainMessage();
    		msg.what = SewebPreferences.CONNECTION_CHANGED; 
    		msg.arg1 = connectionStatusCode;
			mHandler.sendMessage(msg);
		}
		
		/*
		 * (non-Javadoc)
		 * @see fi.seweb.client.xmpp.IXMPPRosterCallback#newChatMessageReceived(java.lang.String, java.lang.String)
		 * This is called when the buddy view activity is available to update the "new message" ticker next to the 
		 * buddy name
		 */
		@Override
		public void newChatMessageReceived(String fromJID, String chatID)
				throws RemoteException {
			Log.i(BuddyListView.TAG, "newChatMessageReceived() called");
			
			String[] args = new String[2];
			args[0] = fromJID;
			args[1] = chatID;
			
			Message msg = mHandler.obtainMessage();
			msg.what = SewebPreferences.NEW_CHAT_MESSAGE;
			msg.obj = args;
			mHandler.sendMessage(msg);
		}
    };
    
    private Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
        	Log.i(BuddyListView.TAG, "handleMessage() called");
            switch (msg.what) {
                case SewebPreferences.PRESENCE_CHANGED:
                    UserPresence presence = (UserPresence) msg.obj;
                    String newStatus = SewebPreferences.presenceAsString(presence.presenceCode);
                    
                    StringBuilder ticker = new StringBuilder();
                    ticker.append(presence.user);
                    ticker.append(" is now ");
                    ticker.append(newStatus);
                    
                    String status = presence.statusMessage;
                    if (status != null && status.length() != 0 ) {
                    	ticker.append(" ");
                    	ticker.append(status);
                    }
                    Toast.makeText(BuddyListView.this, ticker.toString(), Toast.LENGTH_SHORT).show();
                    
                    presenceAdapter.updatePresence(presence);
                    presenceAdapter.notifyDataSetChanged();
                    
                    break;
                case SewebPreferences.NEW_CHAT_MESSAGE:
                	
                	String[] args = (String[]) msg.obj;
                	String jid = args[0];
                	String chatID = args[1];
                	
                	String text = "New message from: " + jid;
                	Toast.makeText(BuddyListView.this, text, Toast.LENGTH_SHORT).show();
                	
                	presenceAdapter.updatePresenceNewMessage(jid);
                	presenceAdapter.notifyDataSetChanged();
                	// TODO:
                	// add a link to the chat somewhere.
                	
                	break;
                case SewebPreferences.CONNECTION_CHANGED:
                	switch (msg.arg1) {
                	case SewebPreferences.CONNECTION_OFFLINE: //Connection goes offline
                		// connection becomes unavailable
                		// force-refresh the UI
                		// store all outbound messages in the queue
                		// reconnect?
                		break;
                	case SewebPreferences.CONNECTION_ONLINE: //Connection goes online
                		// connection becomes available
                		// force-refresh the UI
                		// send all offline messages
                		//updateBuddyList();
                		break;
                	}
                	break;

                default:
                    super.handleMessage(msg);
            }
        }
    };
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	try {
    		mRosterService.unregisterRosterCallback(mCallback);
    	} catch (RemoteException e) {
    		Log.e(TAG, e.getMessage());
    	}
    	unbindService(mRosterConnection);
    	//unbindService(mChatConnection);
    	Log.i(TAG, "OnDestroy() called");
    }
    
    // ignoring the screen rotation changed events.
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      Log.i(TAG, "OnConfigurationChanged() called");
    }
}
