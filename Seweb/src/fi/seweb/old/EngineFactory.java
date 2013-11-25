package fi.seweb.old;

import org.ibicoop.communication.common.CommunicationConstants;
import org.ibicoop.communication.common.CommunicationManager;
import org.ibicoop.communication.common.CommunicationMode;
import org.ibicoop.communication.common.CommunicationOptions;
import org.ibicoop.exceptions.MalformedIbiurlException;
import org.ibicoop.init.IbicoopInit;
import org.ibicoop.sdp.naming.IBIURL;

import android.content.Context;
import android.util.Log;

public class EngineFactory {
	
	//singleton
	private static final EngineFactory INSTANCE = new EngineFactory(); 
	// Configuration parameters:
	// Event queue capacity (in events) // default = 100;
	private final int CAPACITY = 500;
	
	//singleton
	private EngineFactory() {}
	
	//caching the instance of the Engine
	private PubSubEngine cachedEngine = null;
	
	
	//singleton
	public static EngineFactory getInstance() {
		return INSTANCE;
	}
	
	public PubSubEngine build(Context context, String username, String password, String terminal_id) {
		
		if (cachedEngine != null)
			return cachedEngine;
		
		EventQueue queue = new EventQueue(CAPACITY);
		
		// starting IbiCoop
		if (!IbicoopInit.getInstance().isStarted())
			initIbiCoop(context, username, password, terminal_id);
		
				
		EventDispatcher dispatcher = new EventDispatcher();
		Thread thread = new Thread(dispatcher);
		thread.setDaemon(true);
		thread.setName("EventDispatcher");
		
		Object[] dispatcherConfig = initIbiConstants(context, username, password, terminal_id);
		
		IBIURL remoteService = (IBIURL) dispatcherConfig[0];
		IBIURL localService = (IBIURL) dispatcherConfig[1];
		
		CommunicationManager manager = (CommunicationManager) IbicoopInit.getInstance().getCommunicationManager();
		
		dispatcher.init(queue, manager, remoteService, localService);
		thread.start();
		
		PubSubEngineImpl engine = PubSubEngineImpl.getInstance();
		
		CommunicationMgmt my_manager = new CommunicationMgmt(engine, queue);
		my_manager.init(manager, createReceiverURL(username, terminal_id), getOptions());
		
		engine.init(my_manager);
		
		//copying the reference to the cache
		cachedEngine = engine;
		
		return (PubSubEngine) engine; 
	}
	
	private CommunicationOptions getOptions() {
		int mode = CommunicationConstants.MODE_SOCKET;
		CommunicationOptions options = new CommunicationOptions();
		options.setCommunicationMode(new CommunicationMode (mode));
		
		return options;
	}
	
	private void initIbiCoop(Context context, String username, String password, String terminal_id) {

		// check if ibicoop platform is already running
		 if (IbicoopInit.getInstance().isStarted())
			return;
				
		Log.i("EngineFactory", "Launching the IbiCoop platform");
			
		Object[] ibiconf = new Object[4];
		ibiconf[0] = context;
		ibiconf[1] = username;
		ibiconf[2] = password;
		ibiconf[3] = terminal_id; // terminal id which is the same as the engine id
				
		IbicoopInit.getInstance().start(ibiconf);
	}
	
	private static IBIURL createReceiverURL (String username, String term_id) {
		IBIURL localServiceReceiver = null;
		
		try {
			localServiceReceiver = new IBIURL("ibiurl", username, term_id, "pubsubengine", "communication_manager", "eventnotification");
		} catch (Exception e) {
			Log.e("EngineFactory", "Failed to create receiver's URL");
			throw new AssertionError(e.getMessage());
		}
		
		return localServiceReceiver;
	}
	
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
		//CommunicationMgmt.localService = localService;
		
		return dispatcherConfig;
	}

}
