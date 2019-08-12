/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.model;


/**
 * Recognized types of Dynamixel motors (minus dashes in name)
 */
public enum DynamixelType
{
            AX12,
            MX28,
            MX64
            ;
          
 /**
  * @return  a comma-separated list of all block states in a single String.
  */
  public static String names()
  {
    StringBuffer names = new StringBuffer();
    for (DynamixelType type : DynamixelType.values())
    {
      names.append(type.name()+", ");
    }
    return names.substring(0, names.length()-2);
  }
}
