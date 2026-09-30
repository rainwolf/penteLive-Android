package org.pente.gameServer.event;

import java.util.*;

public class SynchronizedQueue<T> {
	
	private final Vector<T> queue = new Vector<>();
	
	public synchronized void add(T obj) {

		queue.addElement(obj);
		
		notifyAll();
	}
	
	public synchronized T remove() throws InterruptedException {
		
		while (queue.isEmpty()) {
			wait();
		}
		
		T o = queue.elementAt(0);
		queue.removeElementAt(0);
		return o;
	}
	public String toString() {
		return Integer.toString(queue.size());
	}
}

