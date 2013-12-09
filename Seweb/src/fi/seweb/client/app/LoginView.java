package fi.seweb.client.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import fi.seweb.R;
import fi.seweb.client.xmpp.SewebNotificationService;

public class LoginView extends Activity {
	private final String TAG = "LoginView";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate called");
		super.onCreate(savedInstanceState);
		
		Intent intent = new Intent(this, SewebNotificationService.class);
		intent.setAction("Starting XMPPService from LoginView");
		startService(intent);
		
		setContentView(R.layout.activity_login);
		final Button btLogin = (Button) findViewById(R.id.btLogin);
		btLogin.setOnClickListener(new View.OnClickListener() {
        	@Override public void onClick(View v) {
        		Intent intent = new Intent();
        		intent.setClass(getApplicationContext(), BuddyListView.class);
        		startActivity(intent);
        		finish();
        	}
		});
	}
	
	
	
	
	
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login_view, menu);
		return true;
	}

}
