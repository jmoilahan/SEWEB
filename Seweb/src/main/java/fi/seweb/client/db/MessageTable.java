package fi.seweb.client.db;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class MessageTable {
	public static final String TAG = "MessagesTable";
	public static final String MESSAGES_TABLE = "messages"; 
	public static final String MESSAGE_ID = "_id";
	public static final String MESSAGE_CHAT_ID = "chat_id";
	public static final String MESSAGE_BODY = "body";
	public static final String MESSAGE_FROM = "sender";
	public static final String MESSAGE_TO = "recepient";
	public static final String MESSAGE_TIMESTAMP = "timestamp";
	public static final String MESSAGE_IS_INCOMING = "is_incoming";
	
	public static final String[] PROJECTION = new String[] {
		MESSAGE_ID,
		MESSAGE_CHAT_ID,
		MESSAGE_BODY,
		MESSAGE_FROM,
		MESSAGE_TO,
		MESSAGE_TIMESTAMP,
		MESSAGE_IS_INCOMING
	};
	
	public static final String CREATE_MESSAGES_TABLE = "CREATE TABLE IF NOT EXISTS " 
	  + MESSAGES_TABLE
	  + " (" 
	  + MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " 
	  + MESSAGE_CHAT_ID + " TEXT NOT NULL, " 
	  + MESSAGE_BODY + " TEXT NOT NULL, " 
	  + MESSAGE_FROM + " TEXT NOT NULL, "
	  + MESSAGE_TO + " TEXT NOT NULL, "
	  + MESSAGE_TIMESTAMP + " INTEGER NOT NULL,"
	  + MESSAGE_IS_INCOMING + " BOOLEAN"
	  + ");";
	
	
	public static final String CREATE_TRIGGER_TIMESTAMP =  "CREATE TRIGGER IF NOT EXISTS updateModDate " +
		    "BEFORE INSERT ON " + MESSAGES_TABLE + " FOR EACH ROW " + 
			"BEGIN " +
		    "UPDATE " + 
		    MESSAGES_TABLE + 
			" SET " + MESSAGE_TIMESTAMP + " = datetime('now', 'localtime') WHERE " + MESSAGE_ID +" = " + "NEW." + MESSAGE_ID + ";"  +
		    " END;";
	// could be also DATETIME("NOW")
	
	public static void onCreate(SQLiteDatabase database) {
		  Log.i(TAG, "onCreate called");
		  Log.i(TAG, CREATE_MESSAGES_TABLE);
		  //Log.i(TAG, CREATE_TRIGGER_TIMESTAMP);
		  database.execSQL("DROP TABLE IF EXISTS " + MESSAGES_TABLE);
		  database.execSQL(CREATE_MESSAGES_TABLE);
		  //database.execSQL(CREATE_TRIGGER_TIMESTAMP);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
	      int newVersion) {
	    
		Log.w(TAG, "Upgrading database from version "
	          + oldVersion + " to " + newVersion
	          + ", all data will be destroyed");
	    
		database.execSQL("DROP TABLE IF EXISTS " + MESSAGES_TABLE);
		onCreate(database);
	}
}
