/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.speech.process

import java.util.logging.Logger

/**
 * This translator takes spoken lines of text and converts them into
 * "Request Bottles".
 */
class StatementTranslator(bot: MessageBottle, private val sharedDictionary: HashMap<SharedKey, Any?>) :
    SpeechSyntaxBaseVisitor<Any?>() {
    private val bottle: MessageBottle
    private val messageTranslator: MessageTranslator

    /**
     * Constructor.
     * @param bot a request container supplied by the framework. It is our job
     * to fully configure it.
     * @param shared a parameter dictionary used to communicate between invocations
     */
    init {
        bottle = bot
        messageTranslator = MessageTranslator()
    }

    // These do the actual translations. Text->RequestBottle.
    // NOTE: Any action, state or pose names require database access to fill in the details.
    // ================================= Overridden Methods =====================================
    // 
    // How tall are you?
    override fun visitAttributeQuestion(ctx: AttributeQuestionContext): Any? {
        bottle.assignRequestType(RequestType.GET_METRIC)
        val attribute: String = ctx.Attribute().getText()
        if (attribute.equals("old", ignoreCase = true)) {
            bottle.setProperty(BottleConstants.METRIC_NAME, MetricType.AGE.name())
        } else if (attribute.equals("tall", ignoreCase = true)) {
            bottle.setProperty(BottleConstants.METRIC_NAME, MetricType.HEIGHT.name())
        } else {
            val msg = String.format("I don't know what %s means", attribute)
            bottle.assignError(msg)
        }
        return null
    }

    // Get internal configuration parameters. There are no options.
    override fun visitConfigurationQuestion(ctx: ConfigurationQuestionContext?): Any? {
        bottle.assignRequestType(RequestType.GET_CONFIGURATION)
        return null
    }

    // Get internal configuration parameters. There are no options.
    override fun visitConfigurationRequest(ctx: ConfigurationRequestContext?): Any? {
        bottle.assignRequestType(RequestType.GET_CONFIGURATION)
        return null
    }

    // you are singing
    override fun visitDeclarePose1(ctx: DeclarePose1Context): Any? {
        val pose: String = visit(ctx.phrase()).toString()
        sharedDictionary[SharedKey.POSE] = pose
        bottle.assignRequestType(RequestType.SAVE_POSE)
        bottle.setProperty(BottleConstants.POSE_NAME, pose)
        bottle.assignText(messageTranslator.randomAcknowledgement())
        return null
    }

    // your pose is sitting
    override fun visitDeclarePose2(ctx: DeclarePose2Context): Any? {
        val pose: String = visit(ctx.phrase()).toString()
        sharedDictionary[SharedKey.POSE] = pose
        bottle.assignRequestType(RequestType.SAVE_POSE)
        bottle.setProperty(BottleConstants.POSE_NAME, pose)
        bottle.assignText(messageTranslator.randomAcknowledgement())
        return null
    }

    // save your pose,  save your pose as an alternate universe
    override fun visitDeclareNoNamePose(ctx: DeclareNoNamePoseContext): Any? {
        bottle.assignRequestType(RequestType.SAVE_POSE)
        if (ctx.phrase() != null) {
            val pose: String = visit(ctx.phrase()).toString()
            sharedDictionary[SharedKey.POSE] = pose
            bottle.setProperty(BottleConstants.POSE_NAME, pose)
        }
        bottle.assignText(messageTranslator.randomAcknowledgement())
        return null
    }

    // Apply "freeze" or "relax" to: Joints, Limbs, or the entire robot. "hold" is the same as "freeze".
    // relax your left arm
    override fun visitEnableTorque(ctx: EnableTorqueContext): Any? {
        var axis = sharedDictionary[SharedKey.AXIS].toString()
        if (ctx.Axis() != null) axis = ctx.Axis().getText()
        sharedDictionary[SharedKey.AXIS] = axis
        // If side was set previously, use it as default
        var side = sharedDictionary[SharedKey.SIDE].toString()
        if (ctx.Side() != null) side = determineSide(ctx.Side().getText(), sharedDictionary)
        sharedDictionary[SharedKey.SIDE] = side
        // If both Limb() and Joint() are null, then we apply to the entire robot
        if (ctx.Freeze() != null || ctx.Relax() != null || ctx.Hold() != null) {
            var cmd = ""
            if (ctx.Freeze() != null || ctx.Hold() != null) cmd = ctx.Freeze().getText().toLowerCase()
            if (ctx.Relax() != null) cmd = ctx.Relax().getText().toLowerCase()
            var joint: Joint = Joint.UNKNOWN
            if (ctx.It() != null && sharedDictionary[SharedKey.IT] == SharedKey.JOINT) {
                joint = sharedDictionary[SharedKey.JOINT] as Joint?
            }
            if (ctx.Joint() != null) {
                joint = determineJoint(ctx.Joint().getText(), axis, side)
            }
            if (!joint.equals(Joint.UNKNOWN)) {
                bottle.assignRequestType(RequestType.SET_MOTOR_PROPERTY)
                bottle.setProperty(BottleConstants.JOINT_NAME, joint.name())
                bottle.setProperty(BottleConstants.PROPERTY_NAME, JointProperty.STATE.name())
                if (ctx.Freeze() != null || ctx.Hold() != null) bottle.setProperty(
                    JointProperty.STATE.name(),
                    BottleConstants.ON_VALUE
                ) else bottle.setProperty(JointProperty.STATE.name(), BottleConstants.OFF_VALUE)
                sharedDictionary[SharedKey.JOINT] = joint
                sharedDictionary[SharedKey.IT] = SharedKey.JOINT
            } else {
                var limb: Limb = Limb.UNKNOWN
                if (ctx.It() != null && sharedDictionary[SharedKey.IT] == SharedKey.LIMB) {
                    limb = sharedDictionary[SharedKey.LIMB] as Limb?
                }
                if (ctx.Limb() != null) {
                    limb = determineLimb(ctx.Limb().getText(), side)
                }
                if (!limb.equals(Limb.UNKNOWN)) {
                    bottle.assignRequestType(RequestType.SET_LIMB_PROPERTY)
                    bottle.setProperty(BottleConstants.LIMB_NAME, limb.name())
                    bottle.setProperty(BottleConstants.PROPERTY_NAME, JointProperty.STATE.name())
                    if (ctx.Freeze() != null || ctx.Hold() != null) bottle.setProperty(
                        JointProperty.STATE.name(),
                        BottleConstants.ON_VALUE
                    ) else bottle.setProperty(JointProperty.STATE.name(), BottleConstants.OFF_VALUE)
                    sharedDictionary[SharedKey.LIMB] = limb
                    sharedDictionary[SharedKey.IT] = SharedKey.LIMB
                } else {
                    bottle.assignRequestType(RequestType.COMMAND)
                    if (ctx.Freeze() != null || ctx.Hold() != null) bottle.setProperty(
                        BottleConstants.COMMAND_NAME,
                        BottleConstants.COMMAND_FREEZE
                    ) else bottle.setProperty(BottleConstants.COMMAND_NAME, BottleConstants.COMMAND_RELAX)
                }
            }
        }
        return null
    }

    // Handle a (possible) multi-word command to take a pose. However we make an initial check for "well-known"
    // commands.
    // carry the torch, go limp.
    override fun visitHandleArbitraryCommand(ctx: HandleArbitraryCommandContext): Any? {
        if (ctx.phrase() != null) {
            val phrase: String = visit(ctx.phrase()).toString()
            // First handle "well-known" commands
            if (!determineCommandFromPhrase(phrase)) {   // Configures bottle
                // Next check to see if this is a pose
                val pose: String = Database.getInstance().getPoseForCommand(phrase)
                if (pose != null) {
                    bottle.assignRequestType(RequestType.SET_POSE)
                    bottle.setProperty(BottleConstants.POSE_NAME, pose)
                    sharedDictionary[SharedKey.POSE] = pose
                } else {
                    val msg = String.format("I do not know how to respond to \"%s\"", phrase)
                    bottle.assignError(msg)
                }
            }
        }
        return null
    }

    // list the limits of your left hip y? (same logic as "handleBulkPropertyRequest)
    override fun visitHandleBulkPropertyQuestion(ctx: HandleBulkPropertyQuestionContext): Any? {
        if (ctx.Limits() != null) bottle.assignRequestType(RequestType.GET_LIMITS) else bottle.assignRequestType(
            RequestType.GET_GOALS
        )

        // If side or axis were set previously, use those jointValues as defaults
        var side = sharedDictionary[SharedKey.SIDE].toString()
        if (ctx.Side() != null) side = determineSide(ctx.Side().getText(), sharedDictionary)
        sharedDictionary[SharedKey.SIDE] = side
        var axis = sharedDictionary[SharedKey.AXIS].toString()
        if (ctx.Axis() != null) axis = ctx.Axis().getText()
        sharedDictionary[SharedKey.AXIS] = axis
        var joint: Joint? = null
        if (ctx.Joint() != null) {
            joint = determineJoint(ctx.Joint().getText(), axis, side)
            bottle.setProperty(BottleConstants.JOINT_NAME, joint.name())
            if (joint.equals(Joint.UNKNOWN)) {
                val msg = java.lang.String.format("I don't have a joint %s, that I know of", ctx.Joint().getText())
                bottle.assignError(msg)
            } else {
                sharedDictionary[SharedKey.JOINT] = joint
                sharedDictionary[SharedKey.IT] = SharedKey.JOINT
            }
        } else {
            val msg = String.format("You didn't specify the name of a joint")
            bottle.assignError(msg)
        }
        return null
    }

    // what are the limits of your left hip y? (same logic as "handleBulkPropertyQuestion)
    override fun visitHandleBulkPropertyRequest(ctx: HandleBulkPropertyRequestContext): Any? {
        if (ctx.Limits() != null) bottle.assignRequestType(RequestType.GET_LIMITS) else bottle.assignRequestType(
            RequestType.GET_GOALS
        )

        // If side or axis were set previously, use those jointValues as defaults
        var side = sharedDictionary[SharedKey.SIDE].toString()
        if (ctx.Side() != null) side = determineSide(ctx.Side().getText(), sharedDictionary)
        sharedDictionary[SharedKey.SIDE] = side
        var axis = sharedDictionary[SharedKey.AXIS].toString()
        if (ctx.Axis() != null) axis = ctx.Axis().getText()
        sharedDictionary[SharedKey.AXIS] = axis
        val joint: Joint = determineJoint(ctx.Joint().getText(), axis, side)
        bottle.setProperty(BottleConstants.JOINT_NAME, joint.name())
        if (joint.equals(Joint.UNKNOWN)) {
            val msg = java.lang.String.format("I don't have a joint %s, that I know of", ctx.Joint().getText())
            bottle.assignError(msg)
        } else {
            sharedDictionary[SharedKey.JOINT] = joint
            sharedDictionary[SharedKey.IT] = SharedKey.JOINT
        }
        return null
    }

    override fun visitHandleGreeting(ctx: HandleGreetingContext?): Any? {
        bottle.assignRequestType(RequestType.NOTIFICATION)
        bottle.setProperty(BottleConstants.TEXT, messageTranslator.randomGreetingResponse())
        return null
    }

    // List the joint properties
    override fun visitHandleListCommand1(ctx: HandleListCommand1Context): Any? {
        bottle.assignRequestType(RequestType.LIST_MOTOR_PROPERTY)
        val pname: String = ctx.Properties().getText() // plural
        try {
            val jp: JointProperty = determineJointProperty(pname)
            bottle.setProperty(BottleConstants.PROPERTY_NAME, jp.name())
            if (ctx.Controller() != null) {
                bottle.setProperty(BottleConstants.CONTROLLER_NAME, determineController(ctx.Controller().getText()))
            }
        } catch (iae: IllegalArgumentException) {
            val msg = String.format(
                "My joints don't hava a property %s, that I know of",
                pname.lowercase(Locale.getDefault())
            )
            bottle.assignError(msg)
        }
        return null
    }

    // Tell me your joint positions
    override fun visitHandleListCommand2(ctx: HandleListCommand2Context): Any? {
        bottle.assignRequestType(RequestType.LIST_MOTOR_PROPERTY)
        val pname: String = ctx.Properties().getText() // plural
        try {
            val jp: JointProperty = determineJointProperty(pname)
            bottle.setProperty(BottleConstants.PROPERTY_NAME, jp.name())
            if (ctx.Controller() != null) {
                bottle.setProperty(BottleConstants.CONTROLLER_NAME, determineController(ctx.Controller().getText()))
            }
        } catch (iae: IllegalArgumentException) {
            val msg = String.format(
                "My joints don't hava a property %s, that I know of",
                pname.lowercase(Locale.getDefault())
            )
            bottle.assignError(msg)
        }
        return null
    }

    // initialize your joints
    override fun visitInitializeJoints(ctx: InitializeJointsContext?): Any? {
        bottle.assignRequestType(RequestType.INITIALIZE_JOINTS)
        return null
    }

    // what is the id of your left hip y?
    override fun visitJointPropertyQuestion(ctx: JointPropertyQuestionContext): Any? {
        bottle.assignRequestType(RequestType.GET_MOTOR_PROPERTY)
        val property: String = ctx.Property().getText().toUpperCase()
        try {
            val jp: JointProperty = determineJointProperty(property)
            bottle.setProperty(BottleConstants.PROPERTY_NAME, jp.name())
            // If side or axis were set previously, use those jointValues as defaults
            var side = sharedDictionary[SharedKey.SIDE].toString()
            if (ctx.Side() != null) side = determineSide(ctx.Side().getText(), sharedDictionary)
            sharedDictionary[SharedKey.SIDE] = side
            var axis = sharedDictionary[SharedKey.AXIS].toString()
            if (ctx.Axis() != null) axis = ctx.Axis().getText()
            sharedDictionary[SharedKey.AXIS] = axis
            val joint: Joint = determineJoint(ctx.Joint().getText(), axis, side)
            bottle.setProperty(BottleConstants.JOINT_NAME, joint.name())
            if (joint.equals(Joint.UNKNOWN)) {
                val msg = java.lang.String.format("I don't have a joint %s, that I know of", ctx.Joint().getText())
                bottle.assignError(msg)
            } else {
                sharedDictionary[SharedKey.JOINT] = joint
                sharedDictionary[SharedKey.IT] = SharedKey.JOINT
            }
        } catch (iae: IllegalArgumentException) {
            val msg = String.format("I don't have a property %s, that I know of", property)
            bottle.assignError(msg)
        }
        return null
    }

    // where is your left ear
    override fun visitLimbLocationQuestion(ctx: LimbLocationQuestionContext): Any? {
        // If axis was set previously, use it as default
        var axis = sharedDictionary[SharedKey.AXIS].toString()
        if (ctx.Axis() != null) axis = ctx.Axis().getText()
        sharedDictionary[SharedKey.AXIS] = axis
        // If side was set previously, use it as default
        var side = sharedDictionary[SharedKey.SIDE].toString()
        if (ctx.Side() != null) side = determineSide(ctx.Side().getText(), sharedDictionary)
        sharedDictionary[SharedKey.SIDE] = side
        if (ctx.Appendage() == null) {
            bottle.assignRequestType(RequestType.GET_JOINT_LOCATION)
            var joint: Joint? = sharedDictionary[SharedKey.JOINT] as Joint?
            if (ctx.Joint() != null) joint = determineJoint(ctx.Joint().getText(), axis, side)
            bottle.setProperty(BottleConstants.JOINT_NAME, joint.name())
            if (joint.equals(Joint.UNKNOWN)) {
                val msg = String.format("I don't have a joint like that")
                bottle.assignError(msg)
            } else {
                sharedDictionary[SharedKey.JOINT] = joint
                sharedDictionary[SharedKey.IT] = SharedKey.JOINT
            }
        } else {
            bottle.assignRequestType(RequestType.GET_APPENDAGE_LOCATION)
            var appendage: Appendage? = sharedDictionary[SharedKey.APPENDAGE] as Appendage?
            if (ctx.Appendage() != null) appendage = determineAppendage(ctx.Appendage().getText(), side)
            bottle.setProperty(BottleConstants.APPENDAGE_NAME, appendage.name())
            if (appendage.equals(Appendage.UNKNOWN)) {
                val msg =
                    java.lang.String.format("I don't have an appendage %s, that I know of", ctx.Appendage().getText())
                bottle.assignError(msg)
            } else {
                sharedDictionary[SharedKey.APPENDAGE] = appendage
            }
        }
        return null
    }

    // What is your duty cycle?
    override fun visitMetricsQuestion(ctx: MetricsQuestionContext): Any? {
        bottle.assignRequestType(RequestType.GET_METRIC)
        val metric: String = ctx.Metric().getText().toUpperCase()
        if (metric.equals("cycle time", ignoreCase = true)) {
            bottle.setProperty(BottleConstants.METRIC_NAME, MetricType.CYCLETIME.name())
        } else if (metric.equals("duty cycle", ignoreCase = true)) {
            bottle.setProperty(BottleConstants.METRIC_NAME, MetricType.DUTYCYCLE.name())
        } else {
            try {
                bottle.setProperty(BottleConstants.METRIC_NAME, MetricType.valueOf(metric).name())
            } catch (iae: IllegalArgumentException) {
                val msg = String.format("I did't know that I had a %s", metric)
                bottle.assignError(msg)
            }
        }
        return null
    }

    // Map a command to holding a pose
    // to stand means to take the pose standing
    override fun visitMapPoseToCommand1(ctx: MapPoseToCommand1Context): Any? {
        bottle.assignRequestType(RequestType.MAP_POSE)
        if (ctx.phrase().size > 1) {
            bottle.setProperty(BottleConstants.COMMAND_NAME, visit(ctx.phrase(0)).toString())
            bottle.setProperty(BottleConstants.POSE_NAME, visit(ctx.phrase(1)).toString())
        } else {
            val msg = String.format("I need both a pose name and associated command")
            bottle.assignError(msg)
        }
        return null
    }

    // to climb means you are climbing
    override fun visitMapPoseToCommand2(ctx: MapPoseToCommand2Context): Any? {
        bottle.assignRequestType(RequestType.MAP_POSE)
        if (ctx.phrase().size > 1) {
            bottle.setProperty(BottleConstants.COMMAND_NAME, visit(ctx.phrase(0)).toString())
            bottle.setProperty(BottleConstants.POSE_NAME, visit(ctx.phrase(1)).toString())
        } else {
            val msg = String.format("I need both a pose name and associated command")
            bottle.assignError(msg)
        }
        return null
    }

    // to eat is to become eating
    override fun visitMapPoseToCommand3(ctx: MapPoseToCommand3Context): Any? {
        bottle.assignRequestType(RequestType.MAP_POSE)
        if (ctx.phrase().size > 1) {
            bottle.setProperty(BottleConstants.COMMAND_NAME, visit(ctx.phrase(0)).toString())
            bottle.setProperty(BottleConstants.POSE_NAME, visit(ctx.phrase(1)).toString())
        } else {
            val msg = String.format("I need both a pose name and associated command")
            bottle.assignError(msg)
        }
        return null
    }

    // when i say climb take the pose climbing
    override fun visitMapPoseToCommand4(ctx: MapPoseToCommand4Context): Any? {
        bottle.assignRequestType(RequestType.MAP_POSE)
        if (ctx.phrase().size > 1) {
            val command: String = visit(ctx.phrase(0)).toString()
            val pose: String = visit(ctx.phrase(1)).toString()
            bottle.setProperty(BottleConstants.COMMAND_NAME, command)
            bottle.setProperty(BottleConstants.POSE_NAME, pose)
        } else {
            val msg = String.format("This mapping requires both a pose name and associated command")
            bottle.assignError(msg)
        }
        return null
    }

    // when you climb then you are climbing
    override fun visitMapPoseToCommand5(ctx: MapPoseToCommand5Context): Any? {
        bottle.assignRequestType(RequestType.MAP_POSE)
        if (ctx.phrase().size > 1) {
            val command: String = visit(ctx.phrase(0)).toString()
            val pose: String = visit(ctx.phrase(1)).toString()
            bottle.setProperty(BottleConstants.COMMAND_NAME, command)
            bottle.setProperty(BottleConstants.POSE_NAME, pose)
        } else {
            val msg = String.format("I need both a pose name and associated command")
            bottle.assignError(msg)
        }
        return null
    }

    // what is the z position of your left hip?
    // Identical to JointPropertyQuestion, but different word order
    override fun visitMotorPropertyQuestion1(ctx: MotorPropertyQuestion1Context): Any? {
        bottle.assignRequestType(RequestType.GET_MOTOR_PROPERTY)
        val property: String = ctx.Property().getText().toUpperCase()
        try {
            val jp: JointProperty = determineJointProperty(property)
            bottle.setProperty(BottleConstants.PROPERTY_NAME, jp.name())
            // If side or axis were set previously, use those jointValues as defaults
            var side = sharedDictionary[SharedKey.SIDE].toString()
            if (ctx.Side() != null) side = determineSide(ctx.Side().getText(), sharedDictionary)
            sharedDictionary[SharedKey.SIDE] = side
            var axis = sharedDictionary[SharedKey.AXIS].toString()
            if (ctx.Axis() != null) axis = ctx.Axis().getText()
            sharedDictionary[SharedKey.AXIS] = axis
            var joint: Joint? = sharedDictionary[SharedKey.JOINT] as Joint?
            if (ctx.Joint() != null) joint = determineJoint(ctx.Joint().getText(), axis, side)
            bottle.setProperty(BottleConstants.JOINT_NAME, joint.name())
            if (joint.equals(Joint.UNKNOWN)) {
                var msg: String? = "You must specify a legal joint"
                if (ctx.Joint() != null) msg =
                    java.lang.String.format("I don't have a joint %s, that I know of", ctx.Joint().getText())
                bottle.assignError(msg)
            } else {
                sharedDictionary[SharedKey.JOINT] = joint
                sharedDictionary[SharedKey.IT] = SharedKey.JOINT
            }
        } catch (iae: IllegalArgumentException) {
            val msg = String.format("I don't have a property %s, that I know of", property)
            bottle.assignError(msg)
        }
        return null
    }

    // what is the speed of your left hip x?
    // Identical to MotorPropertyQuestion1, but different word order
    override fun visitMotorPropertyQuestion2(ctx: MotorPropertyQuestion2Context): Any? {
        bottle.assignRequestType(RequestType.GET_MOTOR_PROPERTY)
        val property: String = ctx.Property().getText().toUpperCase()
        try {
            val jp: JointProperty = determineJointProperty(property)
            bottle.setProperty(BottleConstants.PROPERTY_NAME, jp.name())
            // If side or axis were set previously, use those jointValues as defaults
            var side = sharedDictionary[SharedKey.SIDE].toString()
            if (ctx.Side() != null) side = determineSide(ctx.Side().getText(), sharedDictionary)
            sharedDictionary[SharedKey.SIDE] = side
            var axis = sharedDictionary[SharedKey.AXIS].toString()
            if (ctx.Axis() != null) axis = ctx.Axis().getText()
            sharedDictionary[SharedKey.AXIS] = axis
            var joint: Joint? = sharedDictionary[SharedKey.JOINT] as Joint?
            if (ctx.Joint() != null) joint = determineJoint(ctx.Joint().getText(), axis, side)
            bottle.setProperty(BottleConstants.JOINT_NAME, joint.name())
            if (joint.equals(Joint.UNKNOWN)) {
                var msg: String? = "You must specify a legal joint"
                if (ctx.Joint() != null) msg =
                    java.lang.String.format("I don't have a joint %s, that I know of", ctx.Joint().getText())
                bottle.assignError(msg)
            } else {
                sharedDictionary[SharedKey.JOINT] = joint
                sharedDictionary[SharedKey.IT] = SharedKey.JOINT
            }
        } catch (iae: IllegalArgumentException) {
            val msg = String.format("I don't have a property %s, that I know of", property)
            bottle.assignError(msg)
        }
        return null
    }

    // move your left hip y to 45 degrees
    override fun visitMoveMotor(ctx: MoveMotorContext): Any? {
        bottle.assignRequestType(RequestType.SET_MOTOR_PROPERTY)

        // If side or axis were set previously, use those jointValues as defaults
        var side = sharedDictionary[SharedKey.SIDE].toString()
        if (ctx.Side() != null) side = determineSide(ctx.Side().getText(), sharedDictionary)
        sharedDictionary[SharedKey.SIDE] = side
        var axis = sharedDictionary[SharedKey.AXIS].toString()
        if (ctx.Axis() != null) axis = ctx.Axis().getText()
        sharedDictionary[SharedKey.AXIS] = axis
        var joint: Joint = Joint.UNKNOWN
        if (ctx.It() != null) {
            joint = sharedDictionary[SharedKey.JOINT] as Joint?
        } else if (ctx.Joint() != null) {
            joint = determineJoint(ctx.Joint().getText(), axis, side)
        }
        if (joint.equals(Joint.UNKNOWN)) {
            val msg = String.format("I don't have a joint like that")
            bottle.assignError(msg)
        } else {
            sharedDictionary[SharedKey.JOINT] = joint
            sharedDictionary[SharedKey.IT] = SharedKey.JOINT
            bottle.setProperty(BottleConstants.JOINT_NAME, joint.name())
        }
        bottle.setProperty(BottleConstants.PROPERTY_NAME, JointProperty.POSITION.name())
        bottle.setProperty(JointProperty.POSITION.name(), ctx.Value().getText())
        return null
    }

    // move slowly
    override fun visitMoveSpeed(ctx: MoveSpeedContext): Any? {
        bottle.assignRequestType(RequestType.SET_POSE)
        val pose = poseForAdverb(ctx.Adverb().getText())
        if (pose != null) {
            bottle.setProperty(BottleConstants.POSE_NAME, pose)
            bottle.assignText(java.lang.String.format("I am moving %s", ctx.Adverb().getText()))
        }
        return null
    }

    // What is your current pose?
    override fun visitPoseQuestion(ctx: PoseQuestionContext?): Any? {
        val pose = sharedDictionary[SharedKey.POSE].toString()
        bottle.assignRequestType(RequestType.NOTIFICATION)
        bottle.setProperty(BottleConstants.POSE_NAME, pose)
        bottle.assignText(String.format("My current pose is %s", pose))
        return null
    }

    // set your left hip y to 45 degrees
    // set your left elbow torque to 1.2
    override fun visitSetMotorPosition(ctx: SetMotorPositionContext): Any? {
        bottle.assignRequestType(RequestType.SET_MOTOR_PROPERTY)
        // Property defaults to position
        var property: JointProperty = JointProperty.POSITION
        if (ctx.Property() != null) property = determineJointProperty(ctx.Property().getText())

        // If side or axis were set previously, use those jointValues as defaults
        var side = sharedDictionary[SharedKey.SIDE].toString()
        if (ctx.Side() != null) side = determineSide(ctx.Side().getText(), sharedDictionary)
        sharedDictionary[SharedKey.SIDE] = side
        var axis = sharedDictionary[SharedKey.AXIS].toString()
        if (ctx.Axis() != null) axis = ctx.Axis().getText()
        sharedDictionary[SharedKey.AXIS] = axis
        var joint: Joint = Joint.UNKNOWN
        if (ctx.Joint() != null) {
            joint = determineJoint(ctx.Joint().getText(), axis, side)
            bottle.setProperty(BottleConstants.JOINT_NAME, joint.name())
        }
        if (joint.equals(Joint.UNKNOWN)) {
            val msg = String.format("I don't have a joint like that")
            bottle.assignError(msg)
        }
        bottle.setProperty(BottleConstants.PROPERTY_NAME, property.name())
        bottle.setProperty(property.name(), ctx.Value().getText())
        if (!property.equals(JointProperty.POSITION) &&
            !property.equals(JointProperty.SPEED) &&
            !property.equals(JointProperty.STATE) &&
            !property.equals(JointProperty.TORQUE)
        ) {
            bottle.assignError("Only position, speed, torque and state are settable for a joint")
        }
        sharedDictionary[SharedKey.JOINT] = joint
        sharedDictionary[SharedKey.IT] = SharedKey.JOINT
        return null
    }

    // set the position of your left hip y to 45 degrees
    override fun visitSetMotorProperty(ctx: SetMotorPropertyContext): Any? {
        bottle.assignRequestType(RequestType.SET_MOTOR_PROPERTY)
        // Get the property
        val property: JointProperty = determineJointProperty(ctx.Property().getText())

        // If side or axis were set previously, use those jointValues as defaults
        var side = sharedDictionary[SharedKey.SIDE].toString()
        if (ctx.Side() != null) side = determineSide(ctx.Side().getText(), sharedDictionary)
        sharedDictionary[SharedKey.SIDE] = side
        var axis = sharedDictionary[SharedKey.AXIS].toString()
        if (ctx.Axis() != null) axis = ctx.Axis().getText()
        sharedDictionary[SharedKey.AXIS] = axis
        var joint: Joint = Joint.UNKNOWN
        if (ctx.Joint() != null) {
            joint = determineJoint(ctx.Joint().getText(), axis, side)
            bottle.setProperty(BottleConstants.JOINT_NAME, joint.name())
        }
        if (joint.equals(Joint.UNKNOWN)) {
            val msg = String.format("I don't have a joint like that")
            bottle.assignError(msg)
        } else {
            sharedDictionary[SharedKey.JOINT] = joint
            sharedDictionary[SharedKey.IT] = SharedKey.JOINT
        }
        bottle.setProperty(BottleConstants.PROPERTY_NAME, property.name())
        if (ctx.Value() != null) bottle.setProperty(
            property.name(),
            ctx.Value().getText()
        ) else if (ctx.On() != null) bottle.setProperty(
            property.name(),
            BottleConstants.ON_VALUE
        ) else if (ctx.Off() != null) bottle.setProperty(property.name(), BottleConstants.OFF_VALUE)
        if (!property.equals(JointProperty.POSITION) &&
            !property.equals(JointProperty.SPEED) &&
            !property.equals(JointProperty.STATE) &&
            !property.equals(JointProperty.TORQUE)
        ) {
            bottle.assignError("Only position, speed, torque and state are settable for a joint")
        }
        return null
    }

    // If the joint is not specified, then straighten the entire body
    // straighten your left elbow.
    override fun visitStraightenJoint(ctx: StraightenJointContext): Any? {
        // A real joint
        bottle.assignRequestType(RequestType.SET_MOTOR_PROPERTY)
        // Get the property
        val property: JointProperty = JointProperty.POSITION

        // If side or axis were set previously, use those jointValues as defaults
        var side = sharedDictionary[SharedKey.SIDE].toString()
        if (ctx.Side() != null) side = determineSide(ctx.Side().getText(), sharedDictionary)
        sharedDictionary[SharedKey.SIDE] = side
        var axis = sharedDictionary[SharedKey.AXIS].toString()
        if (ctx.Axis() != null) axis = ctx.Axis().getText()
        sharedDictionary[SharedKey.AXIS] = axis
        var joint: Joint = Joint.UNKNOWN
        if (ctx.It() != null && sharedDictionary[SharedKey.IT] == SharedKey.JOINT) {
            joint = sharedDictionary[SharedKey.JOINT] as Joint?
        }
        if (ctx.Joint() != null) {
            joint = determineJoint(ctx.Joint().getText(), axis, side)
            bottle.setProperty(BottleConstants.JOINT_NAME, joint.name())
        }
        if (joint.equals(Joint.UNKNOWN)) {
            val msg = String.format("Which joint am i supposed to straighten?")
            bottle.assignError(msg)
        } else if (joint.equals(Joint.LEFT_ELBOW_Y) ||
            joint.equals(Joint.RIGHT_ELBOW_Y) ||
            joint.equals(Joint.LEFT_KNEE_Y) ||
            joint.equals(Joint.RIGHT_KNEE_Y) ||
            joint.equals(Joint.LEFT_HIP_Y) ||
            joint.equals(Joint.RIGHT_HIP_Y)
        ) {
            // Straighten means 180 degrees
            val value = 180.0
            bottle.setProperty(BottleConstants.JOINT_NAME, joint.name())
            bottle.setProperty(BottleConstants.PROPERTY_NAME, property.name())
            bottle.setProperty(JointProperty.POSITION.name(), value.toString())
            sharedDictionary[SharedKey.JOINT] = joint
            sharedDictionary[SharedKey.IT] = SharedKey.JOINT
        } else if (joint.equals(Joint.NECK_Y) ||
            joint.equals(Joint.NECK_Z) ||
            joint.equals(Joint.LEFT_HIP_Z) ||
            joint.equals(Joint.RIGHT_HIP_Z)
        ) {
            // Straighten means 0 degrees
            val value = 0.0
            bottle.setProperty(BottleConstants.JOINT_NAME, joint.name())
            bottle.setProperty(BottleConstants.PROPERTY_NAME, property.name())
            bottle.setProperty(JointProperty.POSITION.name(), value.toString())
            sharedDictionary[SharedKey.JOINT] = joint
            sharedDictionary[SharedKey.IT] = SharedKey.JOINT
        }
        return null
    }

    // why do you wear mittens
    override fun visitWhyMittens(ctx: WhyMittensContext?): Any? {
        bottle.assignRequestType(RequestType.GET_METRIC)
        bottle.setProperty(BottleConstants.METRIC_NAME, MetricType.MITTENS.name())
        return null
    }

    // a phrase. Return space-separated words
    override fun visitWordList(ctx: WordListContext): Any? {
        val text = StringBuffer()
        var needsSpace = false
        for (token in ctx.children) {
            if (token == null) continue
            if (needsSpace) {
                text.append(" ")
            }
            needsSpace = true
            text.append(token.getText())
        }
        return text.toString()
    }

    //===================================== Helper Methods ======================================
    // Determine the specific appendage from the body part and side. (Side is not always needed).
    private fun determineAppendage(bodyPart: String, side: String?): Appendage {
        var result: Appendage = Appendage.UNKNOWN
        if (bodyPart.equals("EAR", ignoreCase = true)) {
            if (side != null) {
                result = if (side.equals("left", ignoreCase = true)) Appendage.LEFT_EAR else Appendage.RIGHT_EAR
            }
        } else if (bodyPart.equals("EYE", ignoreCase = true) || bodyPart.equals("EYES", ignoreCase = true)) {
            if (side != null) {
                result = if (side.equals("left", ignoreCase = true)) Appendage.LEFT_EYE else Appendage.RIGHT_EYE
            }
        } else if (bodyPart.equals("FINGER", ignoreCase = true) || bodyPart.equals("HAND", ignoreCase = true)) {
            if (side != null) {
                result = if (side.equals("left", ignoreCase = true)) Appendage.LEFT_FINGER else Appendage.RIGHT_FINGER
            }
        } else if (bodyPart.equals("FOOT", ignoreCase = true) || bodyPart.equals("TOE", ignoreCase = true)) {
            if (side != null) {
                result = if (side.equals("left", ignoreCase = true)) Appendage.LEFT_TOE else Appendage.RIGHT_TOE
            }
        } else if (bodyPart.equals("HEEL", ignoreCase = true)) {
            if (side != null) {
                result = if (side.equals("left", ignoreCase = true)) Appendage.LEFT_HEEL else Appendage.RIGHT_HEEL
            }
        } else if (bodyPart.equals("NOSE", ignoreCase = true)) result = Appendage.NOSE
        if (result.equals(Limb.UNKNOWN)) {
            LOGGER.info(
                String.format(
                    "WARNING: StatementTranslator.determineLimb did not find a match for %s",
                    bodyPart
                )
            )
        }
        return result
    }

    // Return TRUE if the phrase should be interpreted as one of the fixed commands. If so, update the 
    // request bottle appropriately.
    private fun determineCommandFromPhrase(phrase: String): Boolean {
        var success = true
        if (phrase == "die" || phrase == "exit" || phrase == "halt" || phrase == "quit" || phrase == "stop") {
            bottle.assignRequestType(RequestType.COMMAND)
            bottle.setProperty(BottleConstants.COMMAND_NAME, BottleConstants.COMMAND_HALT)
        } else if (phrase == BottleConstants.COMMAND_RELAX || phrase.startsWith("go limp")) {
            bottle.assignRequestType(RequestType.COMMAND)
            bottle.setProperty(BottleConstants.COMMAND_NAME, BottleConstants.COMMAND_RELAX)
        } else if (phrase == BottleConstants.COMMAND_FREEZE || phrase.startsWith("go rigid")) {
            bottle.assignRequestType(RequestType.COMMAND)
            bottle.setProperty(BottleConstants.COMMAND_NAME, BottleConstants.COMMAND_FREEZE)
        } else if (phrase.startsWith("ignore") || phrase.equals(
                "go to sleep",
                ignoreCase = true
            ) || phrase.startsWith("sleep")
        ) {
            sharedDictionary[SharedKey.ASLEEP] = "true"
            bottle.assignRequestType(RequestType.COMMAND)
            bottle.setProperty(BottleConstants.COMMAND_NAME, BottleConstants.COMMAND_SLEEP)
        } else if (phrase.startsWith("pay attention") || phrase.equals("wake up", ignoreCase = true)) {
            sharedDictionary[SharedKey.ASLEEP] = "false"
            bottle.assignRequestType(RequestType.COMMAND)
            bottle.setProperty(BottleConstants.COMMAND_NAME, BottleConstants.COMMAND_WAKE)
        } else if (phrase == "power off" || phrase == "shut down" || phrase == "shutdown") {
            bottle.assignRequestType(RequestType.COMMAND)
            bottle.setProperty(BottleConstants.COMMAND_NAME, BottleConstants.COMMAND_SHUTDOWN)
        } else if (phrase == "reset") {
            bottle.assignRequestType(RequestType.COMMAND)
            bottle.setProperty(BottleConstants.COMMAND_NAME, BottleConstants.COMMAND_RESET)
        } else if (phrase.startsWith("straighten")) {
            bottle.assignRequestType(RequestType.SET_POSE)
            bottle.setProperty(BottleConstants.POSE_NAME, BottleConstants.POSE_HOME)
        } else {
            success = false
        }
        return success
    }

    // Determine controller from the supplied string. 
    @Throws(IllegalArgumentException::class)
    private fun determineController(text: String): String {
        var controller: String = BottleConstants.CONTROLLER_UPPER
        if (text.equals("lower", ignoreCase = true)) controller = BottleConstants.CONTROLLER_LOWER
        return controller
    }

    // Determine the specific joint from the body part, side and axis. (The latter two are
    // not always needed).
    private fun determineJoint(bodyPart: String, axis: String?, side: String?): Joint {
        var axis = axis
        var result: Joint = Joint.UNKNOWN

        // Handle some synonyms
        if (axis != null) {
            if (axis.equals("horizontal", ignoreCase = true)) axis = "Z" else if (axis.equals(
                    "vertical",
                    ignoreCase = true
                ) ||
                axis.equals("why", ignoreCase = true)
            ) axis = "Y" else if (axis.equals("ex", ignoreCase = true)) axis = "X"
        }
        if (bodyPart.equals("ABS", ignoreCase = true)) {
            if (axis != null) {
                result = if (axis.equals("X", ignoreCase = true)) Joint.ABS_X else if (axis.equals(
                        "Y",
                        ignoreCase = true
                    )
                ) Joint.ABS_Y else Joint.ABS_Z
            }
        } else if (bodyPart.equals("ANKLE", ignoreCase = true)) {
            if (side != null) {
                result = if (side.equals("left", ignoreCase = true)) Joint.LEFT_ANKLE_Y else Joint.RIGHT_ANKLE_Y
            }
        } else if (bodyPart.equals("BUST", ignoreCase = true) || bodyPart.equals("CHEST", ignoreCase = true)) {
            if (axis != null) {
                result = if (axis.equals("X", ignoreCase = true)) Joint.BUST_X else Joint.BUST_Y
            }
        } else if (bodyPart.equals("ELBOW", ignoreCase = true)) {
            if (side != null) {
                result = if (side.equals("left", ignoreCase = true)) Joint.LEFT_ELBOW_Y else Joint.RIGHT_ELBOW_Y
            }
        } else if (bodyPart.equals("NECK", ignoreCase = true)) {
            if (axis != null) {
                result = if (axis.equals("Y", ignoreCase = true)) Joint.NECK_Y else Joint.NECK_Z
            }
        } else if (bodyPart.equals("HIP", ignoreCase = true) || bodyPart.equals("THIGH", ignoreCase = true)) {
            if (axis != null && side != null) {
                if (side.equals("left", ignoreCase = true)) {
                    result = if (axis.equals("X", ignoreCase = true)) Joint.LEFT_HIP_X else if (axis.equals(
                            "Y",
                            ignoreCase = true
                        )
                    ) Joint.LEFT_HIP_Y else Joint.LEFT_HIP_Z
                } else if (side.equals("right", ignoreCase = true)) {
                    result = if (axis.equals("X", ignoreCase = true)) Joint.RIGHT_HIP_X else if (axis.equals(
                            "Y",
                            ignoreCase = true
                        )
                    ) Joint.RIGHT_HIP_Y else Joint.RIGHT_HIP_Z
                }
            }
        } else if (bodyPart.equals("KNEE", ignoreCase = true)) {
            if (side != null) {
                result = if (side.equals("left", ignoreCase = true)) Joint.LEFT_KNEE_Y else Joint.RIGHT_KNEE_Y
            }
        } else if (bodyPart.equals("SHOULDER", ignoreCase = true) || bodyPart.equals("ARM", ignoreCase = true)) {
            if (axis != null && side != null) {
                if (side.equals("left", ignoreCase = true)) {
                    result = if (axis.equals("X", ignoreCase = true)) Joint.LEFT_SHOULDER_X else if (axis.equals(
                            "Y",
                            ignoreCase = true
                        )
                    ) Joint.LEFT_SHOULDER_Y else Joint.LEFT_ARM_Z
                } else if (side.equals("right", ignoreCase = true)) {
                    result = if (axis.equals("X", ignoreCase = true)) Joint.RIGHT_SHOULDER_X else if (axis.equals(
                            "Y",
                            ignoreCase = true
                        )
                    ) Joint.RIGHT_SHOULDER_Y else Joint.RIGHT_ARM_Z
                }
            }
        }
        if (result.equals(Joint.UNKNOWN)) {
            LOGGER.info(
                String.format(
                    "WARNING: StatementTranslator.determineJoint did not find a match for %s",
                    bodyPart
                )
            )
        }
        return result
    }

    // Determine a joint property from the supplied string. Take care of recognized
    // aliases in one place. The name may be plural in some settings.
    @Throws(IllegalArgumentException::class)
    private fun determineJointProperty(pname: String): JointProperty {
        var pname = pname
        var result: JointProperty = JointProperty.UNRECOGNIZED
        if (pname.endsWith("s") || pname.endsWith("S")) {
            pname = pname.substring(0, pname.length - 1).uppercase(Locale.getDefault())
        }
        if (pname.equals("angle", ignoreCase = true)) pname = "POSITION" else if (pname.equals(
                "load",
                ignoreCase = true
            )
        ) pname = "TORQUE" else if (pname.equals("max angle", ignoreCase = true)) pname =
            "MAXIMUMANGLE" else if (pname.equals("min angle", ignoreCase = true)) pname =
            "MINIMUMANGLE" else if (pname.equals("maximum angle", ignoreCase = true)) pname =
            "MAXIMUMANGLE" else if (pname.equals("minimum angle", ignoreCase = true)) pname =
            "MINIMUMANGLE" else if (pname.equals("motor type", ignoreCase = true)) pname =
            "MOTORTYPE" else if (pname.equals("speed", ignoreCase = true)) pname =
            "SPEED" else if (pname.equals("state", ignoreCase = true)) pname = "STATE" else if (pname.equals(
                "torque",
                ignoreCase = true
            )
        ) pname = "TORQUE" else if (pname.equals("velocity", ignoreCase = true)) pname =
            "SPEED" else if (pname.equals("velocitie", ignoreCase = true)) pname = "SPEED"
        result = JointProperty.valueOf(pname.uppercase(Locale.getDefault()))
        return result
    }

    // Determine the specific limb from the body part and side. (Side is not always needed).
    // A limb is a grouping of joints, e.g. "arm" includes elbow and shoulder.
    private fun determineLimb(bodyPart: String, side: String?): Limb {
        var result: Limb = Limb.UNKNOWN
        if (bodyPart.equals("arm", ignoreCase = true)) {
            if (side != null) {
                result = if (side.equals("left", ignoreCase = true)) Limb.LEFT_ARM else Limb.RIGHT_ARM
            }
        } else if (bodyPart.equals("leg", ignoreCase = true)) {
            if (side != null) {
                result = if (side.equals("left", ignoreCase = true)) Limb.LEFT_LEG else Limb.RIGHT_LEG
            }
        } else if (bodyPart.equals("back", ignoreCase = true) || bodyPart.equals("torso", ignoreCase = true)) {
            result = Limb.TORSO
        } else if (bodyPart.equals("head", ignoreCase = true)) {
            result = Limb.HEAD
        }
        if (result.equals(Limb.UNKNOWN)) {
            LOGGER.info(
                String.format(
                    "WARNING: StatementTranslator.determineLimb did not find a match for %s",
                    bodyPart
                )
            )
        }
        return result
    }

    // Determine side from the supplied string. If the string is "other", return
    // the side different from the last used.
    @Throws(IllegalArgumentException::class)
    private fun determineSide(text: String, dict: HashMap<SharedKey, Any?>): String {
        var side = "right"
        if (text.equals("left", ignoreCase = true)) side = "left" else if (text.equals("other", ignoreCase = true)) {
            val former = dict[SharedKey.SIDE].toString()
            side = if (former.equals("left", ignoreCase = true)) "right" else "left"
        }
        return side
    }

    // The poses returned here are expected to exist in the Pose table of the database.
    private fun poseForAdverb(adverb: String): String {
        var pose = ""
        pose = if (adverb.lowercase(Locale.getDefault())
                .contains("slow motion")
        ) "very slow speed" else if (adverb.lowercase(Locale.getDefault()).contains("slow")) {
            if (adverb.lowercase(Locale.getDefault()).contains("very")) "very slow speed" else "slow speed"
        } else if (adverb.lowercase(Locale.getDefault()).contains("fast") || adverb.lowercase(Locale.getDefault())
                .contains("quick")
        ) {
            if (adverb.lowercase(Locale.getDefault()).contains("very")) "very fast speed" else "fast speed"
        } else "normal speed"
        return pose
    }

    companion object {
        private const val CLSS = "StatementTranslator"
        private val LOGGER = Logger.getLogger(CLSS)
    }
}