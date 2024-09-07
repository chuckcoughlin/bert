/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.data

/**
 * Interface for listeners for generic JSON data. The JSON
 * object is determined by a tag that accompanies the data.
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
     * state of settings table all at oncw.
     * @param key data type
     * @param json data as a JSON string
     */
    fun resetItem(key: String, json:String)

    /**
     * Update the status of the particular data object
     * @param key data type
     * @param json data as a JSON string
     */
    fun updateItem(key: String, json: String)
}
