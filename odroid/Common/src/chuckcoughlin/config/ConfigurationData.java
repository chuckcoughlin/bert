/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package chuckcoughlin.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;

import chuckcoughlin.xml.XMLUtility;

/**
 *  This class is the keeper of all knowledge of the robot configuration.
 */
public class ConfigurationData  {
	private Document config = null;
	private final Map<Integer,PipeData> pioeMap = new HashMap<>();
	private static final String CLSS = "XMLUtility";
	private static final System.Logger LOGGER = System.getLogger(CLSS);
	private static final System.Logger.Level level = System.Logger.Level.WARNING;
    
    /**
	 * Expand the supplied path as the configuration XML file.
	 * @return the configuration, an XML document.
	 */
	public Document getConfiguration(Path filePath) {
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

    // ================================ Helper Methods  ===============================

}

