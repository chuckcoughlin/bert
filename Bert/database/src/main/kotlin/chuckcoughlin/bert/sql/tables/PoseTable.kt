/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.sql.tables

import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.sql.db.SQLConstants
import chuckcoughlin.bert.sql.db.SQLConstants.SQL_NULL_CONNECTION
import com.google.gson.GsonBuilder
import java.sql.*
import java.util.*
import java.util.logging.Logger

/**
 * A pose is a list of positions for each motor.
 * This class serves as a Kotlin interface to the Pose and PoseMap tables. It provides
 * methods for finding and reading a pose
 */
class PoseTable {
    /**
     * Delete the pose and associated joint details.
     * @cxn an open database connection
     * @param name pose name
     */
    fun deletePose(cxn: Connection?, name: String) {
        if( cxn!=null ) {
            var SQL = "select poseid from PoseName where name = ?"
            var statement = cxn.prepareStatement(SQL)
            var rs: ResultSet? = null
            val pose = name.lowercase(Locale.getDefault())
            var poseid = SQLConstants.NO_POSE

            try {
                statement.setQueryTimeout(10) // set timeout to 10 sec.
                statement.setString(1, pose)
                rs = statement.executeQuery()
                while (rs.next()) {
                    poseid = rs.getLong("poseid")
                    LOGGER.info(String.format("%s.deletePose: %s is %d", CLSS, pose, poseid))
                    break
                }
                if( poseid==SQLConstants.NO_POSE ) return   // Didn't exist
            }
            catch (e: SQLException) {
                // if the error message is "out of memory",
                // it probably means no database file is found
                LOGGER.severe(String.format("%s.deletePose: Error (%s)", CLSS, e.message))
            }
            finally {
                if(rs != null) {
                    try {
                        rs.close()
                        statement.close()
                    }
                    catch (ignore: SQLException) {}
                }
                // Now do the deletions
                var stmt=cxn.createStatement()
                try {
                    SQL=String.format("delete from PoseName where poseid = %d", poseid)
                    stmt.execute(SQL)
                    SQL=String.format("delete from Pose where poseid = %d", poseid)
                    stmt.execute(SQL)
                }
                finally {
                    stmt.close()
                }
            }
        }
    }

    /**
     * Find the pose id given pose name. If the pose does not currently
     * exist, create it. The name is always stored in lower case.
     * @cxn an open database connection
     * @param name user entered string
     * @return the corresponding pose name if it exists, otherwise NULL
     */
    fun getPoseIdForName(cxn: Connection?, name: String): Long {
        var poseid: Long = SQLConstants.NO_POSE   // In case of SQL error
        if( cxn!=null ) {
            var SQL = "select poseid from PoseName where name = ?"
            var prepStatement: PreparedStatement = cxn.prepareStatement(SQL)
            var rs: ResultSet? = null
            val pose = name.lowercase(Locale.getDefault())

            try {
                prepStatement.setQueryTimeout(10) // set timeout to 10 sec.
                prepStatement.setString(1, pose)
                rs = prepStatement.executeQuery()
                while (rs.next()) {
                    poseid = rs.getLong("poseid")
                    LOGGER.info(String.format("%s.getPoseIdForName: %s is %d", CLSS, pose, poseid))
                    break
                }
                rs.close()
            }
            catch (e: SQLException) {
                // if the error message is "out of memory",
                // it probably means no database file is found
                LOGGER.severe(String.format("%s.getPoseIdForName: Error (%s)", CLSS, e.message))
            }
            finally {
                if(rs != null) {
                    try {
                        rs.close()
                    }
                    catch (ignore: SQLException) {}
                }
                try {prepStatement.close()}
                catch (ignore: SQLException) {}
            }
            // If the pose doesn't exist, create it
            if( poseid==SQLConstants.NO_POSE ) {
                var statement: Statement = cxn.createStatement()
                SQL = "select max(poseid) from PoseName"
                try {
                    rs = statement.executeQuery(SQL)
                    poseid = 0L
                    if( rs.next() ) {
                        poseid = rs.getLong(1)
                    }
                    SQL = String.format("insert into PoseName(name,poseid) values('%s',%d)",pose,poseid)
                    statement.executeUpdate(SQL)
                }
                catch (e: SQLException) {
                    // if the error message is "out of memory",
                    // it probably means no database file is found
                    LOGGER.severe(String.format("%s.getPoseIdForName: Error (%s)", CLSS, e.message))
                }
                finally {
                    if(rs != null) {
                        try {
                            rs.close()
                        }
                        catch (ignore: SQLException) {}
                    }
                    try {statement.close()}
                    catch (ignore: SQLException) {}
                }
            }
        }
        return poseid
    }

    /**
     * Return a map of angles by joint name
     *
     * @param poseid
     * @return a map of target positions by joint for the pose
     */
    fun getPoseJointPositions(cxn: Connection?,poseid: Long): Map<Joint, Double> {
        val map: MutableMap<Joint, Double> = HashMap()
        if( cxn!=null ) {
            var statement: PreparedStatement? = null
            var rs: ResultSet? = null
            val SQL = "select * from pose where poseid = ? "
            try {
                statement = cxn.prepareStatement(SQL)
                statement.setQueryTimeout(10) // set timeout to 10 sec.
                statement.setLong(1, poseid)
                rs = statement.executeQuery()
                while (rs.next() ) {
                    val jointName = rs.getString("joint")
                    val joint = Joint.fromString(jointName)
                    val angle = rs.getDouble("position")
                    map[joint] = angle
                    if(DEBUG) LOGGER.info(String.format("%s.getPoseJointPositions: %s for %d = %s",
                                    CLSS, joint.name, angle))
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
     * Return a map of speeds by joint name. The speeds come from the current
     * MotorConfiguration objects.
     *
     * @param poseid
     * @return a map of current speed settings by joint for the pose
     */
    fun getPoseJointSpeeds(cxn: Connection?,poseid: Long,configs:Map<Joint,MotorConfiguration>): Map<Joint, Double> {
        val map: MutableMap<Joint, Double> = HashMap()
        if( cxn!=null ) {
            var statement: PreparedStatement? = null
            var rs: ResultSet? = null
            val SQL = "select * from pose where poseid = ? "
            try {
                statement = cxn.prepareStatement(SQL)
                statement.setQueryTimeout(10) // set timeout to 10 sec.
                statement.setLong(1, poseid)
                rs = statement.executeQuery()
                while (rs.next() ) {
                    val jointName = rs.getString("joint")
                    val joint = Joint.fromString(jointName)
                    val mc = configs[joint]
                    if( mc!=null ) {   // Ignore joint if not in the configuration map
                        map[joint] = mc.speed
                        if(DEBUG) LOGGER.info(String.format("%s.getPoseJointSpeeds: %s for %d = %s",
                                CLSS, joint.name, mc.speed))
                    }
                }
                rs.close()
            }
            catch (e: SQLException) {
                // if the error message is "out of memory",
                // it probably means no database file is found
                LOGGER.severe(String.format("%s.getPoseJointSpeeds: Database error (%s)", CLSS, e.message))
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
     * Return a map of torques by joint name. The torques come from the current
     * MotorConfiguration objects.
     *
     * @param poseid
     * @return a map of current torque values by joint for the pose
     */
    fun getPoseJointTorques(cxn: Connection?,poseid: Long,configs:Map<Joint,MotorConfiguration>): Map<Joint, Double> {
        val map: MutableMap<Joint, Double> = HashMap()
        if( cxn!=null ) {
            var statement: PreparedStatement? = null
            var rs: ResultSet? = null
            val SQL = "select * from pose where poseid = ? "
            try {
                statement = cxn.prepareStatement(SQL)
                statement.setQueryTimeout(10) // set timeout to 10 sec.
                statement.setLong(1, poseid)
                rs = statement.executeQuery()
                while (rs.next() ) {
                    val jointName = rs.getString("joint")
                    val joint = Joint.fromString(jointName)
                    val mc = configs[joint]
                    if( mc!=null ) {   // Ignore joint if not in the configuration map
                        map[joint] = mc.torque
                        if(DEBUG) LOGGER.info(String.format("%s.getPoseJointTorques: %s for %d = %s",
                                CLSS, joint.name, mc.torque))
                    }
                }
                rs.close()
            }
            catch (e: SQLException) {
                // if the error message is "out of memory",
                // it probably means no database file is found
                LOGGER.severe(String.format("%s.getPoseJointTorques: Database error (%s)", CLSS, e.message))
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
     * List the names of all defined poses.
     * @cxn an open database connection
     * @return a list of pose names, comma-separate
     */
    fun getPoseNames(cxn: Connection?): String {
        val names = StringBuffer()
        if( cxn!=null ) {
            val SQL = "select name from PoseName"
            var statement: Statement = cxn.createStatement()
            var rs: ResultSet? = null
            try {
                rs = statement.executeQuery(SQL)
                while (rs.next()) {
                    names.append(rs.getString(1))
                    names.append(", ")
                }
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.getPoseNames: Error (%s)", CLSS, e.message))
            }
            finally {
                if(rs != null) {
                    try { rs.close()}
                    catch (ignore: SQLException) {}
                }
                try {statement.close()}
                catch (ignore: SQLException) {}
            }
        }
        if( names.isNotEmpty() ) return names.substring(0, names.length - 2)
        else return "none"
    }
    /**
     * Associate a new name with the specified pose. If the named pose
     * doesn't exist, create it. Then proceed to tie to second name to it.
     * @cxn an open database connection
     * @param pose existing pose
     * @param name equivalent name or alias
     */
    fun mapNameToPose(cxn: Connection?, poseName:String,name: String) {
        if( cxn!=null ) {
            val pose=poseName.lowercase(Locale.getDefault())
            var poseid=getPoseIdForName(cxn, pose)
            var SQL="inseert into PoseName(poseid,pose) values(?,?)"
            var statement: PreparedStatement=cxn.prepareStatement(SQL)
            val alias=name.lowercase(Locale.getDefault())

            try {
                LOGGER.info(String.format("%s.mapNameToPose: \n%s", CLSS, SQL))
                statement.setLong(2, poseid)
                statement.setString(2, alias)
                statement.executeUpdate()
            }
            // We'll get an exception if the alias already exists.
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.mapCommandToPose: Database error (%s)", CLSS, e.message))
            }
            finally {
                try {
                    statement.close()
                }
                catch (ignore: SQLException) {
                }
            }
        }
    }

    /**
     * @return true if there is a pose or alias of the given name.
     */
    fun poseExists(cxn:Connection?,poseName:String) : Boolean {
        var poseid: Long = SQLConstants.NO_POSE
        if( cxn!=null ) {
            var SQL="select * from PoseName where name = ?"
            var prepStatement: PreparedStatement=cxn.prepareStatement(SQL)
            var rs: ResultSet?=null
            val pose=poseName.lowercase(Locale.getDefault())

            try {
                prepStatement.setQueryTimeout(10) // set timeout to 10 sec.
                prepStatement.setString(1, pose)
                rs=prepStatement.executeQuery()
                while(rs.next()) {
                    poseid=rs.getLong("poseid")
                    LOGGER.info(String.format("%s.poseExists: %s is %d", CLSS, pose, poseid))
                    break
                }
            }
            catch (e: SQLException) {
                // if the error message is "out of memory",
                // it probably means no database file is found
                LOGGER.severe(String.format("%s.poseExists: Error (%s)", CLSS, e.message))
            }
            finally {
                if(rs != null) {
                    try {
                        rs.close()
                    }
                    catch (ignore: SQLException) { }
                }
                try {
                    prepStatement.close()
                }
                catch (ignore: SQLException) {}
            }
        }
        if(poseid==SQLConstants.NO_POSE) {return false}
        else { return true }
    }
    /**
     * @return the names of poses in JSON formatted string
     */
    fun poseNamesToJSON(cxn:Connection?) : String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        var names = mutableListOf<String>()
        if( cxn!=null ) {
            val SQL = "select name from PoseName"
            var statement: Statement = cxn.createStatement()
            var rs: ResultSet? = null
            try {
                rs = statement.executeQuery(SQL)
                while (rs.next()) {
                    names.add(rs.getString(1))
                }
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.poseNamesToJson: Error (%s)", CLSS, e.message))
            }
            finally {
                if(rs != null) {
                    try { rs.close()}
                    catch (ignore: SQLException) {}
                }
                try {statement.close()}
                catch (ignore: SQLException) {}
            }
        }
        return gson.toJson(names)
    }
    /**
     * Save a list of motor position values as a pose. Assign the pose a name equal to the
     * id of the new database record.
     * @param mcmap contains a map of motor configurations with positions that define the pose.
     * @return the new record id as a string.
     */
    fun saveJointAnglesAsNewPose(cxn: Connection?, map: Map<Joint, MotorConfiguration>): String {
        LOGGER.info(String.format("%s.saveJointAnglesAsNewPose:", CLSS))
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
    fun saveJointAnglesForPose(cxn: Connection?, map: Map<Joint, MotorConfiguration>, pz: String) {
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

    private val CLSS = "PoseTable"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_DATABASE)
    }

}