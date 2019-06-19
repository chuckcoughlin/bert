/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.control.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.hipparchus.complex.Quaternion;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import bert.share.control.Appendage;
import bert.share.control.Limb;
import bert.share.motor.Joint;
import bert.share.xml.XMLUtility;

/**
 *  Parse the URDF file and make its contents available as a chain of links.
 *  A chain has a tree structure with a single root.
 */
public class URDFModel  {
	private static final String CLSS = "URDFModel";
	protected Document document;
	private final Chain chain;
	private final Map<Limb,Link> linksByLimb;
	private final Map<Limb,Revolute> revolutesByChild;
	private static final Logger LOGGER = Logger.getLogger(CLSS);

	
	public URDFModel() {
		this.chain = new Chain();
		this.linksByLimb = new HashMap<>();
		this.revolutesByChild = new HashMap<>();
		this.document = null;
	}
    


	/**
	 * @return the tree of links which describes the robot.
	 */
	public Chain getChain() { return this.chain; }

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
				analyzeChain();
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
	private void analyzeChain() {
		if( this.document!=null ) {
			NodeList links = document.getElementsByTagName("link");
			int count = links.getLength();
			int index = 0;
			while(index<count) {
				Node linkNode = links.item(index);
				String name = XMLUtility.attributeValue(linkNode, "name");
				try {
					Limb limb = Limb.valueOf(name);
					Link link = new Link(limb);
					linksByLimb.put(limb, link);
					LOGGER.fine(String.format("%s.analyzeChain: Found link %s",CLSS,name));
					chain.addElement(link);
					
					NodeList appendages = linkNode.getChildNodes();
					int acount = appendages.getLength();
					int aindex=0;
					while(aindex<acount) {
						Node node = appendages.item(aindex);
						if( node.getLocalName().equalsIgnoreCase("appendage")) {
							String aname = XMLUtility.attributeValue(linkNode, "name");
							String text = XMLUtility.attributeValue(linkNode, "xyz");
							if( text!=null ) {
								double[] xyz = doubleArrayFromString(text);
								Appendage a = Appendage.valueOf(aname.toUpperCase());
								QHolder q = new QHolder(xyz,null);  // No rotation
								link.addAppendage(a, q);
							}
						}
						aindex++;
					}

					
				}
				catch(IllegalArgumentException iae) {
					LOGGER.warning(String.format("%s.analyzeChain: link element has unknown name (%s), ignored",CLSS,name));
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
					Limb parent = null;
					Limb child = null;
					// It is required that the Revolute have a parent and a child
					while( nodeIndex<nodeCount ) {
						Node childNode = nodes.item(nodeIndex);
						if( "parent".equals(childNode.getLocalName()) ) {
							String p = XMLUtility.attributeValue(childNode, "link");
							if( p!=null ) {
								try {
									parent = Limb.valueOf(p);
								}
								catch(IllegalArgumentException iae) {
									LOGGER.warning(String.format("%s.analyzeChain: parent of %s has unknown name (%s), ignored",CLSS,joint.name(),p));
								}
							}
							else {
								LOGGER.warning(String.format("%s.analyzeChain: joint %s has no parent, ignored",CLSS,joint.name()));
							}
						}
						else if( "child".equals(childNode.getLocalName()) ) {
							String c = XMLUtility.attributeValue(childNode, "link");
							if( c!=null ) {
								try {
									child = Limb.valueOf(c);
								}
								catch(IllegalArgumentException iae) {
									LOGGER.warning(String.format("%s.analyzeChain: child of %s has unknown name (%s), ignored",CLSS,joint.name(),c));
								}
							}
							else {
								LOGGER.warning(String.format("%s.analyzeChain: joint %s has no child, ignored",CLSS,joint.name()));
							}
						}
						nodeIndex++;
					}
					
					if( parent!=null && child!=null ) {  // If both null, we've logged the errors
						Revolute rev = new Revolute(joint,parent,child);
						revolutesByChild.put(child, rev);
						LOGGER.fine(String.format("%s.analyzeChains: Found revolute %s",CLSS,rev.getName()));
					}
				}
				catch(IllegalArgumentException iae) {
					LOGGER.warning(String.format("%s.analyzeChains: link element has unknown name (%s), ignored",CLSS,name));
				}
				index++;
			}
			
			// Search for origin. Origin is link where no other link has it as a child.
			for(Revolute rev:revolutesByChild.values() ) {
				Limb parent = rev.getParent();
				if( revolutesByChild.get(parent)==null ) {
					Link root = linksByLimb.get(parent);
					root.setOrigin(Quaternion.ZERO);
					chain.setRoot(root);
					break;
				}

			}
		}
	}
	
	// ============================================= Helper Methods ==============================================
	private double[] doubleArrayFromString(String text) {
		double [] result = new double[3];
		String[] raw = text.split(" ");
		for(int i=0;i<raw.length;i++) {
			try {
				result[i] = Double.parseDouble(raw[1]);
			}
			catch(NumberFormatException nfe) {
				LOGGER.warning(String.format("%s.doubleArrayFromString: Error parsing %s (%s);",CLSS,text,nfe.getLocalizedMessage()));
			}
		}
		return result;
	}
}


