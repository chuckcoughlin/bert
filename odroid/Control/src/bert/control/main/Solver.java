/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.control.main;

import java.nio.file.Path;
import java.util.logging.Logger;

import bert.control.urdf.URDFModel;

/**
 *  This class handles various computations pertaining to the robot,
 *  including: trajectory planning. Note that the same link object
 *  may belong to several chains.
 */
public class Solver {
	private final static String CLSS = "Solver";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private final URDFModel model;

	/**
	 * Constructor:
	 */
	public Solver() {
		this.model = new URDFModel();
	}
	
	/**
	 * Analyze the URDF file for robot geometry.
	 * @param urdfPath
	 */
	public void configure(Path urdfPath) {
		LOGGER.info(String.format("%s.analyzePath: URDF file(%s)",CLSS,urdfPath.toAbsolutePath().toString()));
		model.analyzePath(urdfPath);
	}


}

