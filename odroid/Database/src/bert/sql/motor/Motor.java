/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.sql.motor;

import java.lang.System.Logger.Level;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import bert.share.motor.MotorConfiguration;

/**
 * This class serves as a Java interface to the Motor table. It provides 
 * methods to create entries (on startup read of configuration file), and
 * then iterate over the instances.
 */
public class Motor {
	private static final String CLSS = "Motor";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	/** 
	 * Constructor: 
	 */
	public Motor() {

	}
	
	
	/**
	 * Clear the Motor table and insert from the supplied list.
	 * 
	 * @param cxn open database connection
	 * @param motors list of motor configurations
	 * 
	 */
	public void defineMotors(Connection cxn,List<MotorConfiguration> motors)  {
		if( cxn==null ) {
			String msg = String.format("%s.defineMotors: Called before database connection set", CLSS);
			LOGGER.log(Level.ERROR,msg);
			return;
		}

		Statement statement = null;
		PreparedStatement ps = null;
		try {
			String sql = "DELETE FROM Motor";
			statement = cxn.createStatement();
			statement.executeUpdate(sql);
			
			sql = "INSERT INTO Motor() VALUES()";
			
			for(MotorConfiguration mc:motors) {
				ps.setInt(1,mc.getId());
				ps.executeUpdate(sql);
			}
		}
		catch(SQLException e) {
			// if the error message is "out of memory", 
			// it probably means no database file is found
			LOGGER.log(Level.ERROR,String.format("%s.defineMotors: Database error (%s)",CLSS,e.getMessage()));
		}
		finally {
			if( statement!=null ) {
				try { statement.close(); } catch(SQLException ignore) {}
			}
				
			if( ps!=null) {
				try { ps.close(); } catch(SQLException ignore) {}
			}
		}
	}
	
}
