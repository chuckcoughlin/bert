/**
 * Copyright 2022-2026. Charles Coughlin. All Rights Reserved.
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
    private var document: Document?
    private val tree: JointTree

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
            var origin = JointPosition()
            origin.name = Joint.IMU.name
            origin.parent = ConfigurationConstants.NO_ID
            // ================================== IMU ===============================================
            val imus = document!!.getElementsByTagName("imu")
            if (imus.length > 0) {
                LOGGER.info(String.format("%s.analyzeChain: IMU ...",CLSS))
                val imuNode = imus.item(0) // Should only be one
                val xyz = doubleArrayFromString(XMLUtility.attributeValue(imuNode, "xyz"))
                origin.setCoordinates(xyz[0],xyz[1],xyz[2])
            }
            tree.setOrigin(origin)

            // ================================== Links ===============================================
            // Links are a connection between joints or from a joint to extremity (appendage). The link
            // that has no source is the IMU, the root if the tree.
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
                try {
                    // The source is shared with all sub-links
                    var parent = origin
                    val children = linkNode.childNodes
                    val acount = children.length
                    var aindex = 0
                    while (aindex < acount) {
                        val node = children.item(aindex)
                        // Same source must be applied to all joints/appendages in same link element
                        if ("source".equals(node.localName)) {
                            val jname: String = XMLUtility.attributeValue(node, "joint")
                            parent = tree.getJointPositionByName(jname)
                        }
                        aindex++
                    }

                    aindex = 0
                    while (aindex < acount) {
                        val node = children.item(aindex)
                        if ("appendage".equals(node.localName) || "joint".equals(node.localName)) {
                            val aname: String = XMLUtility.attributeValue(node, "name")
                            val jp = tree.getJointPositionByName(aname)
                            var home = 0.0
                            if("appendage".equals(node.localName) ) {
                                jp.isAppendage = true
                            }
                            else  {
                                jp.isAppendage = false
                                home = XMLUtility.attributeValue(node, "home").toDouble()
                            }
                            jp.side = XMLUtility.attributeValue(linkNode, "side")
                            jp.parent = parent.id
                            var jlink = tree.createJointLink(parent,jp)
                            val rpy = doubleArrayFromString(XMLUtility.attributeValue(node, "rpy"))
                            jlink.setRpy(rpy[0],rpy[1],rpy[2])
                            val xyz = doubleArrayFromString(XMLUtility.attributeValue(node, "xyz"))
                            jlink.setDimensions(xyz[0],xyz[1],xyz[2])
                            jlink.sourceJoint.home = home
                        }
                        aindex++
                    }
                }
                catch (iae: IllegalArgumentException) {
                    LOGGER.warning(String.format("%s.analyzeChain: exception on link %d, ignored (%s)",
                        CLSS,index,iae.localizedMessage))
                    iae.printStackTrace()
                }
                index++
            }
        }
    }

    /**
     * Create a JointTree from the existing structure.
     * Note that all joint positions are (0,0,0).
     */
    fun createJointTree() : JointTree {
        val jtree = tree.clone()
        return jtree
    }



    /**
     * @return  a comma-separated string of the names of all extremities.
     */
    fun endEffectorNames(): String {
        val names = StringBuffer()
        for (jp in tree.listJointPositions()) {
            if(jp.isAppendage) {
                names.append(jp.name.lowercase())
                names.append(", ")
            }
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
        for (jp in tree.listJointPositions()) {
            if(jp.isAppendage) {
                names.add(jp.name)
            }
        }
        return gson.toJson(names)
    }

    /**
     * @return  a JSON pretty-printed String array of all property types. Exclude NONE.
     */
    fun jointsToJSON(): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        var names = mutableListOf<String>()
        for (jp in tree.listJointPositions()) {
            if(!jp.isAppendage) {
                names.add(jp.name)
            }
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
        tree = JointTree()
    }
}