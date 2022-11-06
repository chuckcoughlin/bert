/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.sql.db

import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.JointProperty
import chuckcoughlin.bert.common.model.MotorConfiguration
import chuckcoughlin.bert.sql.pose.PoseTable
import org.sqlite.JDBC
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * This class is a wrapper for the entire robot database. It is implemented
 * as a singleton for easy access. The startup() method must be called
 * before it can be used as it opens the database connection.
 *
 * Call shutdown() when database access is no longer required.
 */
class Database private constructor() {
    private var connection: Connection? = null
    private val pose: PoseTable

    /**
     * Static method to create and/or fetch the single instance.
     */
    var database: Database = Database()
        private set

    /**
     * @param command user entered string
     * @return the corresponding pose name if it exists, otherwise NULL
     */
    fun getPoseForCommand(command: String): String? {
        return pose.getPoseForCommand(connection, command)
    }

    /** Return a list of column names with non-null values for the indicated pose
     * property.
     * @param mcmap a map of configurations. Joints not present are ignored.
     * @param pose
     * @param parameter, e.g. "position","speed","torque"
     * @return list of upper-case joint names.
     */
    fun getPoseJointValuesForParameter(
        mcmap: Map<String, MotorConfiguration>,poseName: String,parameter: JointProperty
    ): Map<String, Double> {
        return pose.getPoseJointValuesForParameter(connection, mcmap, poseName, parameter)
    }

    /**
     * @param user-entered command user
     * @param the corresponding pose name
     */
    fun mapCommandToPose(cmd: String, poseName: String) {
        return pose.mapCommandToPose(connection, cmd, poseName)
    }

    /**
     * Save a list of motor position values as a pose.
     * @param mcmap contains a map of motor configurations with positions that define the pose.
     * @param poseName
     */
    fun saveJointPositionsForPose(mcmap: Map<Joint, MotorConfiguration>, poseName: String) {
        pose.saveJointPositionsForPose(connection, mcmap, poseName)
        return
    }

    /**
     * Save a list of motor position values as a pose. Assign the pose a name equal to the
     * id of the new database record.
     * @param mcmap contains a map of motor configurations with positions that define the pose.
     * @return the new record id as a string.
     */
    fun saveJointPositionsAsNewPose(mcmap: Map<Joint, MotorConfiguration>): String {
        return pose.saveJointPositionsAsNewPose(connection, mcmap)
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

    companion object {
        private const val CLSS = "Database"
        private val LOGGER = Logger.getLogger(CLSS)
        private val driver: JDBC = JDBC() // Force driver to be loaded
    }
    /**
     * Initialize the table.
     */
    init {
        pose = PoseTable()
    }
}