/**
 *   (c) 2016  ILS Automation. All rights reserved. 
 */
package chuckcoughlin.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;

/**
 *  This class is the keeper of all knowledge of the robot configuration.
 */
public class ConfigurationData  {
	private Document config = null;
	private final Map<Integer,PipeData> panelMap = new HashMap<>();
    
    /**
	 * Expand the supplied path as the configuration XML file.
	 * @return the configuration, an XML document.
	 */
	public Document getConfiguration(Path filePath) {
		Document contents = null;
		byte[] bytes = Files.readAllBytes(filePath);
		if( bytes!=null ) {
			contents = xmlUtil.documentFromBytes(bytes);
		}
		return contents;
	}

    // ================================ Helper Methods  ===============================

}

