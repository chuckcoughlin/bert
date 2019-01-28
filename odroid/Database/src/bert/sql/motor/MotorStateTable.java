/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.sql.motor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import bert.share.motor.MotorConfiguration;

/**
 * This class serves as a Java interface to the Motor table. It provides 
 * methods to create entries (on startup read of configuration file), and
 * then iterate over the instances.
 */
public class MotorStateTable {
	private static final String CLSS = "MotorStateTable";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	/** 
	 * Constructor: 
	 */
	public MotorStateTable() {

	}

	/**
	 * Clear the MotorState table and insert from the supplied list.
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
			String sql = "DELETE FROM MotorState";
			statement = cxn.createStatement();
			statement.executeUpdate(sql);
			sql = "INSERT INTO MotorState(id,name,controller,type,offset,speed,torque,minAngle,maxAngle,direct) VALUES(?,?,?,?,?,?,?,?,?,?)";
			
			ps = cxn.prepareStatement(sql);
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
				ps.executeUpdate();
			}
		}
		catch(SQLException sqle) {
			// if the error message is "out of memory", 
			// it probably means no database file is found
			LOGGER.log(Level.SEVERE,String.format("%s.defineMotors: Database error (%s)",
																CLSS,sqle.getMessage()),sqle);
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
