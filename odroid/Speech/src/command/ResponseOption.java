/**
 *   (c) 2015  ILS Automation. All rights reserved. 
 */
package com.ils.tf.gateway.command;


/**
 * These are the possible responses for a command. They dictate the script
 * player controller's next action.
 */
public enum ResponseOption
{
		COMPLETE,
		CONTINUE,
		HALT,          // Halt everything
		REPEAT,        // Re-execute same class
		RESET_METRICS,
		START,
		STOP,
		TIMESCALE
       ;
           
 /**
  * @return  a comma-separated list of all command types in a single String.
  */
  public static String names()
  {
    StringBuffer names = new StringBuffer();
    for (ResponseOption type : ResponseOption.values())
    {
      names.append(type.name()+", ");
    }
    return names.substring(0, names.length()-2);
  }
}
