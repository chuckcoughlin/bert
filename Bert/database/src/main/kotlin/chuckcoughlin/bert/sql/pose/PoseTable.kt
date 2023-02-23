/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.sql.pose

import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.JointDynamicProperty
import chuckcoughlin.bert.common.model.MotorConfiguration
import chuckcoughlin.bert.sql.db.SQLConstants.SQL_NULL_CONNECTION
import java.sql.*
import java.util.*
import java.util.logging.Logger

/**
 * A pose is a list of positions for each motor. There are up to
 * three rows in the database for each pose. A row each for:
 * position, speed and torque
 * This class serves as a Java interface to the Pose and PoseMap tables. It provides
 * methods for finding and reading a pose
 */
class PoseTable {
    /**
     * Find the pose associated with a command.
     * @cxn an open database connection
     * @param command user entered string
     * @return the corresponding pose name if it exists, otherwise NULL
     */
    fun getPoseForCommand(cxn: Connection?, cmd: String): String? {
        var pose: String? = null
        if( cxn!=null ) {
            var statement: PreparedStatement? = null
            var rs: ResultSet? = null
            val command = cmd.lowercase(Locale.getDefault())

            val SQL = "select pose from PoseMap where command = ?"
            try {
                statement = cxn.prepareStatement(SQL)
                statement.setQueryTimeout(10) // set timeout to 10 sec.
                statement.setString(1, command)
                rs = statement.executeQuery()
                while (rs.next()) {
                    pose = rs.getString("pose")
                    LOGGER.info(String.format("%s.getPoseForCommand: %s is %s", CLSS, command, pose))
                    break
                }
                rs.close()
            }
            catch (e: SQLException) {
                // if the error message is "out of memory",
                // it probably means no database file is found
                LOGGER.severe(String.format("%s.getPoseForCommand: Error (%s)", CLSS, e.message))
            }
            finally {
                if (rs != null) {
                    try {
                        rs.close()
                    }
                    catch (ignore: SQLException) {}
                }
                if (statement != null) {
                    try {
                        statement.close()
                    }
                    catch (ignore: SQLException) {}
                }
            }
        }
        return pose
    }

    /** Return a list of column names with non-null values for the indicated pose
     * property. There should only be one (or none) row returned.
     * @param pose
     * @param map contains a map of configurations. Joints not in the list are ignored.
     * @param parameter, e.g. "position","speed","torque"
     * @return list of upper-case joint names.
     */
    fun getPoseJointValuesForParameter(cxn: Connection?,p: String,parameter: JointDynamicProperty
    ): Map<Joint, Double> {

        val map: MutableMap<Joint, Double> = HashMap()
        if( cxn!=null ) {
            var pose = p
            var statement: PreparedStatement? = null
            var rs: ResultSet? = null
            pose = pose.lowercase(Locale.getDefault())
            pose = pose.lowercase(Locale.getDefault())
            val SQL = "select * from pose where name = ? and parameter = ? "
            try {
                statement = cxn.prepareStatement(SQL)
                statement.setQueryTimeout(10) // set timeout to 10 sec.
                statement.setString(1, pose)
                statement.setString(2, parameter.name.lowercase())
                rs = statement.executeQuery()
                val meta: ResultSetMetaData = rs.getMetaData()
                val colCount: Int = meta.getColumnCount()
                while (rs.next()) {
                    for (col in 1..colCount) {
                        val name: String = meta.getColumnName(col)
                        if (name.equals("name", ignoreCase = true)) continue
                        if (name.equals("parameter", ignoreCase = true)) continue
                        try {
                            val joint = Joint.valueOf(name.uppercase(Locale.getDefault()))
                            val value = rs.getObject(col) ?: continue
                            if (value.toString().isEmpty()) continue
                            try {
                                val dbl = value.toString().toDouble()
                                map[joint] = dbl
                            }
                            catch (nfe: NumberFormatException) {
                                LOGGER.warning(String.format("%s.getPoseJointValuesForParameter: %s value for %s not a double (%s)",
                                    CLSS, parameter, name, nfe.message))
                            }
                        }
                        catch(iae: IllegalArgumentException) {
                            LOGGER.warning(String.format("%s.getPoseJointValuesForParameter: %s not a joint name (%s)",
                                CLSS, name, iae.message))
                        }
                    }
                }
                rs.close()
            }
            catch (e: SQLException) {
                // if the error message is "out of memory",
                // it probably means no database file is found
                LOGGER.severe(String.format("%s.getPoseJointValuesForParameter: Database error (%s)", CLSS, e.message))
            }
            finally {
                if (rs != null) {
                    try {
                        rs.close()
                    }
                    catch (ignore: SQLException) {}
                }
                if (statement != null) {
                    try {
                        statement.close()
                    }
                    catch (ignore: SQLException) {}
                }
            }
        }
        return map
    }

    /**
     * Associate a pose with the specified command. If the command already exists
     * it will be updated.
     * @cxn an open database connection
     * @param cmd user entered string
     * @param pz the name of the pose to assume
     */
    fun mapCommandToPose(cxn: Connection?, cmd: String, pz: String) {
        if( cxn!=null ) {
            var statement: PreparedStatement? = null
            val command = cmd.lowercase(Locale.getDefault())
            val pose = pz.lowercase(Locale.getDefault())
            var SQL = "UPDATE PoseMap SET pose=? WHERE command = ?"
            try {
                LOGGER.info(String.format("%s.mapCommandToPose: \n%s", CLSS, SQL))
                statement = cxn.prepareStatement(SQL)
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
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.mapCommandToPose: Database error (%s)", CLSS, e.message))
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
    fun saveJointPositionsAsNewPose(cxn: Connection?, map: Map<Joint, MotorConfiguration>): String {
        LOGGER.info(String.format("%s.saveJointPositionsAsNewPose:", CLSS))
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
                LOGGER.info(String.format("%s.saveJointPositionsAsNewPose:\n%s", CLSS, SQL))
                prep = cxn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)
                var index = 1
                for (mc in map.values) {
                    prep.setInt(index, mc.position.toInt())
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
                LOGGER.severe(String.format("%s.saveJointPositionsAsNewPose: Error (%s)", CLSS, e.message))
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
    fun saveJointPositionsForPose(cxn: Connection?, map: Map<Joint, MotorConfiguration>, pz: String) {
        if( cxn!=null ) {
            LOGGER.info(String.format("%s.saveJointPositionsForPose: %s)", CLSS, pz))
            var statement: PreparedStatement? = null
            val pose = pz.lowercase(Locale.getDefault())
            var SQL = StringBuffer("UPDATE Pose SET ")
            var index = 0
            for (mc in map.values) {
                index++
                SQL.append(
                    java.lang.String.format("\n'%s'=%.1f%s",
                        mc.joint.name, mc.position,
                        if (index == map.size) "" else ","
                    )
                )
            }
            SQL.append("\nWHERE name=? AND parameter='position';")
            try {
                LOGGER.info(String.format("%s.saveJointPositionsForPose: \n%s)", CLSS, SQL.toString()))
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
                    LOGGER.info(String.format("%s.saveJointPositionsForPose: \n%s)", CLSS, SQL.toString()))
                    statement = cxn.prepareStatement(SQL.toString())
                    statement.setString(1, pose)
                    index = 2
                    for (mc in map.values) {
                        statement.setInt(index, mc.position.toInt())
                        index++
                    }
                    statement.executeUpdate()
                }
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.saveJointPositionsForPose: Database error (%s)", CLSS, e.message))
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
        private const val CLSS = "PoseTable"
        private val LOGGER = Logger.getLogger(CLSS)
    }

}