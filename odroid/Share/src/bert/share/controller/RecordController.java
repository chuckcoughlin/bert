/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License. 
 */
package bert.share.controller;

/**
 *  A client controller that handles accepting position data from the server
 *  at a configured cadence and writing them to a SQLite database.
 */
public class RecordController implements Controller  {
	protected static final String CLSS = "RecordController";

	public RecordController() {
	}

}
