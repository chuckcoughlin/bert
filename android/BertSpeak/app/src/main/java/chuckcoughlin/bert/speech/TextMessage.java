/**
 * Copyright 2017-2018 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.speech;

import android.graphics.Color;

import java.util.Date;

/**
 * Instances of this class are displayed in the log.
 */
public class TextMessage {
    private final String msg;
    private final MessageType type;
    private final Date timestamp;

    public TextMessage(String text, MessageType typ) {
        this.msg = text;
        this.type = typ;
        this.timestamp = new Date();
    }

    public String getMessage() { return this.msg; }
    public Date getTimestamp() { return this.timestamp; }
    public MessageType getMessageType() { return this.type; }
}
