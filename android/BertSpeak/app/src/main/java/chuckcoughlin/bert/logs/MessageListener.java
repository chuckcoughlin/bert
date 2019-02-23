/**
 * Copyright 2017 Charles Coughlin. All rights reserved.
 * (MIT License)
 */

package chuckcoughlin.bert.logs;


/**
 * Create a model class for various lists - a name/value pair.
 */

public interface MessageListener  {
    public void onNewMessage (LogMessage msg );
}
