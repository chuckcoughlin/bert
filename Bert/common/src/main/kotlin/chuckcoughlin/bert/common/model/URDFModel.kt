/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Axis
import chuckcoughlin.bert.common.util.XMLUtility
import com.google.gson.GsonBuilder
import org.w3c.dom.Document
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Logger

/**
 * Parse the URDF file and make its contents available as a chain of links.
 * A chain has a tree structure with a single root.
 */
object URDFModel {
    val origin: LinkPin
    var document: Document?
    val linkForAppendage: MutableMap<Appendage, Link>
    val linkForJoint: MutableMap<Joint, Link>

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
     * center of gravity. The IMU reacts only to rotation.
     */
    private fun analyzeChain() {
        if (document != null) {
            // ================================== IMU ===============================================
            val imus = document!!.getElementsByTagName("imu")
            if (imus.length > 0) {
                LOGGER.info(String.format("%s.analyzeChain: IMU ...",CLSS))
                val imuNode = imus.item(0) // Should only be one
                IMU.axis = Axis.fromString(XMLUtility.attributeValue(imuNode, "axis"))
            }

            // ================================== Links ===============================================
            // Links are a connection between joints or from a joint to extremity (appendage). The link
            // that has no source is the origin and originates from the IMU.
            //
            // Create all the Links and their LinkPins. A LinkPin encompasses the connecting joint.
            // Create a separate link for each resolute joint and end effector.
            // The URDF file has all angles in degrees. Convert to radians.
            val links = document!!.getElementsByTagName("link")
            val count = links.length
            var index = 0
            while (index < count) {
                val linkNode = links.item(index)
                val name: String = XMLUtility.attributeValue(linkNode, "name")
                if(DEBUG) LOGGER.info(String.format("%s.analyzeChain: link %s ...",CLSS,name))
                try {
                    // The source pin is sharedwith all sub-links
                    var sourcePin = LinkPin(PinType.ORIGIN)
                    val children = linkNode.childNodes
                    val acount = children.length
                    var aindex = 0
                    while (aindex < acount) {
                        val node = children.item(aindex)
                        // Same source must be applied to all joints/appendages in same link element
                        if ("source".equals(node.localName)) {
                            val jname: String = XMLUtility.attributeValue(node, "joint")
                            val joint = Joint.fromString(jname)
                            sourcePin = LinkPin(PinType.REVOLUTE)
                            sourcePin.joint = joint
                        }
                        aindex++
                    }

                    aindex = 0
                    while (aindex < acount) {
                        val node = children.item(aindex)
                        if ("appendage".equals(node.localName)) {
                            val link = Link(name)
                            val aname: String = XMLUtility.attributeValue(node, "name")
                            val appendage: Appendage = Appendage.fromString(aname)
                            val pin = LinkPin(PinType.END_EFFECTOR)
                            pin.appendage = appendage
                            pin.axis = Axis.fromString(XMLUtility.attributeValue(node, "axis"))
                            val home = XMLUtility.attributeValue(node, "home")
                            if( home.isNotBlank() ) pin.home = home.toDouble()*Math.PI/180.0
                            val xyz = doubleArrayFromString(XMLUtility.attributeValue(node, "xyz"))
                            link.coordinates = Point3D(xyz[0],xyz[1],xyz[2])
                            link.endPin = pin
                            link.endPin =pin
                            link.sourcePin = sourcePin
                            linkForAppendage[appendage] = link
                        }
                        else if ("joint".equals(node.localName)) {
                            val link = Link(name)
                            val jname: String = XMLUtility.attributeValue(node, "name")
                            val joint = Joint.fromString(jname)
                            val pin = LinkPin(PinType.REVOLUTE)
                            pin.joint = joint
                            pin.axis = Axis.fromString(XMLUtility.attributeValue(node, "axis"))
                            val home = XMLUtility.attributeValue(node, "home")
                            if( home.isNotBlank() ) pin.home = home.toDouble()*Math.PI/180.0
                            val xyz = doubleArrayFromString(XMLUtility.attributeValue(node, "xyz"))
                            link.coordinates = Point3D(xyz[0],xyz[1],xyz[2])
                            link.endPin =pin
                            link.sourcePin = sourcePin
                            linkForJoint[joint] = link
                        }
                        aindex++
                    }
                }
                catch (iae: IllegalArgumentException) {
                    LOGGER.warning(String.format("%s.analyzeChain: link exception on first pass %s, ignored (%s)",
                        CLSS,name,iae.localizedMessage))
                    iae.printStackTrace()
                }
                index++
            }
        }
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
     * @return  a comma-separated string of the names of all joints.
     */
    fun jointNames(): String {
        var names = StringBuffer()
        for (joint in linkForJoint.keys) {
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
        for (joint in linkForJoint.keys) {
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
        linkForJoint = mutableMapOf<Joint, Link>()
        origin = LinkPin(PinType.ORIGIN)
    }
}