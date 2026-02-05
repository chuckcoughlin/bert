/**
 * Copyright 2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion
import java.util.logging.Logger

/**
 * A JointLink encapsulates a BasicLink to add a transform quaternion.
 * Note: We had trouble with Json when we tried a straightforward extension.
 *
 * @param sourceJoint the source joint
 * @param endJoint the end joint or end effector
 */
class JointLink( )  {
    // Values are degrees.
    var transform: Quaternion
    var home:Double
    val basic: BasicLink

    fun applyTransform(q:Quaternion) : Quaternion {
        val end = q.postMultiplyBy(transform)
        return end
    }
     fun setCoordinates(x:Double,y:Double,z:Double) {
        if (DEBUG) LOGGER.info(String.format("%s.setCoordinates: (%s) %2.2f,%2.2f,%2.2f", CLSS, Joint.NONE.name, x, y, z))
        basic.setCoordinates(x, y, z)
    }

    /*
     * Compute the transform quaternion once the corrdinates
     * and orientation are set.
     */
    fun update(theta:Double) {
        //transform.setRoll(basic.orientation[0])
        //transform.setPitch(basic.orientation[1])
        //transform.setYaw(basic.orientation[2])
        //transform.setTranslation(basic.coordinates[0],basic.coordinates[1],basic.coordinates[2])
        //transform.update()
    }

    fun clone() : JointLink {
        val copy = JointLink()
        copy.transform    = transform.clone()
        copy.home = home
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
        basic = SimpleLink()
    }
}