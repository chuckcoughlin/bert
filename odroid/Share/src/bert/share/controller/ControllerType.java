/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;

/**
 * These are the known types of controller client classes.
 */
public enum ControllerType
{
            COMMAND,
            JOINT,
            PLAYBACK,
            RECORD,
            TERMINAL
            ;
          
 /**
  * @return  a comma-separated list of the types in a single String.
  */
  public static String names()
  {
    StringBuffer names = new StringBuffer();
    for (ControllerType type : ControllerType.values())
    {
      names.append(type.name()+", ");
    }
    return names.substring(0, names.length()-2);
  }
}
