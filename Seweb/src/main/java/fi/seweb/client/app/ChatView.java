package fi.seweb.client.app;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import fi.seweb.R;
import fi.seweb.client.common.SewebPreferences;
import fi.seweb.client.db.MessageContentProvider;
import fi.seweb.client.db.MessageTable;
import fi.seweb.client.db.RosterTable;
import fi.seweb.client.xmpp.IXMPPChatCallback;
import fi.seweb.client.xmpp.IXMPPChatService;


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ChatView extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

	public final static String TAG = "ChatView";
	private SimpleCursorAdapter adapter;
	private boolean mBound = false;
	private IXMPPChatService mChatService;
	private String mRemoteUserJID = null;
	private String mMyJID = null;
	private SewebPreferences mConfig;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "OnCreate() called");
		super.onCreate(savedInstanceState);
		
		mConfig = new SewebPreferences(PreferenceManager
				.getDefaultSharedPreferences(this));
		mMyJID = mConfig.getMyFullJid();
		
		setContentView(R.layout.activity_chat_new);
		 
		//bind using the Chat Service interface
	    bindService(new Intent(IXMPPChatService.class.getName()),
	               mChatServiceConnection, Context.BIND_AUTO_CREATE);
	     
	    Bundle b = getIntent().getExtras();
	    	if (b != null) {
	     		if (getIntent().hasExtra("remoteJID")) {
	     			mRemoteUserJID = b.getString("remoteJID");
	     		} else {
	 		 		Log.e(TAG, "Remote_JID is not found in the bundle!");
	 		 		throw new RuntimeException("Remote JID not found");
	 	 		}
	     	}
	    	
    	final Button btSendMessage = (Button) findViewById(R.id.chatSendButton);	    	
	    btSendMessage.setOnClickListener(mOnClickListener);
	    fillData();
	}
	
	private void fillData() {
		// Fields from the database (projection)
	    String[] from = new String [] { MessageTable.MESSAGE_TIMESTAMP, MessageTable.MESSAGE_BODY };
	    int[] to = new int[] { R.id.txtInfo, R.id.txtMessage };
	    getLoaderManager().initLoader(0, null, this);
	    adapter = new SimpleCursorAdapter(this, R.layout.chat_row, null, from, to, 0);
	    adapter.setViewBinder(new MessageViewBinder());
	    setListAdapter(adapter);
	}
	
	private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
       	@Override public void onClick(View v) {
       		Log.i(TAG, "OnClick() called");
	
       		EditText editText = (EditText) findViewById(R.id.messageEdit);
			String message = editText.getText().toString();
			if (message.length() != 0 && message != null ) {
				if (mBound) {
					try {
						mChatService.sendMessage(mRemoteUserJID, message);
						Log.i(TAG, "message dispatched to the service: " + message);
					} catch (RemoteException e) {
						Log.e(TAG, e.getMessage());
					}
				}
			}
			editText.setText("");
		}
	};
	
	private final Handler mHandler = new Handler(Looper.getMainLooper()) {
		@Override public void handleMessage(android.os.Message msg) {
			Log.i(TAG, "handleMessage() called");
		}
	}; 
	
	private final ServiceConnection mChatServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(final ComponentName name, final IBinder service) {
			Log.i(TAG, "onServiceConnected()");
			mChatService = IXMPPChatService.Stub.asInterface(service);
			try {
				mChatService.registerChatCallback(mChatCallback);
				mChatService.clearNotifications(mRemoteUserJID);
				mBound = true;
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to register chat callback");
			}
		}

		@Override
		public void onServiceDisconnected(final ComponentName name) {
			Log.i(TAG, "onServiceDisconnected()");
			mChatService = null;
			mBound = false;
		}
	};
	
	private IXMPPChatCallback mChatCallback = new IXMPPChatCallback.Stub() {
		@Override
		public void newChatMessageReceived(final String chat, final String message, final long timestamp)
				throws RemoteException {
			Log.i(ChatView.TAG, "newChatMessageReceived() called");
		}

		@Override
		public String getRemoteUserJID() throws RemoteException {
			Log.i(ChatView.TAG, "getRemoteUserJID() called");
			return mRemoteUserJID;
		}
	};
    
	@Override
	public void onDestroy(){
		Log.i(TAG,"OnDestroy() has been called");
		if (mBound) {
			try {
				mChatService.unregisterChatCallback(mChatCallback);
				Log.i(TAG,"Callback unregistered");
			} catch (RemoteException e) {
				Log.e(TAG, e.getMessage());
			}
			mBound = false;
		}
		unbindService(mChatServiceConnection);
		Log.i(TAG,"Service disconnected");
		super.onDestroy();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
    
    // ignoring the screen rotation changed events.
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      Log.i(TAG, "OnConfigurationChanged() called");
    }

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		 String[] projection = MessageTable.PROJECTION;
		 String selection = "( " + MessageTable.MESSAGE_FROM + " = ? AND " + MessageTable.MESSAGE_TO + " = ? )" + " OR " +
				 			"( " + MessageTable.MESSAGE_TO + " = ? AND " + MessageTable.MESSAGE_FROM + " = ? )";
		 String[] selectionArgs = new String[] {mMyJID, mRemoteUserJID, mMyJID, mRemoteUserJID};
		 
		 CursorLoader cursorLoader = new CursorLoader(this,
				 MessageContentProvider.CONTENT_URI, projection, selection, selectionArgs, null);
		 return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		adapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}
}
