package fi.seweb.client.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.jivesoftware.smack.packet.Presence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import fi.seweb.client.common.PresenceStatus;
import fi.seweb.client.db.RosterContentProvider;
import fi.seweb.client.db.RosterTable;

public class RosterCache {
	
	private final Context mContext;
	
	public RosterCache (Context context) {
		mContext = context;
	}

	public void addEntry(Buddy entry) {
		if (entry == null) 
			throw new IllegalArgumentException("Entry is null");
		
		// preparing a data object for the database
	   ContentValues values = new ContentValues();
	   values.put(RosterTable.ENTRY_JID, entry.mJid);
	   values.put(RosterTable.ENTRY_NAME, entry.getName());
	   values.put(RosterTable.ENTRY_STATUS_CODE, entry.getPresence().mPresenceCode);
	   values.put(RosterTable.ENTRY_STATUS_MSG, entry.getPresence().mStatus);
	   values.put(RosterTable.ENTRY_HAS_MESSAGES, false);
		   
	   // preparing params for a query
	   String[] projection = {RosterTable.ENTRY_JID};
	   String selection = RosterTable.ENTRY_JID + " = ?";
	   String[] selectionArgs = new String[] {entry.mJid};
		   
	   Cursor c = mContext.getContentResolver().query(RosterContentProvider.CONTENT_URI, projection, selection, selectionArgs, null);
	   int count = c.getCount(); 
	   c.close();
	   if (count >= 1) {
	       // already exists -> update
		   mContext.getContentResolver().update(RosterContentProvider.CONTENT_URI, values, selection, selectionArgs);
		   System.out.println("LocalRoster: entry updated: " + entry.mJid);
	   } else {
	       // row does not exist -> insert
		   mContext.getContentResolver().insert(RosterContentProvider.CONTENT_URI, values);
		   System.out.println("LocalRoster: entry inserted: " + entry.mJid);
	   }
	}
	
	public void removeEntry(String jid) {
		if (jid == null || jid.length() == 0)
			throw new IllegalArgumentException("jid is empty or null: " + jid);
		
	   // preparing params for a query
	   String where = RosterTable.ENTRY_JID + " = ?";
	   String[] args = new String[] {jid};
	   
	   mContext.getContentResolver().delete(RosterContentProvider.CONTENT_URI, where, args);
	}
	
	public void setName(String jid, String name) {
		if (jid == null || jid.length() == 0)
			throw new IllegalArgumentException("jid is empty or null: " + jid);
		if (name == null || name.length() == 0)
			throw new IllegalArgumentException("name is empty or null: " + name);
		
		// preparing a data object for the database
	   ContentValues values = new ContentValues();
	   values.put(RosterTable.ENTRY_NAME, name);
	   	   
	   // preparing params for a query
	   String selection = RosterTable.ENTRY_JID + " = ?";
	   String[] selectionArgs = new String[] {jid};
			   
	   mContext.getContentResolver().update(RosterContentProvider.CONTENT_URI, values, selection, selectionArgs);
	   System.out.println("LocalRoster: name updated for " + jid + " new name is " + name);
	}
		
	public LocalPresence getPresence(String jid) {
		if (jid == null || jid.length() == 0)
			throw new IllegalArgumentException("jid is empty or null: " + jid);
		
		LocalPresence presence = null;

		// preparing params for a query
	   String[] projection = {RosterTable.ENTRY_JID};
	   String selection = RosterTable.ENTRY_JID + " = ?";
	   String[] selectionArgs = new String[] {jid};
			   
	   Cursor c = mContext.getContentResolver().query(RosterContentProvider.CONTENT_URI, projection, selection, selectionArgs, null);
	   int count = c.getCount(); 
	   if (count == 1 && c != null) {
		   c.moveToFirst();
		   int code = c.getColumnIndexOrThrow(RosterTable.ENTRY_STATUS_CODE);
		   String status = c.getString(c.getColumnIndexOrThrow(RosterTable.ENTRY_STATUS_MSG));
		   boolean hasMessages = c.getInt(c.getColumnIndexOrThrow(RosterTable.ENTRY_HAS_MESSAGES)) > 0;
		   presence = new LocalPresence(status, code, hasMessages);
	   }
	   c.close();
	   
	   return presence;
	}
	
	public void setPresence(String jid, String status, Presence.Type type, Presence.Mode mode) {
		if (jid == null || jid.length() == 0)
			throw new IllegalArgumentException("jid is empty or null: " + jid);
		
		int code = PresenceStatus.getPresenceCode(type, mode); 
		if (status == null) { status = " "; }
		
		// preparing a data object for the database
		ContentValues values = new ContentValues();
		values.put(RosterTable.ENTRY_STATUS_CODE, code);
		values.put(RosterTable.ENTRY_STATUS_MSG, status);
		   	   
		// preparing params for a query
		String selection = RosterTable.ENTRY_JID + " = ?";
		String[] selectionArgs = new String[] {jid};
				   
		mContext.getContentResolver().update(RosterContentProvider.CONTENT_URI, values, selection, selectionArgs);
		System.out.println("LocalRoster: presence updated for " + jid + " presence: " + code);		
	}
	
	public void setPresence(String jid, boolean unreadMessages) {
		if (jid == null || jid.length() == 0)
			throw new IllegalArgumentException("jid is empty or null: " + jid);
		
		// preparing a data object for the database
		ContentValues values = new ContentValues();
		values.put(RosterTable.ENTRY_HAS_MESSAGES, unreadMessages);
		   	   
		// preparing params for a query
		String selection = RosterTable.ENTRY_JID + " = ?";
		String[] selectionArgs = new String[] {jid};
				   
		mContext.getContentResolver().update(RosterContentProvider.CONTENT_URI, values, selection, selectionArgs);
		System.out.println("LocalRoster: " + jid + " updated, has messages: " + unreadMessages);		
	}
	
	public void setDistance(String jid, int distance, long timestamp) {
		if (jid == null || jid.length() == 0)
			throw new IllegalArgumentException("jid is empty or null: " + jid);
		if (distance < 0)
			throw new IllegalArgumentException("distance is negative: " + distance);
		if (timestamp < 0)
			throw new IllegalArgumentException("timestamp is negative: " + timestamp);
		
		long newTimestamp = timestamp / 1000;
		
		// data holder object for the database;
		ContentValues values = new ContentValues();
		values.put(RosterTable.ENTRY_DISTANCE, distance);
		values.put(RosterTable.ENTRY_TIMESTAMP, newTimestamp);
		
		// params for a query
		String where = RosterTable.ENTRY_JID + " = ?";
		String[] selectionArgs = new String[] {jid};
		
		mContext.getContentResolver().update(RosterContentProvider.CONTENT_URI, values, where, selectionArgs);
		System.out.println("LocalRoster: " + jid + " updated, new distance: " + distance + " timestamp: " + newTimestamp);
	}
	
	public void setPresenceAllOffline() {
		
		// preparing a data object for the database
		ContentValues values = new ContentValues();
		values.put(RosterTable.ENTRY_STATUS_CODE, PresenceStatus.offline.ordinal());

		// sql will update all rows if where clause is null
		mContext.getContentResolver().update(RosterContentProvider.CONTENT_URI, values, null, null);
		System.out.println("LocalRoster:  all set offline");
	}
	
	public void clear() {
		mContext.getContentResolver().delete(RosterContentProvider.CONTENT_URI, null, null);
	}
	
	public void update() {
		mContext.getContentResolver().notifyChange(RosterContentProvider.CONTENT_URI, null);
	}
	
}
