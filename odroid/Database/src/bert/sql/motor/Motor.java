/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.sql.motor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.logging.Logger;

import bert.share.motor.MotorConfiguration;

/**
 * This class serves as a Java interface to the Motor table. It provides 
 * methods to create entries (on startup read of configuration file), and
 * then iterate over the instances.
 */
public class Motor {
	private static final String CLSS = "Motor";
	private static Logger LOGGER = Logger.getLogger(CLSS);
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
	public void defineMotors(Connection cxn,Collection<MotorConfiguration> motors)  {
		if( cxn==null ) {
			String msg = String.format("%s.defineMotors: Called before database connection set", CLSS);
			LOGGER.severe(msg);
			return;
		}

		Statement statement = null;
		PreparedStatement ps = null;
		try {
			String sql = "DELETE FROM Motor";
			statement = cxn.createStatement();
			statement.executeUpdate(sql);
			
			sql = "INSERT INTO Motor(id,name,controller,type,offset,speed,torque,minAngle,maxAngle,direct) VALUES(?,?,?,?,?,?,?,?,?,?)";
			
			for(MotorConfiguration mc:motors) {
				ps.setInt(1,mc.getId());
				ps.setString(2,mc.getName().name());
				ps.setString(3,mc.getController());
				ps.setString(4,mc.getType().name());
				ps.setDouble(5,mc.getOffset());
				ps.setDouble(6,mc.getSpeed());
				ps.setDouble(7,mc.getTorque());
				ps.setDouble(8,mc.getMinAngle());
				ps.setDouble(9,mc.getMaxAngle());
				ps.setInt(10,(mc.isDirect()?1:0));
				ps.executeUpdate(sql);
			}
		}
		catch(SQLException e) {
			// if the error message is "out of memory", 
			// it probably means no database file is found
			LOGGER.severe(String.format("%s.defineMotors: Database error (%s)",CLSS,e.getMessage()));
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
