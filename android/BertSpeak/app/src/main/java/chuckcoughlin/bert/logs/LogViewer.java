/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.logs;

import java.util.List;

import chuckcoughlin.bert.speech.TextMessage;

/**
 * Interface to fragments that display log messages. The "viewer"
 * is the interested fragment and provides only messages of its
 * configured type.
 */
public interface LogViewer {
  /**
   * Retrieve a log message from the retained list.
   * @param position index of message
   * @return the message at the indicated index
   */
  public TextMessage getLogAtPosition(int position);
  /**
   * Retrieve the current list of log files. The list has a
   * fixed maximum and may be truncated as time goes on.
   * @return a list of log messages.
   */
  public List<TextMessage> getLogs();
}
