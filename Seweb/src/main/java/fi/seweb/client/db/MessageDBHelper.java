package fi.seweb.client.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MessageDBHelper extends SQLiteOpenHelper {
	private static final String TAG ="MessageDBHelper";
	private static final String DATABASE_NAME = "messages.db";
	private static final int DATABASE_VERSION = 1;
	
	public MessageDBHelper(Context context) { 
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i(TAG, "OnCreate");
		MessageTable.onCreate(db);
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(TAG, "OnUpgrade");
		MessageTable.onUpgrade(db, oldVersion, newVersion);
	}
	
}
