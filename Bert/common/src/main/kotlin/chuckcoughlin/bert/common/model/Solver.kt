/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import java.nio.file.Path
import java.util.logging.Logger

/**
 * This class handles various computations pertaining to the robot,
 * including: trajectory planning. Note that the same link object
 * may belong to several chains.
 *
 * The URDFModel is the tree of links which describes the robot.
 */
object Solver {
    val model: URDFModel
    private var motorConfigurations: Map<Joint, MotorConfiguration>

    /**
     * Traverse the tree, setting the current angles from the
     * motor configurations. Mark the links as "dirty".
     */
    fun setTreeState() {
        val links: Collection<Link> = model.chain.linksByBone.values
        for (link in links) {
            for( endPoint in link.linkPoints) {
                link.setDirty()
                if( endPoint.type.equals(LinkPointType.REVOLUTE)) {
                    val joint = endPoint.joint
                    val mc: MotorConfiguration? = motorConfigurations[joint]
                    link.jointAngle = mc!!.angle
                }
            }
        }
    }

    /**
     * Analyze the URDF file for robot geometry. This must be called before
     * we set a tree state.
     * @param mc a map of MotorConfigurations
     * @param urdfPath
     */
    fun configure(mc: Map<Joint, MotorConfiguration>, urdfPath: Path) {
        motorConfigurations = mc
        LOGGER.info(String.format("%s.configure: URDF file(%s)", CLSS, urdfPath.toAbsolutePath().toString()))
        model.analyzePath(urdfPath)
    }

    /**
     * Return the location of a specified appendage in x,y,z coordinates in meters from the
     * robot origin in the pelvis.
     */
    fun getLocation(extremity: Extremity): DoubleArray {
        val subchain: List<Link> = model.chain.partialChainToExtremity(extremity)
        return if (subchain.size > 0) subchain[0].coordinates else ERROR_POSITION
    }

    /**
     * Return the location of a specified joint in x,y,z coordinates in meters from the
     * robot origin in the pelvis in the inertial reference frame.
     */
    fun getLocation(joint: Joint): DoubleArray {
        val subchain: List<Link> = model.chain.partialChainToJoint(joint)
        return if (subchain.size > 0) subchain[0].coordinates else ERROR_POSITION
    }

    fun getMotorConfigurations(): Map<Joint, MotorConfiguration> {
        return motorConfigurations
    }

    /**
     * Set the position of a joint. This is primarily for testing. It does not
     * cause a serial write to the motor.
     * @param joint
     * @param pos
     */
    fun setJointPosition(joint: Joint?, pos: Double) {
        val mc: MotorConfiguration? = motorConfigurations[joint]
        mc!!.angle = pos
    }


    private const val CLSS = "Solver"
    private val LOGGER = Logger.getLogger(CLSS)
    private val ERROR_POSITION = doubleArrayOf(0.0, 0.0, 0.0)


    /**
     * Constructor:
     */
    init {
        model = URDFModel
        motorConfigurations = HashMap<Joint, MotorConfiguration>()
    }
}