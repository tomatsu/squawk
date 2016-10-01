package java.lang.ref;

import com.sun.squawk.Sink;

public class ReferenceQueue<T> {

	Sink sink;
	
    /**
     * Constructs a new reference-object queue.
     */
    public ReferenceQueue() {
		this.sink = new Sink();
	}

    public Reference poll() {
		return (Reference)sink.poll();
	}
	
    /**
     * Removes the next reference object in this queue, blocking until either
     * one becomes available or the given timeout period expires.
     *
     * <p> This method does not offer real-time guarantees: It schedules the
     * timeout as if by invoking the {@link Object#wait(long)} method.
     *
     * @param  timeout  If positive, block for up to <code>timeout</code>
     *                  milliseconds while waiting for a reference to be
     *                  added to this queue.  If zero, block indefinitely.
     *
     * @return  A reference object, if one was available within the specified
     *          timeout period, otherwise <code>null</code>
     *
     * @throws  IllegalArgumentException
     *          If the value of the timeout argument is negative
     *
     * @throws  InterruptedException
     *          If the timeout wait is interrupted
     */
    public Reference remove(long timeout)
        throws IllegalArgumentException, InterruptedException
    {
		return (Reference)sink.remove(timeout);
    }

    /**
     * Removes the next reference object in this queue, blocking until one
     * becomes available.
     *
     * @return A reference object, blocking until one becomes available
     * @throws  InterruptedException  If the wait is interrupted
     */
    public Reference remove() throws InterruptedException {
        return remove(0);
    }

}
