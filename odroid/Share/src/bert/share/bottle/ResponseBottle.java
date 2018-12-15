/**
 * Copyright 2018 ILS Automation. All rights reserved.
 */
package bert.share.bottle;

import java.io.Serializable;

/**
 * This class holds a response from the server to the client. As it is passed
 * across the named pipe, it is serialized into JSON.
 */
public class ResponseBottle implements Serializable, Cloneable {
	private static final String CLSS = "ResponseBottle";
	private static final long serialVersionUID = 2311476737631983034L;
	private final String status;
	
	public ResponseBottle(String s) {
		this.status = s;
	}

	public String getStatus() { return this.status; }
}
