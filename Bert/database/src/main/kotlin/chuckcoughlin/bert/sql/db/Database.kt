/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.sql.db

import chuckcoughlin.bert.common.model.FacialDetails
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.MotorConfiguration
import chuckcoughlin.bert.common.model.PoseDefinition
import chuckcoughlin.bert.sql.tables.ActionTable
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
    fun actionExists(actionName:String) :Boolean {
        return action.actionExists(connection,actionName)
    }
    /**
     * Save a list of motor position, torques and speeds as a new pose.
     * Insert or update the Pose table
     * @param mcmap contains a map of motor configurations with positions that define the pose.
     * @param poseName
     * @param index
     */
    fun createAction(name:String,series: String) {
        action.createAction(connection, name,series)
    }
    /**
     * Save a list of motor position, torques and speeds as a new pose.
     * Insert or update the Pose table
     * @param mcmap contains a map of motor configurations with positions that define the pose.
     * @param poseName
     * @param index
     */
    fun createPose(mcmap: Map<Joint, MotorConfiguration>, poseName: String,index: Int) {
        pose.createPose(connection, mcmap, poseName,index)
    }
    /**
     * @param name pose name. Delete the pose and its joint map from the
     *         database. If the pose does not exist, no action is taken.
     */
    fun deleteAction(name: String) {
        return action.deleteAction(connection, name)
    }
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
    fun deletePose(name: String,index:Int) {
        return pose.deletePose(connection, name,index)
    }
    fun faceExists(actionName:String) :Boolean {
        return face.faceExists(connection,actionName)
    }
    /**
     * @return a Json string with the names of all known faces
     */
    fun faceNamesToJSON() : String {
        return face.faceNamesToJSON(connection)
    }
    /**
     * @return the names of saved faces in a comma-separated list
     */
    fun getActionNames() : String {
        return action.getActionNames(connection)
    }
    /**
     * @return the names of saved faces in a comma-separated list
     */
    fun getFaceNames() : String {
        return face.getFaceNames(connection)
    }
    /**
     * @param name user entered pose name. If the pose does not
     *             exist, it will be created.
     * @return the corresponding pose id if it exists, otherwise NO_POSE
     */
    fun getPoseIdForName(name: String,index:Int): Long {
        return pose.getPoseIdForName(connection, name, index)
    }
    /**
     * Return a map of angles by joint name
     *
     * @param poseid
     * @return a map of target positions by joint for the pose and joints to be considered
     */
    fun getPoseJointPositions( poseid: Long,configs:Map<Joint, MotorConfiguration> ): Map<Joint,Double > {
        return pose.getPoseJointPositions(connection, poseid, configs)
    }
    /**
     * Return a map of speeds by joint name. These speeds may, or may not
     * have been previously configured in the joint.
     *
     * @param poseid
     * @return a map of target speeds by joint for joints of interest  in the pose
     */
    fun getPoseJointSpeeds( poseid: Long,map:Map<Joint, MotorConfiguration>): Map<Joint,Double > {
        return pose.getPoseJointSpeeds(connection, poseid,map)
    }
    /**
     * Return a map of speeds by joint name. These speeds may, or may not
     * have been previously configured in the joint.
     *
     * @param poseid
     * @return a map of target speeds by joint for joints of interest in the pose
     */
    fun getPoseJointTorques( poseid: Long ,map:Map<Joint, MotorConfiguration>): Map<Joint,Double > {
        return pose.getPoseJointTorques(connection, poseid,map)
    }
    /**
     * @return the names of saved faces in a comma-separated list
     */
    fun getPoseNames() : String {
        return pose.getPoseNames(connection)
    }

    /**
     * @return an ordered list of Poses comprising an action.
     *         The inter-pose delay is returned along with the pose name.
     */
    fun getPosesForAction(act:String): List<PoseDefinition> {
        return action.getPosesForAction(connection,act);
    }
    /**
     * @param details facial details of the face we're searching for
     * @return the face id of the match, else NO_FACE
     */
    fun matchDetailsToFace(datails:FacialDetails):Long {
        return face.matchDetailsToFace(connection,datails)
    }

    fun poseExists(poseName:String, index:Int) :Boolean {
        return pose.poseExists(connection,poseName,index)
    }
    /**
     * @param posename
     * @param index
     * @return a list of pose joint, angle,speed and torque converted to a JSON string
     */
    fun poseDetailsToJSON( poseName:String,index:Int): String {
        return pose.poseDetailsToJSON(connection, poseName,index)
    }
    /**
     * @return a Json string with the names of all known faces
     */
    fun poseNamesToJSON() : String {
        return pose.poseNamesToJSON(connection)
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
    private val action: ActionTable
    private val face: FaceTable
    private val pose: PoseTable

    /**
     * Initialize the table.
     */
    init {
        action = ActionTable()
        face = FaceTable()
        pose = PoseTable()
    }
}