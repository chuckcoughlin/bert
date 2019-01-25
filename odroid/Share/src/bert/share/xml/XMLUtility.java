/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * A class with static utility methods dealing with XML files. These methods 
 * are typically designed to return an error string, where a null implies success.
 */
public class XMLUtility {
	private static final String CLSS = "XMLUtility";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	
	
	public static Document documentFromBytes(byte[] bytes) {
		Document xml = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    factory.setNamespaceAware(true);
	    try {
	    	DocumentBuilder builder = factory.newDocumentBuilder();
	    	xml = builder.parse(new ByteArrayInputStream(bytes));
	    }
	    catch(ParserConfigurationException pce) {
	    	LOGGER.warning(String.format("%s.documentFromBytes: Failed to create builder (%s)",CLSS,pce.getLocalizedMessage()));
	    }
	    catch(SAXException saxe) {
	    	LOGGER.warning(String.format("%s.documentFromBytes: Illegal XML document (%s)",CLSS,saxe.getLocalizedMessage()));
	    }
	    catch(IOException ioe) {
	    	LOGGER.warning(String.format("%s.documentFromBytes: IOException parsing XML (%s)",CLSS,ioe.getLocalizedMessage()));
	    }
	    
	    return xml;
	}
	
	// =========================  Helper Functions ==============================
	public static String attributeValue(Node element,String name) {
		String value = "";
		NamedNodeMap attributes = element.getAttributes();
		Node node = attributes.getNamedItem(name);
		if( node!=null ) value = node.getNodeValue();
		return value;
	}

}