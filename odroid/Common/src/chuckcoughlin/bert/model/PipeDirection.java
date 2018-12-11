/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package chuckcoughlin.bert.model;

/**
 * INPUT implies Bert->Core
 * OUTPUT implies Core->Bert
 */
public enum PipeDirection
{
            INPUT,
            OUTPUT
            ;
          
 /**
  * @return  a comma-separated list of the two directions in a single String.
  */
  public static String names()
  {
    StringBuffer names = new StringBuffer();
    for (PipeDirection type : PipeDirection.values())
    {
      names.append(type.name()+", ");
    }
    return names.substring(0, names.length()-2);
  }
}
