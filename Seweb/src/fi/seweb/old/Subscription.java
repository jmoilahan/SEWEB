package fi.seweb.old;

import fi.seweb.util.Channel;

public class Subscription {
	
	private EventListener handler;
	private Channel channel;
	
	Subscription(EventListener handler, Channel channel) {
		if (channel == null)
			throw new IllegalArgumentException("Channel must not be null");
		if (handler == null)
			throw new IllegalArgumentException("Handler must not be null");
		
		this.handler = handler;
		this.channel = channel;
	}
	
	/*Subscription (EventListener handler) {
		if (handler == null)
			throw new IllegalArgumentException("Handler must not be null");
		
		this.handler = handler;
		this.channel = new Channel("ALL");
	}*/
	
	EventListener handler() {
		return handler;
	}
	
	Channel getChannel() {
		return channel;
	}
	
	
}
