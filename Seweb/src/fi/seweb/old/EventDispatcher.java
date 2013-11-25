package fi.seweb.old;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.ibicoop.communication.common.CommunicationConstants;
import org.ibicoop.communication.common.CommunicationManager;
import org.ibicoop.communication.common.CommunicationMode;
import org.ibicoop.communication.common.CommunicationOptions;
import org.ibicoop.communication.common.IbiSender;
import org.ibicoop.communication.common.SenderListener;
import org.ibicoop.exceptions.ConnectionFailedException;
import org.ibicoop.init.IbicoopInit;
import org.ibicoop.sdp.naming.IBIURL;

import fi.seweb.util.Event;
import android.util.Log;

/*
 * A daemon thread for emptying up the event queue.
 *  to be able to run, it needs
 *  1) Initialized IbiCoop
 *  2) Initialized Event Queue
 *   
 */

public class EventDispatcher implements Runnable, SenderListener {
	
	private static EventQueue queue = null;
	private static CommunicationOptions options;
	private static CommunicationManager manager;
	private static IBIURL remoteService;
	private static IBIURL localService;

	public EventDispatcher() {
				
	}
	
	/* Initializes the thread. Absolutely must be called before thread.start()!
	 * 
	 * Sets a reference to the queue,
	 * caches a RemoteService URL (i.e. all other pub/sub engines) and LocalService URL (the very same device)
	 * 
	 * @throws IllegalStateException if the thread is not able to get a reference to a running IbiCoop Communication Manager.
	 *  
	 *  */
	
	public void init (EventQueue queue, CommunicationManager manager, IBIURL remoteService,  IBIURL localService) {
				
		Log.i("[Event Dispatcher]", "Initializing the thread");
		
		if (manager == null) 
			throw new IllegalArgumentException("IbiCoop Communication Manager is null");
		if (queue == null) 
			throw new IllegalArgumentException("Event queue is null");
		if (remoteService == null || localService == null)
			throw new IllegalArgumentException("Passing null remoteService/localService");
		if (!remoteService.isValidIbiurl()) 
			throw new IllegalArgumentException("Not valid/null remoteService IbiURL!");
		if (!localService.isValidIbiurl())	
			throw new IllegalArgumentException("Not valid/null localService IbiURL!");
		
		EventDispatcher.queue = queue;
		EventDispatcher.remoteService = remoteService;
		EventDispatcher.localService  = localService;
		EventDispatcher.manager = manager; 
		
		int mode = CommunicationConstants.MODE_SOCKET;
		EventDispatcher.options = new CommunicationOptions();
		EventDispatcher.options.setCommunicationMode(new CommunicationMode (mode));
	}
	
	@Override
	public void run() {
		Log.i("[Event Dispatcher]", "Running the thread");
		
		while (true) {
			if (queue.isEmpty()) {
				synchronized (queue) {
					try {
						Log.i("Event Dispatcher", "Waiting for new events to be added to the queue...");
						queue.wait();
					} catch (InterruptedException e){ Log.i("Event Dispatcher", e.getMessage());}
				//wakeUpAndDispatch();
				}
			}
			dispatch();
		}
	}
	
	// this method is called by the event queue when a new event is added
	// it calls a) creates a new sender createSender()
	//          b) sends the event to this sender sendEvent()
	
	public void dispatch() {
		
		//Log.i("[Event Dispatcher]", "Dispatching events from the queue.");
		
		while (!queue.isEmpty()) {
			Event e = queue.get();
			IbiSender s = createSender();
			if ( (s != null) && (e != null) ) {
				//Log.i("[Event Dispatcher]", "s != null, e != null");
				//Log.i("[Event Dispatcher]", "Dispatching an event from the queue.");
				sendEvent(s, e);
			}
		}
		
		/*		 
		if (queue.isEmpty()) {
			Log.i("[Event Dispatcher]", "Queue is empty, PERKELE!");
			System.out.println("Queue is empty, PERKELE!, quitting");
			return;
		}*/
		
		
		/*
		if (queue.size() > 1) {
			Log.i("[Event Dispatcher]", "Inside If (1)");
			Iterator<Event> i = queue.iterator();
			
			while (i.hasNext()) {
				Event e = i.next();
				IbiSender s = createSender();
				if (s != null) {
					Log.i("[Event Dispatcher]", "s != null");
					sendEvent(s, e);
					i.remove();
				}
			}
		} else if (queue.size() == 1) {
			
			Log.i("[Event Dispatcher]", "Inside If (2)");
			
			Event e = queue.poll();
			IbiSender s = createSender();
			if ( (s != null) && (e != null) ) {
				Log.i("[Event Dispatcher]", "s != null, e != null");
				sendEvent(s, e);
			}
		}
		*/
		
		
	}
	
	private IbiSender createSender() {
		IbiSender s = null;

		try {
			//Log.i("[Event Dispatcher]", "LocalService URL" + EventDispatcher.localService);
			//Log.i("[Event Dispatcher]", "RemoteService URL" + EventDispatcher.remoteService);
			
			s = manager.createSender(EventDispatcher.localService, EventDispatcher.remoteService,
					                 EventDispatcher.options, this);
			
			Log.i("[Event Dispatcher]", "Creating a sender: success!...");
			
		} catch (ConnectionFailedException e) {
			Log.e("[Event Dispatcher]", "Failed to create an IbiSender: " + e.getMessage());
		}
		
		return s;
	}
	
	private void sendEvent(IbiSender s, Event event) {
		if (event == null)
			throw new IllegalStateException("Failed to send an event. Event is null");
		if (s == null)
			throw new IllegalStateException("Failed to send an event. Sender is null");
		
		try {
			byte[] data = null;
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = null;
			
			out = new ObjectOutputStream(bos);   
			out.writeObject(event);
			
			data = bos.toByteArray();
			
			out.close();
			bos.close();
			
			Log.i("[Event Dispatcher]", "Event is serialized! ");
			
			s.send(data);
			
			Log.i("[Event Dispatcher]", "Event is sent! ");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	//A part of the ibicoop SenderListener interface
	@Override
	public void connectionStatus(IbiSender sender, int statusCode,
			String statusMessage) {
		
	}

	//A part of the ibicoop SenderListener interface
	@Override
	public void receivedMessageResponse(IbiSender sender, String receiverId,
			int requestId, byte[] data) {
		
	}

}
