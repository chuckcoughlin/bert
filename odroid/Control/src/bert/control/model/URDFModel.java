/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.control.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	protected Document document;
	private final Chain chain;
	private final Map<Limb,LinkPoint> revolutesByChild;
	

	public URDFModel() {
		this.chain = new Chain();
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
			// ================================== IMU ===============================================
			NodeList imus = document.getElementsByTagName("imu");
			if( imus.getLength()>0 ) {  
				//LOGGER.info(String.format("%s.analyzeChain: IMU ...",CLSS));
				Node imuNode = imus.item(0); // Should only be one
				NodeList childNodes = imuNode.getChildNodes();
				int childCount = childNodes.getLength();
				int childIndex=0;
				String text = null;
				while(childIndex<childCount) {
					Node cNode = childNodes.item(childIndex);
					if( "origin".equalsIgnoreCase(cNode.getLocalName())) {
						text  = XMLUtility.attributeValue(cNode, "xyz");
						double[] xyz = doubleArrayFromString(text);
						chain.setOrigin(xyz);
					}
					else if( "axis".equalsIgnoreCase(cNode.getLocalName())) {
						text = XMLUtility.attributeValue(cNode, "xyz");
						double[] xyz = doubleArrayFromString(text);
						chain.setAxes(xyz);
					}
					childIndex++;
				}
			}
			
			// ================================== Links ===============================================
			NodeList links = document.getElementsByTagName("link");
			int count = links.getLength();
			int index = 0;
			while(index<count) {
				Node linkNode = links.item(index);
				String name = XMLUtility.attributeValue(linkNode, "name");
				LOGGER.info(String.format("%s.analyzeChain: Link %s ...",CLSS,name));
				try {
					chain.createLink(name.toUpperCase());

					NodeList appendages = linkNode.getChildNodes();
					int acount = appendages.getLength();
					int aindex=0;
					while(aindex<acount) {
						Node node = appendages.item(aindex);
						if( "appendage".equalsIgnoreCase(node.getLocalName())) {
							String aname = XMLUtility.attributeValue(node, "name");

							NodeList childNodes = node.getChildNodes();
							int childCount = childNodes.getLength();
							int childIndex=0;
							double[] xyz = null;
							double[] ijk = null;
							while(childIndex<childCount) {
								Node cNode = childNodes.item(childIndex);
								if( "origin".equalsIgnoreCase(cNode.getLocalName()))   xyz  = doubleArrayFromString(XMLUtility.attributeValue(cNode, "xyz"));
								else if("axis".equalsIgnoreCase(cNode.getLocalName())) ijk  = doubleArrayFromString(XMLUtility.attributeValue(cNode, "xyz"));
								childIndex++;
							}
							
							Appendage a = Appendage.valueOf(aname.toUpperCase());
							chain.createLink(a.name());
							LinkPoint end = new LinkPoint(a,ijk,xyz);
							chain.setEndPoint(a.name(),end);
						}
						aindex++;
					}
				}
				catch(IllegalArgumentException iae) {
					LOGGER.warning(String.format("%s.analyzeChain: link or appendage has unknown name: %s, ignored (%s)",CLSS,name,iae.getLocalizedMessage()));
					iae.printStackTrace();
				}
				index++;
			}
			// ================================== Joints ===============================================
			NodeList joints = document.getElementsByTagName("joint");
			count = joints.getLength();
			index = 0;
			while(index<count) {
				Node jointNode = joints.item(index);
				String name = XMLUtility.attributeValue(jointNode, "name");
				LOGGER.info(String.format("%s.analyzeChain: Joint %s ...",CLSS,name));
				try {
					Joint joint = Joint.valueOf(name);
					NodeList childNodes = jointNode.getChildNodes();
					int childCount = childNodes.getLength();
					int childIndex = 0;
					Limb parent = null;
					Limb child = null;
					double[] xyz = null;
					double[] ijk = null;
					// It is required that the LinkPoint have a parent and a child
					while( childIndex<childCount ) {
						Node childNode = childNodes.item(childIndex);
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
						else if( "origin".equalsIgnoreCase(childNode.getLocalName())) xyz  = doubleArrayFromString(XMLUtility.attributeValue(childNode, "xyz"));
						else if("axis".equalsIgnoreCase(childNode.getLocalName()))    ijk  = doubleArrayFromString(XMLUtility.attributeValue(childNode, "xyz"));
						childIndex++;
					}

					LinkPoint rev = new LinkPoint(joint,ijk,xyz);
					if(parent!=null) {
						Link parentLink = chain.getLinkForLimb(parent);
						parentLink.setEndPoint(rev);
						
						if(child!=null ) {
							Link childLink = chain.getLinkForLimb(child);
							chain.setOriginPoint(childLink,rev);
						}
					}
					revolutesByChild.put(child, rev);
					LOGGER.fine(String.format("%s.analyzeChains: Found revolute %s",CLSS,rev.getName()));
				}
				catch(IllegalArgumentException iae) {
					LOGGER.warning(String.format("%s.analyzeChains: link element has unknown name (%s), ignored",CLSS,name));
				}
				index++;
			}

			// Search for origin aka root. Origin is link where no joint has it as a child.
			for(Link link:chain.getLinks() ) {
				LinkPoint candidate = revolutesByChild.get(link.getName());
				if( candidate==null ) {
					chain.setRoot(link);
					break;
				}
			}
		}
	}
	
	// ============================================= Helper Methods ==============================================
	private double[] doubleArrayFromString(String text) {
		if( text==null ) return null; 
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
