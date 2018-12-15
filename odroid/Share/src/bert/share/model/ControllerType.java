/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.model;

/**
 * ASYNCHRONOUS means that the caller does not wait for a response.
 * SYNCHRONOUS  is for real-time interactions where the caller blocks waiting for a response.
 */
public enum ControllerType
{
            ASYNCHRONOUS,
            SYNCHRONOUS
            ;
          
 /**
  * @return  a comma-separated list of the two types in a single String.
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
