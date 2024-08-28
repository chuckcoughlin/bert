/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.data

import chuckcoughlin.bertspeak.common.NameValue

/**
 * Interface for listeners one a list of text strings.
 */
interface TextObserver {
    /**
     * Allow only one observer of a given name. This is the observer key
     * @return the name of the observer
     */
    val name: String

    /**
     * Call this method after an observer newly registers. The
     * intention is to allow the observer to "catch-up" with the
     * state of settings table all at oncw.
     * @param list of NameValue that describe the current settings
     */
    fun resetList(list: List<String>)

    /**
     * Mark one element of the list os special in some context
     */
    fun selectItem(index:Int)
    fun updateItem(index:Int,value: String)
}
