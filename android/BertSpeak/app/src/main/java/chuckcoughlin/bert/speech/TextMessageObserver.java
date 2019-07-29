/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.speech;

import chuckcoughlin.bert.service.TextManager;

/**
 * Interface for entities which need to be informed about new text messages
 * that are destined to be logged or enunciated.
 */
public interface TextMessageObserver {
  /**
   * Allow only one observer of a given name.
   * @return the name of the observer
   */
  String getName();
  /**
   * Call this method after an observer newly registers. The
   * manager allows the observer to "catch-up" with the
   * current state of the message list. The manager should be
   * retained and used to refresh the displayed list at will.
   * @param mgr the text manager
   */
  void initialize(TextManager mgr);
  /**
   * Notify the observer that a new text message has been added
   * to the manager's list. Only appropriate client types are
   * included.
   * @param msg the new message
   */
  void update(TextMessage msg);
}
