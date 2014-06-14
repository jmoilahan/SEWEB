package fi.seweb.client.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import fi.seweb.client.core.Buddy;

public class RosterDBHelper extends SQLiteOpenHelper {
	private static final String TAG ="RosterDBHelper";
	private static final String DATABASE_NAME = "rostertable.db";
	private static final int DATABASE_VERSION = 1;
	
	public RosterDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i(TAG, "onCreate()");
		RosterTable.onCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		RosterTable.onUpgrade(db, oldVersion, newVersion);
	}
	
	/*public static long addRosterEntry(SQLiteDatabase db, Buddy entry) {
		ContentValues entryValues = new ContentValues();
		
		entryValues.put(RosterTable.ENTRY_JID, entry.mJid);
		entryValues.put(RosterTable.ENTRY_NAME, entry.getName());
		entryValues.put(RosterTable.ENTRY_STATUS_CODE, entry.getPresence().mPresenceCode);
		entryValues.put(RosterTable.ENTRY_STATUS_MSG, entry.getPresence().mStatus);
		entryValues.put(RosterTable.ENTRY_HAS_MESSAGES, entry.getPresence().mUnreadMessages);
		
		return db.insert(RosterTable.TABLE_ROSTER, null, entryValues);
	}*/
}
