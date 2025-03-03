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
    val destinationPinForJoint: MutableMap<Joint,LinkPin>
    val destinationPinForAppendage: MutableMap<Appendage,LinkPin>
    val bone = bone
    private var dirty = true // Requires calculations
    var sourcePin: LinkPin
    private var angle: Double
    // Co-ordinates are position of link joint with respect to the
    // parent (source) joint. X is the direction from source to joint.
    // Z is the center of the parent joint. Co-ordinates are NOT used
    // for kinematics calculations.
    var coordinates: DoubleArray

    fun coordinatesToPoint():Point3D {
        return Point3D(coordinates[0],coordinates[1],coordinates[2])
    }
    fun coordinatesToText():String {
        return String.format("%3.3f,%3.3f,%3.3f",coordinates[0],coordinates[1],coordinates[2])
    }
    /**
     * Mark link as needing new calculations. We do this because sub-chains
     * that share links can avoid redundant computations.
     */
    fun setDirty() {
        dirty = true
    }

    /**
     * The joint angle is the motor position. Set it in degrees, read it in radians.
     * Note that changing the angle does not invalidate the current link, just its children.
     * @return
     */// Convert to radians
    var jointAngle: Double
        get() = angle
        set(a) {
            angle = a * Math.PI / 180.0
        }

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

    /**
     * Recalculate the position of this link from an updated location of the parent
     * and its joint position.
     *
     */
    fun updateLocation() {
        if( dirty ) {
            if(sourcePin.type==PinType.REVOLUTE) {
            }
        }
        dirty = false
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
        angle = Math.PI
        dirty = true
        destinationPinForAppendage = mutableMapOf<Appendage,LinkPin>()
        destinationPinForJoint = mutableMapOf<Joint,LinkPin>()
        sourcePin = LinkPin(PinType.ORIGIN)   // Origin, for now
        coordinates = doubleArrayOf(0.0, 0.0, 0.0)
    }
}