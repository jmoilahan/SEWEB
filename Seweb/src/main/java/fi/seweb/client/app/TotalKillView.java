package fi.seweb.client.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import fi.seweb.R;
import fi.seweb.client.xmpp.SewebNotificationService;

public class TotalKillView extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_total_kill_view);
		
		Button btKill = (Button) findViewById(R.id.btKillAll);
		btKill.setOnClickListener((OnClickListener) new LKillAll());
	}
	
	/* A listener for the button that kills the app and the service */
	private class LKillAll implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			Intent intent = new Intent(TotalKillView.this, SewebNotificationService.class);
			intent.setAction("Stopping the SewebNotificationService from TotalKillView");
			stopService(intent);
			finish();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.total_kill_view, menu);
		return true;
	}

}
