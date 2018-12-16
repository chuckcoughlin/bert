/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import bert.share.xml.XMLUtility;

/**
 *  This is the base class for a collection of models that keep basic configuration
 *  information, all reading from the same file. The information retained is specific
 *  to the scope.
 */
public abstract class AbstractRobotModel  {
	private static final String CLSS = "AbstractRobotModel";
	protected final Document document;
	private final Properties properties;

	private static final System.Logger LOGGER = System.getLogger(CLSS);
	private static final System.Logger.Level level = System.Logger.Level.WARNING;

	
	public AbstractRobotModel(Path configPath) {
		this.document = analyzePath(configPath);
		this.properties = new Properties();
	}
    
	/**
	 *  Analyze the document. The information retained is dependent on the context
	 *  (client or server). This must be called before the model is accessed.
	 */
	public abstract void populate();
	public String getProperty(String key,String defaultValue) {
		return this.properties.getProperty(key,defaultValue);
	}
	
    /**
	 * Expand the supplied path as the configuration XML file.
	 * @return the configuration, an XML document.
	 */
	private Document analyzePath(Path filePath) {
		Document contents = null;
		try {
			byte[] bytes = Files.readAllBytes(filePath);
			if( bytes!=null ) {
				contents = XMLUtility.documentFromBytes(bytes);
			}
		}
		catch( IOException ioe) {
			LOGGER.log(level, String.format("%s.getConfiguration: Failed to read file %s (%s)",
											CLSS,filePath.toAbsolutePath().toString(),ioe.getLocalizedMessage()));
		}
		return contents;
	}
	

    // ================================ Auxiliary Methods  ===============================
	/**
	 * Search the model for property elements. The results are saved in the properties element.
	 * Call this if the model has any properties of interest.
	 * @param index
	 * @param model
	 */
	protected void analyzeProperties() {
		if( this.document!=null ) {
			NodeList elements = document.getElementsByTagName("property");
			int count = elements.getLength();
			int index = 0;
			while(index<count) {
				Node propertyNode = elements.item(index);
				String key = XMLUtility.attributeValue(propertyNode, "name");
				if( key==null ) {
					LOGGER.log(level,String.format("%s.analyzeProperties: Missing name attribute in property",CLSS));
				}
				else {
					String value = propertyNode.getTextContent();
					if( value!=null && !value.isBlank() ) {
						properties.put(key.toLowerCase(), value);
					}
				}
				index++;
			}
		}
	}
}


