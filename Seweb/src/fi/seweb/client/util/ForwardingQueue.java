package fi.seweb.client.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

// we don't need any serialization machinery for this class
@SuppressWarnings("serial")
public class ForwardingQueue<T> extends LinkedBlockingQueue<T> {
	private final LinkedBlockingQueue<T> q;
	
	
	public ForwardingQueue() {
		q = new LinkedBlockingQueue<T>();
	}
	
	public ForwardingQueue(int capacity) {
		q = new LinkedBlockingQueue<T> (capacity);
	}
	
	@Override public void clear() { q.clear(); }
	@Override public int drainTo(Collection<? super T> c) { return q.drainTo(c); }
	@Override public int drainTo(Collection<? super T> c, 
			int maxElements) { return q.drainTo(c, maxElements); }
	@Override public Iterator<T> iterator() { return q.iterator(); }
	@Override public boolean add(T e) { return q.add(e); }
	@Override public boolean addAll(Collection<? extends T> c) { return q.addAll(c); }
	
	@SuppressWarnings("hiding")
	@Override public <T> T[] toArray(T[] a) { return q.toArray(a); }
	@Override public boolean remove(Object o) { return q.remove(o); }
	@Override public T peek() { return q.peek(); }
	@Override public T poll() { return q.poll(); }
	@Override public T take() 
			throws InterruptedException { return q.take(); }
	@Override public void put(T e) 
			throws InterruptedException { q.put(e); }
	@Override public int size() { return q.size(); }
	@Override public int remainingCapacity() { return q.remainingCapacity(); }
	@Override public boolean isEmpty() { return q.isEmpty(); } 
	
}
