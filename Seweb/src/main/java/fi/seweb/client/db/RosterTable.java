package fi.seweb.client.db;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class RosterTable {
	  public static final String TAG = "RosterTable";	
		
	  // Roster table
	  public static final String TABLE_ROSTER = "roster";
	  public static final String ENTRY_ID = "_id";
	  public static final String ENTRY_JID = "jid";
	  public static final String ENTRY_NAME = "name";
	  public static final String ENTRY_STATUS_MSG = "status_msg";
	  public static final String ENTRY_STATUS_CODE = "status_code";
	  public static final String ENTRY_HAS_MESSAGES = "has_messages";
	  public static final String ENTRY_DISTANCE = "distance";
	  public static final String ENTRY_TIMESTAMP = "timestamp";
	  
	  public static final String[] PROJECTION = new String[] {
			RosterTable.ENTRY_ID,
			RosterTable.ENTRY_NAME,
			RosterTable.ENTRY_JID,
			RosterTable.ENTRY_STATUS_CODE,
			RosterTable.ENTRY_STATUS_MSG,
			RosterTable.ENTRY_HAS_MESSAGES,
			RosterTable.ENTRY_DISTANCE,
			RosterTable.ENTRY_TIMESTAMP
	  };
	  
	  // The SQL statement which creates the roster database table 
	  private static final String CREATE_ROSTER_TABLE = "CREATE TABLE IF NOT EXISTS " 
		  + TABLE_ROSTER
	      + " (" 
	      + ENTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " 
	      + ENTRY_JID + " TEXT NOT NULL, " 
	      + ENTRY_NAME + " TEXT NOT NULL, " 
	      + ENTRY_STATUS_MSG + " TEXT NOT NULL, "
	      + ENTRY_STATUS_CODE + " INTEGER, "
	      + ENTRY_HAS_MESSAGES + " BOOLEAN, "
	      + ENTRY_DISTANCE + " INTEGER, "
	      + ENTRY_TIMESTAMP + " INTEGER"
	      + ");";
	  
	  public static void onCreate(SQLiteDatabase database) {
		  Log.i(TAG, "onCreate called");
		  database.execSQL("DROP TABLE IF EXISTS " + TABLE_ROSTER);
		  database.execSQL(CREATE_ROSTER_TABLE);
		  Log.i(TAG, CREATE_ROSTER_TABLE);
	  }

	  public static void onUpgrade(SQLiteDatabase database, int oldVersion,
	      int newVersion) {
	    
		  Log.w(TAG, "Upgrading database from version "
	        + oldVersion + " to " + newVersion
	        + ", all data will be destroyed");
	    
		  database.execSQL("DROP TABLE IF EXISTS " + TABLE_ROSTER);
		  onCreate(database);
	  }
}
