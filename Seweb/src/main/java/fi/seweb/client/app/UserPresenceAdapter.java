package fi.seweb.client.app;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import fi.seweb.R;
import fi.seweb.client.common.SewebPreferences;
import fi.seweb.client.core.UserPresence;
import fi.seweb.client.core.UserPresence.Builder;

public class UserPresenceAdapter extends ArrayAdapter<UserPresence> {
	
	public static final String TAG = "UserPresenceAdapter";
    private final ArrayList<UserPresence> items;
    private final LayoutInflater vi;

    public UserPresenceAdapter(Context context, int textViewResourceId,
            ArrayList<UserPresence> items) {
        super(context, textViewResourceId, items);
        this.items = items;
        this.vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    
    /*
     * Updates the dataset with a new value of the presence.
     * Adds the new presence object if the dataset is empty 
     */
    public void updatePresence(UserPresence newPresence) {
    	
    	boolean found = false;
    	
    	if (!items.isEmpty()) {
    		for (UserPresence p : items) {
    			String user = p.user; 
    			if (user.equalsIgnoreCase(newPresence.user)) { // found the user
    				found = true;
    				//the index of the current element
    				int index = items.indexOf(p);
    				//replace the exising presence
    				items.set(index, newPresence);
    				Log.i(TAG, "Updated the presence: " + user);
    			}
    		}
    	}
    	
    	if (items.isEmpty() || !found) {
    		items.add(newPresence);
    	}
    }
    
    /*
     * jid has to be a full jid, like:
     */
    public void updatePresenceNewMessage(String jid) {
    	Log.i(TAG, "updatePresenceNewMessage() called: " + jid);
    	
    	if (!items.isEmpty()) {
    		for (UserPresence p : items) {
    			String user = p.user; 
    			if (user.equalsIgnoreCase(jid)) { // found the user
    				//the index of the current element
    				int index = items.indexOf(p);
    				//replace the exising presence
    				UserPresence.Builder builder = new Builder(p.user, p.presenceCode, p.statusMessage);
    				UserPresence newPresence = builder.setUnreadMessage().build(); 
    				items.set(index, newPresence);
    				Log.i(TAG, "Updated the presence: " + user);
    			}
    		}
    	}
    }
    
    public void clearNotification(String jid) {
    	Log.i(TAG, "clearNotification() called " + jid);
    	
    	if (!items.isEmpty()) {
    		for (UserPresence p : items) {
    			String user = p.user; 
    			if (user.equalsIgnoreCase(jid)) { // found the user
    				//the index of the current element
    				int index = items.indexOf(p);
    				//replace the exising presence
    				items.set(index, (new UserPresence.Builder(p.user, p.presenceCode, p.statusMessage)).build());
    				Log.i(TAG, "Updated the presence: " + user);
    			}
    		}
    	}
    }
    
    
	
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = vi.inflate(R.layout.row_buddy_list, null);
        }
        
        UserPresence presence = items.get(position);
        Log.i(TAG, "Presence: " + presence.user);

        if (presence != null) {
            TextView jidView = (TextView) v.findViewById(R.id.buddyJid);
            ImageView iv = (ImageView) v.findViewById(R.id.buddyIcon);
            TextView statusView = (TextView) v.findViewById(R.id.buddyStatus);
            
            int img; 
            
            switch (presence.presenceCode) {
            case SewebPreferences.PRESENCE_OFFLINE:
            	img = R.drawable.presence_offline;
            	break;
            case SewebPreferences.PRESENCE_AVAILABLE:
            	img = R.drawable.presence_online;
            	break;
            case SewebPreferences.PRESENCE_AWAY:
            	img = R.drawable.presence_away;
            	break;
            case SewebPreferences.PRESENCE_CHAT:
            	img = R.drawable.presence_online;
            	break;
            case SewebPreferences.PRESENCE_DND:
            	img = R.drawable.presence_dnd;
            	break;
            case SewebPreferences.PRESENCE_XA:
            	img = R.drawable.presence_away;
            	break;
            default:
            	img = R.drawable.presence_online;
            	break;
            }
            iv.setImageResource(img);

            if (jidView != null) {
            	if (presence.unreadMessages) {
            		jidView.setText(presence.user + " (+1 new message)");
                	jidView.setPaintFlags(Paint.FAKE_BOLD_TEXT_FLAG);
                } else {
                	jidView.setText(presence.user);
                }
                jidView.setVisibility(View.VISIBLE);
            }
            
            if (statusView != null) {
            	statusView.setText(presence.statusMessage);
            	statusView.setVisibility(View.VISIBLE);
            }
        }
        return v;
    }
}