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
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import bert.share.model.Joint;
import bert.share.model.MotorConfiguration;

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
	 * @param map contains a map of configurations. Joints not in the list are ignored.
	 * @param parameter, e.g. "position","speed","torque"
	 * @return list of upper-case joint names.
	 */
	public Map<String,Double> getPoseJointValuesForParameter(Connection cxn,Map<String,MotorConfiguration>mcmap,String pose,String parameter) {
		Map<String,Double> map = new HashMap<>();
		PreparedStatement statement = null;
		ResultSet rs = null;
		pose = pose.toLowerCase();
		pose = pose.toLowerCase();
		String SQL = "select * from pose where name = ? and parameter = ? ";
		try {
			statement = cxn.prepareStatement(SQL);
			statement.setQueryTimeout(10);  // set timeout to 10 sec.
			statement.setString(1,pose);
			statement.setString(2,parameter);
			rs = statement.executeQuery();

			ResultSetMetaData meta = rs.getMetaData();
			int colCount = meta.getColumnCount();
			while(rs.next() ) {
				for( int col=1;col<=colCount;++col) {
					String name = meta.getColumnName(col);
					if( name.equalsIgnoreCase("name"))      continue;
					if( name.equalsIgnoreCase("parameter")) continue;
					if( !mcmap.containsKey(name) )          continue;
					Object val = rs.getObject(col);
					if( val==null ) continue;
					if( val.toString().isEmpty()) continue;
					try {
						Double dbl = Double.parseDouble(val.toString());
						map.put(name.toUpperCase(), dbl);
					}
					catch(NumberFormatException nfe) {
						LOGGER.warning(String.format("%s.getPoseJointValuesForParameter: %s value for %s not a double (%s)",
								CLSS,parameter,name,nfe.getMessage()));
					}
				}
			}
			rs.close();
		}
		catch(SQLException e) {
			// if the error message is "out of memory", 
			// it probably means no database file is found
			LOGGER.severe(String.format("%s.getPoseJointValuesForParameter: Database error (%s)",CLSS,e.getMessage()));
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
	/**
	 * Associate a pose with the specified command.
	 * @cxn an open database connection
	 * @param command user entered string
	 * @param the name of the pose to assume
	 */
	public void mapCommandToPose(Connection cxn,String command,String pose) {
		Statement statement = null;
		command = command.toLowerCase();
		pose = pose.toLowerCase();

		StringBuffer buf = new StringBuffer("INSERT INTO PoseMap (command,pose)");
			buf.append("VALUES('%s','%s')");
			buf.append("ON CONFLICT(command)"); 
			buf.append("DO UPDATE SET pose=excluded.pose");
		String SQL = String.format(buf.toString(), command,pose);
		try {
			statement = cxn.createStatement();
			statement.execute(SQL);
		}
		catch(SQLException e) {
			LOGGER.severe(String.format("%s.mapCommandToPose: Database error (%s)",CLSS,e.getMessage()));
		}
		finally {
			if( statement!=null) {
				try { statement.close(); } catch(SQLException ignore) {}
			}
		}
	}
	/** 
	 * Save a list of motor position values as a pose. Assign the pose a name equal to the
	 * id of the new database record.
	 * @param mcmap contains a map of motor configurations with positions that define the pose.
	 * @return the new record id as a string.
	 */
	public String saveJointPositionsAsNewPose(Connection cxn,Map<Joint,MotorConfiguration>mcmap) {
		 return "";
	}
	/** 
	 * Save a list of motor position values as a pose. Try an update first. If no rows are affected
	 * then do an insert.
	 * @param mcmap contains a map of motor configurations. Joints not in the list are ignored.
	 * @param pose name
	 */
	public void saveJointPositionsForPose(Connection cxn,Map<Joint,MotorConfiguration>mcmap,String pose) {
		Map<String,Double> map = new HashMap<>();
		Statement statement = null;
		StringBuffer updateBuffer = new StringBuffer("UPDATE Pose SET ");
		
		pose = pose.toLowerCase();
		String where = String.format("\nWHERE name='%s' AND parameter='position' ", pose);
		
		
		String SQL = "select * from pose where name = ? and parameter = ? ";
		try {
			statement = cxn.createStatement();
			statement.execute(SQL);
			if( statement.getUpdateCount()==0) {
				// There was nothing to update. Do an insert.
				statement.close();
				StringBuffer insertBuffer = new StringBuffer("INSERT INTO Pose ('name','parameter',");
				StringBuffer valuesBuffer = new StringBuffer("VALUES (");
				statement = cxn.createStatement();
				statement.execute(SQL);
			}


		}
		catch(SQLException e) {
			LOGGER.severe(String.format("%s.saveJointPositionsForPose: Database error (%s)",CLSS,e.getMessage()));
		}
		finally {
			if( statement!=null) {
				try { statement.close(); } catch(SQLException ignore) {}
			}
		}
	}

}
