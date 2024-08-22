/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.util.XMLUtility
import com.google.gson.GsonBuilder
import org.w3c.dom.Document
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.logging.Logger

/**
 * Parse the URDF file and make its contents available as a chain of links.
 * A chain has a tree structure with a single root.
 */
object URDFModel {
    var document: Document?

    /**
     * @return the tree of links which describes the robot.
     */
    val chain: Chain

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
     * Search the model for link and joint elements.
     */
    private fun analyzeChain() {
        if (document != null) {
            // ================================== IMU ===============================================
            val imus = document!!.getElementsByTagName("imu")
            if (imus.length > 0) {
                //LOGGER.info(String.format("%s.analyzeChain: IMU ...",CLSS));
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
                        chain.setOrigin(xyz)
                    }
                    else if ("axis".equals(cNode.localName, ignoreCase = true)) {
                        text = XMLUtility.attributeValue(cNode, "xyz")
                        val xyz = doubleArrayFromDirectionString(text)
                        chain.setAxes(xyz)
                    }
                    childIndex++
                }
            }

            // ================================== Links ===============================================
            val links = document!!.getElementsByTagName("link")
            var count = links.length
            var index = 0
            while (index < count) {
                val linkNode = links.item(index)
                val name: String = XMLUtility.attributeValue(linkNode, "name")
                //LOGGER.info(String.format("%s.analyzeChain: Link %s ...",CLSS,name));
                try {
                    chain.createLink(name.uppercase(Locale.getDefault()))
                    val appendages = linkNode.childNodes
                    val acount = appendages.length
                    var aindex = 0
                    while (aindex < acount) {
                        val node = appendages.item(aindex)
                        if ("appendage".equals(node.localName, ignoreCase = true)) {
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
                            val a: Appendage = Appendage.fromString(aname)
                            chain.createLink(a.name)
                            val end = LinkPoint(a, ijk, xyz)
                            chain.setEndPoint(a.name, end)
                            chain.setParent(a.name, name.uppercase(Locale.getDefault()))
                        }
                        aindex++
                    }
                }
                catch (iae: IllegalArgumentException) {
                    LOGGER.warning(String.format("%s.analyzeChain: link or appendage has unknown name: %s, ignored (%s)",
                                    CLSS,name,iae.localizedMessage))
                    iae.printStackTrace()
                }
                index++
            }
            // ================================== Joints ===============================================
            val joints = document!!.getElementsByTagName("joint")
            count = joints.length
            index = 0
            while (index < count) {
                val jointNode = joints.item(index)
                val name: String = XMLUtility.attributeValue(jointNode, "name")
                //LOGGER.info(String.format("%s.analyzeChain: Joint %s ...",CLSS,name));
                try {
                    val joint: Joint = Joint.fromString(name)
                    val childNodes = jointNode.childNodes
                    val childCount = childNodes.length
                    var childIndex = 0
                    var parent: String? = null
                    var child: String? = null
                    var xyz: DoubleArray? = null
                    var ijk: DoubleArray? = null
                    // It is required that the LinkPoint have a parent and a child
                    while (childIndex < childCount) {
                        val childNode = childNodes.item(childIndex)
                        if ("parent" == childNode.localName) {
                            parent = XMLUtility.attributeValue(childNode, "link")
                        }
                        else if ("child" == childNode.localName) {
                            child = XMLUtility.attributeValue(childNode, "link")
                        }
                        else if ("origin".equals(childNode.localName, ignoreCase = true)) xyz =
                            doubleArrayFromString(XMLUtility.attributeValue(childNode, "xyz")) else if ("axis".equals(
                                childNode.localName,
                                ignoreCase = true
                            )
                        ) ijk = doubleArrayFromDirectionString(XMLUtility.attributeValue(childNode, "xyz"))
                        childIndex++
                    }
                    val rev = LinkPoint(joint, ijk!!, xyz!!)
                    if(DEBUG) LOGGER.info(String.format(" %s    xyz   = %.2f,%.2f,%.2f",
                            joint.name,xyz[0],xyz[1],xyz[2]))
                    if (parent != null) {
                        val parentLink = chain.getLinkForLimbName(parent)
                        chain.setEndPoint(parentLink!!.name, rev)
                        if (child != null) {
                            val childLink = chain.getLinkForLimbName(child)
                            childLink!!.parent = parentLink
                        }
                        else {
                            LOGGER.warning(String.format("%s.analyzeChain: joint %s has no child",
                                    CLSS,joint.name))
                        }
                    }
                    else {
                        LOGGER.warning(String.format("%s.analyzeChain: joint %s has no parent",
                                CLSS,joint.name))
                    }
                }
                catch (iae: IllegalArgumentException) {
                    LOGGER.warning(String.format("%s.analyzeChains: link element has unknown name (%s), ignored",
                            CLSS,name))
                }
                index++
            }

            // Search for origin aka root. Choose any random link and follow to root.
            val linkWalker: Iterator<Link?> = chain.links.iterator()
            if (linkWalker.hasNext()) {
                var link = linkWalker.next()
                while (link != null) {
                    val parent = link.parent
                    if (parent == null) {
                        chain.root = link
                        break
                    }
                    link = parent
                }
            }
        }
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
        if(DEBUG) LOGGER.info(String.format("doubleArrayFromString: text %s = %s,%s,%s",text,raw[0],raw[1],raw[2]));
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
    private val DEBUG = false
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        chain = Chain()
        document = null
    }
}