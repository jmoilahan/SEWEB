package fi.seweb.client.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import fi.seweb.R;
import fi.seweb.client.common.PresenceStatus;
import fi.seweb.client.core.Buddy;
import fi.seweb.client.core.LocalPresence;

public class BuddyListAdapter extends ArrayAdapter<Buddy> {	
	public static final String TAG = "BuddyListAdapter";
    private final ArrayList<Buddy> items;
    private final LayoutInflater vi;

    public BuddyListAdapter(Context context, int textViewResourceId,
            ArrayList<Buddy> items) {
        super(context, textViewResourceId, items);
        this.items = items;
        this.vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    
    @Override
    public void notifyDataSetChanged() {
    	sortItemsByPresence();
    	super.notifyDataSetChanged();
    }
    /* moves offline buddies down & online buddies up the list */
    private void sortItemsByPresence() {
    	if (items.isEmpty()) {
    		return;
    	}
    	Collections.sort(items, mBuddyPresenceComparator);
    }
    
    public void updatePresenceAllOffline() {
    	if (!items.isEmpty()) {
    		for (Buddy b : items) {
    			b.setPresenceOffline();
    		}
    	}
    }
    
    /*
     * Updates the dataset with a new value of the presence.
     */
    public void updatePresence(String jid, LocalPresence presence) {
    	if (jid == null || jid.length() == 0)
    		throw new IllegalArgumentException("jid is empty / null");
    	if (presence == null)
    		throw new IllegalArgumentException("presence is null");
    	
    	int index = containsBuddy(jid);
    	
    	if (index >= 0) {
    		Buddy b = items.get(index);
    		b.setPresence(presence.mStatus, presence.mPresenceCode, presence.mUnreadMessages);
			Log.i(TAG, "Presence updated: " + jid);
    	}
    }
    
    
    public void updatePresence(String jid, String status, int code) {
    	if (jid == null || jid.length() == 0)
    		throw new IllegalArgumentException("jid is empty / null");
    	    	
    	int index = containsBuddy(jid);
    	
    	if (index >= 0) {
    		Buddy b = items.get(index);
    		// preserve the notification
    		boolean hasMessages = b.getPresence().mUnreadMessages;
    		b.setPresence(status,  code, hasMessages);
			Log.i(TAG, "Presence updated: " + jid);
    	}
    }
    
    public void updatePresence(String jid, boolean hasMessages) {
    	if (jid == null || jid.length() == 0)
    		throw new IllegalArgumentException("jid is empty / null");
    	    	
    	int index = containsBuddy(jid);
    	
    	if (index >= 0) {
    		Buddy b = items.get(index);
    		// preserve the status & the code
    		String status = b.getPresence().mStatus;
    		int code = b.getPresence().mPresenceCode;
    		// update the presence
    		b.setPresence(status,  code, hasMessages);
			Log.i(TAG, "Presence updated: " + jid);
    	}
    }
    
    
    /* Adds a set of items */
    public void addItems(ArrayList<Buddy> newData) {
    	if (newData == null || newData.isEmpty())
    		throw new IllegalArgumentException("arraylist is null or empty");
    	
    	/*for (Buddy b : newData) {
    		addItem(b);
    	}*/
    	items.clear();
    	items.addAll(newData);
    	Log.i(TAG, "addItems: added " + items.size() + " items");
    }
    
    /* adds a single item */
    public void addItem(Buddy buddy) {
    	if (buddy == null)
    		throw new IllegalArgumentException("buddy is null");
    	
    	int index = containsBuddy(buddy.mJid); 
    	if  (index >= 0) { 
    		//items.remove(index);
    		items.add(index, buddy);
    	} else {
    		items.add(buddy);
    	}
    }
    
    private int containsBuddy(String jid) {
    	if (jid == null || jid.length() == 0)
    		throw new IllegalArgumentException("jid is empty / null");
    	
    	if (items.isEmpty()) {
    		return -1;
    	} else {
    		for (Buddy b : items) {
    			if (b.mJid.equalsIgnoreCase(jid)) {
    				return items.indexOf(b);
    			}
    		}
    	}
    	return -1;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
    	
    	View v = convertView;
        if (v == null) {
            v = vi.inflate(R.layout.row_buddy_list, null);
        }
        
        Buddy buddy = items.get(position);
    	Log.i(TAG, "Buddy selected: " + buddy.mJid);

        if (buddy != null) {
            TextView jidView = (TextView) v.findViewById(R.id.buddyJid);
            ImageView iv = (ImageView) v.findViewById(R.id.buddyIcon);
            TextView statusView = (TextView) v.findViewById(R.id.buddyStatus);
            
            int img; 
            int code = buddy.getPresence().mPresenceCode;
            boolean hasMessages = buddy.getPresence().mUnreadMessages;
            
            if (hasMessages) {
            	img = R.drawable.chat;
            } else {
            	img = PresenceStatus.getRosterIconId(code);
            }
            
            /*
            switch (code) {
            case SewebPreferences.PRESENCE_OFFLINE:
            	if (hasMessages) {
            		//img = R.drawable.presence_offline_message;
            		img = R.drawable.chat;
            	} else {
            		img = R.drawable.presence_offline;
            	}
            	break;
            case SewebPreferences.PRESENCE_AVAILABLE:
            	if (hasMessages) {
            		//img = R.drawable.presence_online_message;
            		img = R.drawable.chat;
            	} else {
                	img = R.drawable.presence_online;
            	}
            	break;
            case SewebPreferences.PRESENCE_AWAY:
            	if (hasMessages) {
            		//img = R.drawable.presence_away_message;
            		img = R.drawable.chat;
            	} else {
                	img = R.drawable.presence_away;
            	}
            	break;
            case SewebPreferences.PRESENCE_CHAT:
            	if (hasMessages) {
            		//img = R.drawable.presence_online_message;
            		img = R.drawable.chat;
            	} else {
            		img = R.drawable.presence_online;
            	}
            	break;
            case SewebPreferences.PRESENCE_DND:
            	if (hasMessages) {
            		//img = R.drawable.presence_dnd_message;
            		img = R.drawable.chat;
            	} else {
                	img = R.drawable.presence_dnd;
            	}
            	break;
            case SewebPreferences.PRESENCE_XA:
            	if (hasMessages) {
            		//img = R.drawable.presence_away_message;
            		img = R.drawable.chat;
            	} else {
            		img = R.drawable.presence_na;
            	}
            	break;
            default:
            	if (hasMessages) {
            		img = R.drawable.chat;
            		//img = R.drawable.presence_online_message;
            	} else {
                	img = R.drawable.presence_online;
            	}
            	break;
            }
            */
            iv.setImageResource(img);

            if (jidView != null) {
            	jidView.setText(buddy.getName());
                jidView.setVisibility(View.VISIBLE);
            }
            
            if (statusView != null) {
            	statusView.setText(buddy.getPresence().mStatus);
            	statusView.setVisibility(View.VISIBLE);
            }
        }
        return v;
    }
    
    /* sorts only by presence value (online -> first, offline -> last) */
    private static Comparator<Buddy> mBuddyPresenceComparator = new Comparator<Buddy> () {
		@Override
		public int compare(Buddy b1, Buddy b2) {
			
			int firstCode = b1.getPresence().mPresenceCode;
			int secondCode = b2.getPresence().mPresenceCode;
			
			if (firstCode == secondCode) {
				return 0;
			}
				
			/*if (firstCode == SewebPreferences.PRESENCE_OFFLINE) {
				return 1;
			}
			
			if (secondCode == SewebPreferences.PRESENCE_OFFLINE) {
				return -1;
			}*/
			
			if (firstCode == PresenceStatus.offline.ordinal()) {
				return 1;
			}
			
			if (secondCode == PresenceStatus.offline.ordinal()) {
				return -1;
			}
			
			return 0;
		}
    };
    
}