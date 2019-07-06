/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.logs;

import java.util.List;

import chuckcoughlin.bert.speech.TextMessage;

/**
 * Interface fragments that display log messages. The "viewer"
 * is in-fact a proxy for the DispatchService that has the actual
 * log message list.
 */
public interface LogViewer {
  /**
   * Retrieve a message from the retained list.
   * @param position index of message
   * @return the messsage at the indicated index
   */
  public TextMessage getLogAtPosition(int position);
  /**
   * Retrieve the current list of log files. The list may be
   * truncated as time goew on.
   * @return the messsage at the indicated index
   */
  public List<TextMessage> getLogs();
}
