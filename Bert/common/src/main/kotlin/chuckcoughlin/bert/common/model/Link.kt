/**
 * Copyright 2023-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import org.hipparchus.complex.Quaternion
import java.util.logging.Logger

/**
 * A link is a skeletal structure analagous to a bone and is named for it.
 * A Link has "pins" or connection points with other links or end points.
 * There is also a link called the "parent" which describes the root
 * connection to the current link.
 *
 * The joints are always "revolutes", that is rotational only. There
 * is no translation. The joint axis is always at 0 or 90 degrees to a
 * line from the origin to linkPoint.
 *
 * Links with no destination joint are called "end effectors" and can
 * be expected to have one or more "extremities" for which 3D locations
 * can be calculated.
 *
 * Within the linkPin object, coordinates are with respect to the
 * link's parent and are static. Position within the link object is
 * temporary and are calculated with respect to the inertial frame of
 * reference. Any corrections due to IMU readings are handled externally.
 *
 * Define a link with a bone reference. Its name must be unique.
 * A single link may hold several joints and/or extremities.
 * @param bone bone
 */
class Link( bone: Bone) {
    // These are for links that have multiple pins or end effectors
    val destinationPinForJoint: MutableMap<Joint,LinkPin>
    val destinationPinForAppendage: MutableMap<Appendage,LinkPin>
    val bone = bone
    var sourcePin: LinkPin

    /**
     * An endpoint is an extremity or a RESOLUTE link
     */
    fun addEndPoint(end: LinkPin) {
        if( end.type.equals(PinType.END_EFFECTOR)) {
            destinationPinForAppendage[end.appendage] = end
        }
        else if( end.type.equals(PinType.REVOLUTE)) {
            destinationPinForJoint[end.joint] = end
        }
    }

    private fun rotationFromCoordinates(cc: DoubleArray): DoubleArray {
        var len = Math.sqrt(cc[0] * cc[0] + cc[1] * cc[1] + cc[2] * cc[2])
        if (len == 0.0) len = 1.0 // All angles will be 90 deg
        val rot = DoubleArray(3)
        rot[0] = Math.acos(cc[0] / len)
        rot[0] = Math.acos(cc[1] / len)
        rot[0] = Math.acos(cc[2] / len)
        return rot
    }

    private val CLSS = "Link"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean
    /**
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        destinationPinForAppendage = mutableMapOf<Appendage,LinkPin>()
        destinationPinForJoint = mutableMapOf<Joint,LinkPin>()
        sourcePin = LinkPin(PinType.ORIGIN)   // Origin, for now
    }
}