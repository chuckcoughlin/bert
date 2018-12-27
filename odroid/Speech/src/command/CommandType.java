/**
 *   (c) 2015  ILS Automation. All rights reserved. 
 */
package com.ils.tf.gateway.command;


/**
 * This enumeration class represents possible types of a command.
 */
public enum CommandType
{
		NONE,
		HALT,
		LIST_ASSERTION,            // "contains"
		LIST_SIZE_ASSERTION,       // "size"
		LOG_MESSAGE,               // Text to be logged
		NOTIFICATION_DEFINITION,        // alias for a notification
		NUMERIC_PARAMETER_SETTER,  // set a numeric parameter
		RANGE_ASSERTION,
		RELATIONAL_ASSERTION,
		REQUIRE,
		RUN,
		SCRIPT,                    // run a Python script
		SCRIPT_DEFINITION,         // alias for a Python script
		SELECT,                    // make a selection on a screen
		SHOW,                      // display a screen
		START,                     // start a secondary player
		STOP,                      // stop a secondary player
		TAG_DATA,                  // a time-stamped line of data
		TAG_DEFINITION,            // alias for a tag-path
		TAG_PROVIDER_SETTER,       // configure a tag provider 
		TAG_SET_DEFINITION,        // specify a set of tag names
		TAG_SET_SELECTION,         // specify which tag set is current
        TEXT_PARAMETER_SETTER,     // set a text parameter 
        TIME_PARAMETER_SETTER,     // set a time parameter  (value is always seconds)
        VARIABLE_DEFINITION,       // define a variable
        WAIT,                      // wait for a fixed time interval
        UNTIL                      // wait until an assertion becomes true
       ;
           
 /**
  * @return  a comma-separated list of all command types in a single String.
  */
  public static String names()
  {
    StringBuffer names = new StringBuffer();
    for (CommandType type : CommandType.values())
    {
      names.append(type.name()+", ");
    }
    return names.substring(0, names.length()-2);
  }
}
