/**
 *   (c) 2014-2015  ILS Automation. All rights reserved. 
 */
package chuckcoughlin.bert.common;


/**
 * This enumeration class represents the permissible states of a diagram.
 */
public enum DynamixelType
{
            ACTIVE,
            DISABLED,
            ISOLATED,
            UNSET
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
