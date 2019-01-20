/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.motor;

/**
 * These are properties of each stepper motor.
 */
public enum JointProperty
{
	ID,
	MAXIMUMANGLE,
	MINIMUMANGLE,
	MOTORTYPE,
	OFFSET,
	ORIENTATION,
	POSITION,
	SPEED,
	TORQUE
	;
          
 /**
  * @return  a comma-separated list of all property types in a single String.
  */
  public static String names()
  {
    StringBuffer names = new StringBuffer();
    for (JointProperty type : JointProperty.values())
    {
      names.append(type.name()+", ");
    }
    return names.substring(0, names.length()-2);
  }
}
