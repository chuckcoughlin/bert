/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.sql.pose;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * This class serves as a Java interface to the Pose table. It provides 
 * methods to extract a pose
 */
public class PoseTable {
	private static final String CLSS = "PoseTable";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	/** 
	 * Constructor: 
	 */
	public PoseTable() {

	}
	
	
	/**
	 * A pose is a list of positions for each motor. There is only
	 * one row in the database for each pose.
	 * 
	 * @param cxn open database connection
	 * @param name of the pose that is desired
	 * 
	 * @return a list of motor positions. If the pose did not
	 *         exist, the list will be empty.
	 */
	public void getPose(Connection cxn,String name) {
		ResultSet rs = null;
		try {
			Statement statement = cxn.createStatement();
			statement.setQueryTimeout(10);  // set timeout to 10 sec.
			
			rs = statement.executeQuery("select * from SfcClassMap");
			while(rs.next())
			{
				String g2 = rs.getString("G2Class");
			}
			rs.close();
		}
		catch(SQLException e) {
			// if the error message is "out of memory", 
			// it probably means no database file is found
			LOGGER.severe(String.format("%s.startup: Database error (%s)",CLSS,e.getMessage()));
		}
		finally {
			if( rs!=null) {
				try { rs.close(); } catch(SQLException ignore) {}
			}
		}
	}
	
}