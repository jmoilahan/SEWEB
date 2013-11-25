package fi.seweb.old;

import java.io.ByteArrayInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import org.ibicoop.communication.common.CommunicationManager;
import org.ibicoop.communication.common.CommunicationOptions;
import org.ibicoop.communication.common.IbiReceiver;
import org.ibicoop.communication.common.ReceiverListener;
import org.ibicoop.exceptions.ConnectionFailedException;
import org.ibicoop.sdp.naming.IBIURL;
import org.ibicoop.utils.DataBuffer;

import fi.seweb.util.Event;
import android.util.Log;

public class CommunicationMgmt implements ReceiverListener {
	
	// a callback reference to the engine
	private static PubSubEngineImpl ENGINE;
	
	private static EventQueue EVENT_QUEUE; 
	
	//private IbiReceiver receiver = null;
	
	//private static CommunicationMgmt self;
	//singleton
	//private static final CommunicationMgmt INSTANCE = new CommunicationMgmt();
	
	//singleton
	//private CommunicationMgmt() {}
	
	//singleton
	//public static CommunicationMgmt getInstance() {return INSTANCE;}
	
	//this enum and the field is to track initialization
	private enum State { NEW, INITIALIZING, INITIALIZED, TERMINATED};
	private final static AtomicReference<State> init = 
			new AtomicReference<State>(State.NEW);
	
	private static boolean isAcceptingEvents = false;
	
	private static IBIURL localService = null;
	
	//new code:
	public CommunicationMgmt(PubSubEngineImpl engine, EventQueue queue) {
		CommunicationMgmt.ENGINE = engine;
		CommunicationMgmt.EVENT_QUEUE = queue;
	}
	
	//new code:
	public void init(CommunicationManager manager, IBIURL receiverURL,
			CommunicationOptions options) {
		
		//State management
		if (init.get() == State.INITIALIZED)
			return;
				
		if ((init.get() == State.INITIALIZING))
			throw new IllegalStateException(
				"[Communication Management]: Already being initialized!");
				
		init.set(State.INITIALIZING);
		
		CommunicationMgmt.localService = receiverURL;

		initReceiver(manager, receiverURL, options);
		
		//Log.i("Communication Management", "Everything is OK! until now");
			//throw new IllegalStateException("[Communication Management]: Failed to initialize IbiReceiver()");
		
		
		// State management
		init.set(State.INITIALIZED);
		Log.i("Communication Management", "Initialized");
	
	}

	/*
	//refactored method
	public IbiReceiver initReceiver(final CommunicationManager manager, final IBIURL localServiceReceiver, final CommunicationOptions options) {
		//checking parameters
		if (manager == null)
			throw new IllegalArgumentException("IbiCoop Communication manager is null!");
		if (localServiceReceiver == null)
			throw new IllegalArgumentException("IBIURL localServiceReceiver is null!");
		if (options == null)
			throw new IllegalArgumentException("Communication options parameter is null!");
		
		IbiReceiver receiver = null;
		Callable<IbiReceiver> task = new ReceiverCreatorTask(manager, localServiceReceiver, options, this);
		ExecutorService executor = new ScheduledThreadPoolExecutor(1);
		Future<IbiReceiver> future = executor.submit(task);
		
		try {
			receiver = future.get();
		} catch (Exception e) {
		      e.printStackTrace();
		}
		Log.i("Communication Management", "Returning (2) ");
		return receiver;
	}
	
	private class ReceiverCreatorTask implements Callable<IbiReceiver> {
				
		private final CommunicationManager manager;
		private final IBIURL url;
		private final CommunicationOptions options;
		private final ReceiverListener listener;
		
		ReceiverCreatorTask(CommunicationManager manager, IBIURL localServiceReceiver, 
							CommunicationOptions options, ReceiverListener listener) {
			
			this.manager = manager;
			this.url = localServiceReceiver;
			this.options = options;
			this.listener = listener;
		}
		@Override
		public IbiReceiver call() throws Exception {
			IbiReceiver receiver = null;
    		try {
    			Log.i("Communication Management", "Attempting to create a IbiReceiver");
        		receiver = manager.createReceiver(url, options, null, listener);
        		} catch (ConnectionFailedException e) {
        			throw new IllegalStateException("[Communication Management]: Failed to create a IbiReceiver");
        		}
    		Log.i("Communication Management", "IbiReceiver created... returning");
			return receiver;
		}
	}
	*/
	
	
	// this method puts an outcoming event to the event queue.
	// the queue will notify the dispatcher to handle to event.
	
	public void sendEvent(Event e) {
		if (isStarted()) {
				CommunicationMgmt.EVENT_QUEUE.add(e);
		} else // not initialized
			throw new IllegalStateException("Has to be initialized to send events");	
	}
	
	//uninitializing the component
	
	public void terminate() {
		// stop receiving events;
		CommunicationMgmt.isAcceptingEvents = false;

		if (isStarted()) {
			
			// TODO: destroy the dispatcher thread and the queue
			
			// 	destroy all running processes
			// 	close all connections
		}
		init.set(State.TERMINATED);
	}
	
	//disables/enables the engine from/to receiving events
	public void acceptEvents(boolean flag) {
		isAcceptingEvents = flag;
	}
	
	// a public accessor method
	public boolean isStarted() {
		return ((init.get() == State.INITIALIZED)) ? true : false;
	}

	// A part of the ReceiverListener interface
	@Override
	public void connectionStatus(IbiReceiver receiver, int statusCode) {
		// TODO Auto-generated method stub
	}
	
	// A part of the ReceiverListener interface
	@Override
	public byte[] receivedMessageRequest(IbiReceiver receiver, String senderId,
			int requestId, int senderConnectionId, byte[] data) {
		// TODO Auto-generated method stub
		return null;
	}

	// this helper method compares two IBIURLs (one passes as a param, second is in the field) 
	// to determine whether an event has to be discarded (if urls match)
	// or kept (if urls do not match)
	
	private boolean discardEvent(String senderId) {
		
		boolean result = false;
		
		// compare two ibiurls using the user's name and terminal id.
		
		String terminal = CommunicationMgmt.localService.getTermid();
		String user = CommunicationMgmt.localService.getUserid();
		
		if (senderId.contains(terminal))
			if(senderId.contains(user))
				//the event has been sent by us! discard it.
				result = true;
				
		return result;
	}
	
	// A part of the ReceiverListener interface
	// This method is called when a new data (bytes) is received.
	
	@Override
	public void receivedMessageData(IbiReceiver receiver, String senderId,
			int requestId, DataBuffer data) {
		
		if (!isStarted())
			return;
		
		// We need to
		//-> 1) Discard the event is if the sender is us, i.e. the very same device.
		//-> 2) Deserialize the event
		//-> 3) Dispatch the event
		
		Log.i("Communication Management","New incoming data (event)");
		
		//-> 1) Discard the event is if the sender is us, i.e. the very same device and the user.
		if (discardEvent(senderId)) {
			Log.i("Communication Management","This is our own event. Skipping...");
			return;
		}
				
		ByteArrayInputStream bis = new ByteArrayInputStream(data.toArray());
		ObjectInput in = null; 
		Event event = null;
		Object obj = null;
		
		// -> 2) Deserializing the event
		try {
			Log.i("Communication Management","Deserializing an object...");
			
			in = new ObjectInputStream(bis);
			obj = in.readObject();
			 
			bis.close();
			in.close();
			  
		} catch (Exception e) {
			Log.i("Communication Management","Deserializing failed");
			throw new AssertionError(e);
		} 
		
		if (obj != null) {
			if (obj instanceof Event) {
				event = (Event) obj;
			}
		} else {
			Log.i("Communication Management","Reading an obj from the incoming datastream failed");
			throw new AssertionError("Deserialization failed");
		}
				
		// -> 3) dispatching the event
		if (event != null) {
			Log.i("Communication Management","Event is deserialized successfully. Dispatching...");
			ENGINE.dispatchEvent(event);
		} else {
			Log.i("Communication Management","Casting an event from object failed...");
			throw new AssertionError("Deserialization failed");
		}
	}

	// A part of the ReceiverListener interface
	@Override
	public boolean acceptSenderConnection(IbiReceiver receiver, String senderID) {
		// the component has to be started and has to have the AcceptEvents set to true
		// i.e. the client has to call engine.init() and engine.startNotification()
		return ((isStarted()) && (isAcceptingEvents));
	}

	/*
	@Override
	public boolean handleMessage(Message msg) {
		// TODO Auto-generated method stub
		return false;
	}
	*/

	//OLD CODE:
		// the method is called by the engine to create and cache 
		// an instance of an IbiCoop communication manager
		// throws IllegalStateException if 1) ibicoop communication management fails to start
		// 2) it fails to create IBIURLs 
	/*	
		public static void init(PubSubEngineImpl engine, Context context, String username, String password, String terminal_id) {
			
			//State management
			if (init.get() == State.INITIALIZED)
				return;
			
			if ((init.get() == State.INITIALIZING))
				throw new IllegalStateException(
						"[Communication Management]: Already being initialized!");
			
			init.set(State.INITIALIZING);
				
			CommunicationMgmt.ENGINE = engine;
			
			//starting IbiCoop
			if (!IbicoopInit.getInstance().isStarted())
			
			initIbiCoop(context, username, password, terminal_id);

			//Creating an IbiCoopReceiver
			initReceiver(username, terminal_id);
			
			//Configuring IbiCoop Communication Manager and the dispatcher thread
			initDispatcher(initIbiConstants(context, username, password, terminal_id));
			
			// State management
			init.set(State.INITIALIZED);
			Log.i("Communication Management", "Initialized");
		}
		
		// starts the IbiCoop platform
		private static void initIbiCoop(Context context, String username, String password, String terminal_id) {

			// check if ibicoop platform is already running
			 if (IbicoopInit.getInstance().isStarted())
				return;
					
			Log.i("Communication Management", "Starting the IbiCoop platform");
				
			Object[] ibiconf = new Object[4];
			ibiconf[0] = context;
			ibiconf[1] = username;
			ibiconf[2] = password;
			ibiconf[3] = terminal_id; // terminal id which is the same as the engine id
					
			IbicoopInit.getInstance().start(ibiconf);
		}
		
		
		// setting constants and configuring IbiCoop Communication Manager
		// this operation has to be done separately from  ibicoopInit();
		
		private static Object[] initIbiConstants(Context context, String username,
				String password, String terminal_id) {

			// a return parameter to configure the dispatcher thread
			Object[] dispatcherConfig = new Object[2];
			
			// 0 - Remote IbiURL
			// 1 - Local Client IbiURL
			
			IBIURL remoteService, localService = null;
			
			try {
				//setting up ibiCoop constants (for caching purpose)
				//remoteService = new IBIURL("ibiurl", "*", "*", "communication_manager", "pubsubengine", "eventnotification");
				remoteService = new IBIURL("ibiurl", "*", "*", "pubsubengine", "communication_manager", "eventnotification");
				localService = new IBIURL("ibiurl", username, terminal_id, "pubsubengine", "clientcallback", "eventnotification");
				
				Log.i("Communication Management", "Remote IBIURL: " + remoteService);
				Log.i("Communication Management", "Local IBIURL: " + localService);
					
			} catch (MalformedIbiurlException e) {
				// translating low -> high level exception
				e.printStackTrace();
				throw new IllegalStateException(
					"[Communication Management]: Failed to create the sender's and receiver's IBIURLs");
			}
			
			// preparing a return parameter for the dispatcher.
			dispatcherConfig[0] = remoteService;
			dispatcherConfig[1] = localService;
			
			//caching an instance of the sender's URL for filtering incoming events, used in receivedMessageData();
			CommunicationMgmt.localService = localService;
			
			return dispatcherConfig;
		}
	
		//old
		// creates an IbicoopReceiver and initializes the Ibicoop Communication Manager
		private static void initReceiver(final String username, final String term_id) {

			final CommunicationManager comMgmt = (CommunicationManager) IbicoopInit.getInstance().getCommunicationManager();
			
			/*
			 new Thread(new Runnable() {
				 	
				 	@Override
			        public void run() {
			        	Log.i("Communication Management", "Creating the IbiCoop Receiver");
			        	
			        	int mode = CommunicationConstants.MODE_SOCKET;
						CommunicationOptions options = new CommunicationOptions();
						options.setCommunicationMode(new CommunicationMode (mode));
						
						IBIURL localServiceReceiver = null; 
						IbiReceiver receiver = null;
						ReceiverListener listener = (ReceiverListener) self;
						
			        	if (comMgmt != null) {
			        		Log.i("Communication Management", "Got IbiCoop Communication Mgmt");
			        		
			        		try {
			        			Log.i("Communication Management", "Inside Try");
			        			
			        			localServiceReceiver = new IBIURL("ibiurl", username, term_id, "pubsubengine", "communication_manager", "eventnotification");
			        					        			
			        			Log.i("Communication Management", "IBI URL created");
			        			
			        			receiver = comMgmt.createReceiver(localServiceReceiver, options, null, listener);
								
								Log.i("Communication Management", "Creating IbiCoop Receiver: Success! " + localServiceReceiver);
			        			
			        		} catch (MalformedIbiurlException e) {
			        			throw new IllegalStateException("[Communication Management]: Bad IBIURL");
			        			
			        		} catch (ConnectionFailedException e) {
			        			throw new IllegalStateException("[Communication Management]: Failed to create a IbiReceiver");
			        		}
			        	}
			        	
			        	if (receiver == null)
			        		throw new IllegalStateException("[Communication Management]: Failed to initialize IbiReceiver()");
			        	
			        }
			    }).start();*/
	/*
		}
		
		// creates and starts the dispatcher thread;
		// creates the event queue
		
		private static void initDispatcher(Object[] config) {
			
			EVENT_QUEUE = new EventQueue();
			
			EventDispatcher dispatcher = new EventDispatcher();
			Thread thread = new Thread(dispatcher);
			thread.setDaemon(true);
			thread.setName("EventDispatcher");
			
		
			IBIURL remoteService = (IBIURL) config[0];
			IBIURL localService = (IBIURL) config[1];
			
			//TODO: change it to either a factory or 
			// to a pass parameter on init()
			
			CommunicationManager manager = (CommunicationManager) IbicoopInit.getInstance().getCommunicationManager();
			
			dispatcher.init(EVENT_QUEUE, manager, remoteService, localService);
			
			thread.start();
		}
		*/
}
