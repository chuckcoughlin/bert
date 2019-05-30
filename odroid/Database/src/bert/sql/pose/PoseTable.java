/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.sql.pose;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A pose is a list of positions for each motor. There are up to
 * three rows in the database for each pose. A row each for:
 * 		position, speed and torque
 * This class serves as a Java interface to the Pose and PoseMap tables. It provides 
 * methods for finding and reading a pose
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
	 * Find the pose associated with a command.
	 * @cxn an open database connection
	 * @param command user entered string
	 * @return the corresponding pose name if it exists, otherwise NULL
	 */
	public String getPoseForCommand(Connection cxn,String command) {
		PreparedStatement statement = null;
		ResultSet rs = null;
		command = command.toLowerCase();
		String pose = null;
		String SQL = "select pose from PoseMap where command = ?"; 
		try {
			statement = cxn.prepareStatement(SQL);
			statement.setQueryTimeout(10);  // set timeout to 10 sec.
			statement.setString(1,command);
			rs = statement.executeQuery();
			while(rs.next()) {
				pose = rs.getString("pose");
				LOGGER.info(String.format("%s.getPoseForCommand: %s is %s",CLSS,command,pose));
				break;
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
			if( statement!=null) {
				try { statement.close(); } catch(SQLException ignore) {}
			}
		}
		return pose;
	}
	
	/** Return a list of column names with non-null values for the indicated pose
	 * property. There should only be one (or none) row returned.
	 * @param pose
	 * @param parameter, e.g. "position","speed","torque"
	 * @return list of upper-case joint names.
	 */
	public Map<String,Double> getPoseJointValuesForParameter(Connection cxn,String pose,String parameter) {
		Map<String,Double> map = new HashMap<>();
		PreparedStatement statement = null;
		ResultSet rs = null;
		pose = pose.toLowerCase();
		pose = pose.toLowerCase();
		String SQL = "select * from pose where pose = ? and parameter = ? ";
		try {
			statement = cxn.prepareStatement(SQL);
			statement.setQueryTimeout(10);  // set timeout to 10 sec.
			statement.setString(1,pose);
			statement.setString(2,parameter);
			rs = statement.executeQuery();

			ResultSetMetaData meta = rs.getMetaData();
			int colCount = meta.getColumnCount();
			if( rs.first()) {
				rs.next();
				for( int col=1;col<=colCount;++col) {
					String name = meta.getColumnName(col);
					if( name.equalsIgnoreCase("pose")) continue;
					if( name.equalsIgnoreCase("parameter")) continue;
					Object val = rs.getObject(col);
					if(val != null ) {
						Double dbl = Double.valueOf(val.toString());
						map.put(name.toUpperCase(), dbl);
					}
				}
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
			if( statement!=null) {
				try { statement.close(); } catch(SQLException ignore) {}
			}
		}
		return map;
	}
	
}
