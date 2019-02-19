/**
 * Copyright 2018 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bert.common;

import java.util.LinkedList;

/**
 * Extend a LinkedList to bound its size.
 */

public class FixedSizeList<E> extends LinkedList<E> {
    private static final long serialVersionUID = 5843873110336467006L;
    private int bufferSize = 10;

    public FixedSizeList(int length) {
        this.bufferSize = length;
    }

    public int getBufferSize() { return bufferSize; }

    public synchronized void setBufferSize(int size) {
        // Whittle down the list, if necessary
        if( size<1 ) size = 0;
        while( this.size()>size ) {
            remove();
        }
        bufferSize = size;
    }
    public boolean isFull() {
        return size()>=bufferSize;
    }

    @Override
    public synchronized boolean add(E o) {
        if( bufferSize>0) {
            super.add(o);
            while (size() > bufferSize) {
                remove();
            }
        }
        return true;
    }
}
