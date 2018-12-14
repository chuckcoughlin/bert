/**
 * Copyright 2018 ILS Automation. All rights reserved.
 */
package chuckcoughlin.bert.bottle;

import java.io.Serializable;

/**
 * This class holds a request to the server posted by the client. As it is passed
 * across the named pipe, it is serialized into JSON.
 */
public class RequestBottle implements Serializable{
	private static final String CLSS = "RequestBottle";
	private static final long serialVersionUID = -8230038990523790456L;
	private final String command;
	
	public RequestBottle(String cmd) {
		this.command = cmd;
	}

	public String getCommand() { return this.command; }
}
