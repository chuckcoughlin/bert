/**
 * Copyright 2024-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import java.util.logging.Logger

/**
 * A LinkPin is either a hinged joint or an end effector (unmoving).
 * The offset coordinates are a 3D location of the joint/end effector
 * with respect to the origin of the link. The origin of a Link is
 * a REVOLUTE LinkPin belonging to the parent of that link.
 *
 * The alignment array is a unit-length vector showing the direction of the
 * axis of the joint motor with respect to a line from the joint
 * to the link origin. The alignment coordinates are with respect
 * to the link origin. In most cases, the linkPin
 * is along the z axis.
 */
class LinkPin (val type:PinType ) {
    var appendage: Appendage  // End effector
    var mc: MotorConfiguration? = null
    var offset: Double  // joint angle equivalent to "straight"

    var angle: Double = 0.0
        get() = if(mc==null) 0.0 else mc!!.angle + offset
    /*
     * The joint implies a motor configuration. From it we get the rotational angle.
     * Note that changing the angle does not invalidate the current link, just its children.
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
        appendage = Appendage.NONE
        joint     = Joint.NONE
        offset = 0.0
    }

}