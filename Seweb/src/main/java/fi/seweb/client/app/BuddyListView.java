package fi.seweb.client.app;


import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import fi.seweb.R;
import fi.seweb.client.common.PresenceStatus;
import fi.seweb.client.common.SewebPreferences;
import fi.seweb.client.core.Buddy;
import fi.seweb.client.db.RosterContentProvider;
import fi.seweb.client.db.RosterTable;
import fi.seweb.client.xmpp.IXMPPRosterCallback;
import fi.seweb.client.xmpp.IXMPPRosterService;


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class BuddyListView extends ListActivity implements
								LoaderManager.LoaderCallbacks<Cursor> {
	
	private static final String TAG = "BuddyListView";
	private SimpleCursorAdapter adapter;
	private IXMPPRosterService mRosterService = null;
	private Boolean mBound = false;
	private SewebPreferences mConfig;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//load preferences
		mConfig = new SewebPreferences(PreferenceManager
					.getDefaultSharedPreferences(this)); 
				
		//retrieve the saved status
		int code = mConfig.getPresenceStatusCode();
		String statusMessage = mConfig.getStatusMessage();
				
		//set custom title bar
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_buddy_list);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);
				
		bindService(new Intent(IXMPPRosterService.class.getName()),
					mRosterConnection, Context.BIND_AUTO_CREATE);
		
		fillData();
		
		//"extra" menu
	    ImageView titleBarMenuIcon = (ImageView) findViewById(R.id.titleBarMenuIcon);
	    titleBarMenuIcon.setOnClickListener((OnClickListener) mTitleBarMenuListener);
	    
	    //status message
	    TextView myStatusView = (TextView) findViewById(R.id.userStatusText);
	    myStatusView.setText(statusMessage);
	    myStatusView.setOnClickListener(mStatusViewListener);
	    
	    //status icon
	    ImageView myStatusIcon = (ImageView) findViewById(R.id.userStatusIcon);
	    myStatusIcon.setImageResource(PresenceStatus.getUserStatusIconId(code));
	    myStatusIcon.setOnClickListener(mStatusViewListener);
	}

	private void fillData() {
	    // Fields from the database (projection)
	    String[] from = new String[] { RosterTable.ENTRY_NAME, RosterTable.ENTRY_STATUS_MSG, RosterTable.ENTRY_STATUS_CODE, RosterTable.ENTRY_HAS_MESSAGES, RosterTable.ENTRY_DISTANCE, RosterTable.ENTRY_TIMESTAMP };
	    int[] to = new int[] { R.id.buddyJid, R.id.buddyStatus, R.id.buddyIcon };

	    getLoaderManager().initLoader(0, null, this);
	    adapter = new SimpleCursorAdapter(this, R.layout.row_buddy_list, null, from, to, 0);
	    adapter.setViewBinder(new RosterViewBinder());
	    setListAdapter(adapter);
	  }
	
	private class RosterViewBinder implements SimpleCursorAdapter.ViewBinder {

	    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
	        int viewId = view.getId();
	        switch(viewId) {
	            case R.id.buddyJid:
	                TextView buddyName = (TextView) view;
	                int index = cursor.getColumnIndexOrThrow(RosterTable.ENTRY_NAME);
	                buddyName.setText(cursor.getString(index));
	                //buddyName.setText(cursor.getString(columnIndex));
	            break;
	            case R.id.buddyStatus:
	            	TextView buddyStatus = (TextView) view;
	            	int maxLength = 10;
	            	InputFilter[] fArray = new InputFilter[1];
	            	fArray[0] = new InputFilter.LengthFilter(maxLength);
	            	
	            	index = cursor.getColumnIndexOrThrow(RosterTable.ENTRY_STATUS_MSG);
	            	String status = cursor.getString(index);
	            	
	            	int indexDist = cursor.getColumnIndexOrThrow(RosterTable.ENTRY_DISTANCE);
	            	int indexTime = cursor.getColumnIndexOrThrow(RosterTable.ENTRY_TIMESTAMP);
	            	
	            	int distance = cursor.getInt(indexDist);
	            	if (distance > 0) {
	            		
	            		int timeInt = cursor.getInt(indexTime);
	            		long timestamp = ((long) timeInt) * 1000;
	            		
	            		DateTime dt = new DateTime(timestamp);
	            		DateTime now = DateTime.now();
	            		
	            		Period period = new Period(dt, now);
	            		PeriodFormatter HHMMSSFormater = new PeriodFormatterBuilder()
	        				.printZeroAlways()
	        				.minimumPrintedDigits(2)
	        				.appendHours()
	        				.appendSeparator(":")
	        				.appendMinutes()
	        				.appendSeparator(":")
	        				.appendSeconds()
	        				.toFormatter(); // produce thread-safe formatter
	            		String when =  HHMMSSFormater.print(period);
	            		buddyStatus.setText("[distance]: " + distance + " meters " + when + " ago");
	            	} /*else {
	            		buddyStatus.setText(status);
	            		buddyStatus.setEllipsize(TextUtils.TruncateAt.END);
	            		buddyStatus.setFilters(fArray);
	            	}*/
	            break;
	            case R.id.buddyIcon:
	                ImageView buddyIcon = (ImageView) view;
	                int codeIndex = cursor.getColumnIndexOrThrow(RosterTable.ENTRY_STATUS_CODE);
	                int hasMsgIndex = cursor.getColumnIndexOrThrow(RosterTable.ENTRY_HAS_MESSAGES);
	                int presenceCode = cursor.getInt(codeIndex);
	                boolean hasMessages = cursor.getInt(hasMsgIndex) > 0;
	                int img;
	                
	                if (hasMessages) {
		            	img = R.drawable.chat;
		            } else {
		            	img = PresenceStatus.getRosterIconId(presenceCode);
		            }
		            buddyIcon.setImageResource(img);
	    	        buddyIcon.setVisibility(View.VISIBLE);
	            break;
	        }
	        return true;
	    }
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		// Start a chat.
		Uri rosterUri = Uri.parse(RosterContentProvider.CONTENT_URI + "/" + id);
		Buddy buddy = fetchData(rosterUri);
		String jid = buddy.mJid;
		Bundle b = new Bundle();
		b.putString("remoteJID", jid);
		b.putString(RosterContentProvider.CONTENT_ITEM_TYPE, rosterUri.toString());
		
		Intent chatViewIntent = new Intent();
		chatViewIntent.setClass(getApplicationContext(), ChatView.class);
		chatViewIntent.putExtras(b);
		startActivity(chatViewIntent);
		
	  }
	
	private Buddy fetchData(Uri rosterUri) {
		String[] projection = RosterTable.PROJECTION; 
				/*{ RosterTable.ENTRY_JID, RosterTable.ENTRY_NAME,
		        RosterTable.ENTRY_STATUS_CODE, RosterTable.ENTRY_STATUS_MSG,
		        RosterTable.ENTRY_HAS_MESSAGES };*/
		    
		Cursor cursor = getContentResolver().query(rosterUri, projection, null,
				null, null);
		
		Buddy buddy = null;
		
		if (cursor != null) {
			cursor.moveToFirst();
		    String jid = cursor.getString(cursor
		          .getColumnIndexOrThrow(RosterTable.ENTRY_JID));
		    String name = cursor.getString(cursor
		    		.getColumnIndexOrThrow(RosterTable.ENTRY_NAME));
		    int code = cursor.getInt(cursor
		    		.getColumnIndexOrThrow(RosterTable.ENTRY_STATUS_CODE));
		    String status = cursor.getString(cursor
		    		.getColumnIndexOrThrow(RosterTable.ENTRY_STATUS_MSG));
		    boolean hasMessages = cursor.getInt(cursor.
		    		getColumnIndexOrThrow(RosterTable.ENTRY_HAS_MESSAGES)) > 0;
		    		
		    buddy = new Buddy(jid);
		    buddy.setName(name);
		    buddy.setPresence(status, code, hasMessages);
		}
	
		 cursor.close();
		 return buddy;    
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.buddy_list, menu);
		return true;
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		 /*String[] projection = { RosterTable.ENTRY_ID, 
				 				 RosterTable.ENTRY_JID, 
				 				 RosterTable.ENTRY_NAME, 
				 				 RosterTable.ENTRY_STATUS_MSG, 
				 				 RosterTable.ENTRY_STATUS_CODE, 
				 				 RosterTable.ENTRY_HAS_MESSAGES };*/
		String[] projection = RosterTable.PROJECTION;
		    CursorLoader cursorLoader = new CursorLoader(this,
		        RosterContentProvider.CONTENT_URI, projection, null, null, null);
		    return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		adapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursor) {
		adapter.swapCursor(null);
	}
	
	/**
     * Class for interacting with the roster interface of the SewebNotificationService.
     */
    private ServiceConnection mRosterConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            mRosterService = IXMPPRosterService.Stub.asInterface(service);
            try {
                mRosterService.registerRosterCallback(mCallback);
                mBound = true;
            } catch (RemoteException e) {
                // the service has crashed -> do nothing
            	Log.e(TAG, e.getMessage());
            }
            Toast.makeText(BuddyListView.this, R.string.remote_service_connected,
                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // the service process has crashed (or stopped).
            mRosterService = null;
			mBound = false;
			
            Toast.makeText(BuddyListView.this, R.string.remote_service_disconnected,
                    Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * This implementation is used to receive callbacks from the SewebNotificationService.
     */
    private IXMPPRosterCallback mCallback = new IXMPPRosterCallback.Stub() {
    	
    	/*
    	 * This is called when the connection goes offline / online, hence the UI has to be updated.
    	 * @see fi.seweb.client.xmpp.IXMPPRosterCallback#connectionStatusChanged(int)
    	 */
		@Override
		public void connectionStatusChanged(int connectionStatusCode)
				throws RemoteException {
			Log.i(BuddyListView.TAG, "connectionStatusChanged() called");
		}
		
    };

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override public void handleMessage(Message msg) {
        	Log.i(BuddyListView.TAG, "handleMessage() called");
        }
    };
    
    private final OnClickListener mTitleBarMenuListener = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			Toast.makeText(BuddyListView.this, "Title Bar Menu Clicked", Toast.LENGTH_SHORT).show();
		}
	};
	
	private final OnClickListener mStatusViewListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			new UserStatusDialog(BuddyListView.this).show();
		}
	};
    
    @Override
    public void onDestroy() {
    	if (mBound) {
    		try {
    			mRosterService.unregisterRosterCallback(mCallback);
    		} catch (RemoteException e) {
    			Log.e(TAG, e.getMessage());
    		}
    	}
    	unbindService(mRosterConnection);
    	Log.i(TAG, "OnDestroy() called");
    	super.onDestroy();
    }
    
    // ignoring the screen rotation changed events.
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      Log.i(TAG, "OnConfigurationChanged() called");
    }

	public void setAndSaveStatus(PresenceStatus status, String message, int i) {
		Log.i(TAG, "setAndSaveStatus() called");
		
		// save to the preferences
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(this).edit();
	
		// do not save "offline" to prefs, or else!
		if (status != PresenceStatus.offline) {
			editor.putString(SewebPreferences.TAG_STATUS_MESSAGE, message);
			editor.putInt(SewebPreferences.TAG_ONLINE_STATUS, status.ordinal());
			editor.commit();
		}
		
		// update the remote service
		try {
			mRosterService.updatePresence(status.ordinal(), message);
			
			ImageView img = (ImageView) findViewById(R.id.userStatusIcon);
			img.setImageResource(status.getUserStatusIconId());
			
			TextView text = (TextView) findViewById(R.id.userStatusText);			
			text.setText(message);
			
			String toastText = "New status: " + status.toString() + " " + message;
			Toast.makeText(BuddyListView.this, toastText, Toast.LENGTH_SHORT).show();
			
		} catch (RemoteException e) {
			Log.e(TAG, e.getMessage());
		}
		
		// TODO: connection management!
		// if connected and status == offline -> go offline
		// if was offline and connected -> go online
	}
	
	public String getCurrentStatusMessage() {
		return mConfig.getStatusMessage();
	}
	
	public int getCurrentPresenceCode() {
		return mConfig.getPresenceStatusCode();
	}
	
}


	
