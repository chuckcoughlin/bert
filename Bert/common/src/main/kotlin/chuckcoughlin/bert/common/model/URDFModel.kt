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
                val rpy = doubleArrayFromString(XMLUtility.attributeValue(imuNode, "rpy"))
                IMU.setRpy(rpy[0],rpy[1],rpy[2])
            }

            // ================================== Links ===============================================
            // Links are a connection between joints or from a joint to extremity (appendage). The link
            // that has no source is the origin and originates from the IMU.
            //
            // Create all the Links and their LinkPins. A LinkPin encompasses the connecting joint.
            // Create a separate link for each resolute joint and end effector.
            // The URDF file has all angles in degrees. Convert to radians.
            //
            val links = document!!.getElementsByTagName("link")
            val count = links.length
            var index = 0
            while (index < count) {
                val linkNode = links.item(index)
                val name: String = XMLUtility.attributeValue(linkNode, "name")
                if(DEBUG) LOGGER.info(String.format("%s.analyzeChain: link %s ...",CLSS,name))
                try {
                    // The source pin is shared with all sub-links
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
                            val aname: String = XMLUtility.attributeValue(node, "name")
                            val link = Link(aname)
                            val side: String = XMLUtility.attributeValue(node, "side")
                            link.side = Side.fromString(side)
                            val appendage: Appendage = Appendage.fromString(aname)
                            val pin = LinkPin(PinType.END_EFFECTOR)
                            pin.appendage = appendage
                            val rpy = doubleArrayFromString(XMLUtility.attributeValue(node, "rpy"))
                            link.setRpy(rpy[0],rpy[1],rpy[2])
                            val xyz = doubleArrayFromString(XMLUtility.attributeValue(node, "xyz"))
                            link.setCoordinates(xyz[0],xyz[1],xyz[2])
                            link.endPin = pin
                            link.sourcePin = sourcePin
                            linkForAppendage[appendage] = link
                        }
                        else if ("joint".equals(node.localName)) {
                            val jname: String = XMLUtility.attributeValue(node, "name")
                            val link = Link(jname)
                            val side: String = XMLUtility.attributeValue(node, "side")
                            link.side = Side.fromString(side)
                            val joint = Joint.fromString(jname)
                            val pin = LinkPin(PinType.REVOLUTE)
                            pin.joint = joint
                            pin.home = XMLUtility.attributeValue(node, "home").toDouble()
                            val rpy = doubleArrayFromString(XMLUtility.attributeValue(node, "rpy"))
                            link.setRpy(rpy[0],rpy[1],rpy[2])
                            val xyz = doubleArrayFromString(XMLUtility.attributeValue(node, "xyz"))
                            link.setCoordinates(xyz[0],xyz[1],xyz[2])
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
        //if(DEBUG) LOGGER.info(String.format("%s.doubleArrayFromString: text %s = %.2f,%.2f,%.2f",CLSS,text,result[0],result[1],result[2]))
        return result
    }

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