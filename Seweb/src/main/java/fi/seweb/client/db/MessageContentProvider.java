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

public class MessageContentProvider extends ContentProvider {
	
	private static final String TAG = "MessageContentProvider";
	
	// database
	private MessageDBHelper database;

	private static final String AUTHORITY = "fi.seweb.provider.Messages";
	private static final String BASE_PATH = "messages";
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
	
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/message";
	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/message";
	
	// constants used for the UriMacher
	private static final int ALL = 100;
	private static final int MESSAGE_ID = 200;

	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	
	static {
		sURIMatcher.addURI(AUTHORITY, BASE_PATH, ALL);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/*", MESSAGE_ID);
	}

	@Override
	public boolean onCreate() {
	   database = new MessageDBHelper(getContext());
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
		queryBuilder.setTables(MessageTable.MESSAGES_TABLE);
		
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case ALL:
			Log.i(TAG, "query, ALL");
			if (sortOrder == null) {
				sortOrder = MessageTable.MESSAGE_TIMESTAMP;
			}
			break;
		case MESSAGE_ID:
			Log.i(TAG, "query, MESSAGE_ID");
			// adding the ID to the original query
			queryBuilder.appendWhere(MessageTable.MESSAGE_ID + "=" + uri.getLastPathSegment());
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
		case MESSAGE_ID:
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
	    	id = sqlDB.insert(MessageTable.MESSAGES_TABLE, null, values);
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
	    	rowsDeleted = sqlDB.delete(MessageTable.MESSAGES_TABLE, selection,
	          selectionArgs);
	    	break;
	    case MESSAGE_ID:
	    	String id = uri.getLastPathSegment();
	    	if (TextUtils.isEmpty(selection)) {
	    		rowsDeleted = sqlDB.delete(MessageTable.MESSAGES_TABLE,
	        		MessageTable.MESSAGE_ID + "=" + id, 
	        		null);
	    	} else {
	    		rowsDeleted = sqlDB.delete(MessageTable.MESSAGES_TABLE,
	    			MessageTable.MESSAGE_ID + "=" + id 
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
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		Log.i(TAG, "update()");
		    
	    int uriType = sURIMatcher.match(uri);
	    SQLiteDatabase sqlDB = database.getWritableDatabase();
	    int rowsUpdated = 0;
	    switch (uriType) {
	    case ALL:
	    	Log.i(TAG, "update(): case ALL");
	    	rowsUpdated = sqlDB.update(MessageTable.MESSAGES_TABLE, 
	          values, 
	          selection,
	          selectionArgs);
	    	break;
	    case MESSAGE_ID:
	    	Log.i(TAG, "update(): case MESSAGE_ID: ");
	    	String id = uri.getLastPathSegment();
	    	if (TextUtils.isEmpty(selection)) {
	    		rowsUpdated = sqlDB.update(MessageTable.MESSAGES_TABLE,
	    			values,
	    			MessageTable.MESSAGE_ID + "=" + id, 
	    			null);
	    		Log.i(TAG, "update(): selection is empty");
	    	} else {
	    		rowsUpdated = sqlDB.update(MessageTable.MESSAGES_TABLE,
	    			values,
	    			MessageTable.MESSAGE_ID + "=" + id 
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
		if (projection != null) {
			HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
		    HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(MessageTable.PROJECTION));
		    // check if all the columns which are requested are available
		    if (!availableColumns.containsAll(requestedColumns)) {
		    	// print projection:
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
