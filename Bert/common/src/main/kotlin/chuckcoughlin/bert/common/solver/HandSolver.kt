/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.solver

import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.Appendage
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.JointDynamicProperty
import chuckcoughlin.bert.common.model.Point3D
import chuckcoughlin.bert.common.model.RobotModel
import java.util.logging.Logger

/**
 * This class handles inverse kinematics calculations for end effectors
 * on the right or left hands.
 */
object HandSolver {

    /**
     * Create a series of motor control messages from the given placement message.
     * We move only the shoulder and elbow joints. This applies only to LEFT and
     * RIGHT FINGER end effectors.
     * @param msg the placement requuest message
     * @param list the list to be filled with control messages
     */
    fun place(msg: MessageBottle,list:MutableList<MessageBottle>): String {
        var error = BottleConstants.NO_ERROR
        var shoulder_x = Joint.RIGHT_SHOULDER_X
        var shoulder_y =Joint.RIGHT_SHOULDER_Y
        var shoulder_z = Joint.RIGHT_SHOULDER_Z
        var elbowJoint = Joint.RIGHT_ELBOW_Y
        if( msg.appendage==Appendage.LEFT_FINGER) {
            shoulder_x = Joint.LEFT_SHOULDER_X
            shoulder_y =Joint.LEFT_SHOULDER_Y
            shoulder_z = Joint.LEFT_SHOULDER_Z
            elbowJoint = Joint.LEFT_ELBOW_Y
        }
        val current = ForwardSolver.computePosition(msg.appendage.name)
        val elbow = ForwardSolver.computePosition(elbowJoint.name)
        val shoulder = ForwardSolver.computePosition(shoulder_z.name)
        val target = Point3D(msg.values[0],msg.values[1],msg.values[2])

        val a = distance(shoulder,elbow)   // Upper arm
        val b = distance(elbow,current)    // Forearm
        val c = distance(target,shoulder)  // Desired shoulder -> hand

        if( a+b < c ) {
            // Use Law of Cosines to determine elbow position to get correct distance
            val cosc = (a * a + b * b + c * c) / (2.0 * a * b)
            var angle = Math.acos(cosc)*180.0/Math.PI
            if( DEBUG ) LOGGER.info(String.format("%s.place: Elbow angle = %2.0f",CLSS,angle))
            val mc = RobotModel.motorsByJoint[elbowJoint]!!
            if( angle > mc.maxAngle ) {
                error = String.format("I cannot straighten my elbow beyond %2.0f degrees",angle)
            }
            else if( angle < mc.minAngle ) {
                error = String.format("I cannot bend my elbow to the required %2.0f degrees",angle)
            }
            else {
                var motorMsg = MessageBottle(RequestType.SET_MOTOR_PROPERTY)
                motorMsg.joint = elbowJoint
                motorMsg.jointDynamicProperty = JointDynamicProperty.ANGLE
                motorMsg.values[0] = angle
                list.add(motorMsg)

                // Set shoulder x to 0 (straight up and down
                motorMsg = MessageBottle(RequestType.SET_MOTOR_PROPERTY)
                motorMsg.joint = shoulder_x
                motorMsg.jointDynamicProperty = JointDynamicProperty.ANGLE
                motorMsg.values[0] = 0.0
                list.add(motorMsg)

                // Rotate in Y
                angle = Math.atan2(target.z - shoulder.z,c)*180.0/Math.PI
                if( DEBUG ) LOGGER.info(String.format("%s.place: Shoulder y angle = %2.0f",CLSS,angle))
                motorMsg = MessageBottle(RequestType.SET_MOTOR_PROPERTY)
                motorMsg.joint = shoulder_z
                motorMsg.jointDynamicProperty = JointDynamicProperty.ANGLE
                motorMsg.values[0] = angle
                list.add(motorMsg)

                // Rotate in Z
                angle = Math.atan2(target.y - shoulder.y,c)*180.0/Math.PI
                if( DEBUG ) LOGGER.info(String.format("%s.place: Shoulder z angle = %2.0f",CLSS,angle))
                motorMsg = MessageBottle(RequestType.SET_MOTOR_PROPERTY)
                motorMsg.joint = shoulder_z
                motorMsg.jointDynamicProperty = JointDynamicProperty.ANGLE
                motorMsg.values[0] = angle
                list.add(motorMsg)
            }
        }
        else {
            error = String.format("My arm does not reach the target")
        }
        return error
    }

    private fun distance(p1:Point3D,p2:Point3D): Double {
        val d = Math.sqrt(((p1.x-p2.x)*(p1.x-p2.x)) + ((p1.y-p2.y)*(p1.y-p2.y)) + ((p1.z-p2.z)*(p1.z-p2.z)) )
        return d
    }

    private const val CLSS = "HandSolver"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean

    /**
     * Constructor:
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
    }
}