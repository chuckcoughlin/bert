/**
 * Copyright 2024-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.data

/**
 * Interface for listeners for generic JSON data. The JSON
 * object is determined by a type tag that accompanies the data.
 * Listeners are free to ignore data with tags that are not
 * of interest.
 */
interface JsonDataObserver {
    /**
     * Allow only one observer of a given name. This is the observer key
     * @return the name of the observer
     */
    val name: String

    /**
     * Call this method after an observer newly registers. The
     * intention is to allow the observer to "catch-up" with the
     * state of the Json data table all at oncw.
     * @param map Json string by type
     */
    fun resetItem(map:Map<JsonType,String>)

    /**
     * Allow the observer to pick whatever type(s) are appropriate.
     * @param map Json string by type
     */
    fun updateItem(map:Map<JsonType,String>)
}
