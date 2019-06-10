package bert.control.model;

import java.util.LinkedList;

/**
 * A Chain represents a linked list of Links starting with the 
 * origin link. The name of the chain is the name of its end effector.
 */
public class Chain {
	private final static String CLSS = "Chain";
	private final LinkedList<Limb> links;
	private final String name;
	
	public Chain(String nam) {
		this.name = nam;
		this.links = new LinkedList<>();
	}
	
	/**
	 * The new link is added to the front. The assumption is that we are
	 * traversing a list by parents, end to the origin.
	 * @param link
	 */
	public void addElement( Limb link) {
		links.addFirst(link);
	}

	public Limb getEnd() { return links.getLast(); }
	public Limb getOrigin() { return links.getFirst(); }
	public LinkedList<Limb> getLinks() { return this.links; }
	public String getName() { return this.name; }
	
	/**
	def forward_kinematics(self, joints, full_kinematics=False):
        """Returns the transformation matrix of the forward kinematics
        Parameters
        ----------
        joints: list
            The list of the positions of each joint. Note : Inactive joints must be in the list.
        full_kinematics: bool
            Return the transformation matrices of each joint
        Returns
        -------
        frame_matrix:
            The transformation matrix
        """
        frame_matrix = np.eye(4)

        if full_kinematics:
            frame_matrixes = []

        if len(self.links) != len(joints):
            raise ValueError("Your joints vector length is {} but you have {} links".format(len(joints), len(self.links)))

        for index, (link, joint_angle) in enumerate(zip(self.links, joints)):
            # Compute iteratively the position
            # NB : Use asarray to avoid old sympy problems
            frame_matrix = np.dot(frame_matrix, np.asarray(link.get_transformation_matrix(joint_angle)))
            if full_kinematics:
                # rotation_axe = np.dot(frame_matrix, link.rotation)
                frame_matrixes.append(frame_matrix)

        # Return the matrix, or matrixes
        if full_kinematics:
            return frame_matrixes
        else:
            return frame_matrix
            		
        def inverse_kinematics(self, target, initial_position=None, **kwargs):
            		        """Computes the inverse kinematic on the specified target
            		        Parameters
            		        ----------
            		        target: numpy.array
            		            The frame target of the inverse kinematic, in meters. It must be 4x4 transformation matrix
            		        initial_position: numpy.array
            		            Optional : the initial position of each joint of the chain. Defaults to 0 for each joint
            		        Returns
            		        -------
            		        The list of the positions of each joint according to the target. Note : Inactive joints are in the list.
            		        """
            		        # Checks on input
            		        target = np.array(target)
            		        if target.shape != (4, 4):
            		            raise ValueError("Your target must be a 4x4 transformation matrix")

            		        if initial_position is None:
            		            initial_position = [0] * len(self.links)

            		return ik.inverse_kinematic_optimization(self, target, starting_nodes_angles=initial_position, **kwargs)
            				
            def from_urdf_file(cls, urdf_file, base_elements=None, last_link_vector=None, base_element_type="link", active_links_mask=None, name="chain"):
            			        """Creates a chain from an URDF file
            			        Parameters
            			        ----------
            			        urdf_file: str
            			            The path of the URDF file
            			        base_elements: list of strings
            			            List of the links beginning the chain
            			        last_link_vector: numpy.array
            			            Optional : The translation vector of the tip.
            			        name: str
            			            The name of the Chain
            			        base_element_type: str
            			        active_links_mask: list[bool]
            			        """
            			        if base_elements is None:
            			            base_elements = ["base_link"]

            			        links = URDF_utils.get_urdf_parameters(urdf_file, base_elements=base_elements, last_link_vector=last_link_vector, base_element_type=base_element_type)
            			        # Add an origin link at the beginning
            			        return cls([link_lib.OriginLink()] + links, active_links_mask=active_links_mask, name=name)
**/

}

