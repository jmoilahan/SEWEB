package fi.seweb.old;

import fi.seweb.util.Event;

/*
 * This interface is implement by all client applications that need to receive events. 
 */

public interface EventListener {
	
	/*
	 * @param Event
	 * The event that the pubsub engine notifies the client application about.
	 */
	
	public void onEventReceived(Event e);
}
