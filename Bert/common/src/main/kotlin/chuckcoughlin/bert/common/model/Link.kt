/**
 * Copyright 2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import org.hipparchus.complex.Quaternion
import java.util.logging.Logger

/**
 * A Link is a solid member between two "LinkPoints" called the "origin"
 * and "linkPoint". Multiple links may be connected to the same source.
 * The joints are always "revolutes", that is rotational only. There
 * is no translation. The joint axis is always at 0 or 90 degrees to a
 * line from the origin to linkPoint.
 *
 * Links with no destination joint are called "end effectors" and can
 * be expected to have one or more "appendages" for which 3D locations
 * can be calculated.
 *
 * Within the linkPoint object, coordinates are with respect to the
 * link's origin and are static. Coordinates within the link object are
 * temporary and are calculated with respect to the inertial frame of
 * reference. Any corrections due to IMU readings are handled externally.
 *
 * Define a link given the name. The name must be unique.
 * @param name either a limb or appendage name
 */
class Link(val name: String) {
    lateinit var linkPoint: LinkPoint
        private set
    private var initialized = false // Requires calculations
    var parent: Link? = null        // This is never set
    private var angle: Double
    var coordinates = doubleArrayOf(0.0, 0.0, 0.0)

    /**
     * Mark link as needing new calculations. We do this because sub-chains
     * that share links can avoid redundant computations.
     */
    fun setDirty() {
        initialized = false
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

    fun setEndPoint(end: LinkPoint) {
        linkPoint = end
    }

    /**
     * Return the coordinates of the endpoint relative to inertial frame. The endpoint is
     * either a joint or appendage.
     *
     * 1) Get the parent's rotation, add the orientation of the origin. This represents
     * the rotation angle in the inertial frame.
     * 2) Take the local offset, use quaternion multiplication to compute the local
     * relative position of the joint in intertial frame.
     * 3) Add this position to the parent coordinates resulting in the absolute position
     * of the joint with respect to the inertial frame.
     *
     * The coordinates are returned immediately if the link is "clean", otherwise the
     * above calculations are required, possibly up the chain to the root node.
     *
     * @return the coordinates of the joint/appendage associated with this link in meters
     * with respect to the inertial frame of reference.
     */
    fun getEndPointCoordinates(): DoubleArray {
        var coords: DoubleArray // Coordinates in progress
        var rotation: DoubleArray
        var alpha: Double // = 0.0
        if( !initialized ) {
            if (parent != null) {
                coords = parent!!.getEndPointCoordinates()
                alpha = parent!!.jointAngle
                val orient = parent!!.linkPoint.orientation
                rotation = rotationFromCoordinates(coords)
                rotation[0] = rotation[0] + orient[0]
                rotation[1] = rotation[1] + orient[1]
                rotation[2] = rotation[2] + orient[2]
            }
            else {  // Reference the origin
                coords = LinkPoint().offset
                rotation = LinkPoint().orientation
                alpha = Math.PI
            }
            LOGGER.info(String.format("%s.getCoordinates: %s (%s) ---------------",
                    CLSS,name,linkPoint.joint.name ))
            LOGGER.info(String.format("           rotation = %.2f,%.2f,%.2f", rotation[0], rotation[1], rotation[2]))
            val offset = linkPoint.offset
            LOGGER.info(String.format("           offset   = %.2f,%.2f,%.2f", offset[0], offset[1], offset[2]))
            val q0 = Quaternion(alpha, rotation[0], rotation[1], rotation[2])
            LOGGER.info(String.format("           q0       = %.2f,%.2f,%.2f,%.2f",
                    q0.getQ0(),
                    q0.getQ1(),
                    q0.getQ2(),
                    q0.getQ3()
                )
            )
            val v = Quaternion(0.0, offset[0], offset[1], offset[2])
            LOGGER.info(String.format("           v        = %.2f,%.2f,%.2f,%.2f",
                    v.getQ0(), v.getQ1(),v.getQ2(),v.getQ3()))
            val inverse: Quaternion = q0.getInverse()
            LOGGER.info(String.format("           inverse  = %.2f,%.2f,%.2f,%.2f",
                    inverse.getQ0(),
                    inverse.getQ1(),
                    inverse.getQ2(),
                    inverse.getQ3()
                )
            )
            val result: Quaternion = q0.multiply(v).multiply(inverse)
            LOGGER.info(String.format("           result   = %.2f,%.2f,%.2f,%.2f",
                    result.getQ0(),
                    result.getQ1(),
                    result.getQ2(),
                    result.getQ3()
                )
            )
            coordinates[0] = coords[0] + result.getQ1()
            coordinates[1] = coords[1] + result.getQ2()
            coordinates[2] = coords[2] + result.getQ3()
            LOGGER.info(String.format("      coordinates   = %.2f,%.2f,%.2f",
                    coordinates[0],coordinates[1],coordinates[2]))
            initialized = true
        }
        return coordinates
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

    /**
     */
    init {
        angle = Math.PI
        initialized = false
    }
}