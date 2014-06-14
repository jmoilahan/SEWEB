package fi.seweb.client.app;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import android.database.Cursor;
import android.view.View;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;
import fi.seweb.R;
import fi.seweb.client.common.StringUtils;
import fi.seweb.client.db.MessageTable;

public class MessageViewBinder implements ViewBinder {

	@Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				
        int viewId = view.getId();
        switch(viewId) {
            case R.id.txtInfo: //timestamp
                TextView textView = (TextView) view;
                int indexTime = cursor.getColumnIndexOrThrow(MessageTable.MESSAGE_TIMESTAMP);
                int indexFrom = cursor.getColumnIndexOrThrow(MessageTable.MESSAGE_FROM);
                String from = cursor.getString(indexFrom);
                int timeInt = cursor.getInt(indexTime);
               
                long timestamp = ((long) timeInt) * 1000;
        		DateTime dt = new DateTime(timestamp);
        		DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy, MMMM, dd [HH:mm:ss] ");
        		String time = fmt.print(dt);
               	textView.setText(time + from);
            break;
            case R.id.txtMessage: //message body
            	TextView messageView = (TextView) view;
            	int index = cursor.getColumnIndexOrThrow(MessageTable.MESSAGE_BODY);
            	messageView.setText(cursor.getString(index));
            break;
        }
        return true;
    }
/*	
	private boolean isIncomingMessage(Cursor cursor) {
		if (cursor == null)
			return false;
		
		int index = cursor.getColumnIndexOrThrow(MessageTable.MESSAGE_IS_INCOMING);
		return cursor.getInt(index) > 0;
	}
*/
	/*
	private void setAlignment(TextView txt, boolean isIncoming, LinearLayout content, LinearLayout contentWithBG) {
        if (isIncoming) {
            contentWithBG.setBackgroundResource(R.drawable.incoming_message_bg);

            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) contentWithBG.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            contentWithBG.setLayoutParams(layoutParams);

            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) content.getLayoutParams();
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            content.setLayoutParams(lp);
            
            layoutParams = (LinearLayout.LayoutParams) txt.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            txt.setLayoutParams(layoutParams);
            
        } else {
            contentWithBG.setBackgroundResource(R.drawable.outgoing_message_bg);

            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) contentWithBG.getLayoutParams();
            layoutParams.gravity = Gravity.LEFT;
            contentWithBG.setLayoutParams(layoutParams);

            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) content.getLayoutParams();
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            content.setLayoutParams(lp);
            
            layoutParams = (LinearLayout.LayoutParams) txt.getLayoutParams();
            layoutParams.gravity = Gravity.LEFT;
            txt.setLayoutParams(layoutParams);
        }
    }*/
}

