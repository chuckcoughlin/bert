/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.server.controller;


/**
 * Recognized names of SequentialQueues managed by the InternalController. These correspond to
 * roughly independent sub-chains of the joint tree.
 */
public enum QueueName
{
            HEAD,
            RIGHT_ARM,
            RIGHT_LEG,
            LEFT_ARM,
            LEFT_LEG,
            GLOBAL
            ;
          
 /**
  * @return  a comma-separated list of all queue name in a single String.
  */
  public static String names()
  {
    StringBuffer names = new StringBuffer();
    for (QueueName type : QueueName.values())
    {
      names.append(type.name()+", ");
    }
    return names.substring(0, names.length()-2);
  }
}
