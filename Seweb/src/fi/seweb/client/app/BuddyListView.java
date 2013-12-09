package fi.seweb.client.app;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import fi.seweb.R;
import fi.seweb.client.common.SewebPreferences;
import fi.seweb.client.xmpp.IXMPPRosterCallback;
import fi.seweb.client.xmpp.IXMPPRosterService;

public class BuddyListView extends Activity {
	
	private ListView mainListView;  
	private ArrayAdapter<String> listAdapter;
	private IXMPPRosterService mRosterService = null;
	private boolean mIsBound = false;
	private static final String TAG = "BuddyListView";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_buddy_list);
		
		
		bindService(new Intent(IXMPPRosterService.class.getName()),
                mRosterConnection, Context.BIND_AUTO_CREATE);
		
		
		/*
		if (mRosterService != null) {
			try {
				//Connect to the Server
				mRosterService.connect();
				Toast.makeText(BuddyListView.this, "BOUND!",
	                    Toast.LENGTH_SHORT).show();
			} catch (RemoteException e) {
				Log.e(TAG, e.getMessage());
			}
		}*/
		
		// Find the ListView resource.   
	    mainListView = (ListView) findViewById(R.id.lvBuddyList);  
	    
	    // Create and populate a List of planet names.  
	    String[] planets = new String[] { "Mercury", "Venus", "Earth", "Mars",  
	                                      "Jupiter", "Saturn", "Uranus", "Neptune"};    
	    ArrayList<String> planetList = new ArrayList<String>();  
	    planetList.addAll( Arrays.asList(planets) );  
	      
	    // Create ArrayAdapter using the planet list.  
	    listAdapter = new ArrayAdapter<String>(this, R.layout.row_buddy_list, planetList);  
	      
	    // Add more planets. If you passed a String[] instead of a List<String>   
	    // into the ArrayAdapter constructor, you must not add more items.   
	    // Otherwise an exception will occur.  
	    listAdapter.add( "Ceres" );  
	    listAdapter.add( "Pluto" );  
	    listAdapter.add( "Haumea" );  
	    listAdapter.add( "Makemake" );  
	    listAdapter.add( "Eris" );
	      
	    // Set the ArrayAdapter as the ListView's adapter.  
	    mainListView.setAdapter( listAdapter );
	    mainListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {
				//getting the text from the list item
				final String item = listAdapter.getItem(position);
				runOnUiThread(new Runnable() {
			        @Override
			        public void run() {
			            Toast.makeText(parent.getContext(), item, Toast.LENGTH_SHORT).show();
			        }
			    });
			}
	    });
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.buddy_list, menu);
		return true;
	}
	
    /**
     * Class for interacting with the roster interface of the SewebNotificationService.
     */
    private ServiceConnection mRosterConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mRosterService = IXMPPRosterService.Stub.asInterface(service);
            //mKillButton.setEnabled(true);
            //mCallbackText.setText("Attached.");

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                mRosterService.registerRosterCallback(mCallback);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            	Log.e(TAG, e.getMessage());
            }
            
            // As part of the sample, tell the user what happened.
            Toast.makeText(BuddyListView.this, R.string.remote_service_connected,
                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mRosterService = null;
            //mKillButton.setEnabled(false);
            //mCallbackText.setText("Disconnected.");

            // As part of the sample, tell the user what happened.
            Toast.makeText(BuddyListView.this, R.string.remote_service_disconnected,
                    Toast.LENGTH_SHORT).show();
        }
        
        /*
        public void connect() {
        	if (mRosterService != null) {
        		try {
					mRosterService.connect();
				} catch (RemoteException e) {
	                // In this case the service has crashed before we could even
	                // do anything with it; we can count on soon being
	                // disconnected (and then reconnected if it can be restarted)
	                // so there is no need to do anything here.
					Log.e(TAG, e.getMessage());
				}
        	}
        }*/
    };
    
    /**
     * This implementation is used to receive callbacks from the SewebNotificationService.
     */
    private IXMPPRosterCallback mCallback = new IXMPPRosterCallback.Stub() {
        /**
         * This is called by the remote service regularly to tell us about
         * new values.  Note that IPC calls are dispatched through a thread
         * pool running in each process, so the code executing here will
         * NOT be running in our main thread like most other things -- so,
         * to update the UI, we need to use a Handler to hop over there.
         */

		@Override
		public void connectionStatusChanged() throws RemoteException {
			mHandler.sendMessage(mHandler.obtainMessage(SewebPreferences.PRESENSE_CHANGED, 0, 0));
		}
    };
    
    private Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case SewebPreferences.PRESENSE_CHANGED:
                    //mCallbackText.setText("Received from service: " + msg.arg1);
                	Toast.makeText(BuddyListView.this, R.string.remote_service_connected,
                            Toast.LENGTH_SHORT).show();
                    break;
                case SewebPreferences.SERVER_DISCONNECTED:
                	break;
                default:
                    super.handleMessage(msg);
            }
        }
        
    };
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	unbindService(mRosterConnection);
    }
}
