package fi.seweb.client.core;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import fi.seweb.client.db.MessageContentProvider;
import fi.seweb.client.db.MessageTable;

public class MessageStorage {
	
	public static final String TAG = "MessageStorage";
	
	private Context mContext;
	private final String mJid;
	
	public MessageStorage(Context ctx, String myJid) {
		mContext = ctx;
		mJid = myJid;
	}
	
	/* ChatID = the id set by the XMPP protocol when the chat is created
	 * Message = the message object */
	public void addMessage(String chatID, Message message) {
		if (chatID == null || chatID.length() == 0) 
			throw new IllegalArgumentException("Chat ID is null / empty!");
		if (message == null)
			throw new IllegalArgumentException("Message param is null!");
		
		String to = StringUtils.parseBareAddress(message.getTo());
		String from = StringUtils.parseBareAddress(message.getFrom());
		boolean isIncoming = to.equalsIgnoreCase(mJid);
		
		long timestamp = ( DateTime.now().getMillis() / 1000 );
		
		ContentValues values = new ContentValues();
		values.put(MessageTable.MESSAGE_CHAT_ID, chatID);
		values.put(MessageTable.MESSAGE_BODY, message.getBody());
		values.put(MessageTable.MESSAGE_FROM, from);
		values.put(MessageTable.MESSAGE_TO, to);
		values.put(MessageTable.MESSAGE_TIMESTAMP, timestamp);
		values.put(MessageTable.MESSAGE_IS_INCOMING, isIncoming);
		
		Uri uri = mContext.getContentResolver().insert(MessageContentProvider.CONTENT_URI, values);
		Log.i(TAG, "message inserted: " + uri);
	}
	
}
