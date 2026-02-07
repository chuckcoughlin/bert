/**
 * Copyright 2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion
import java.util.logging.Logger

/**
 * A JointLink extends a BasicLink to add a transform quaternion.
 */
class JointLink(source:Joint,end:Joint) : BasicLink(source,end)  {
    // Values are degrees.
    var transform: Quaternion
    var home:Double

    fun applyTransform(q:Quaternion) : Quaternion {
        val end = q.postMultiplyBy(transform)
        return end
    }

    override fun setCoordinates(x: Double, y: Double, z: Double) {
        super.setCoordinates(x, y, z)
        if(DEBUG) LOGGER.info(String.format("%s.setCoordinates: %s [%3.2f,%3.2f,%3.2f]",CLSS,endJoint.name,
                                        coordinates[0],coordinates[1],coordinates[2]))
    }

    override fun setRpy(roll: Double, pitch: Double, yaw: Double) {
        super.setRpy(roll, pitch, yaw)
        if(DEBUG) LOGGER.info(String.format("%s.setRpy: %s [%3.2f,%3.2f,%3.2f]",CLSS,endJoint.name,
                                        orientation[0],orientation[1],orientation[2]))
    }

    /*
     * Compute the transform quaternion once the corrdinates
     * and orientation are set.
     */
    fun update(theta:Double) {
        transform.setRoll(orientation[0])
        transform.setPitch(orientation[1])
        transform.setYaw(orientation[2])
        transform.setTranslation(coordinates[0],coordinates[1],coordinates[2])
        transform.update()
    }

    fun clone() : JointLink {
        val copy = JointLink(sourceJoint,endJoint)
        copy.transform    = transform.clone()
        copy.home = home
        copy.setCoordinates(coordinates[0],coordinates[1],coordinates[2])
        copy.setRpy(orientation[0],orientation[1],orientation[2])
        return copy
    }

    private val CLSS = "JointLink"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean
    /**
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        transform    = Quaternion()
        home = 0.0
    }
}