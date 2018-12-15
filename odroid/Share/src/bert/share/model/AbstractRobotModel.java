/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.w3c.dom.Document;

import bert.share.xml.XMLUtility;

/**
 *  This is the base class for a collection of models that keep basic configuration
 *  information, all reading from the same file. The information retained is specific
 *  to the scope.
 */
public abstract class AbstractRobotModel  {
	private static final String CLSS = "AbstractRobotModel";
	protected final Document document;

	private static final System.Logger LOGGER = System.getLogger(CLSS);
	private static final System.Logger.Level level = System.Logger.Level.WARNING;

	
	public AbstractRobotModel(Path configPath) {
		this.document = analyzePath(configPath);
	}
    
	/**
	 *  Analyze the document. The information retained is dependent on the context
	 *  (client or server). This must be called before the model is accessed.
	 */
	public abstract void populate();
	
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
	
}

