package chuckcoughlin.bert.control.model

import bert.share.common.PathConstants
import java.nio.file.Paths
import java.util.logging.Logger

/**
 * A Chain represents a tree of Links starting with the
 * "root" link. The position of links within the chain are
 * all relative to the root link (i.e. origin). The URDF
 * file format doesn't define things in the most convenient
 * order.
 *
 * Changes to the "inertial frame" as detected by the IMU
 * are all handled here.
 */
class Chain {
    var root: Link? = null
    private val linkByAppendage: MutableMap<Appendage?, Link>
    private val jointParent: MutableMap<Joint?, Link>
    private val linksByLimbName: MutableMap<String?, Link?>
    private var origin: DoubleArray? = doubleArrayOf(0.0, 0.0, 0.0)
    private var axis: DoubleArray? = doubleArrayOf(0.0, 0.0, 0.0)

    init {
        jointParent = HashMap<Joint?, Link>()
        linkByAppendage = HashMap<Appendage?, Link>()
        linksByLimbName = HashMap()
    }

    /**
     * As we add origin and endpoints, the new link gets added to the various
     * maps that allow us to navigate the chain.
     * @param name the new link or limb name.
     */
    fun createLink(name: String) {
        val link = Link(name.uppercase(Locale.getDefault()))
        linksByLimbName[link.name] = link
    }

    fun setEndPoint(name: String?, lp: LinkPoint) {
        val link = linksByLimbName[name]
        if (link != null) {
            if (lp.type == LinkPointType.APPENDAGE) {
                linkByAppendage[lp.appendage] = link
            } else if (lp.type == LinkPointType.REVOLUTE) {
                val j: Joint? = lp.joint
                //LOGGER.info(String.format("Chain.setEndPoint: add joint %s", j.name()));
                jointParent[j] = link
            }
            link.setEndPoint(lp)
        } else {
            LOGGER.warning(String.format("%s.setEndPoint: No link %s found", CLSS, name))
        }
    }

    val links: Collection<Link?>
        get() = linksByLimbName.values

    fun getLinkForLimbName(name: String): Link? {
        return linksByLimbName[name.uppercase(Locale.getDefault())]
    }

    /**
     * There may be multiple joints with the same parent, but only one parent per joint.
     * @param jointName
     * @return the parent link of the named joint. If not found, return null.
     */
    fun getParentLinkForJoint(jointName: String): Link? {
        val joint: Joint = Joint.valueOf(jointName.uppercase(Locale.getDefault()))
        return jointParent[joint]
    }

    /**
     * Work back toward the root from the specified appendage. The chain
     * is ordered to start from the root.
     * @param appendage
     * @return
     */
    fun partialChainToAppendage(appendage: Appendage?): List<Link> {
        val partial: LinkedList<Link> = LinkedList<Link>()
        var link = linkByAppendage[appendage]
        while (link != null) {
            partial.addFirst(link)
            link = link.parent
        }
        return partial
    }

    /**
     * Work back toward the root link beginning with the indicated joint.
     * The chain starts with the root.
     * @param joint, the source
     * @return
     */
    fun partialChainToJoint(joint: Joint?): List<Link> {
        val partial: LinkedList<Link> = LinkedList<Link>()
        var link = jointParent[joint]
        while (link != null) {
            partial.addFirst(link)
            link = link.parent
        }
        return partial
    }

    /**
     * The axes are the Euler angles in three dimensions between the robot and the reference frame.
     * @param a three dimensional array of rotational offsets between the robot and reference frame.
     */
    fun setAxes(a: DoubleArray?) {
        axis = a
    }

    /**
     * The origin is the offset of the IMU with respect to the origin of the robot.
     * @param o three dimensional array of offsets to the origin of the chain
     */
    fun setOrigin(o: DoubleArray?) {
        origin = o
    }

    /**
     * Connect the child link to its parent.
     * @param childName
     * @param parentName
     */
    fun setParent(childName: String?, parentName: String?) {
        val parent = linksByLimbName[parentName]
        val child = linksByLimbName[childName]
        if (parent != null && child != null) child.parent = parent
    }

    companion object {
        private const val CLSS = "Chain"
        private val LOGGER = Logger.getLogger(CLSS)

        /**
         * Test construction of the chain of robot "limbs" based on the URDF file in
         * $BERT_HOME/etc on the development machine.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            // Analyze command-line argument to obtain the robot root directory.
            val arg = args[0]
            val path = Paths.get(arg)
            PathConstants.setHome(path)
            // Setup logging to use only a file appender to our logging directory
            val LOG_ROOT = CLSS.lowercase(Locale.getDefault())
            LoggerUtility.getInstance().configureTestLogger(LOG_ROOT)
            // Analyze the xml for motor configurations. Initialize the motor configurations.
            val model = TestRobotModel(PathConstants.CONFIG_PATH)
            model.populate() //
            val solver = Solver()
            solver.configure(model.getMotors(), PathConstants.URDF_PATH)
            val chain: Chain = solver.getModel().getChain()
            val root = chain.root
            println(String.format("%s: root = %s ", CLSS, root.getName()))
            // Test the links to some appendages
            println("=========================================================================")
            var subchain = chain.partialChainToAppendage(Appendage.LEFT_EAR)
            for (link in subchain) {
                println(String.format("\t%s ", link.name))
            }
            println("=========================================================================")
            subchain = chain.partialChainToAppendage(Appendage.RIGHT_FINGER)
            for (link in subchain) {
                println(String.format("\t%s ", link.name))
            }
            println("=========================================================================")
            subchain = chain.partialChainToAppendage(Appendage.RIGHT_TOE)
            for (link in subchain) {
                println(String.format("\t%s ", link.name))
            }
            println("=========================================================================")
            subchain = chain.partialChainToJoint(Joint.ABS_Y)
            for (link in subchain) {
                println(String.format("\t%s ", link.name))
            }
        }
    }
}