/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.bert.common

import android.content.Intent

/**
 * Interface for entities which need to be informed about state
 * and other changes within the various managers. Recipients should
 * filter on the intent action.
 */
interface IntentObserver {
    /**
     * Allow only one observer of a given name
     * @return the name of the observer
     */
    val name: String

    /**
     * Call this method after an observer newly registers. The
     * intention is to allow the observer to "catch-up" with the
     * state of the manager.
     * @param list of intents that describe the current state
     */
    fun initialize(list: List<Intent>)
    fun update(intent: Intent)
}