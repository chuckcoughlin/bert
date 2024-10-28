/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.sql.db

import chuckcoughlin.bert.common.model.FacialDetails
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.MotorConfiguration
import chuckcoughlin.bert.sql.tables.FaceTable
import chuckcoughlin.bert.sql.tables.PoseTable
import org.sqlite.JDBC
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * This static class (object) is a wrapper for the entire robot database. The startup()
 * method must be called before it can be used as it opens the database connection.
 *
 * Call shutdown() when database access is no longer required.
 */
object Database  {
    /**
     * @param name user or face name. Delete the face and assocuiated details.
     * If the face does not exist, no action is taken.
     */
    fun deleteFace(name: String) {
        return face.deleteFace(connection, name)
    }
    /**
     * @param name pose name. Delete the pose and its joint map from the
     *         database. If the pose does not exist, no action is taken.
     */
    fun deletePose(name: String) {
        return pose.deletePose(connection, name)
    }
    /**
     * @param name user entered pose name. If the pose does not
     *             exist, it will be created.
     * @return the corresponding pose id if it exists, otherwise NO_POSE
     */
    fun getPoseIdForName(name: String): Long {
        return pose.getPoseIdForName(connection, name)
    }
    /**
     * Return a map of angles by joint name
     *
     * @param poseid
     * @return a map of target positions by joint for the pose
     */
    fun getPoseJointPositions( poseid: Long ): Map<Joint,Double > {
        return pose.getPoseJointPositions(connection, poseid)
    }
    /**
     * Return a map of speeds by joint name. These speeds may, or may not
     * have been previously configured in the joint.
     *
     * @param poseid
     * @return a map of target speeds by joint for joints in the pose
     */
    fun getPoseJointSpeeds( poseid: Long,map:Map<Joint, MotorConfiguration>): Map<Joint,Double > {
        return pose.getPoseJointSpeeds(connection, poseid,map)
    }
    /**
     * Return a map of speeds by joint name. These speeds may, or may not
     * have been previously configured in the joint.
     *
     * @param poseid
     * @return a map of target speeds by joint for joints in the pose
     */
    fun getPoseJointTorques( poseid: Long ,map:Map<Joint, MotorConfiguration>): Map<Joint,Double > {
        return pose.getPoseJointTorques(connection, poseid,map)
    }

    /**
     * @param posename name of an existing pose
     * @param alias an additional name for the pose.
     */
    fun mapNameToPose(posename: String,alias:String) {
        return pose.mapNameToPose(connection,posename,alias)
    }
    /**
     * @param user-entered command user
     * @param the corresponding pose name
     */
    fun mapFaceNameToDetails(name: String, details: FacialDetails) {
        //return pose.mapCommandToPose(connection, cmd, poseName)
    }
    fun poseExists(poseName:String) :Boolean {
        return pose.poseExists(connection,poseName)
    }
    /**
     * Save a list of motor position values as a pose.
     * @param mcmap contains a map of motor configurations with positions that define the pose.
     * @param poseName
     */
    fun saveJointAnglesForPose(mcmap: Map<Joint, MotorConfiguration>, poseName: String) {
        pose.saveJointAnglesForPose(connection, mcmap, poseName)
        return
    }

    /**
     * Save a list of motor position values as a pose. Assign the pose a name equal to the
     * id of the new database record.
     * @param mcmap contains a map of motor configurations with positions that define the pose.
     * @return the new record id as a string.
     */
    fun saveJointAnglesAsNewPose(mcmap: Map<Joint, MotorConfiguration>): String {
        return pose.saveJointAnglesAsNewPose(connection, mcmap)
    }

    /**
     * Create a database connection. Use this for all subsequent queries.
     * @param path to database instance
     */
    fun startup(path: Path) {
        val connectPath = "jdbc:sqlite:$path"
        LOGGER.info(String.format("%s.startup: database path = %s", CLSS, path.toString()))
        try {
            connection = DriverManager.getConnection(connectPath)
        }
        catch (e: SQLException) {
            // if the error message is "out of memory", 
            // it probably means no database file is found
            LOGGER.log(Level.SEVERE, String.format("%s.startup: Database error (%s)", CLSS, e.message))
        }
    }

    /**
     * Close the database connection prior to stopping the application.
     *
     * @param path to database instance
     */
    fun shutdown() {
        LOGGER.info(String.format("%s.shutdown", CLSS))
        if (connection != null) {
            try {
                connection!!.close()
            }
            catch (e: SQLException) {
                // if the error message is "out of memory", 
                // it probably means no database file is found
                LOGGER.warning(String.format("%s.shutdown: Error closing database (%s)", CLSS, e.message))
            }
        }
    }

    private const val CLSS = "Database"
    private val LOGGER = Logger.getLogger(CLSS)
    private val driver: JDBC = JDBC() // Force driver to be loaded
    private var connection: Connection? = null
    private val face: FaceTable
    private val pose: PoseTable

    /**
     * Initialize the table.
     */
    init {
        face = FaceTable()
        pose = PoseTable()
    }
}