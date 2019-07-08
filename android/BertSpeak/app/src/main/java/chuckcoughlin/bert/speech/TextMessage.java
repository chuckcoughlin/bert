/**
 * Copyright 2017-2018 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.speech;

import java.util.Date;

import chuckcoughlin.bert.common.MessageType;

/**
 * Instances of this class are displayed in the log.
 */
public class TextMessage {
    private final String msg;
    private final MessageType type;
    private final Date timestamp;

    public TextMessage(MessageType typ, String text) {
        this.msg = text;
        this.type = typ;
        this.timestamp = new Date();
    }

    public String getMessage() { return this.msg; }
    public Date getTimestamp() { return this.timestamp; }
    public MessageType getMessageType() { return this.type; }
}
