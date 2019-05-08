/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.message;


/**
 * Recognized types of messsage handler. These are used to match specific
 * process instance with definitions in the configuration file. 
 * The "dispatcher" is a server application and accesses all controllers.
 */
public enum HandlerType
{
            COMMAND,
            REQUEST,
            SERIAL,
            TABLET,
            TERMINAL
            ;
          
 /**
  * @return  a comma-separated list of all handler types in a single String.
  */
  public static String names()
  {
    StringBuffer names = new StringBuffer();
    for (HandlerType type : HandlerType.values())
    {
      names.append(type.name()+", ");
    }
    return names.substring(0, names.length()-2);
  }
}
