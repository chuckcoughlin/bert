/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.control.urdf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import bert.control.model.Chain;
import bert.control.model.Limb;
import bert.control.model.Link;
import bert.control.model.Revolute;
import bert.share.motor.Joint;
import bert.share.xml.XMLUtility;

/**
 *  Parse the URDF file and make its contents available as chains of links.
 */
public class URDFModel  {
	private static final String CLSS = "URDFModel";
	protected Document document;
	private final Map<String,Chain> chains;
	private final List<Revolute> ends;
	private final Map<Link,Limb> limbsByLink;
	private final Map<Link,Revolute> revolutesByChild;
	private static final Logger LOGGER = Logger.getLogger(CLSS);

	
	public URDFModel() {
		this.chains = new HashMap<>();
		this.ends   = new ArrayList<>();
		this.limbsByLink = new HashMap<>();
		this.revolutesByChild = new HashMap<>();
		this.document = null;
	}
    


	/**
	 * @return a map of type names for each message handler used by this application.
	 */
	public Map<String,Chain> getChains() { return this.chains; }

    /**
	 * Expand the supplied path as the URDF XML file.
	 * @return the geometry, an XML document.
	 */
	public void analyzePath(Path filePath) {
		LOGGER.info(String.format("%s.analyzePath: URDF file(%s)",CLSS,filePath.toAbsolutePath().toString()));
		try {
			byte[] bytes = Files.readAllBytes(filePath);
			if( bytes!=null ) {
				this.document = XMLUtility.documentFromBytes(bytes);
				analyzeChains();
			}
		}
		catch( IOException ioe) {
			LOGGER.severe(String.format("%s.analyzePath: Failed to read file %s (%s)",
											CLSS,filePath.toAbsolutePath().toString(),ioe.getLocalizedMessage()));
		}
	}
	

    // ================================ Auxiliary Methods  ===============================
	

	
	/**
	 * Search the model for link and joint elements. 
	 */
	private void analyzeChains() {
		if( this.document!=null ) {
			NodeList links = document.getElementsByTagName("link");
			int count = links.getLength();
			int index = 0;
			while(index<count) {
				Node linkNode = links.item(index);
				String name = XMLUtility.attributeValue(linkNode, "name");
				try {
					Link link = Link.valueOf(name);
					Limb limb = new Limb(link);
					limbsByLink.put(link, limb);
					LOGGER.fine(String.format("%s.analyzeChains: Found link %s",CLSS,limb.getName()));
				}
				catch(IllegalArgumentException iae) {
					LOGGER.warning(String.format("%s.analyzeChains: link element has unknown name (%s), ignored",CLSS,name));
				}
				index++;
			}
			
			NodeList joints = document.getElementsByTagName("joint");
			count = joints.getLength();
			index = 0;
			while(index<count) {
				Node jointNode = joints.item(index);
				String name = XMLUtility.attributeValue(jointNode, "name");
				try {
					Joint joint = Joint.valueOf(name);
					NodeList nodes = jointNode.getChildNodes();
					int nodeCount = nodes.getLength();
					int nodeIndex = 0;
					Link parent = null;
					Link child = null;
					// We expect at most one child and one parent
					while( nodeIndex<nodeCount ) {
						Node childNode = nodes.item(nodeIndex);
						if( "parent".equals(childNode.getLocalName()) ) {
							String p = XMLUtility.attributeValue(childNode, "link");
							if( p!=null ) {
								try {
									parent = Link.valueOf(p);
								}
								catch(IllegalArgumentException iae) {
									LOGGER.warning(String.format("%s.analyzeChains: parent of %s has unknown name (%s), ignored",CLSS,joint.name(),p));
								}
							}
						}
						else if( "child".equals(childNode.getLocalName()) ) {
							String c = XMLUtility.attributeValue(childNode, "link");
							if( c!=null ) {
								try {
									child = Link.valueOf(c);
								}
								catch(IllegalArgumentException iae) {
									LOGGER.warning(String.format("%s.analyzeChains: child of %s has unknown name (%s), ignored",CLSS,joint.name(),c));
								}
							}
						}
						nodeIndex++;
					}
					
					if( parent!=null || child!=null ) {  // If both null, we've logged the errors
						Revolute rev = new Revolute(joint,parent,child);
						if( parent == null ) {
							Chain chain = new Chain(child.name());
							chain.addElement(new Limb(child));
							LOGGER.info(String.format("%s.analyzeChains: New chain(%s)",CLSS,child.name()));
							ends.add(rev);
						}
						else if(child!=null) {
							revolutesByChild.put(child, rev);
						}
						LOGGER.fine(String.format("%s.analyzeChains: Found revolute %s",CLSS,rev.getName()));
					}
				}
				catch(IllegalArgumentException iae) {
					LOGGER.warning(String.format("%s.analyzeChains: link element has unknown name (%s), ignored",CLSS,name));
				}
				index++;
			}
			
			// Now complete the chains. When complete chain will begin at
			// the origin and terminate with the end effector.
			for(Revolute rev:ends ) {
				Chain chain = chains.get(rev.getName());
				Link parent = rev.getParent();
				while( parent!=null ) {
					chain.addElement(new Limb(parent));
					rev = revolutesByChild.get(rev.getChild());
					parent = rev.getParent();
				}
			}
		}
	}
}


