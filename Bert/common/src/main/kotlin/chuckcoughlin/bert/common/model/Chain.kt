/**
 * Copyright 2023-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.model.RobotModel.limbsByJoint
import com.google.gson.GsonBuilder
import java.util.*
import java.util.logging.Logger

/**
 * A Chain represents a tree of Links starting with the
 * "root" link. The position of links within the chain are
 * all relative to the root link (i.e. origin). The URDF
 * file format doesn't define things in the most convenient
 * order.
 *
 * Changes to the "inertial frame" as detected by the IMU
 * are all handled here.
 */
class Chain {
    var root: Link
    val linksByBone: MutableMap<Bone, Link>
    private val linksByExtremity: MutableMap<Extremity, Link>
    private val linksByJoint: MutableMap<Joint, Link>
    private var origin: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)
    private var axis: DoubleArray   = doubleArrayOf(0.0, 0.0, 0.0)

    /**
     * As we add origin and endpoints, the new link gets added to the various
     * maps that allow us to navigate the chain. For now we just add
     * to the main
     *
     * @param the new link.
     */
    fun addLink(link:Link) {
        //LOGGER.info(String.format("%s.addLink: Added link %s",CLSS,link.bone.name))
        linksByBone[link.bone] = link
    }

    fun linkForBoneName(name:String) : Link? {
        val bone = Bone.fromString(name)
        val link = linksByBone[bone]
        return link
    }
    fun linkForJointName(name:String) : Link? {
        val joint = Joint.fromString(name)
        val link = linksByJoint[joint]
        return link
    }

    fun setLimb(joint:Joint,limb:Limb) {
        limbsByJoint[joint] = limb
    }
    fun setLinkForJoint(joint:Joint,link:Link) {
        linksByJoint[joint] = link
    }

    /**
     * @return  a comma-separated string of the names of all links.
     */
    fun boneNames(): String {
        var names = StringBuffer()
        for (bone in linksByBone.keys) {
            names.append(bone.name)
            names.append(", ")
        }
        if( names.isNotEmpty() ) return names.substring(0, names.length - 2)
        else return "none"
    }
    /**
     * @return  a JSON pretty-printed String array of all property types. Exclude NONE.
     */
    fun bonesToJSON(): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        var names = mutableListOf<String>()
        for (bone in linksByBone.keys) {
            names.add(bone.name)
        }
        return gson.toJson(names)
    }
    /**
     * @return  a comma-separated string of the names of all extremities.
     */
    fun extremityNames(): String {
        var names = StringBuffer()
        for (extremity in linksByExtremity.keys) {
            names.append(extremity.name.lowercase())
            names.append(", ")
        }
        if( names.isNotEmpty() ) return names.substring(0, names.length - 2)
        else return "none"
    }
    /**
     * @return  a JSON pretty-printed String array of all appendaged.
     */
    fun extremitiesToJSON(): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        var names = mutableListOf<String>()
        for (extremity in linksByExtremity.keys) {
            names.add(extremity.name)
        }
        return gson.toJson(names)
    }
    /**
     * @return  a comma-separated string of the names of all joints.
     */
    fun jointNames(): String {
        var names = StringBuffer()
        for (joint in linksByJoint.keys) {
            names.append(joint.name.lowercase())
            names.append(", ")
        }
        if( names.isNotEmpty() ) return names.substring(0, names.length - 2)
        else return "none"
    }
    /**
     * @return  a JSON pretty-printed String array of all property types. Exclude NONE.
     */
    fun jointsToJSON(): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        var names = mutableListOf<String>()
        for (joint in linksByJoint.keys) {
            names.add(joint.name)
        }
        return gson.toJson(names)
    }


    /**
     * There may be multiple joints with the same parent, but only one parent per joint.
     * @param jointName
     * @return the parent link of the named joint. If not found, return null.
     */
    fun linkForJoint(joint: Joint): Link? {
        return linksByJoint[joint]
    }

    /**
     * Work back toward the root from the specified extremity. The chain
     * is ordered beginning from the root.
     * @param extremity
     * @return
     */
    fun partialChainToExtremity(extremity: Extremity?): List<Link> {
        val partial: LinkedList<Link> = LinkedList<Link>()
        var link = linksByExtremity[extremity]
        while (link != null) {
            partial.addFirst(link)
            if( link.parent.type.equals(LinkPointType.ORIGIN)) break
            link = linkForJoint(link.parent.joint)
        }
        return partial
    }

    /**
     * Work back toward the root link beginning with the indicated joint.
     * The chain starts with the root.
     * @param joint, the source
     * @return
     */
    fun partialChainToJoint(joint: Joint): List<Link> {
        val partial: LinkedList<Link> = LinkedList<Link>()
        var link = linksByJoint[joint]
        while (link != null) {
            partial.addFirst(link)
            if( link.parent.type.equals(LinkPointType.ORIGIN)) break
            link = linkForJoint(link.parent.joint)
        }
        return partial
    }

    /**
     * The axes are the Euler angles in three dimensions between the robot and the reference frame.
     * @param a three dimensional array of rotational offsets between the robot and reference frame.
     */
    fun setAxes(a: DoubleArray) {
        axis = a
    }

    /**
     * The origin is the offset of the IMU with respect to the origin of the robot.
     * @param o three dimensional array of offsets to the origin of the chain
     */
    fun setOrigin(o: DoubleArray) {
        origin = o
    }


    private val CLSS = "Chain"
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        linksByExtremity = mutableMapOf<Extremity, Link>()
        linksByBone      = mutableMapOf<Bone,Link>()
        linksByJoint     = HashMap<Joint,Link>()
        root = Link(Bone.NONE)    // Must be reset to be useful
    }
}