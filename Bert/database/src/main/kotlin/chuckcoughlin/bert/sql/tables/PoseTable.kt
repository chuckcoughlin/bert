/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.sql.tables

import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.common.util.TextUtility
import chuckcoughlin.bert.sql.db.SQLConstants
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
     * Create a new pose or update an existing one from a list of motor position, torque and speed values.
     * @param mcmap is a map of motor configurations with positions that define the pose
     * @param pose
     * @param index
     */
    fun createPose(cxn: Connection?, mcmap: Map<Joint, MotorConfiguration>,pose:String,index:Int ){
        if( cxn!=null ) {
            var statement: Statement?
            val name = pose.lowercase()
            var poseid = getPoseIdForName(cxn,name,index)
            LOGGER.info(String.format("%s.createPose: %s %d is id %d", CLSS,name,index,poseid))
            statement = cxn.createStatement()
            if( poseid == SQLConstants.NO_POSE ) {
                poseid = getNextPoseId(cxn)
                val SQL = String.format("insert into Pose(poseid,series,executeOrder,delay) values(%d,'%s',%d,1000)",poseid,pose,index)
                if(DEBUG) LOGGER.info(String.format("%s.createPose: executing %s)", CLSS, SQL))
                statement.executeUpdate(SQL)
            }
            else {
                // Pose exists, so delete any existing data from PoseJoint table
                val SQL = String.format("delete from PoseJoint where poseid = %d",poseid)
                if(DEBUG) LOGGER.info(String.format("%s.createPose: executing %s)", CLSS, SQL))
                statement.executeUpdate(SQL)
            }

            try {
                // An unmoving speed doesn't make sense as part of a pose
                for (mc in mcmap.values) {
                    if( mc.speed < MIN_SPEED ) mc.speed = mc.maxSpeed * ConfigurationConstants.HALF_SPEED
                    val SQL = String.format("insert into PoseJoint(poseid,joint,angle,torque,speed) values(%d,'%s',%2.3f,%2.3f,%2.1f)",poseid,
                        mc.joint.name,mc.angle,mc.torque,mc.speed)
                    if(DEBUG) LOGGER.info(String.format("%s.createPose: executing %s)", CLSS, SQL))
                    statement.executeUpdate(SQL)
                }
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.createPose: Error (%s)", CLSS, e.message))
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
    /**
     * Delete a specific pose and associated joint details.
     * @cxn an open database connection
     * @param name pose name
     * @param index execution order
     */
    fun deletePose(cxn: Connection?, name: String,index:Int) {
        if( cxn!=null ) {
            var SQL = "select poseid from Pose where series = ? and executeOrder = ?"
            var statement = cxn.prepareStatement(SQL)
            var rs: ResultSet? = null
            val pose = name.lowercase(Locale.getDefault())
            var poseid = SQLConstants.NO_POSE

            try {
                statement.setQueryTimeout(10) // set timeout to 10 sec.
                statement.setString(1, pose)
                statement.setInt(2,index)
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
                    SQL=String.format("delete from PoseJoint where poseid = %d", poseid)
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
     * Delete all poses and associated joint details with a given name
     * @cxn an open database connection
     * @param name pose name
     */
    fun deletePose(cxn: Connection?, name: String) {
        if( cxn!=null ) {
            var SQL = "select poseid from Pose where series = ?"
            var prep = cxn.prepareStatement(SQL)
            var stmt=cxn.createStatement()
            var rs: ResultSet? = null
            val pose = name.lowercase(Locale.getDefault())
            var poseid = SQLConstants.NO_POSE

            try {
                prep.setQueryTimeout(10) // set timeout to 10 sec.
                prep.setString(1, pose)
                rs = prep.executeQuery()
                while (rs.next()) {
                    poseid = rs.getLong("poseid")
                    LOGGER.info(String.format("%s.deletePose: %s is %d", CLSS, pose, poseid))
                    // Delete the pose and its joint
                    SQL=String.format("delete from PoseJoint where poseid = %d", poseid)
                    stmt.execute(SQL)
                    SQL=String.format("delete from Pose where poseid = %d", poseid)
                    stmt.execute(SQL)
                }
            }
            catch (e: SQLException) {
                // if the error message is "out of memory",
                // it probably means no database file is found
                LOGGER.severe(String.format("%s.deletePose: Error (%s)", CLSS, e.message))
            }
            finally {
                try {
                  if(rs != null) {

                      rs.close()
                      prep.close()

                  }
                    stmt.close()
                }
                catch (ignore: SQLException) {}
            }
        }
    }

    /**
     * Find a pose id one larger than the current maximum.
     * @cxn an open database connection
     * @return an unused pose id
     */
    private fun getNextPoseId(cxn: Connection?): Long {
        var poseid: Long = 1
        if( cxn!=null ) {
            var SQL = "select max(poseid) from Pose"
            var statement: Statement = cxn.createStatement()
            var rs: ResultSet? = null

            try {
                rs = statement.executeQuery(SQL)
                while (rs.next()) {
                    poseid = rs.getLong(1)
                    poseid = poseid + 1
                    break
                }
                rs.close()
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.getNextPoseId: Error (%s)", CLSS, e.message))
            }
            finally {
                if (rs != null) {
                    try {
                        rs.close()
                    }
                    catch (ignore: SQLException) {
                    }
                }
                try {
                    statement.close()
                }
                catch (ignore: SQLException) {
                }
            }
        }
        return poseid
    }

    /**
     * Find the pose id given pose name and index. If the pose does not
     * exist, return NO_POSE. The name is always stored in lower case.
     * @cxn an open database connection
     * @param name user entered string
     * @param index series order
     * @return the corresponding pose id if it exists, otherwise NO_POSE
     */
    fun getPoseIdForName(cxn: Connection?, name: String, index: Int): Long {
        var poseid: Long = SQLConstants.NO_POSE
        if( cxn!=null ) {
            var SQL = "select poseid from Pose where series = ? and executeOrder = ?"
            var prepStatement: PreparedStatement = cxn.prepareStatement(SQL)
            var rs: ResultSet? = null
            val pose = name.lowercase(Locale.getDefault())

            try {
                prepStatement.setQueryTimeout(10) // set timeout to 10 sec.
                prepStatement.setString(1, pose)
                prepStatement.setInt(2,index)
                rs = prepStatement.executeQuery()
                while (rs.next()) {
                    poseid = rs.getLong("poseid")
                    LOGGER.info(String.format("%s.getPoseIdForName: %s %d is %d", CLSS, pose,index, poseid))
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
        }
        return poseid
    }

    /**
     * Return a map of angles by joint name. The configuration map that is supplied
     * contains the motors of interest. Other motors are ingnored.
     *
     * @param poseid
     * @return a map of target positions by joint for the pose
     */
    fun getPoseJointPositions(cxn: Connection?,poseid: Long, configs:Map<Joint,MotorConfiguration>): Map<Joint, Double> {
        val map: MutableMap<Joint, Double> = HashMap()
        if( cxn!=null ) {
            var statement: PreparedStatement? = null
            var rs: ResultSet? = null
            val SQL = "select joint,angle from posejoint where poseid = ? "
            try {
                statement = cxn.prepareStatement(SQL)
                statement.setQueryTimeout(10) // set timeout to 10 sec.
                statement.setLong(1, poseid)
                rs = statement.executeQuery()
                while (rs.next() ) {
                    val jointName = rs.getString("joint")
                    val joint = Joint.fromString(jointName)
                    if( configs[joint] != null ) {
                        val angle = rs.getDouble("angle")
                        map[joint] = angle
                        //if(DEBUG) LOGGER.info(String.format("%s.getPoseJointPositions: for %s = %2.0f",
                        //                CLSS, joint.name, angle))
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
     * Return a map of speeds in the pose  by joint name. Motors not in the supplied list are ignored
     *
     * @param poseid
     * @return a map of current speed settings by joint for the pose
     */
    fun getPoseJointSpeeds(cxn: Connection?,poseid: Long,configs:Map<Joint,MotorConfiguration>): Map<Joint, Double> {
        val map: MutableMap<Joint, Double> = HashMap()
        if( cxn!=null ) {
            var statement: PreparedStatement? = null
            var rs: ResultSet? = null
            val SQL = "select joint,speed from posejoint where poseid = ? "
            try {
                statement = cxn.prepareStatement(SQL)
                statement.setQueryTimeout(10) // set timeout to 10 sec.
                statement.setLong(1, poseid)
                rs = statement.executeQuery()
                while (rs.next() ) {
                    val jointName = rs.getString("joint")
                    val joint = Joint.fromString(jointName)
                    if( configs[joint]!=null ) {
                        val speed = rs.getDouble("speed")
                        map[joint] = speed
                        //if(DEBUG) LOGGER.info(String.format("%s.getPoseJointSpeeds: for %s = %2.0f",
                        //            CLSS, joint.name, speed))
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
     * MotorConfiguration objects. Motors not in the supplied list are ignored.
     *
     * @param poseid
     * @return a map of current torque values by joint for the pose
     */
    fun getPoseJointTorques(cxn: Connection?,poseid: Long,configs:Map<Joint,MotorConfiguration>): Map<Joint, Double> {
        val map: MutableMap<Joint, Double> = HashMap()
        if( cxn!=null ) {
            var statement: PreparedStatement? = null
            var rs: ResultSet? = null
            val SQL = "select joint,torque from posejoint where poseid = ? "
            try {
                statement = cxn.prepareStatement(SQL)
                statement.setQueryTimeout(10) // set timeout to 10 sec.
                statement.setLong(1, poseid)
                rs = statement.executeQuery()
                while (rs.next() ) {
                    val jointName = rs.getString("joint")
                    val joint = Joint.fromString(jointName)
                    if( configs[joint]!=null ) {
                        val torque = rs.getDouble("torque")
                        map[joint] = torque
                        //if(DEBUG) LOGGER.info(String.format("%s.getPoseJointTorques: for %s = %2.0f",
                        //            CLSS, joint.name, torque))
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
        val names = mutableListOf<String>()
        if( cxn!=null ) {
            val SQL = "select distinct(series) from Pose"
            var statement: Statement = cxn.createStatement()
            var rs: ResultSet? = null
            try {
                rs = statement.executeQuery(SQL)
                while (rs.next()) {
                    names.add(rs.getString(1))
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
        return TextUtility.createTextForSpeakingFromList(names)
    }
    /**
     * @return true if there is a pose the given name and index value.
     */
    fun poseExists(cxn:Connection?,poseName:String,index:Int) : Boolean {
        var poseid: Long = SQLConstants.NO_POSE
        if( cxn!=null ) {
            var SQL="select poseid from Pose where series = ? and executeOrder = ?"
            if( DEBUG ) LOGGER.info( String.format("%s.poseExists: %s", CLSS,SQL))
            var prepStatement: PreparedStatement=cxn.prepareStatement(SQL)
            var rs: ResultSet?=null
            val pose=poseName.lowercase(Locale.getDefault())

            try {
                prepStatement.setQueryTimeout(10) // set timeout to 10 sec.
                prepStatement.setString(1, pose)
                prepStatement.setInt(2, index)
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
     * @param posename
     * @param index
     * @return a map of pose angle,speed and torque by joint name converted to a JSON string
     */
    fun poseDetailsToJSON( cxn:Connection?,poseName:String,index:Int): String{
        val gson = GsonBuilder().setPrettyPrinting().create()
        var details = mutableListOf<PoseDetail>()

        if( cxn!=null ) {
            val poseid = getPoseIdForName(cxn, poseName, index)
            if (poseid != SQLConstants.NO_POSE) {
                val SQL = String.format("select joint,angle,torque,speed from PoseJoint where poseid=%d", poseid)
                var statement: Statement = cxn.createStatement()
                var rs: ResultSet? = null
                try {
                    rs = statement.executeQuery(SQL)
                    while (rs.next()) {
                        val detail = PoseDetail(rs.getString(1), rs.getDouble(2), rs.getDouble(3),rs.getDouble(4))
                        details.add(detail)
                    }
                }
                catch (e: SQLException) {
                    LOGGER.severe(String.format("%s.poseNamesToJSON: Error (%s)", CLSS, e.message))
                }
                finally {
                    if (rs != null) {
                        try {
                            rs.close()
                        }
                        catch (ignore: SQLException) {
                        }
                    }
                    try {
                        statement.close()
                    }
                    catch (ignore: SQLException) {
                    }
                }
            }
        }
        return gson.toJson(details)
    }
    /**
     * @return the names of poses in JSON formatted string
     */
    fun poseNamesToJSON(cxn:Connection?) : String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        var names = mutableListOf<PoseDefinition>()
        if( cxn!=null ) {
            val SQL = "select series,executeOrder,delay from Pose"
            var statement: Statement = cxn.createStatement()
            var rs: ResultSet? = null
            try {
                rs = statement.executeQuery(SQL)
                while (rs.next()) {
                    val ni = PoseDefinition(rs.getString(1),rs.getInt(2),rs.getLong(3))
                    names.add(ni)
                }
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.poseNamesToJSON: Error (%s)", CLSS, e.message))
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


    private val CLSS = "PoseTable"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean
    private val MIN_SPEED = 10.0  // Minimum speed reasonable for a pose

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_DATABASE)
    }
}