/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.model.URDFModel.document
import chuckcoughlin.bert.common.model.URDFModel.linkForBone
import chuckcoughlin.bert.common.model.URDFModel.linkForExtremity
import chuckcoughlin.bert.common.model.URDFModel.linkForJoint
import chuckcoughlin.bert.common.model.URDFModel.linkForSourcePin
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
    var document: Document?
    val linkForSourcePin : MutableMap<LinkPin,Link>
    val linkForBone: MutableMap<Bone, Link>
    val linkForExtremity: MutableMap<Extremity, Link>
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
     * center of gravity.
     */
    private fun analyzeChain() {
        if (document != null) {
            // ================================== IMU ===============================================
            val origin = LinkPin()
            val imus = document!!.getElementsByTagName("imu")
            if (imus.length > 0) {
                LOGGER.info(String.format("%s.analyzeChain: IMU ...",CLSS));
                val imuNode = imus.item(0) // Should only be one
                val childNodes = imuNode.childNodes
                val childCount = childNodes.length
                var childIndex = 0
                var text: String?
                while (childIndex < childCount) {
                    val cNode = childNodes.item(childIndex)
                    if ("origin".equals(cNode.localName, ignoreCase = true)) {
                        text = XMLUtility.attributeValue(cNode, "xyz")
                        val xyz = doubleArrayFromString(text)
                        origin.offset = xyz
                    }
                    else if ("axis".equals(cNode.localName, ignoreCase = true)) {
                        text = XMLUtility.attributeValue(cNode, "xyz")
                        val xyz = doubleArrayFromDirectionString(text)
                        origin.orientation = xyz
                    }
                    childIndex++
                }
            }

            // ================================== Links ===============================================
            // Links correspond to Bone datatypes. Links can have extremities and/or joints, plus a
            // source (parent).
            //      ---------------------------- First Pass ----------------------------------
            // Create all the Links and their LinkPins. A LinkPin encompasses the connecting joint
            val links = document!!.getElementsByTagName("link")
            var count = links.length
            var index = 0
            while (index < count) {
                val linkNode = links.item(index)
                val name: String = XMLUtility.attributeValue(linkNode, "name")
                if(DEBUG) LOGGER.info(String.format("%s.analyzeChain: first pass ink %s ...",CLSS,name));
                try {
                    val bone = Bone.fromString(name)
                    if( !bone.equals(Bone.NONE)) {
                        val link = Link(bone)
                        linkForBone[bone] = link
                        val children = linkNode.childNodes
                        val acount = children.length
                        var aindex = 0
                        while (aindex < acount) {
                            val node = children.item(aindex)
                            if ("extremity".equals(node.localName, ignoreCase = true)) {
                                val aname: String = XMLUtility.attributeValue(node, "name")
                                val childNodes = node.childNodes
                                val childCount = childNodes.length
                                var childIndex = 0
                                var xyz = DoubleArray(3, { 0.0 })
                                var ijk = DoubleArray(3, { 0.0 })
                                while (childIndex < childCount) {
                                    val cNode = childNodes.item(childIndex)
                                    if ("origin".equals(cNode.localName, ignoreCase = true)) {
                                        xyz = doubleArrayFromString(
                                            XMLUtility.attributeValue(cNode, "xyz"))
                                    }
                                    else if ("axis".equals(cNode.localName, ignoreCase = true)) {
                                        ijk = doubleArrayFromDirectionString(XMLUtility.attributeValue(cNode, "xyz"))
                                    }
                                    childIndex++
                                }
                                val extremity: Extremity = Extremity.fromString(aname)
                                val end = LinkPin(extremity, ijk, xyz)
                                link.addEndPoint(end)
                            }
                            else if ("joint".equals(node.localName, ignoreCase = true)) {
                                val aname: String = XMLUtility.attributeValue(node, "name")
                                val joint = Joint.fromString(aname)
                                val childNodes = node.childNodes
                                val childCount = childNodes.length
                                var childIndex = 0
                                var xyz: DoubleArray? = null
                                var ijk: DoubleArray? = null
                                while (childIndex < childCount) {
                                    val childNode = childNodes.item(childIndex)
                                    if ("origin".equals(childNode.localName, ignoreCase = true)) {
                                        xyz = doubleArrayFromString(XMLUtility.attributeValue(childNode, "xyz"))
                                    }
                                    else if ("axis".equals(childNode.localName,ignoreCase = true)) {
                                        ijk = doubleArrayFromDirectionString(XMLUtility.attributeValue(childNode, "xyz"))
                                    }
                                    childIndex++
                                }

                                val rev = LinkPin(joint, ijk!!, xyz!!)
                                //if (DEBUG) LOGGER.info(String.format(" %s    xyz   = %.2f,%.2f,%.2f", joint.name,xyz[0],xyz[1],xyz[2]))
                                link.addEndPoint(rev)
                                linkForJoint[joint] = link
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

            //      ---------------------------- Second Pass ----------------------------------
            // Define the parent
            index = 0
            while (index < count) {
                val linkNode = links.item(index)
                val name: String = XMLUtility.attributeValue(linkNode, "name")
                //if(DEBUG) LOGGER.info(String.format("%s.analyzeChain: second pass ink %s ...",CLSS,name));
                try {
                    val bone = Bone.fromString(name)
                    if( !bone.equals(Bone.NONE)) {
                        val link:Link? = linkForBone[bone]
                        val children = linkNode.childNodes
                        val acount = children.length
                        var aindex = 0
                        while (aindex < acount) {
                            val node = children.item(aindex)
                            // The only node we care about is the parent link
                            if ("source".equals(node.localName, ignoreCase = true)) {
                                val jname: String = XMLUtility.attributeValue(node, "joint")
                                val joint = Joint.fromString(jname)
                                val parent = linkForJoint[joint]
                                if( parent!=null) {
                                    val parentPin=parent.sourcePin
                                    linkForSourcePin[parentPin] = link!!
                                }
                            }
                            aindex++
                        }
                    }
                    else {
                        LOGGER.warning(String.format("%s.analyzeChain: second pass link refers to an unknown bone: %s, ignored",CLSS,name))
                    }
                }
                catch (iae: IllegalArgumentException) {
                    LOGGER.warning(String.format("%s.analyzeChain: link exception on second pass %s, ignored (%s)",
                                CLSS,name,iae.localizedMessage))
                    iae.printStackTrace()
                }
                index++
            }

            // Search for origin aka root.
            val linkWalker: Iterator<Link?> = linkForBone.values.iterator()
            if (linkWalker.hasNext()) {
                while(linkWalker.hasNext()) {
                    val link = linkWalker.next()
                    if( link!!.sourcePin.type.equals(PinType.ORIGIN)) {
                        link.sourcePin = origin
                        Chain.root = link
                        break
                    }
                }
            }
            else {
                LOGGER.warning(String.format("%s.analyzeChains: chain has no links",CLSS))
            }

        }
    }


    /**
     * @return  a comma-separated string of the names of all links.
     */
    fun boneNames(): String {
        var names = StringBuffer()
        for (bone in linkForBone.keys) {
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
        for (bone in linkForBone.keys) {
            names.add(bone.name)
        }
        return gson.toJson(names)
    }
    /**
     * @return  a comma-separated string of the names of all extremities.
     */
    fun extremityNames(): String {
        var names = StringBuffer()
        for (extremity in linkForExtremity.keys) {
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
        for (extremity in linkForExtremity.keys) {
            names.add(extremity.name)
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
                LOGGER.warning(String.format("%s.doubleArrayFromString: Error parsing %s (%s);",
                        CLSS,text,nfe.localizedMessage ) )
            }
        }
        //if(DEBUG) LOGGER.info(String.format("doubleArrayFromString: text %s = %s,%s,%s",text,raw[0],raw[1],raw[2]));
        if(DEBUG) LOGGER.info(String.format("doubleArrayFromString: text %s = %.2f,%.2f,%.2f",text,result[0],result[1],result[2]));
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



    private val CLSS = "URDFModel"
    private val DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        DEBUG= RobotModel.debug.contains(ConfigurationConstants.DEBUG_CONFIGURATION)
        document = null
        linkForSourcePin = mutableMapOf<LinkPin,Link>()
        linkForExtremity = mutableMapOf<Extremity, Link>()
        linkForBone      = mutableMapOf<Bone,Link>()
        linkForJoint     = HashMap<Joint,Link>()
    }
}