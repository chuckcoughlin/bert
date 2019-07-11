/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 * This class should be identical in both Linux and Android worlds.
 */
package bert.share.message;

/**
 * These are the recognized header strings for messages between the tablet
 * and robot. The header is separated from the body of the message by a ':'.
 * The message is terminated with a ';'.
 */
public enum MessageType
{
	ANS,	// Reply from the robot. The tablet should "speak" the contents
	LOG,    // A system message meant to be appended to the "Log" panel.
	MSG,    // Request or query from the tablet, plain english  
	ROW,    // Pipe-delimited data to be added to the most recent table          
	TBL		// Define a table for the table panel. Pipe-delimited title and column headings
    ;
          
 /**
  * @return  a comma-separated list of the types in a single String.
  */
  public static String names()
  {
    StringBuffer names = new StringBuffer();
    for (MessageType type : MessageType.values())
    {
      names.append(type.name()+", ");
    }
    return names.substring(0, names.length()-2);
  }
}
