package fi.seweb.client.app;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import fi.seweb.R;
import fi.seweb.client.common.SewebPreferences;
import fi.seweb.client.core.ObjectConverter;
import fi.seweb.client.xmpp.IXMPPChatCallback;
import fi.seweb.client.xmpp.IXMPPChatService;


public class ChatView extends Activity {

	public final static String TAG = "ChatView";
	
	private boolean mBound;
	private IXMPPChatService mChatService;
	private String mRemoteUserJID = null;
	//TODO: Remove it, we probably dont need to have this.
	private boolean mStartedFromBuddyList = false;
	
	 @Override
	 public void onCreate(Bundle savedInstanceState) {
		 Log.i(TAG, "OnCreate() called");
		 super.onCreate(savedInstanceState);
	     
		 setContentView(R.layout.activity_chat);
		 
		 //bind using the Chat Service interface
	     bindService(new Intent(IXMPPChatService.class.getName()),
	                mChatServiceConnection, Context.BIND_AUTO_CREATE);
	     
	     // since we know where the activity was started from 
	     // (either the buddylist or the tray notification)
	     // we need to get the remote_jid when sending our messages 
	     
	     Bundle b = getIntent().getExtras();
	     	if (b != null) {
	     		if (getIntent().hasExtra("remoteJID")) {
	     			mRemoteUserJID = b.getString("remoteJID");
	     		} else {
	 		 		Log.e(TAG, "Remote_JID is not found in the bundle!");
	 	 		}
	 	 		
	 	 		if (getIntent().hasExtra("StartedFromBuddyList")) {
	 	 			mStartedFromBuddyList = b.getBoolean("StartedFromBuddyList");
	 	 		}
	     	}
	     
	     TextView tvChat = (TextView) findViewById(R.id.tvChatAll);
	     tvChat.setMovementMethod(new ScrollingMovementMethod());
	  
	     final Button btSendMessage = (Button) findViewById(R.id.btSendMessage);
	     btSendMessage.setOnClickListener(new View.OnClickListener() {
	       	@Override public void onClick(View v) {
	       		Log.i(TAG, "OnClick() called");
	       		// adding our new message to the text view
				EditText editText = (EditText) findViewById(R.id.editMessage);
				String message = editText.getText().toString();
				if (message.length() != 0 && message != null ) {
					String from = SewebPreferences.USERNAME; // our name
					addNewChatMessage(from, message, Color.RED);
					try {
						if (mBound) {
							mChatService.sendMessage(mRemoteUserJID, message);
						}
					} catch (RemoteException e) {
						Log.e(TAG, e.getMessage());
					}
				}
				editText.setText("");
			}
		});
	 }
	
	private final Handler mHandler = new Handler(Looper.getMainLooper()) {
		@Override public void handleMessage(android.os.Message msg) {
			Log.i(TAG, "handleMessage() called");
			if (msg.what == SewebPreferences.NEW_CHAT_MESSAGE) {
				// new incoming chat message has arrived
				String chatMessage = (String) msg.obj;
				if (chatMessage != null && !chatMessage.equals("")) {
					final String from = mRemoteUserJID; //StringUtils.parseName(address);
					final String body = chatMessage; //chatMessage.getBody();
					//chatMessageReceived(from, body, Color.GREEN);
					Log.i(TAG, String.format("Adding new message '%1$s' from %2$s", body, from));
					addNewChatMessage(from, body, Color.GREEN);
				}
			}
		}
	}; 
	
	private final ServiceConnection mChatServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(final ComponentName name, final IBinder service) {
			Log.i(TAG, "onServiceConnected()");
			mChatService = IXMPPChatService.Stub.asInterface(service);
			mBound = true;
			
			try {
				mChatService.registerChatCallback(mChatCallback);
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to register chat callback");
			}
			
			/* Show the last message */
			try {
				String lastMessageStr = mChatService.obtainLastMessage(mRemoteUserJID);
				if (lastMessageStr != null && lastMessageStr.length() != 0) {
					Message message = ObjectConverter.toMessage(lastMessageStr);
					String from = StringUtils.parseBareAddress(message.getFrom());
					if (from.equalsIgnoreCase(mRemoteUserJID)) {
						addNewChatMessage(from, message.getBody(), Color.GREEN);
					} else {
						addNewChatMessage(from, message.getBody(), Color.RED);
					}
				} // do nothing if message is null or empty 
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to obtain the most recent message");
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
		public void newChatMessageReceived(String chat, String message)
				throws RemoteException {
			Log.i(ChatView.TAG, "newChatMessageReceived() called");
			//Log.i(ChatView.TAG, "chat id: " + chat);
			//Log.i(ChatView.TAG, "message: " + message);
			
			android.os.Message msg = mHandler.obtainMessage();
    		msg.what = SewebPreferences.NEW_CHAT_MESSAGE;
    		msg.obj = message;
			mHandler.sendMessage(msg);
		}
	};
    
    private static void addColoredText(TextView tv, String text, int color){
      	     int start = tv.getText().length();
    	     tv.append(text);
    	     int end = tv.getText().length();
    	     Spannable spannableText = (Spannable) tv.getText();
    	     spannableText.setSpan(new ForegroundColorSpan(color), start, end, 0);
    }
    
    private void addNewChatMessage (String from, String msg, int color) {
    	TextView chatView = (TextView) findViewById(R.id.tvChatAll);
    	    	
    	SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm:ss]", Locale.UK);
    	String currentTime = sdf.format(new Date());
    	
    	addColoredText(chatView, currentTime, Color.GRAY);
    	addColoredText(chatView, " " + from + ": ", color);
    	chatView.append(msg + "\n");
    	
    	final Layout layout = chatView.getLayout();
        if (layout != null){
            int scrollOffset = layout.getLineBottom(chatView.getLineCount() - 1) 
                - chatView.getScrollY() - chatView.getHeight();
            if (scrollOffset > 0)
                chatView.scrollBy(0, scrollOffset);
        }
    }
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		Log.i(TAG,"OnDestroy() has been called");

		if (mBound) {
			try {
				mChatService.unregisterChatCallback(mChatCallback);
				Log.i(TAG,"Callback unregistered");
			} catch (RemoteException e) {
				Log.e(TAG, e.getMessage());
			}
			unbindService(mChatServiceConnection);
			Log.i(TAG,"Service disconnected");
		}
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
    
}
