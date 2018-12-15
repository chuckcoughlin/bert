/* **********************************************************************
 *   BoundedBuffer.java
 * **********************************************************************
 */
 package bert.share.util;

/**
 * The BoundedBuffer class is designed for use as an object-passing
 * mechanism between a collection of producer and consumer threads.
 *
 * @author adapted from Tom Cargill from "Advanced Java Programming Topics"
 */
public class BoundedBuffer
{
    private Object[]	queue;
    private int		    head = 0;       // Next spot to save
    private int		    tail = 0;       // Next spot to retrieve
    private int         count = 0;      // Current objects in queue
    private int         capacity;


    /**
     * Create a new bounded object buffer of the specified capacity.
     */
    public BoundedBuffer(int cap) {
        if(cap<1) cap = 1;
        this.capacity = cap;
        this.queue = new Object[capacity];
    }

    /**
     * Retrieve an object from the queue. If there are none, this call
     * blocks until an object is entered by another thread.
     */
    public synchronized Object get() throws InterruptedException    {
        waitWhileEmpty();

        Object obj = queue[tail];
        //System.out.println("BoundedBuffer"+this+": get " + obj.getClass().getName()+" at "+tail+" ("+count+")");
        queue[tail] = null;       // Free for garbage collection
        tail = (tail+1) % capacity;
        count--;

        return(obj);
    }

    /**
     * Retrieve all objects from the queue. If there are none, this call
     * blocks until at least one object is entered by another thread.
     */
    public synchronized Object[] getAll() throws InterruptedException  {
        //System.out.println("BoundedBuffer"+this+": getAll waiting while empty ...");
        waitWhileEmpty();

        Object list[] = new Object[count];

        for(int i=0;i<list.length;i++)
        {
            list[i] = get();
        }

        return(list);
    }
    /**
     * Add an object into the queue. If the buffer is at capacity, this call
     * blocks until an object is removed by another thread.
     */
    public synchronized void put(Object obj) throws InterruptedException {
        waitWhileFull();

        queue[head] = obj;
        //System.out.println("BoundedBuffer"+this+": put " + obj.getClass().getName()+" at "+head+" ("+count+")");
        head = (head+1) % capacity;
        count++;
        notifyAll();
        head %= queue.length;
    }

    /**
     * Add an object array into the queue. If the buffer is cannot hold the
     * entire array, this call blocks until objects are removed by another thread.
     */
    public synchronized void putAll(Object [] arr) throws InterruptedException {
        for(int i=0;i<arr.length;i++) {
            put(arr[i]);
        }
    }

    /**
     * Wait until the buffer has at least one object in it.
     */
    public synchronized void waitWhileEmpty() throws InterruptedException   {
        while( isEmpty() ) {
            wait();   // Can be interrupted
        }
    }
    /**
     * Wait until the buffer has at least one free spot in it.
     */
    public synchronized void waitWhileFull() throws InterruptedException {
        while( isFull() )  {
            wait();   // Can be interrupted
        }
    }

    /* **********************************************************************
     *                       Getters/Setters
     * **********************************************************************
     */
    /** @return the queue capacity. */
    public int getCapacity() { return(this.capacity); }
    /** @return a count of the number of objects in the queue.*/
    public synchronized int getCount() { return(this.count); }
    /** @return true if the queue is empty.*/
    public synchronized boolean isEmpty() { return(count==0); }
    /** @return true if the queue is full.*/
    public synchronized boolean isFull() { return(count==capacity); }

}
