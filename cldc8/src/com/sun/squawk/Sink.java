package com.sun.squawk;
import java.lang.ref.*;

public class Sink {
    /**
     * Constructs a new reference-object queue.
     */
    public Sink() { }

    final private Object lock = new Object();

	Ref head;

	final boolean put(Ref r) {
		if (r.sink == null) return false;
		r.sink = null;
		r.next = head;
		this.head = r;
		return true;
	}
	
    public Object poll() {
		synchronized (lock) {
			Ref r = head;
			if (r != null) {
				head = r.next;
				return r.self;
			}
		}
		return null;
	}
	
    public Object remove(long timeout)
        throws IllegalArgumentException, InterruptedException
    {
        if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout value");
        }
        synchronized (lock) {
            Ref r = head;
            if (r != null) {
				head = r.next;
				return r.self;
			}
            for (;;) {
                lock.wait(timeout);
                r = head;
                if (r != null) {
					head = r.next;
					return r.self;
				}
                if (timeout != 0) return null;
            }
        }
    }

    /**
     * Removes the next reference object in this queue, blocking until one
     * becomes available.
     *
     * @return A reference object, blocking until one becomes available
     * @throws  InterruptedException  If the wait is interrupted
     */
    public Object remove() throws InterruptedException {
        return remove(0);
    }

}
