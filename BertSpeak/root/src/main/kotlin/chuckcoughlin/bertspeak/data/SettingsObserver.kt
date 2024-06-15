/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.data

import chuckcoughlin.bertspeak.common.NameValue

/**
 * Interface for entities which need to be informed about one
 * or more values in the database settings table.
 */
interface SettingsObserver {
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
    fun resetSettings(list: List<NameValue>)
    fun updateSetting(data: NameValue)
}