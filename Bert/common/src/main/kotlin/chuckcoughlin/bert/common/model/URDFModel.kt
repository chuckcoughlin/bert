/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.util.XMLUtility
import com.google.gson.GsonBuilder
import org.w3c.dom.Document
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.HashMap
import java.util.logging.Logger

/**
 * Parse the URDF file and make its contents available as a chain of links.
 * A chain has a tree structure with a single root.
 */
object URDFModel {
    val origin: LinkPin
    var document: Document?
    val linkForBone: MutableMap<Bone, Link>
    val linkForAppendage: MutableMap<Appendage, Link>
    val revoluteLinkForJoint: MutableMap<Joint, Link>
    val linkForSourceJoint: MutableMap<Joint, Link>
    val sourceJointForLink: MutableMap<Link,Joint>
    val revoluteForJoint : MutableMap<Joint,LinkPin>

    /**
     * Expand the supplied path as the URDF XML file.
     * @return the geometry, an XML document.
     */
    fun analyzePath(filePath: Path) {
        LOGGER.info(String.format("%s.analyzePath: URDF file(%s)", CLSS, filePath.toAbsolutePath().toString()))
        try {
            val bytes = Files.readAllBytes(filePath)
            document = XMLUtility.documentFromBytes(bytes)
            analyzeChain()
        }
        catch (ioe: IOException) {
            LOGGER.severe(String.format("%s.analyzePath: Failed to read file %s (%s)",
                    CLSS, filePath.toAbsolutePath().toString(), ioe.getLocalizedMessage()))
        }
    }
    // ================================ Auxiliary Methods  ===============================
    /**
     * Search the model for IMU, link and joint elements.
     * IMU maps the location of the start of the chain with respect to the
     * center of gravity.
     */
    private fun analyzeChain() {
        if (document != null) {
            // ================================== IMU ===============================================
            val imus = document!!.getElementsByTagName("imu")
            if (imus.length > 0) {
                LOGGER.info(String.format("%s.analyzeChain: IMU ...",CLSS))
                val imuNode = imus.item(0) // Should only be one
                val axis = doubleArrayFromString(XMLUtility.attributeValue(imuNode, "axis"))
                origin.axis = axis
                val xyz = doubleArrayFromString(XMLUtility.attributeValue(imuNode, "xyz"))
                origin.coordinates = Point3D(xyz[0],xyz[1],xyz[2])
            }

            // ================================== Links ===============================================
            // Links correspond to Bone data types. Links can have extremities and/or joints, plus a
            // source (parent). The link that has no parent is the origin
            //      ---------------------------- First Pass ----------------------------------
            // Create all the Links and their LinkPins. A LinkPin encompasses the connecting joint
            val links = document!!.getElementsByTagName("link")
            val count = links.length
            var index = 0
            while (index < count) {
                val linkNode = links.item(index)
                val name: String = XMLUtility.attributeValue(linkNode, "name")
                if(DEBUG) LOGGER.info(String.format("%s.analyzeChain: link %s ...",CLSS,name))
                try {
                    val bone = Bone.fromString(name)
                    if( !bone.equals(Bone.NONE)) {
                        val link = Link(bone)
                        linkForBone[bone] = link
                        val axis = doubleArrayFromString(XMLUtility.attributeValue(linkNode, "axis"))
                        val children = linkNode.childNodes
                        val acount = children.length
                        var aindex = 0
                        while (aindex < acount) {
                            val node = children.item(aindex)
                            if ("appendage".equals(node.localName, ignoreCase = true)) {
                                val aname: String = XMLUtility.attributeValue(node, "name")
                                val appendage: Appendage = Appendage.fromString(aname)
                                val pin = LinkPin(PinType.END_EFFECTOR)
                                pin.appendage = appendage
                                val offset = XMLUtility.attributeValue(node, "offset")
                                if( offset.isNotBlank() ) pin.offset = offset.toDouble()
                                val xyz = doubleArrayFromString(XMLUtility.attributeValue(node, "xyz"))
                                pin.coordinates = Point3D(xyz[0],xyz[1],xyz[2])
                                pin.axis = axis
                                link.addEndPoint(pin)
                                linkForAppendage[appendage] = link
                            }
                            else if ("joint".equals(node.localName)) {
                                val aname: String = XMLUtility.attributeValue(node, "name")
                                val joint = Joint.fromString(aname)
                                val pin = LinkPin(PinType.REVOLUTE)
                                pin.joint = joint
                                val offset = XMLUtility.attributeValue(node, "offset")
                                if( offset.isNotBlank() ) pin.offset = offset.toDouble()
                                val xyz = doubleArrayFromString(XMLUtility.attributeValue(node, "xyz"))
                                pin.coordinates = Point3D(xyz[0],xyz[1],xyz[2])
                                pin.axis = axis
                                link.addEndPoint(pin)
                                revoluteForJoint[joint] = pin
                                revoluteLinkForJoint[joint] = link
                            }
                            else if ("source".equals(node.localName)) {
                                val aname: String = XMLUtility.attributeValue(node, "joint")
                                val joint = Joint.fromString(aname)
                                sourceJointForLink[link]  = joint
                                linkForSourceJoint[joint] = link
                            }
                            aindex++
                        }
                    }
                    else {
                        LOGGER.warning(String.format("%s.analyzeChain: link refers to an unknown bone: %s, ignored",CLSS,name))
                    }
                }
                catch (iae: IllegalArgumentException) {
                    LOGGER.warning(String.format("%s.analyzeChain: link exception on first pass %s, ignored (%s)",
                            CLSS,name,iae.localizedMessage))
                    iae.printStackTrace()
                }
                index++
            }

            //      ---------------------------- Connect source pin for link ----------------------------------
            for(link in linkForBone.values) {
                val joint = sourceJointForLink[link]
                if( joint!=null ) {
                    val pin = revoluteForJoint[joint]
                    if( pin==null) {
                        LOGGER.warning(String.format("%s.analyzeChain: no link pin found for: %s, ignored",CLSS,joint.name))
                    }
                    else {
                        link.sourcePin = pin
                    }
                }
                else {
                    link.sourcePin = origin
                }
            }
        }
    }

    /**
     * @return  the bone that has the specified
     *          joint as its source
     */
    fun boneForJoint(joint:Joint): Bone {
        var bone = Bone.PELVIS
        val link = linkForSourceJoint[joint]
        if( link!=null) {
            bone = link.bone
        }
        return bone
    }
    /**
     * @return  a comma-separated string of the names of all links.
     */
    fun boneNames(): String {
        val names = StringBuffer()
        for (bone in linkForBone.keys) {
            names.append(bone.name)
            names.append(", ")
        }
        if( names.isNotEmpty() ) {
            return names.substring(0, names.length - 2)
        }
        return "none"
    }
    /**
     * @return  a JSON pretty-printed String array of all property types. Exclude NONE.
     */
    fun bonesToJSON(): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        var names = mutableListOf<String>()
        for (bone in linkForBone.keys) {
            names.add(bone.name)
        }
        return gson.toJson(names)
    }
    /**
     * @return  a comma-separated string of the names of all extremities.
     */
    fun endEffectorNames(): String {
        val names = StringBuffer()
        for (appendage in linkForAppendage.keys) {
            names.append(appendage.name.lowercase())
            names.append(", ")
        }
        if( names.isNotEmpty() ) return names.substring(0, names.length - 2)
        else return "none"
    }
    /**
     * @return  a JSON pretty-printed String array of all appendages.
     */
    fun endEffectorNamesToJSON(): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val names = mutableListOf<String>()
        for (appendage in linkForAppendage.keys) {
            names.add(appendage.name)
        }
        return gson.toJson(names)
    }
    /**
     * @return  the joint associated with the
     *          source of the bone
     */
    fun jointForBone(bone:Bone): Joint {
        var joint = Joint.NONE
        val link = linkForBone[bone]
        if( link!=null) {
            joint = link.sourcePin.joint
        }
        return joint
    }
    /**
     * @return  a comma-separated string of the names of all joints.
     */
    fun jointNames(): String {
        var names = StringBuffer()
        for (joint in revoluteLinkForJoint.keys) {
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
        for (joint in revoluteLinkForJoint.keys) {
            names.add(joint.name)
        }
        return gson.toJson(names)
    }
    // ============================================= Helper Methods ==============================================
    private fun doubleArrayFromString(text: String): DoubleArray {
        val result = DoubleArray(3)
        val raw = text.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (i in raw.indices) {
            try {
                result[i] = raw[i].toDouble()
            }
            catch (nfe: NumberFormatException) {
                LOGGER.warning(String.format("%s.doubleArrayFromString: Error parsing %s raw(%d)=%s (%s)",
                        CLSS,text,i,raw[i],nfe.localizedMessage ) )
            }
        }
        if(DEBUG) LOGGER.info(String.format("doubleArrayFromString: text %s = %.2f,%.2f,%.2f",text,result[0],result[1],result[2]))
        return result
    }

    /**
     * The input array is a unit vector indicating direction.
     * @param text
     * @return
     */
    private fun doubleArrayFromDirectionString(text: String): DoubleArray {
        val result = DoubleArray(3)
        val raw = text.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (i in raw.indices) {
            try {
                result[i] = 180.0 * raw[1].toDouble()
            }
            catch (nfe: NumberFormatException) {
                LOGGER.warning(String.format("%s.doubleArrayFromString: Error parsing %s (%s);",
                        CLSS,text,nfe.localizedMessage) )
            }
        }
        return result
    }
    // Recursively walk from the root to all extremities
    // This is a test.



    private val CLSS = "URDFModel"
    private val DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        DEBUG= RobotModel.debug.contains(ConfigurationConstants.DEBUG_CONFIGURATION)
        document = null
        linkForAppendage = mutableMapOf<Appendage, Link>()
        linkForBone      = mutableMapOf<Bone,Link>()
        revoluteLinkForJoint = mutableMapOf<Joint,Link>()
        linkForSourceJoint = mutableMapOf<Joint,Link>()
        sourceJointForLink = mutableMapOf<Link,Joint>()
        revoluteForJoint = mutableMapOf<Joint,LinkPin>()
        origin = LinkPin(PinType.ORIGIN)
    }
}