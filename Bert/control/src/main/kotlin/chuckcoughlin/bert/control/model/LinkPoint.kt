package chuckcoughlin.bert.control.model

import bert.share.model.Appendage

/**
 * A LinkPoint is a hinged joint (as they all are).
 * The coordinates are a 3D location of the joint
 * with respect to the origin of the link.
 *
 * The orientation array shows the direction of the
 * axis of the joint with respect a line from the joint
 * to the link origin. The offset coordinates are with respect
 * to the link origin. In most cases, the linkPoint
 * is along the z axis.
 */
class LinkPoint {
    var orientation: DoubleArray?
    var offset // Joint offset
            : DoubleArray
        private set
    val type: LinkPointType
    private val appendage: Appendage?
    private val joint: Joint?

    constructor(app: Appendage?, rot: DoubleArray?, pos: DoubleArray) {
        type = LinkPointType.APPENDAGE
        appendage = app
        joint = null
        offset = pos
        orientation = degreesToRadians(rot)
    }

    constructor(j: Joint?, rot: DoubleArray?, pos: DoubleArray) {
        type = LinkPointType.REVOLUTE
        appendage = null
        joint = j
        offset = pos
        orientation = degreesToRadians(rot)
    }

    /**
     * Special constructor for the origin.
     */
    constructor() {
        type = LinkPointType.ORIGIN
        appendage = null
        joint = Joint.UNKNOWN
        offset = doubleArrayOf(0.0, 0.0, 0.0)
        orientation = doubleArrayOf(0.0, 0.0, 0.0)
    }

    val name: String
        get() = joint.name()

    fun getAppendage(): Appendage? {
        return appendage
    }

    fun getJoint(): Joint? {
        return joint
    }

    private fun degreesToRadians(array: DoubleArray?): DoubleArray? {
        if (array != null) {
            var i = 0
            while (i < array.size) {
                array[i] = array[i] * Math.PI / 180.0
                i++
            }
        }
        return array
    }

    companion object {
        private const val CLSS = "LinkPoint"

        /**
         * Create a LinkPoint representing the origin of the link chain.
         * @return the origin
         */
        var origin: LinkPoint? = null
            get() {
                if (field == null) field = LinkPoint()
                return field
            }
            private set
    }
}