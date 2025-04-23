/**
 * Copyright 2024-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Axis
import java.util.logging.Logger

/**
 * A LinkPin is either a hinged joint or an end effector (unmoving).
 * "home" is the anglar position that corresponds to the "straight"
 * joint/end effector motor angle.
 *
 * Axis refers to the orientation of the joint axis with respect to the
 * reference frame when the robot is in its "straight" position. "home"
 * is the joint angle in this position.
 *
 * All angles are in radians.
 */
class LinkPin (val type:PinType ) {
    var axis:Axis             // Joint direction
    var appendage: Appendage  // End effector
    var mc: MotorConfiguration? = null
    var home:Double

    var angle: Double = 0.0
        get() = if(mc!=null) mc!!.angle*Math.PI/180.0 else 0.0

    /*
     * The joint implies a motor configuration. From this we read the
     * real-time rotational angle.
     */
    var joint:Joint
        get() =
            if (mc == null) Joint.NONE
            else mc!!.joint

        set(j) {
            mc = RobotModel.motorsByJoint[j]
        }

    val DEBUG: Boolean
    private val CLSS =  "LinkPin"
    val LOGGER = Logger.getLogger(CLSS)

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        axis = Axis.X
        appendage = Appendage.NONE
        home      = 0.0
        joint     = Joint.NONE
    }

}