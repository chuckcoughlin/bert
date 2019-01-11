/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.bottle;

/**
 * These are the recognized commands from command controller to dispatcher.
 */
public enum RequestType
{
            GET_PROPERTY,
            PLAY_STEP,
			RECORD_STEP,
			SET_STATE,       // For a particular motor
			REQUEST_STATE,   // Same as recording a step or pose
			SET_POSE,
			GET_POSE,
			NONE
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
