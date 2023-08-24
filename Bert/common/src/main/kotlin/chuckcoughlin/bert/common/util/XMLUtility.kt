/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.util

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.SAXException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.logging.Logger
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/**
 * A class with static utility methods dealing with XML files. These methods
 * are typically designed to return an error string, where a null implies success.
 */
object XMLUtility {
    private const val CLSS = "XMLUtility"
    private val LOGGER = Logger.getLogger(CLSS)
    fun documentFromBytes(bytes: ByteArray): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        var xml = builder.newDocument()
        try {
            xml = builder.parse(ByteArrayInputStream(bytes))
        }
        catch (pce: ParserConfigurationException) {
            LOGGER.warning(String.format("%s.documentFromBytes: Failed to create builder (%s)",
                    CLSS,pce.localizedMessage))
        }
        catch (saxe: SAXException) {
            LOGGER.warning(String.format("%s.documentFromBytes: Illegal XML document (%s)",
                    CLSS,saxe.localizedMessage))
        }
        catch (ioe: IOException) {
            LOGGER.warning(String.format("%s.documentFromBytes: IOException parsing XML (%s)",
                    CLSS,ioe.localizedMessage))
        }
        return xml
    }

    // =========================  Helper Functions ==============================
    fun attributeValue(element: Node, name: String): String {
        var value:String  = ""
        val attributes = element.attributes
        val node = attributes.getNamedItem(name)
        if (node != null) {
            if( node.nodeValue!=null) {
                value = node.nodeValue
            }
        }
        return value
    }
}