/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.common

import kotlin.jvm.Synchronized
import java.util.*

/**
 * Extend a LinkedList to bound its size.
 */
class FixedSizeList<E>(length: Int) : LinkedList<E>() {
    private var bufferSize = 10
    fun getBufferSize(): Int {
        return bufferSize
    }

    @Synchronized
    fun setBufferSize(sz: Int) {
        // Whittle down the list, if necessary
        var size = sz
        if (size < 1) size = 0
        while (this.size > sz) remove()
        bufferSize = size
    }

    fun isFull(): Boolean {
        return size >= bufferSize
    }

    @Synchronized
    override fun add(element: E): Boolean {
        if (bufferSize > 0) {
            super.add(element)
            while (size > bufferSize) {
                remove()
            }
        }
        return true
    }

    // Return an iterable list
    @Synchronized
    fun toList() : List<E> {
        val copy = mutableListOf<E>()
        copy.addAll(this)
        return copy
    }

    companion object {
        private const val serialVersionUID = 5843873110336467006L
    }

    init {
        bufferSize = length
    }
}
