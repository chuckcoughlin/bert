/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.sql.db;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.sqlite.JDBC;

import bert.sql.pose.Pose;
/**
 * This class is a wrapper for the entire robot database. It is implemented
 * as a singleton for easy access. The startup() method must be called
 * before it can be used as it opens the database connection.
 * 
 * Call shutdown() when database access is no longer required.
 */
public class Database {
	private final static String CLSS = "Database";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	private Connection connection = null;
	
	private static Database instance = null;
	@SuppressWarnings("unused")
	private final static JDBC driver = new JDBC(); // Force driver to be loaded
	private final Pose pose;
 

	/**
	 * Constructor is private per Singleton pattern.
	 */
	private Database() {
		this.pose = new Pose();
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
	 * Create a database connection. Use this for all subsequent queries.
	 * 
	 * @param path to database instance
	 */
	public void startup(Path path) {
		String connectPath = "jdbc:sqlite:"+path.toString();
		LOGGER.log(Level.INFO, String.format("%s.startup: database path = %s",CLSS,path.toString()));

		try {
			connection = DriverManager.getConnection(connectPath);
		}
		catch(SQLException e) {
			// if the error message is "out of memory", 
			// it probably means no database file is found
			LOGGER.log(Level.ERROR,String.format("%s.startup: Database error (%s)",CLSS,e.getMessage()));
		}
		finally {
			try {
				if(connection != null)
					connection.close();
			} 
			catch(SQLException e) {
				// connection close failed.
				LOGGER.log(Level.ERROR,String.format("%s.startup: Error closing database (%s)",CLSS,e.getMessage()));
			}
		}
	}

	/**
	 * Close the database connection prior to stopping the application.
	 * 
	 * @param path to database instance
	 */
	public void shutdown() {
		LOGGER.log(Level.INFO, String.format("%s.shutdown",CLSS));

		if( connection!=null) {
			try {
				connection.close();
			}
			catch(SQLException e) {
				// if the error message is "out of memory", 
				// it probably means no database file is found
				LOGGER.log(Level.WARNING,String.format("%s.shutdown: Error closing database (%s)",CLSS,e.getMessage()));
			}
		}
	}

}
