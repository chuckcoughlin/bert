/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.bottle;

/**
 * Success or failure indication regarding the status of the accompanying request.
 */
public enum ResponseType
{
            OK,
            NONE
            ;
          
 /**
  * @return  a comma-separated list of status types in a single String.
  */
  public static String names()
  {
    StringBuffer names = new StringBuffer();
    for (ResponseType type : ResponseType.values())
    {
      names.append(type.name()+", ");
    }
    return names.substring(0, names.length()-2);
  }
}
