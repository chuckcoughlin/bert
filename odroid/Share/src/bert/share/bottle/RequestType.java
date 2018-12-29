/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.bottle;

/**
 * LOCAL implies that the request need never be sent to the server..
 */
public enum RequestType
{
            LOCAL
            ;
          
 /**
  * @return  a comma-separated list of the types in a single String.
  */
  public static String names()
  {
    StringBuffer names = new StringBuffer();
    for (RequestType type : RequestType.values())
    {
      names.append(type.name()+", ");
    }
    return names.substring(0, names.length()-2);
  }
}
