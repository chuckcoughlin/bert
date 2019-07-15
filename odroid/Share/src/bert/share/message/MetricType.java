/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.message;

/**
 * These quantities are attributes of the robot as a whole.
 */
public enum MetricType
{
            AGE,
            CADENCE,
            CYCLECOUNT,
            CYCLETIME,
            DUTYCYCLE,
            HEIGHT,
            MITTENS,  // why
            NAME
            ;
          
 /**
  * @return  a comma-separated list of status types in a single String.
  */
  public static String names()
  {
    StringBuffer names = new StringBuffer();
    for (MetricType type : MetricType.values())
    {
      names.append(type.name()+", ");
    }
    return names.substring(0, names.length()-2);
  }
}
