/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion
import chuckcoughlin.bert.common.model.Solver.model
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
     * Return the orientation of a specified appendage in x,y,z coordinates in meters from the
     * robot origin in the pelvis. The named end effector or appendage is
     * last in the chain.
     */
    fun computeDirection(appendage: Appendage): DoubleArray {
        val subchain: List<Link> = Chain.partialChainToAppendage(appendage)
        val q = computeQuaternionFromChain(subchain)
        return q.direction()
    }

    /**
     * Return the orientation of a specified joint in x,y,z coordinates in meters from the
     * robot origin in the pelvis in the inertial reference frame. The named joint is
     * last in the chain.
     */
    fun computeDirection(joint:Joint): DoubleArray {
        val subchain: List<Link> = Chain.partialChainToJoint(joint)
        val q = computeQuaternionFromChain(subchain)
        return q.direction()
    }

    /**
     * Return a string for debugging use containing both position and direction
     */
    fun computeLocationDescription(joint:Joint): String {
        val subchain: List<Link> = Chain.partialChainToJoint(joint)
        val q = computeQuaternionFromChain(subchain)
        return String.format("%s [%s]",q.positionToText(),q.directionToText())
    }
    /**
     * Return a string for debugging use containing both position and direction
     */
    fun computeLocationDescription(appendage:Appendage): String {
        val subchain: List<Link> = Chain.partialChainToAppendage(appendage)
        val q = computeQuaternionFromChain(subchain)
        return String.format("%s [%s]",q.positionToText(),q.directionToText())
    }
    /**
     * Return the location of a specified appendage in x,y,z coordinates in meters from the
     * robot origin in the pelvis. The named end effector or appendage is
     * last in the chain.
     */
    fun computeLocation(appendage: Appendage): Point3D {
        val subchain: List<Link> = Chain.partialChainToAppendage(appendage)
        val q = computeQuaternionFromChain(subchain)
        return q.position()
    }

    /**
     * Return the location of a specified joint in x,y,z coordinates in meters from the
     * robot origin in the pelvis in the inertial reference frame. The named joint is
     * last in the chain.
     */
    fun computeLocation(joint:Joint): Point3D {
        val subchain: List<Link> = Chain.partialChainToJoint(joint)
        val q = computeQuaternionFromChain(subchain)
        return q.position()
    }

    /**
     * Update the link coordinates in a chain starting from the IMU, then multiply
     * quaternion matrices to get final position. The final position includes the
     * x,y,z position of the end effector with the orientation of the attached link.
     */
    private fun computeQuaternionFromChain(subchain: List<Link>):Quaternion {
        // Start with the IMU (Its orientation is updated externally)
        IMU.update()
        var q = IMU.quaternion   // Update for any rotation.
        if(DEBUG) LOGGER.info(String.format("%s.computeQuaternionFromChain: IMU = (%s|%s) ",
            CLSS,q.positionToText(),q.directionToText()))
        // Now continue up the chain
        for(link in subchain) {
            //if(DEBUG) LOGGER.info(String.format("%s.computeQuaternionFromChain: %s source %s = (%s) ",
            //    CLSS,link.name,if( link.sourcePin.joint==Joint.NONE) "IMU" else link.sourcePin.joint.name,q.position().toText()))
            link.update()  // In case motor has moved since last use
            //if(DEBUG) LOGGER.info(link.quaternion.dump())
            q = q.postMultiplyBy(link.quaternion)
            if(DEBUG) LOGGER.info(String.format("%s.computeQuaternionFromChain: %s end    %s = (%s|%s) ",
                CLSS,link.name,link.endPin.joint.name,q.positionToText(),q.directionToText()))
            //if(DEBUG) LOGGER.info(q.dump())
        }
        return q
    }

    /**
     *
     */
    fun linkLocationsToJSON():String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val list = mutableListOf<LinkLocation>()
        fillLocations(list)
        return gson.toJson(list)
    }

    /**
     * Populate a list of locations for all joints and extremities
     * NOTE: There must be a more effecient way of doing this.
     */
    private fun fillLocations(list:MutableList<LinkLocation>) {
        for(link in URDFModel.linkForAppendage.values) {
            val loc = LinkLocation()
            loc.updateFromLink(link)
            var pos = computeLocation(link.sourcePin.joint)
            loc.updateSource(pos)
            pos = computeLocation(link.endPin.appendage)
            loc.updateEnd(pos)
            list.add(loc)
        }
        for(link in URDFModel.linkForJoint.values) {
            val loc = LinkLocation()
            loc.updateFromLink(link)
            var pos = computeLocation(link.sourcePin.joint)
            loc.updateSource(pos)
            pos = computeLocation(link.endPin.joint)
            loc.updateEnd(pos)
            list.add(loc)
        }
    }


    private const val CLSS = "Solver"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean

    /**
     * Constructor:
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        model = URDFModel
    }
}