/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

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
     * robot origin in the pelvis. he named extremity is
     * last in the chain.
     */
    fun computeLocation(extremity: Extremity): Point3D {
        val subchain: List<Link> = Chain.partialChainToExtremity(extremity)
        for(link in subchain) {

        }
        return if (subchain.size > 0) subchain[0].coordinatesToPoint() else ERROR_POSITION
    }

    /**
     * Return the location of a specified joint in x,y,z coordinates in meters from the
     * robot origin in the pelvis in the inertial reference frame. The named joint is
     * last in the chain.
     */
    fun computeLocation(joint: Joint): Point3D {
        val subchain: List<Link> = Chain.partialChainToJoint(joint)
        for(link in subchain) {

        }
        return if (subchain.size > 0) subchain[0].coordinatesToPoint() else ERROR_POSITION
    }

    /**
     */
    fun extremityLocationToJSON(extremity:Extremity):String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val loc = ExtremityLocation(extremity,computeLocation(extremity))
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
        val links: Collection<Link> = Chain.linksByBone.values
        for (link in links) {
            for( endPoint in link.linkPinsByJoint.values) {
                if( endPoint.type.equals(PinType.REVOLUTE)) {  //redundant
                    val joint = endPoint.joint
                    val mc: MotorConfiguration = RobotModel.motorsByJoint[joint]!!
                    if(link.jointAngle!=mc.angle)  {
                        link.jointAngle = mc.angle
                        link.setDirty()
                    }
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
        for(extremity in Extremity.values()) {
            if(extremity!=Extremity.NONE) {
                val loc = ExtremityLocation(extremity,computeLocation(extremity))
                list.add(loc)
            }
        }
    }


    private const val CLSS = "Solver"
    private val LOGGER = Logger.getLogger(CLSS)
    private val ERROR_POSITION = Point3D(0.0, 0.0, 0.0)


    /**
     * Constructor:
     */
    init {
        model = URDFModel
    }
}