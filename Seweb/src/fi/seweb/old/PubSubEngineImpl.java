/**
 * 
 */
package fi.seweb.old;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import fi.seweb.util.Channel;
import fi.seweb.util.Event;
import android.content.Context;
import android.util.Log;

/**
 * @author davidyuk
 *
 */
public class PubSubEngineImpl implements PubSubEngine{
	
	// using a thread safe structure for storing subscriptions
	private static CopyOnWriteArrayList<Subscription> subscriptions;
	//private ArrayList<Subscription> subscriptions;
	
	//a local instance of Communication Mgmt - wrapper for ibicoop CommunicationManager class
	private CommunicationMgmt CMANAGER; //= (new CommunicationMgmt(PubSubEngineImpl.getInstance()));
	
	// singleton
	private static final PubSubEngineImpl INSTANCE = new PubSubEngineImpl();
	
	// singleton
	public static PubSubEngineImpl getInstance() {return INSTANCE;}
	
	// singleton
	private PubSubEngineImpl() {}
	
	// this enum and the field are used for tracking the engine's state 
	private enum State {NEW, INITIALIZING, INITIALIZED};
	
	private final AtomicReference<State> init = new AtomicReference<State>(State.NEW);
	
	//@Override
	public void init(Context context, String username, String password, String terminal_id) {
		if(!init.compareAndSet(State.NEW, State.INITIALIZING))
			throw new IllegalStateException("Already initialized");
		
		//how about initializing subscriptions???
		
		// initializing a CommunicationMgmt wrapper.
		
		//CMANAGER = CommunicationMgmt.getInstance();
		
		//CommunicationMgmt.init(this, context, username, password, terminal_id);
		
		init.set(State.INITIALIZED);
	}
	
	//@Override
	public void init(CommunicationMgmt manager) {
		if(!init.compareAndSet(State.NEW, State.INITIALIZING))
			throw new IllegalStateException("Already initialized");
		
		//how about initializing subscriptions???
		
		CMANAGER = manager;
		
		init.set(State.INITIALIZED);
	}
	
	/* (non-Javadoc)
	 * @see org.arles.pubsubcore.PubSubEngine#startNotification()
	 */
	@Override
	public void startNotification() {
		checkInit();
		CMANAGER.acceptEvents(true);
	}

	/* (non-Javadoc)
	 * @see org.arles.pubsubcore.PubSubEngine#stopNotification()
	 */
	@Override
	public void stopNotification() {
		checkInit();
		CMANAGER.acceptEvents(false);
	}
		
	@Override
	public void publish(Event event) {
		checkInit();
		
		// forward the event to the communication manager
		CMANAGER.sendEvent(event);
	}
	
	// this method has to be removed later
	// to be removed as deprecated
	/*
	@Override
	public Subscription subscribe(EventListener listener) {
		checkInit();
		
		//listener should not be null
		if (listener == null)
			throw new IllegalArgumentException("Passing null as the listener parameter");
		
		if (subscriptions == null) {// lazy initialization;
			subscriptions = new CopyOnWriteArrayList<Subscription>();
			//Log.i("[PubSubEngine]", "subscribe(1): size: " + subscriptions.size());
		}
		
		Subscription subscription = new Subscription(listener, new Channel("ALL"));
		subscriptions.add(subscription);
		
		//Log.i("[PubSubEngine]", "subscribe(2): size: " + subscriptions.size());
		
		return subscription;
	}
	*/
	
	//this is the new method, it should be kept.
	@Override
	public Subscription subscribe(EventListener listener, Channel channel) {
		checkInit();
		
		if (listener == null)
			throw new IllegalArgumentException("Passing null as the listener parameter");
		if (channel == null)
			throw new IllegalArgumentException("Passing null as the channel parameter");
		
		if (subscriptions == null) {// lazy initialization;
			subscriptions = new CopyOnWriteArrayList<Subscription>();
			//Log.i("[PubSubEngine]", "subscribe(1): size: " + subscriptions.size());
		}
		
		Subscription subscription = new Subscription(listener, channel);
		subscriptions.add(subscription);
		
		return null;
	}
	
	
	
	@Override
	public void unsubscribe(Subscription subscription){
		checkInit();
		if (subscriptions != null)
			subscriptions.remove(subscription);
	}

	//dispatching an INCOMING event
	void dispatchEvent(Event event) {
		//	checkInit();
		
		if (subscriptions == null)
			return;
		
		//TODO: Do we need a synchronized block for
		//this invocation??
		
		//A new event has arrived
		//notify all the subscribers about this event.
		Log.i("[PubSubEngine]", "Dispatching an event: " + event);
		Log.i("[PubSubEngine]", "Subscriptions size: " + subscriptions.size());
		
		for (Subscription s : subscriptions){
			// check if the event's channel matches the subscription's channel
			// if (s.getChannel...
			if (s.getChannel().getTopic().equalsIgnoreCase(event.getChannel().getTopic()))
				s.handler().onEventReceived(event);
		}
	}
	

	@Override
	public Channel createChannel(String topic) {
		Channel channel = new Channel(topic);
		
		return channel;
	}
	
	
	@Override
	public void destroyChannel(Channel channel) {
		// TODO Auto-generated method stub
		
		// Destroy channel and
		// Update subscriptions and filters
		
	}
	
	//must be called from all public and protected methods.
	private void checkInit() {
		if (init.get() != State.INITIALIZED)
			throw new IllegalStateException("Uninitialized");
		}
		
	public void terminate() {
		if (init.get() == State.INITIALIZED) {
			CMANAGER.acceptEvents(false);
			CMANAGER.terminate();
			PubSubEngineImpl.subscriptions.clear();
		}
		init.set(State.NEW);
	}
	
}
