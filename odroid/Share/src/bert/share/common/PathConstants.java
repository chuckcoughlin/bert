/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package bert.share.common;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *  These paths can be considered static constants once BERT_HOME has been set.
 *  This should be done soon after startup.
 */
public class PathConstants   {   
	public static Path ROBOT_HOME = Paths.get(System.getProperty("user.dir")).getRoot();
	public static Path CONFIG_PATH= null; 
	public static Path DB_PATH    = null; 
	public static Path DEV_DIR    = null;
	public static Path LOG_DIR    = null;
	public static String LOG_FILE = "bert.log";
	
	static {
		setHome(ROBOT_HOME);
	}


	public static void setHome(Path home) {
		ROBOT_HOME = home;
		CONFIG_PATH= Paths.get(ROBOT_HOME.toFile().getAbsolutePath(),"etc","bert.xml"); 
		DB_PATH    = Paths.get(ROBOT_HOME.toFile().getAbsolutePath(),"db","actions.idb"); 
		DEV_DIR    = Paths.get(ROBOT_HOME.toFile().getAbsolutePath(),"dev");
		LOG_DIR    = Paths.get(ROBOT_HOME.toFile().getAbsolutePath(),"logs");  
	}
}
