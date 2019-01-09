/**
 * Copyright 2018 ILS Automation. All rights reserved.
 */
package bert.share.bottle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Properties;

/**
 * This class holds a request to the server posted by the client. As it is passed
 * across the named pipe, it is serialized into JSON.
 */
public class RequestBottle extends BasicBottle implements Serializable{
	private static final String CLSS = "RequestBottle";
	private static final long serialVersionUID = -8230038990523790456L;
	private RequestType command;
	
	public RequestBottle() {
		this.properties = new Properties();
		this.targets = new ArrayList<>();
		this.motors  = new ArrayList<>();
	}
	
	public RequestBottle(RequestType cmd) {
		this.command = cmd;
		this.properties = new Properties();
	}

	public RequestType getCommand() { return this.command; }
	public void setCommand(RequestType cmd) { this.command = cmd; }
	
}
