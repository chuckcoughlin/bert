/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;


/**
 * Recognized types of controllers. These are used to match specific
 * controller instance definitions in the configuration file with what
 * is needed by the particular application. The distributer application
 * accesses all controllers.
 */
public enum ControllerType
{
            COMMAND,
            SERIAL,
            TERMINAL
            ;
          
 /**
  * @return  a comma-separated list of all controller types in a single String.
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
