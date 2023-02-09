/**
 * Copyright 2022=2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import jssc.SerialPort
import java.nio.file.Path
import java.util.logging.Logger

/**
 * This generic robot model adds convenience methods for extracting specifics
 * without worrying about nulls.
 */
open class RobotModel(configPath: Path) : BaseRobotModel(configPath) {

    /**
     * @return a named String property. If the requested property is not defined,
     *         return the string PROPERTY_NONE.
     */
    fun getProperty(key: String?, defaultValue: String?): String {
        return properties.getProperty(key, defaultValue)
    }
    fun getJointsForController(controller: String): List<Joint>? {
        return jointsByController.get(controller)
    }

    fun getPortForController(controller: String): SerialPort? {
        return ports.get(controller)
    }

    private val CLSS = "RobotMotorModel"
    private val LOGGER = Logger.getLogger(CLSS)

    init {

    }
}