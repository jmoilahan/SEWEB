package fi.seweb.client.db;

import java.util.Arrays;
import java.util.HashSet;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class RosterContentProvider extends ContentProvider {

	private static final String TAG = "RosterContentProvider";
	
	// database
	private RosterDBHelper database;

	private static final String AUTHORITY = "fi.seweb.provider.Roster";
	private static final String BASE_PATH = "roster";
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
	
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/roster";
	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/roster";
	
	// constants used for the UriMacher
	private static final int ALL = 100;
	private static final int ROSTER_ID = 20;

	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	
	static {
		sURIMatcher.addURI(AUTHORITY, BASE_PATH, ALL);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/*", ROSTER_ID);
	}
	
	@Override
	public boolean onCreate() {
	   database = new RosterDBHelper(getContext());
	   return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
	      String[] selectionArgs, String sortOrder) {
		
		Log.i(TAG, "query()");
		
		// Using SQLiteQueryBuilder instead of query() method
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		// check if the caller has requested an existing column
		checkColumns(projection);

		// Set the table
		queryBuilder.setTables(RosterTable.TABLE_ROSTER);
		
		// SELECT RosterTable.ENTRY_ID, RosterTable.ENTRY_JID, RosterTable.ENTRY_NAME,
		// RosterTable.ENTRY_STATUS_MSG, RosterTable.ENTRY_STATUS_CODE, RosterTable.ENTRY_HAS_MESSAGES
		// from RosterTable.TABLE_ROSTER
		// INNER JOIN RosterTable.TABLE_GROUP ON RosterTable.ENTRY_ID = RosterTable.GROUP_ID
		// WHERE
		
		//http://www.copperykeenclaws.com/setting-up-an-android-contentprovider-with-a-join/
		
		//sport: _ID, name, periodType, updatedDt
		//team: _ID, name, sportId, updatedDt
		
		//String tables = "team LEFT OUTER JOIN sport ON (team.sportId = sport._id)";
		//queryBuilder.setTables(tables)
		
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case ALL:
			Log.i(TAG, "query, ALL");
			if (sortOrder == null) {
				sortOrder = RosterTable.ENTRY_STATUS_CODE;
			}
			break;
		case ROSTER_ID:
			Log.i(TAG, "query, ROSTER_ID");
			// adding the ID to the original query
			queryBuilder.appendWhere(RosterTable.ENTRY_ID + "=" + uri.getLastPathSegment());
	    	break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		
		SQLiteDatabase db = database.getWritableDatabase();
		Cursor cursor = queryBuilder.query(db, projection, selection,
	        selectionArgs, null, null, sortOrder);
		
		// make sure that potential listeners are getting notified
		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case ALL:
			return CONTENT_TYPE;
		case ROSTER_ID:
			return CONTENT_ITEM_TYPE;
		default:
			return null;
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.i(TAG, "insert()");
		
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();

	    long id = 0;
	    switch (uriType) {
	    case ALL:
	    	id = sqlDB.insert(RosterTable.TABLE_ROSTER, null, values);
	    	break;
	    default:
	    	throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
	    getContext().getContentResolver().notifyChange(uri, null);
	    return Uri.parse(BASE_PATH + "/" + id);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		Log.i(TAG, "delete()");
		
		int uriType = sURIMatcher.match(uri);
		
		SQLiteDatabase sqlDB = database.getWritableDatabase();
	    int rowsDeleted = 0;
	    switch (uriType) {
	    case ALL:
	    	rowsDeleted = sqlDB.delete(RosterTable.TABLE_ROSTER, selection,
	          selectionArgs);
	    	break;
	    case ROSTER_ID:
	    	String id = uri.getLastPathSegment();
	    	if (TextUtils.isEmpty(selection)) {
	    		rowsDeleted = sqlDB.delete(RosterTable.TABLE_ROSTER,
	        		RosterTable.ENTRY_ID + "=" + id, 
	        		null);
	    	} else {
	    		rowsDeleted = sqlDB.delete(RosterTable.TABLE_ROSTER,
	        		RosterTable.ENTRY_ID + "=" + id 
	        		+ " and " + selection,
	        		selectionArgs);
	    	}
	    	break;
	    default:
	    	throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
	    getContext().getContentResolver().notifyChange(uri, null);
	    return rowsDeleted;
	  }	

	  @Override
	  public int update(Uri uri, ContentValues values, String selection,
	      String[] selectionArgs) {
		  Log.i(TAG, "update()");
		    
	    int uriType = sURIMatcher.match(uri);
	    SQLiteDatabase sqlDB = database.getWritableDatabase();
	    
	    int rowsUpdated = 0;
	    
	    switch (uriType) {
	    case ALL:
	    	Log.i(TAG, "update(): case ALL");
	    	rowsUpdated = sqlDB.update(RosterTable.TABLE_ROSTER, 
	          values, 
	          selection,
	          selectionArgs);
	    	break;
	    case ROSTER_ID:
	    	Log.i(TAG, "update(): case ROSTER_ID: ");
	    	String id = uri.getLastPathSegment();
	    	if (TextUtils.isEmpty(selection)) {
	    		rowsUpdated = sqlDB.update(RosterTable.TABLE_ROSTER,
	    			values,
	    			RosterTable.ENTRY_ID + "=" + id, 
	    			null);
	    		Log.i(TAG, "update(): selection is empty");
	    	} else {
	    		rowsUpdated = sqlDB.update(RosterTable.TABLE_ROSTER,
	    			values,
	    			RosterTable.ENTRY_ID + "=" + id 
	    			+ " and " 
	    			+ selection,
	    			selectionArgs);
	    		Log.i(TAG, "update(): selection: " + selection);
	    	}
	    	break;
	    default:
	    	throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
	    
	    getContext().getContentResolver().notifyChange(uri, null);
	    return rowsUpdated;
	  }

	  private void checkColumns(String[] projection) {
	    String[] available = RosterTable.PROJECTION; 
	    		/*{ RosterTable.ENTRY_ID, RosterTable.ENTRY_JID, RosterTable.ENTRY_NAME, RosterTable.ENTRY_STATUS_CODE,
	    		RosterTable.ENTRY_HAS_MESSAGES, RosterTable.ENTRY_STATUS_MSG  TODO: ADD GROUP COLUMNS};*/
	    
	    if (projection != null) {
	      HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
	      HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
	      // check if all the columns which are requested are available
	      if (!availableColumns.containsAll(requestedColumns)) {
	    	  StringBuilder builder = new StringBuilder();
		    	for (String s : projection) {
		    		builder.append(" ");
		    		builder.append(s);
		    	}
		        throw new IllegalArgumentException("Unknown columns in projection: " + builder.toString());
	      }
	    }
	  }

}
