/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.sql.tables

import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.sql.db.SQLConstants.SQL_NULL_CONNECTION
import java.sql.*
import java.util.*
import java.util.logging.Logger

/**
 * Handle the storage and retrieval of parameters for recognized faces.
 * This class serves as a Kotlin interface to the Face and FaceContour tables. It provides
 * methods for finding and reading a face for purposes of comparison
 */
class FaceTable {


    /**
     * Associate a name with the facial detection details. If the name is new it will be added.
     * @cxn an open database connection
     * @param name of user associated with detection details
     * @param details facial detection details
     */
    fun mapFaceNameToDetails(cxn: Connection?, name: String, details: FacialDetectionDetails) {
        if( cxn!=null ) {
            var statement: PreparedStatement? = null
            var SQL = "UPDATE PoseMap SET pose=? WHERE command = ?"
            try {
                LOGGER.info(String.format("%s.mapFaceNameToDetails: \n%s", CLSS, SQL))
                statement = cxn.prepareStatement(SQL)
                /*
                statement.setString(1, pose)
                statement.setString(2, command)
                statement.executeUpdate()
                if (statement.getUpdateCount() == 0) {
                    statement.close()
                    SQL = "INSERT INTO PoseMap (command,pose) VALUES(?,?)"
                    LOGGER.info(String.format("%s.mapCommandToPose: \n%s", CLSS, SQL))
                    statement = cxn.prepareStatement(SQL)
                    statement.setString(1, command)
                    statement.setString(2, pose)
                    statement.executeUpdate()
                }

                 */
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.mapFaceNameToDetails: Database error (%s)", CLSS, e.message))
            }
            finally {
                if (statement != null) {
                    try {
                        statement.close()
                    }
                    catch (ignore: SQLException) {}
                }
            }
        }
    }

    /**
     * Save a list of motor position values as a pose. Assign the pose a name equal to the
     * id of the new database record.
     * @param mcmap contains a map of motor configurations with positions that define the pose.
     * @return the new record id as a string.
     */
    fun saveJointLocationsAsNewPose(cxn: Connection?, map: Map<Joint, MotorConfiguration>): String {
        LOGGER.info(String.format("%s.saveJointLocationsAsNewPose:", CLSS))
        var id: Long = SQL_NULL_CONNECTION
        if( cxn!=null ) {
            var statement: Statement? = null
            var prep: PreparedStatement? = null

            try {
                val sb = StringBuffer("INSERT INTO Pose (name,parameter")
                val valuesBuffer = StringBuffer("VALUES ('NEWPOSE','position'")
                for (mc in map.values) {
                    sb.append(",")
                    sb.append(mc.joint.name)
                    valuesBuffer.append(",?")
                }
                var SQL = sb.append(") ").append(valuesBuffer).append(")").toString()
                LOGGER.info(String.format("%s.saveJointLocationsAsNewPose:\n%s", CLSS, SQL))
                prep = cxn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)
                var index = 1
                for (mc in map.values) {
                    prep.setInt(index, mc.angle.toInt())
                    index++
                }
                prep.executeUpdate()
                val generatedKeys: ResultSet = prep.getGeneratedKeys()
                if (generatedKeys.next()) {
                    id = generatedKeys.getLong(1)
                }
                SQL = "UPDATE Pose Set name = id WHERE name='NEWPOSE'"
                statement = cxn.createStatement()
                statement.execute(SQL)
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.saveJointLocationsAsNewPose: Error (%s)", CLSS, e.message))
            }
            finally {
                if (prep != null) {
                    try {
                        prep.close()
                    }
                    catch (ignore: SQLException) {
                    }
                }
                if (statement != null) {
                    try {
                        statement.close()
                    }
                    catch (ignore: SQLException) {
                    }
                }
            }
        }
        return id.toString()
    }

    /**
     * Save a list of motor position values as a pose. Try an update first. If no rows are affected
     * then do an insert.
     * @param mcmap contains a map of motor configurations. Joints not in the list are ignored.
     * @param pz name
     */
    fun saveJointLocationsForPose(cxn: Connection?, map: Map<Joint, MotorConfiguration>, pz: String) {
        if( cxn!=null ) {
            LOGGER.info(String.format("%s.saveJointAnglesForPose: %s)", CLSS, pz))
            var statement: PreparedStatement? = null
            val pose = pz.lowercase(Locale.getDefault())
            var SQL = StringBuffer("UPDATE Pose SET ")
            var index = 0
            for (mc in map.values) {
                index++
                SQL.append(
                    java.lang.String.format("\n'%s'=%.1f%s",
                        mc.joint.name, mc.angle,
                        if (index == map.size) "" else ","
                    )
                )
            }
            SQL.append("\nWHERE name=? AND parameter='position';")
            try {
                LOGGER.info(String.format("%s.saveJointAnglesForPose: \n%s)", CLSS, SQL.toString()))
                statement = cxn.prepareStatement(SQL.toString())
                statement.setString(1, pose)
                statement.executeUpdate()
                if (statement.getUpdateCount() == 0) {
                    // There was nothing to update. Do an insert. This will auto-increment primary key.
                    statement.close()
                    SQL = StringBuffer("INSERT INTO Pose (name,parameter")
                    val valuesBuffer = StringBuffer("VALUES (?,'position'")
                    for (mc in map.values) {
                        SQL.append(",")
                        SQL.append(mc.joint.name)
                        valuesBuffer.append(",?")
                    }
                    SQL.append(") ").append(valuesBuffer).append(")").toString()
                    LOGGER.info(String.format("%s.saveJointAnglesForPose: \n%s)", CLSS, SQL.toString()))
                    statement = cxn.prepareStatement(SQL.toString())
                    statement.setString(1, pose)
                    index = 2
                    for (mc in map.values) {
                        statement.setInt(index, mc.angle.toInt())
                        index++
                    }
                    statement.executeUpdate()
                }
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.saveJointAnglesForPose: Database error (%s)", CLSS, e.message))
            }
            finally {
                if (statement != null) {
                    try {
                        statement.close()
                    }
                    catch (ignore: SQLException) {
                    }
                }
            }
        }
    }

    companion object {
        private const val CLSS = "FaceTable"
        private val LOGGER = Logger.getLogger(CLSS)
        private val DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_DATABASE)
    }
}