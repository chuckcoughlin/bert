/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion
import com.google.gson.GsonBuilder
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

    /**
     * Return the location of a specified appendage in x,y,z coordinates in meters from the
     * robot origin in the pelvis. The named end effector or appendage is
     * last in the chain.
     */
    fun computeLocation(appendage: Appendage): Point3D {
        val subchain: List<LinkPin> = Chain.partialChainToAppendage(appendage)
        val q = computeQuaternionFromChain(subchain)
        return q.resultToPoint()
    }

    /**
     * Return the location of a specified joint in x,y,z coordinates in meters from the
     * robot origin in the pelvis in the inertial reference frame. The named joint is
     * last in the chain.
     */
    fun computeLocation(joint:Joint): Point3D {
        val subchain: List<LinkPin> = Chain.partialChainToJoint(joint)
        val q = computeQuaternionFromChain(subchain)
        return q.resultToPoint()
    }

    /**
     * Update the link coordinates in a chain starting from the origin, then multiply
     * quaternion matrices to get final position.
     */
    private fun computeQuaternionFromChain(subchain: List<LinkPin>):Quaternion {
        var q = Quaternion.identity()
        for(pin in subchain) {
            pin.updateQuaternion()
            q = q.multiplyBy(pin.quaternion)
            if(DEBUG) LOGGER.info(String.format("%s.computeQuaternionFromChain: %s at %2.0f deg (%s) ",
                CLSS,pin.joint,q.resultToPoint().toText()))
        }
        return q
    }

    /**
     */
    fun appendageLocationToJSON(appendage:Appendage):String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val loc = AppendageLocation(appendage,computeLocation(appendage))
        return gson.toJson(loc)
    }

    /**
     *
     */
    fun limbLocationsToJSON(limb:Limb):String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val list = mutableListOf<Location>()
        // If the limb type is NONE, get all locations.
        if( limb==Limb.NONE ) {
            fillLocations(list)
        }
        else {
            for(mc:MotorConfiguration in RobotModel.motorsByJoint.values) {
                val lim = mc.limb
                if( lim==limb ) {

                }
            }
        }
        return gson.toJson(list)
    }

    /**
     * Traverse the tree, setting the current angles from the
     * motor configurations. If the angle has changed, mark the
     * link as "dirty". This should be called before any computation
     * of limb location.
     */
    fun updateLinkAngles() {
        val links: Collection<Link> = URDFModel.linkForBone.values
        for (link in links) {
            for( endPoint in link.destinationPinForJoint.values) {
                if( endPoint.type.equals(PinType.REVOLUTE)) {  //redundant
                    val joint = endPoint.joint
                    val mc: MotorConfiguration = RobotModel.motorsByJoint[joint]!!
                }
            }
        }
    }

    /**
     * Populate a list of locations for all joints and extremities
     */
    private fun fillLocations(list:MutableList<Location>) {
        for(joint in Joint.values()) {
            if(joint!=Joint.NONE) {
                val loc = JointLocation(joint,computeLocation(joint))
                list.add(loc)
            }
        }
        for(appendage in Appendage.values()) {
            if(appendage!=Appendage.NONE) {
                val loc = AppendageLocation(appendage,computeLocation(appendage))
                list.add(loc)
            }
        }
    }


    private const val CLSS = "Solver"
    private val LOGGER = Logger.getLogger(CLSS)
    private val ERROR_POSITION = Point3D(0.0, 0.0, 0.0)
    private val DEBUG: Boolean

    /**
     * Constructor:
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        model = URDFModel
    }
}