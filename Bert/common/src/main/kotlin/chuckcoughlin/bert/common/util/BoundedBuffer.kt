/* **********************************************************************
 *   BoundedBuffer.java
 * **********************************************************************
 */
package chuckcoughlin.bert.common.util

/**
 * The BoundedBuffer class is designed for use as an object-passing
 * mechanism between a collection of producer and consumer threads.
 *
 * @author adapted from Tom Cargill from "Advanced Java Programming Topics"
 */
class BoundedBuffer(cap: Int) {
    private val queue: Array<Any?>
    private var head = 0 // Next spot to save
    private var tail = 0 // Next spot to retrieve
    private var count = 0 // Current objects in queue
    private val capacity: Int

    /**
     * Create a new bounded object buffer of the specified capacity.
     */
    init {
        var cap = cap
        if (cap < 1) cap = 1
        capacity = cap
        queue = arrayOfNulls(capacity)
    }

    /**
     * Retrieve an object from the queue. If there are none, this call
     * blocks until an object is entered by another thread.
     */
    @Synchronized
    @Throws(InterruptedException::class)
    fun get(): Any? {
        waitWhileEmpty()
        val obj = queue[tail]
        //System.out.println("BoundedBuffer"+this+": get " + obj.getClass().getName()+" at "+tail+" ("+count+")");
        queue[tail] = null // Free for garbage collection
        tail = (tail + 1) % capacity
        count--
        return obj
    }//System.out.println("BoundedBuffer"+this+": getAll waiting while empty ...");

    /**
     * Retrieve all objects from the queue. If there are none, this call
     * blocks until at least one object is entered by another thread.
     */
    @get:Throws(InterruptedException::class)
    @get:Synchronized
    val all: Array<Any?>
        get() {
            //System.out.println("BoundedBuffer"+this+": getAll waiting while empty ...");
            waitWhileEmpty()
            val list = arrayOfNulls<Any>(count)
            for (i in list.indices) {
                list[i] = get()
            }
            return list
        }

    /**
     * Add an object into the queue. If the buffer is at capacity, this call
     * blocks until an object is removed by another thread.
     */
    @Synchronized
    @Throws(InterruptedException::class)
    fun put(obj: Any?) {
        waitWhileFull()
        queue[head] = obj
        //System.out.println("BoundedBuffer"+this+": put " + obj.getClass().getName()+" at "+head+" ("+count+")");
        head = (head + 1) % capacity
        count++
        notifyAll()
        head %= queue.size
    }

    /**
     * Add an object array into the queue. If the buffer is cannot hold the
     * entire array, this call blocks until objects are removed by another thread.
     */
    @Synchronized
    @Throws(InterruptedException::class)
    fun putAll(arr: Array<Any?>) {
        for (i in arr.indices) {
            put(arr[i])
        }
    }

    /**
     * Wait until the buffer has at least one object in it.
     */
    @Synchronized
    @Throws(InterruptedException::class)
    fun waitWhileEmpty() {
        while (isEmpty) {
            wait() // Can be interrupted
        }
    }

    /**
     * Wait until the buffer has at least one free spot in it.
     */
    @Synchronized
    @Throws(InterruptedException::class)
    fun waitWhileFull() {
        while (isFull) {
            wait() // Can be interrupted
        }
    }
    /* **********************************************************************
     *                       Getters/Setters
     * **********************************************************************
     */
    /** @return the queue capacity.
     */
    fun getCapacity(): Int {
        return capacity
    }

    /** @return a count of the number of objects in the queue.
     */
    @Synchronized
    fun getCount(): Int {
        return count
    }

    /** @return true if the queue is empty.
     */
    @get:Synchronized
    val isEmpty: Boolean
        get() = count == 0

    /** @return true if the queue is full.
     */
    @get:Synchronized
    val isFull: Boolean
        get() = count == capacity
}