/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.speech;

import java.util.List;

/**
 * Interface for entities which need to be informed about new text messages
 * that are destined to be logged or enunciated.
 */
public interface TextMessageObserver {
  /**
   * Call this method after an observer newly registers. The
   * intention is to allow the observer to "catch-up" with the
   * state of the manager. This is also called after the user
   * hits "clear".
   * @param list of messages being the most recent retained by the manager.
   */
  void initialize(final List<TextMessage> list);

    /**
     * Notify the observer that a new text meesage has been detected.
     * @param msg the new message
     */
    void update(final TextMessage msg);
}
