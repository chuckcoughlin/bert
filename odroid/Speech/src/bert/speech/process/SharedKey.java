/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.speech.process;

/**
 * These are properties in the shared dictionary that can be shared from
 * instance to instance.
 */
public enum SharedKey
{
            AXIS,
            SIDE
            ;
          
 /**
  * @return  a comma-separated list keys in a single String.
  */
  public static String names()
  {
    StringBuffer names = new StringBuffer();
    for (SharedKey type : SharedKey.values())
    {
      names.append(type.name()+", ");
    }
    return names.substring(0, names.length()-2);
  }
}