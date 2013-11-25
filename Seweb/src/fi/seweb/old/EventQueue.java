package fi.seweb.old;

import java.util.Collection;
import java.util.Iterator;

import fi.seweb.util.Event;
import fi.seweb.util.ForwardingQueue;
import fi.seweb.util.ForwardingVector;


// this class provides an abstraction of implementation details of the event queue
// the class is thread safe, and this is ensured by the internal synchronization 
// mechanism of the LinkedBlockingQueue (see the ForwardingQueue class for details). 

//we don't need to serialize this class.
@SuppressWarnings("serial")
public class EventQueue extends ForwardingQueue<Event> {
	
	private static final int DEFAULT_CAPACITY = 100;

	public EventQueue() {
		super(DEFAULT_CAPACITY);
		
	}
	
	public EventQueue(int capacity) {
		super(capacity);
	}
	
	@Override public boolean add (Event e) {

		boolean result = false;
		
		synchronized (this) {
			result = super.add(e); 
			notify();
		}
		
		return result;
	}
	
	@Override public boolean addAll(Collection<? extends Event> c) {
		boolean result = false;
		
		synchronized (this) {
				result = super.addAll(c); 
				notify();
			}
		return result; 
	}
	
	
	@Override public Iterator<Event> iterator() {
		return super.iterator();
	}
	
	// returns null of the queue is empty
	public Event get() {
		return super.poll();
	}
	
	@Override public int size() {
		return super.size();
	}
	
	@Override public int drainTo(Collection<? super Event> c) {
		return super.drainTo(c);
	}
	
	@Override public boolean isEmpty() {
		return super.isEmpty();
	}
	
}
