package fi.seweb.client.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Environment;
import android.util.Log;


public class SewebPreferences implements OnSharedPreferenceChangeListener {
	public static final String TAG = "SewebPreferences";
	
	//Variables
	public static String PASSWORD;
	public static String DOMAIN;
	public static String USERNAME;
	
	/* User information is read from ~/SEWEB/userpass.txt and saved
	 * Could use some changes for cleanier code and better handling of errors
	 */
	File sdcard = Environment.getExternalStorageDirectory();
	File file = new File(sdcard,"SEWEB/userpass.txt");

	StringBuilder text = new StringBuilder(); {

	try {
	    BufferedReader br = new BufferedReader(new FileReader(file));
	    
	    PASSWORD = br.readLine();
	    DOMAIN = br.readLine();
	    USERNAME = br.readLine();

	    br.close();
	    
	    /* If password, domain or username is empty or null, force login screen to show an error
	     * Possible TODO: fix error handling to a better one; this one works most of the time but is messy
	     */
	    if (PASSWORD == "" || PASSWORD == null) {
	    	PASSWORD = "";
		    DOMAIN = "";
		    USERNAME = "";
	    } else if (DOMAIN == "" || DOMAIN == null) {
	    	PASSWORD = "";
		    DOMAIN = "";
		    USERNAME = "";
	    } else if (USERNAME == "" || USERNAME == null) {
	    	PASSWORD = "";
		    DOMAIN = "";
		    USERNAME = "";
	    }
	    
	    // If file is missing, force login screen to show an error
	} catch (IOException e) {
		
	    PASSWORD = "";
	    DOMAIN = "";
	    USERNAME = "";
	    
	    }
	}	
	
	/* Seweb Location Sharing Servlet */
	public final static String SERVLET_URL = "http://107.170.73.18:8080//SewebServer/Seweb";
		
	/* Default port for normal connections is 5222, for SSL connections is 5223 */
	public final static String DEFAULT_PORT = "5222";
	public final static int DEFAULT_PORT_INT = 5222;
	public final static String DEFAULT_STATUS_MESSAGE = "Online";
	public final static int DEFAULT_STATUS_PRIORITY = 128;
	public final static int DEFAULT_PRESENCE = PresenceStatus.available.ordinal();
	public final static boolean DEFAULT_SIGNINONSTART = true;
	
	public final static String TAG_PASSWORD = "password";
	public final static String TAG_DOMAIN = "domain";
	public final static String TAG_PORT = "port";
	public final static String TAG_PRIORITY = "priority";
	public final static String TAG_USERNAME = "username";
	public final static String TAG_FULL_JID = "full_jid";
	public final static String TAG_STATUS_MESSAGE = "status_message";
	public final static String TAG_ONLINE_STATUS = "online_status";
	public final static String TAG_SIGNINONSTART = "signinonstart";
	
	private String mUsername;
	private String mPassword;
	private String mDomain;
	private int mPort;
	
	private int mPriority;
	private String mStatusMessage;
	private int mPresenceStatusCode;
	
	private boolean mSignInOnStart;
	
	private final SharedPreferences mPreferences; 
	
	public SewebPreferences(SharedPreferences preferences) {
		this.mPreferences = preferences;
		mPreferences.registerOnSharedPreferenceChangeListener(this);
		loadPreferences(mPreferences);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
		Log.i(TAG, "onSharedPreferenceChanged(): " + key);
		loadPreferences(preferences);
	}
	
	@Override
	protected void finalize() {
		mPreferences.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	private void loadPreferences(SharedPreferences preferences) {
		
		this.mUsername = preferences.getString(SewebPreferences.TAG_USERNAME, SewebPreferences.USERNAME);
		this.mDomain =  preferences.getString(SewebPreferences.TAG_DOMAIN, SewebPreferences.DOMAIN);
			
		/* A workaround for invalid Jids and missing login information
		 * Possible TODO: fix error handling to a better one, old IllegalArgumentException caused crashes in login if
		 * there were problems with the login file information
		 */

		if (!fi.seweb.client.common.StringUtils.isValidJid(mUsername + "@" + mDomain)) {
			this.mUsername = null;
			this.mDomain = "invalid.jid";
		}
		 //Replaces this
		 //if (!fi.seweb.client.common.StringUtils.isValidJid(mUsername + "@" + mDomain)) {
		 //throw new IllegalArgumentException("Incorrect username or/and domain: " + mUsername + "@" + mDomain );
		
		/* A workaround for missing password
		 * Possible TODO: fix error handling to a better one, old IllegalArgumentException caused crashes in login if
		 * there were problems with the login file information
		 * 
		 * Had to add this as well because different devices reacted differently
		 */
		
		this.mPassword = preferences.getString(SewebPreferences.TAG_PASSWORD, SewebPreferences.PASSWORD);
		if (mPassword == null || mPassword == "") {
			this.mUsername = null;
			this.mDomain = "invalid.jid";
		}
		//this.mPassword = preferences.getString(SewebPreferences.TAG_PASSWORD, SewebPreferences.PASSWORD);	
		//if (mPassword == null)
		//	throw new IllegalArgumentException("Password is null");
		
		this.mPort = preferences.getInt(SewebPreferences.TAG_PORT, SewebPreferences.DEFAULT_PORT_INT);
		
		this.mPriority = preferences.getInt(SewebPreferences.TAG_PRIORITY, SewebPreferences.DEFAULT_STATUS_PRIORITY);
		if (mPriority > Integer.MAX_VALUE || mPriority < Integer.MIN_VALUE)
			throw new IllegalArgumentException("Incorrect status message priority: " + mPriority);
		
		this.mPresenceStatusCode = preferences.getInt(SewebPreferences.TAG_ONLINE_STATUS, SewebPreferences.DEFAULT_PRESENCE);
		if (!PresenceStatus.isCorrectStatusCode(mPresenceStatusCode))
			throw new IllegalArgumentException("Incorrect presence code: " + mPresenceStatusCode);
		
		this.mStatusMessage = preferences.getString(SewebPreferences.TAG_STATUS_MESSAGE, SewebPreferences.DEFAULT_STATUS_MESSAGE);
		if (mStatusMessage == null)
			throw new IllegalArgumentException("Status message is null");
		
		this.mSignInOnStart = preferences.getBoolean(SewebPreferences.TAG_SIGNINONSTART, SewebPreferences.DEFAULT_SIGNINONSTART);
	}
	
	public int getPresenceStatusCode() {
		return this.mPresenceStatusCode;
	}
	
	public String getStatusMessage() {
		return this.mStatusMessage;
	}
	
	public String getUsername() {
		return this.mUsername;
	}
	
	public String getPassword() {
		return this.mPassword;
	}
	
	public String getDomain() {
		return this.mDomain;
	}
	
	public int getPort() {
		return this.mPort;
	}
	
	public boolean getSignInOnStart() {
		return this.mSignInOnStart;
	}
	
	public String getMyFullJid() {
		String jid = mUsername + "@" + mDomain;
		
		if (!fi.seweb.client.common.StringUtils.isValidJid(jid)) {
			throw new IllegalArgumentException("Incorrect jid: " + jid);
		}
		return jid;
		
	}
}
