/**
 * Copyright 2017-2018 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.logs;

import java.util.Date;

/**
 * Instances of this class are displayed in the log.
 */
public class LogMessage {
    private final String msg;
    private final boolean request;
    private final Date timestamp;

    public LogMessage(String text,boolean in) {
        this.msg = text;
        this.request = in;
        this.timestamp = new Date();
    }

    public String getMessage() { return this.msg; }
    public Date getTimestamp() { return this.timestamp; }
    public boolean isRequest() { return this.request; }
}
