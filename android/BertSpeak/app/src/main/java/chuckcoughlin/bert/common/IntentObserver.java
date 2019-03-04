/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */
package chuckcoughlin.bert.common;

import android.content.Intent;

import java.util.List;

/**
 * Interface for entities which need to be informed about state
 * and other changes within the various managers. Recipients should
 * filter on the intent action.
 */
public interface IntentObserver {
    /**
     * Call this method after an observer newly registers. The
     * intention is to allow the observer to "catch-up" with the
     * state of the manager.
     * @param list of intents that describe the current state
     */
    void initialize(final List<Intent> list);
    void update(final Intent intent);
}
