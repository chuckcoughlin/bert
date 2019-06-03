/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.sql.db;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sqlite.JDBC;

import bert.share.motor.MotorConfiguration;
import bert.sql.motor.MotorStateTable;
import bert.sql.pose.PoseTable;
/**
 * This class is a wrapper for the entire robot database. It is implemented
 * as a singleton for easy access. The startup() method must be called
 * before it can be used as it opens the database connection.
 * 
 * Call shutdown() when database access is no longer required.
 */
public class Database {
	private final static String CLSS = "Database";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	@SuppressWarnings("unused")
	private final static JDBC driver = new JDBC(); // Force driver to be loaded
	
	private Connection connection = null;
	private static Database instance = null;
	private final MotorStateTable motor;
	private final PoseTable pose;
 

	/**
	 * Constructor is private per Singleton pattern.
	 */
	private Database() {
		this.motor = new MotorStateTable();
		this.pose = new PoseTable();
	}
	/**
	 * Static method to create and/or fetch the single instance.
	 */
	public static Database getInstance() {
		if( instance==null) {
			synchronized(Database.class) {
				instance = new Database();
			}
		}
		return instance;
	}
	/**
	 * 
	 * @param command user entered string
	 * @return the corresponding pose name if it exists, otherwise NULL
	 */
	public String getPoseForCommand(String command) {
		return pose.getPoseForCommand(connection,command);
	}
	
	/** Return a list of column names with non-null values for the indicated pose
	 * property.
	 * @param mcmap a map of configurations. Joints not present are ignored.
	 * @param pose
	 * @param parameter, e.g. "position","speed","torque"
	 * @return list of upper-case joint names.
	 */
	public Map<String,Double> getPoseJointValuesForParameter(Map<String,MotorConfiguration>mcmap,String poseName,String parameter) {
		return pose.getPoseJointValuesForParameter(connection,mcmap,poseName,parameter);
	}
	/**
	 * Populate the Motor table with information from the configuration file.
	 * This table is useful in joins, thus the apparent duplication.
	 * 
	 * @param motors map of motor configurations by uppercase name
	 */
	public void populateMotors(Map<String,MotorConfiguration> motors) {
		Collection<MotorConfiguration> motorList = motors.values();
		motor.defineMotors(connection, motorList);
	}
	/**
	 * Create a database connection. Use this for all subsequent queries. 
	 * @param path to database instance
	 */
	public void startup(Path path) {
		String connectPath = "jdbc:sqlite:"+path.toString();
		LOGGER.info(String.format("%s.startup: database path = %s",CLSS,path.toString()));

		try {
			connection = DriverManager.getConnection(connectPath);
		}
		catch(SQLException e) {
			// if the error message is "out of memory", 
			// it probably means no database file is found
			LOGGER.log(Level.SEVERE,String.format("%s.startup: Database error (%s)",CLSS,e.getMessage()));
		}
	}

	/**
	 * Close the database connection prior to stopping the application.
	 * 
	 * @param path to database instance
	 */
	public void shutdown() {
		LOGGER.info(String.format("%s.shutdown",CLSS));

		if( connection!=null) {
			try {
				connection.close();
			}
			catch(SQLException e) {
				// if the error message is "out of memory", 
				// it probably means no database file is found
				LOGGER.warning(String.format("%s.shutdown: Error closing database (%s)",CLSS,e.getMessage()));
			}
		}
	}

}
