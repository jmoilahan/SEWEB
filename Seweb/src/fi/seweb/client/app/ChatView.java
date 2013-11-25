package fi.seweb.client.app;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
import fi.seweb.R;
import fi.seweb.client.xmpp.LocalBinder;
import fi.seweb.client.xmpp.XMPPService;

import org.jivesoftware.smack.packet.Message;


public class ChatView extends Activity {

	private static CharSequence lastMessage;
	//private static ChatAppManager manager = null;
	private boolean mBound;
	private XMPPService mService;
	private final String TAG = "ChatView";
	private final Handler mHandler = new Handler(Looper.getMainLooper()) {
		@Override public void handleMessage(android.os.Message msg) {
			Message mXmppMessage = (Message) msg.obj;
			if (mXmppMessage != null) { 
				String address = mXmppMessage.getFrom();
				final String from = address.substring(0, address.indexOf("@"));
				final String body = mXmppMessage.getBody();
				chatMessageReceived(from, body, Color.GREEN);
			}
		}
	}; 
	
	private final ServiceConnection mConnection = new ServiceConnection() {
		@SuppressWarnings("unchecked")
		@Override
		public void onServiceConnected(final ComponentName name, final IBinder service) {
			mService = ((LocalBinder<XMPPService>) service).getService();
			mService.registerHandler(mHandler);
			mBound = true;
			Log.d(TAG, "onServiceConnected");
		}

		@Override
		public void onServiceDisconnected(final ComponentName name) {
			mService = null;
			mBound = false;
			Log.d(TAG, "onServiceDisconnected");
		}
	};
	
	private void chatMessageReceived(String from, String body, int color) {
		Log.i("DEBUG", String.format("Received message '%1$s' from %2$s", body, from));
		addNewChatMessage(from, body, color);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        doBindService();
        
        TextView tvChat = (TextView) findViewById(R.id.tvChatAll);
        tvChat.setMovementMethod(new ScrollingMovementMethod());
  
        final Button btSendMessage = (Button) findViewById(R.id.btSendMessage);
        btSendMessage.setOnClickListener(new View.OnClickListener() {
        	@Override public void onClick(View v) {
        		// add a new message to the view
				EditText editText = (EditText) findViewById(R.id.editMessage);
				String message = editText.getText().toString();
				if (message.length() != 0 && message != null ) {
					addNewChatMessage("Me", message, Color.RED);
					mService.sendMessage(message);
				}
				editText.setText("");
			}
		});
    }
    
    public static void addColoredText(TextView tv, String text, int color){
      	     int start = tv.getText().length();
    	     tv.append(text);
    	     int end = tv.getText().length();
    	     Spannable spannableText = (Spannable) tv.getText();
    	     spannableText.setSpan(new ForegroundColorSpan(color), start, end, 0);
    }
    
    public void addNewChatMessage (String from, String msg, int color) {
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
        this.setLastMessage(msg);    	
    }
	
    private void doBindService() {
		bindService(new Intent(this, XMPPService.class), mConnection, Context.BIND_AUTO_CREATE);
	}
    
    private void doUnbindService() {
		if (mBound) {
			unbindService(mConnection);
		}
	}
    
	public void setLastMessage(CharSequence msg){
		if (msg != null && msg.length() != 0) {
			ChatView.lastMessage = msg;
		}
	}
	
	public CharSequence getLastMessage(){
		return ChatView.lastMessage;
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		doUnbindService();
		Log.i(TAG,"OnDestroy() has been called");		
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
