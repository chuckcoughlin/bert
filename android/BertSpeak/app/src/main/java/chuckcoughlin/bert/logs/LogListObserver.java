/**
 * Copyright 2017-2018 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.logs;

/**
 * Define an observer of the log list. It will always be the
 * first entry in the list that is removed, if any.
 */
public interface LogListObserver {
  public void notifyListAppended();
  public void notifyListCleared();
}
