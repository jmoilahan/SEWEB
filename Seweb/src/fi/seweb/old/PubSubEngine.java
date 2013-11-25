package fi.seweb.old;

import fi.seweb.util.Channel;
import fi.seweb.util.Event;
import android.content.Context;

public interface PubSubEngine {
	
	public void startNotification();
	
	public void stopNotification();
	
	public void publish (Event event);
	
	//to be removed
	//public Subscription subscribe(EventListener listener);
	
	public Subscription subscribe(EventListener listener, Channel channel);
	
	public void unsubscribe(Subscription subscription);
	
	//public void init (Context context, String username, String password, String terminal_id);
	
	//public void init (CommunicationMgmt manager);
	
	public Channel createChannel(String topic);
	
	public void destroyChannel(Channel channel);
	
	/*
	public Channel[] searchChannels(String filter);
	*/
}
