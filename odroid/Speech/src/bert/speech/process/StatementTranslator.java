/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.speech.process;

import java.util.HashMap;
import java.util.logging.Logger;

import org.antlr.v4.runtime.tree.ParseTree;

import bert.share.common.BottleConstants;
import bert.share.message.MessageBottle;
import bert.share.message.MetricType;
import bert.share.message.RequestType;
import bert.share.model.Appendage;
import bert.share.model.Joint;
import bert.share.model.JointProperty;
import bert.share.model.Limb;
import bert.speech.antlr.SpeechSyntaxBaseVisitor;
import bert.speech.antlr.SpeechSyntaxParser;
import bert.sql.db.Database;

/**
 *  This translator takes spoken lines of text and converts them into
 *  "Request Bottles".
 */
public class StatementTranslator extends SpeechSyntaxBaseVisitor<Object>  {
	private static final String CLSS = "StatementTranslator";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private final HashMap<SharedKey,Object> sharedDictionary;
	private final MessageBottle bottle;
	private final MessageTranslator messageTranslator;
	
	/**
	 * Constructor.
	 * @param bot a request container supplied by the framework. It is our job 
	 *            to fully configure it.
	 * @param shared a parameter dictionary used to communicate between invocations
	 */
	public StatementTranslator(MessageBottle bot,HashMap<SharedKey,Object> shared) {
		this.sharedDictionary = shared;
		this.bottle = bot;
		this.messageTranslator = new MessageTranslator();
	}
	
	// These do the actual translations. Text->RequestBottle.
	// NOTE: Any action, state or pose names require database access to fill in the details.
	// ================================= Overridden Methods =====================================
	// 
	@Override 
	// How tall are you?
	public Object visitAttributeQuestion(SpeechSyntaxParser.AttributeQuestionContext ctx) {
		bottle.assignRequestType(RequestType.GET_METRIC);
		String attribute = ctx.Attribute().getText();
		if( attribute.equalsIgnoreCase("old") ) {
			bottle.setProperty(BottleConstants.METRIC_NAME,MetricType.AGE.name());
		}
		else if( attribute.equalsIgnoreCase("tall")   ) {
			bottle.setProperty(BottleConstants.METRIC_NAME,MetricType.HEIGHT.name());
		}
		else {
			String msg = String.format("I don't know what %s means",attribute);
			bottle.assignError(msg);
		}
		return null;
	}
	@Override 
	// Get internal configuration parameters. There are no options.
	public Object visitConfigurationQuestion(SpeechSyntaxParser.ConfigurationQuestionContext ctx) {
		bottle.assignRequestType(RequestType.GET_CONFIGURATION);
		return null;
	}
	@Override 
	// Get internal configuration parameters. There are no options.
	public Object visitConfigurationRequest(SpeechSyntaxParser.ConfigurationRequestContext ctx) {
		bottle.assignRequestType(RequestType.GET_CONFIGURATION);
		return null;
	}
	@Override 
	// you are singing
	public Object visitDeclarePose1(SpeechSyntaxParser.DeclarePose1Context ctx) {
		String pose    = visit(ctx.phrase()).toString();
		sharedDictionary.put(SharedKey.POSE, pose);
		bottle.assignRequestType(RequestType.SAVE_POSE);
		bottle.setProperty(BottleConstants.POSE_NAME,pose);
		bottle.assignText(messageTranslator.randomAcknowledgement());
		return null;
	}
	@Override
	// your pose is sitting
	public Object visitDeclarePose2(SpeechSyntaxParser.DeclarePose2Context ctx) {
		String pose    = visit(ctx.phrase()).toString();
		sharedDictionary.put(SharedKey.POSE, pose);
		bottle.assignRequestType(RequestType.SAVE_POSE);
		bottle.setProperty(BottleConstants.POSE_NAME,pose);
		bottle.assignText(messageTranslator.randomAcknowledgement());
		return null;
	}
	@Override
	// save your pose,  save your pose as an alternate universe
	public Object visitDeclareNoNamePose(SpeechSyntaxParser.DeclareNoNamePoseContext ctx) {
		bottle.assignRequestType(RequestType.SAVE_POSE);
		if(ctx.phrase()!=null ) {
			String pose    = visit(ctx.phrase()).toString();
			sharedDictionary.put(SharedKey.POSE, pose);
			bottle.setProperty(BottleConstants.POSE_NAME,pose);
		}
		bottle.assignText(messageTranslator.randomAcknowledgement());
		return null;
	}
	@Override
	// Apply "freeze" or "relax" to: Joints, Limbs, or the entire robot. "hold" is the same as "freeze".
	// relax your left arm
	public Object visitEnableTorque(SpeechSyntaxParser.EnableTorqueContext ctx) {
		String axis = sharedDictionary.get(SharedKey.AXIS).toString();
		if( ctx.Axis()!=null ) axis = ctx.Axis().getText();
		sharedDictionary.put(SharedKey.AXIS, axis);
		// If side was set previously, use it as default
		String side = sharedDictionary.get(SharedKey.SIDE).toString();
		if( ctx.Side()!=null ) side = determineSide(ctx.Side().getText(),sharedDictionary);
		sharedDictionary.put(SharedKey.SIDE, side);
		// If both Limb() and Joint() are null, then we apply to the entire robot
		if( ctx.Freeze()!=null || ctx.Relax()!=null || ctx.Hold()!=null ) {
			String cmd = "";
			if( ctx.Freeze()!=null || ctx.Hold()!=null ) cmd = ctx.Freeze().getText().toLowerCase();
			if( ctx.Relax()!=null )  cmd = ctx.Relax().getText().toLowerCase();
			Joint joint = Joint.UNKNOWN;
			if(ctx.It()!=null && sharedDictionary.get(SharedKey.IT).equals(SharedKey.JOINT) ) {
				joint = (Joint)sharedDictionary.get(SharedKey.JOINT);
			}
			if(ctx.Joint()!=null ) {
				joint = determineJoint(ctx.Joint().getText(),axis,side);
			}
			if( !joint.equals(Joint.UNKNOWN)) {
				bottle.assignRequestType(RequestType.SET_MOTOR_PROPERTY);
				bottle.setProperty(BottleConstants.JOINT_NAME,joint.name());
				bottle.setProperty(BottleConstants.PROPERTY_NAME,JointProperty.STATE.name());
				if( ctx.Freeze()!=null || ctx.Hold()!=null ) bottle.setProperty(JointProperty.STATE.name(),BottleConstants.ON_VALUE);
				else bottle.setProperty(JointProperty.STATE.name(),BottleConstants.OFF_VALUE);
				sharedDictionary.put(SharedKey.JOINT,joint);
				sharedDictionary.put(SharedKey.IT,SharedKey.JOINT);
			}
			// Joint is not specified, try a limb
			else {
				Limb limb = Limb.UNKNOWN;
				if(ctx.It()!=null && sharedDictionary.get(SharedKey.IT).equals(SharedKey.LIMB) ) {
					limb = (Limb)sharedDictionary.get(SharedKey.LIMB);
				}
				if(ctx.Limb()!=null) {
					limb = determineLimb(ctx.Limb().getText(),side);
				}
				if( !limb.equals(Limb.UNKNOWN)) {
					bottle.assignRequestType(RequestType.SET_LIMB_PROPERTY);
					bottle.setProperty(BottleConstants.LIMB_NAME,limb.name());
					bottle.setProperty(BottleConstants.PROPERTY_NAME,JointProperty.STATE.name());
					if( ctx.Freeze()!=null || ctx.Hold()!=null) bottle.setProperty(JointProperty.STATE.name(),BottleConstants.ON_VALUE);
					else bottle.setProperty(JointProperty.STATE.name(),BottleConstants.OFF_VALUE);
					sharedDictionary.put(SharedKey.LIMB,limb);
					sharedDictionary.put(SharedKey.IT,SharedKey.LIMB);
				}
				// Limb and Joint are UNKNOWN. Apply to the entire body
				else {
					bottle.assignRequestType(RequestType.COMMAND);
					if( ctx.Freeze()!=null || ctx.Hold()!=null ) bottle.setProperty(BottleConstants.COMMAND_NAME,BottleConstants.COMMAND_FREEZE);
					else bottle.setProperty(BottleConstants.COMMAND_NAME,BottleConstants.COMMAND_RELAX);
				}
			}
		}
		return null;
	}
	@Override
	// Handle a (possible) multi-word command to take a pose. However we make an initial check for "well-known"
	// commands.
	// carry the torch, go limp.
	public Object visitHandleArbitraryCommand(SpeechSyntaxParser.HandleArbitraryCommandContext ctx) {
		if(ctx.phrase()!=null) {
			String phrase  = visit(ctx.phrase()).toString();
			// First handle "well-known" commands
			if( !determineCommandFromPhrase(phrase)) {   // Configures bottle
				// Next check to see if this is a pose
				String pose = Database.getInstance().getPoseForCommand(phrase);
				if( pose!=null ) {
					bottle.assignRequestType(RequestType.SET_POSE);
					bottle.setProperty(BottleConstants.POSE_NAME,pose );
					sharedDictionary.put(SharedKey.POSE, pose);
				}
				else {
					String msg = String.format("I do not know how to respond to \"%s\"",phrase);
					bottle.assignError(msg);
				}
			}
		}
		return null;
	}
	@Override 
	// list the limits of your left hip y? (same logic as "handleBulkPropertyRequest)
	public Object visitHandleBulkPropertyQuestion(SpeechSyntaxParser.HandleBulkPropertyQuestionContext ctx) {
		if( ctx.Limits() != null ) bottle.assignRequestType(RequestType.GET_LIMITS);
		else bottle.assignRequestType(RequestType.GET_GOALS);

		// If side or axis were set previously, use those jointValues as defaults
		String side = sharedDictionary.get(SharedKey.SIDE).toString();
		if( ctx.Side()!=null ) side = determineSide(ctx.Side().getText(),sharedDictionary);
		sharedDictionary.put(SharedKey.SIDE, side);
		String axis = sharedDictionary.get(SharedKey.AXIS).toString();
		if( ctx.Axis()!=null ) axis = ctx.Axis().getText();
		sharedDictionary.put(SharedKey.AXIS, axis);
		Joint joint = null;
		if( ctx.Joint()!=null ) {
			joint = determineJoint(ctx.Joint().getText(),axis,side);
			bottle.setProperty(BottleConstants.JOINT_NAME,joint.name());
			if( joint.equals(Joint.UNKNOWN)) {
				String msg = String.format("I don't have a joint %s, that I know of",ctx.Joint().getText());
				bottle.assignError(msg);
			}
			else {
				sharedDictionary.put(SharedKey.JOINT, joint);
				sharedDictionary.put(SharedKey.IT,SharedKey.JOINT);
			}
		}
		else {
			String msg = String.format("You didn't specify the name of a joint");
			bottle.assignError(msg);
		}
		return null;
	}
	@Override 
	// what are the limits of your left hip y? (same logic as "handleBulkPropertyQuestion)
	public Object visitHandleBulkPropertyRequest(SpeechSyntaxParser.HandleBulkPropertyRequestContext ctx) {
		if( ctx.Limits() != null ) bottle.assignRequestType(RequestType.GET_LIMITS);
		else bottle.assignRequestType(RequestType.GET_GOALS);

		// If side or axis were set previously, use those jointValues as defaults
		String side = sharedDictionary.get(SharedKey.SIDE).toString();
		if( ctx.Side()!=null ) side = determineSide(ctx.Side().getText(),sharedDictionary);
		sharedDictionary.put(SharedKey.SIDE, side);
		String axis = sharedDictionary.get(SharedKey.AXIS).toString();
		if( ctx.Axis()!=null ) axis = ctx.Axis().getText();
		sharedDictionary.put(SharedKey.AXIS, axis);
		Joint joint = determineJoint(ctx.Joint().getText(),axis,side);
		bottle.setProperty(BottleConstants.JOINT_NAME,joint.name());
		if( joint.equals(Joint.UNKNOWN) ) {
			String msg = String.format("I don't have a joint %s, that I know of",ctx.Joint().getText());
			bottle.assignError(msg);
		}
		else {
			sharedDictionary.put(SharedKey.JOINT, joint);
			sharedDictionary.put(SharedKey.IT,SharedKey.JOINT);
		}
		return null;
	}

	@Override 
	public Object visitHandleGreeting(SpeechSyntaxParser.HandleGreetingContext ctx) {
		bottle.assignRequestType(RequestType.NOTIFICATION);
		bottle.setProperty(BottleConstants.TEXT, messageTranslator.randomGreetingResponse());
		return null;
	}
	// List the joint properties
	@Override 
	public Object visitHandleListCommand1(SpeechSyntaxParser.HandleListCommand1Context ctx) {
		bottle.assignRequestType(RequestType.LIST_MOTOR_PROPERTY);
		String pname = ctx.Properties().getText();  // plural
		try {
			JointProperty jp = determineJointProperty(pname);
			bottle.setProperty(BottleConstants.PROPERTY_NAME,jp.name());
			if(ctx.Controller()!=null) {
				bottle.setProperty(BottleConstants.CONTROLLER_NAME, determineController(ctx.Controller().getText()));
			}
		}
		catch(IllegalArgumentException iae) {
			String msg = String.format("My joints don't hava a property %s, that I know of",pname.toLowerCase());
			bottle.assignError(msg);
		}
		return null;
	}
	// Tell me your joint positions
	@Override 
	public Object visitHandleListCommand2(SpeechSyntaxParser.HandleListCommand2Context ctx) {
		bottle.assignRequestType(RequestType.LIST_MOTOR_PROPERTY);
		String pname = ctx.Properties().getText();  // plural
		try {
			JointProperty jp = determineJointProperty(pname);
			bottle.setProperty(BottleConstants.PROPERTY_NAME,jp.name());
			if(ctx.Controller()!=null) {
				bottle.setProperty(BottleConstants.CONTROLLER_NAME, determineController(ctx.Controller().getText()));
			}
		}
		catch(IllegalArgumentException iae) {
			String msg = String.format("My joints don't hava a property %s, that I know of",pname.toLowerCase());
			bottle.assignError(msg);
		}
		return null;
	}
		
	@Override 
	// initialize your joints
	public Object visitInitializeJoints(SpeechSyntaxParser.InitializeJointsContext ctx) {
		bottle.assignRequestType(RequestType.INITIALIZE_JOINTS);
		return null;
	}
	
	@Override 
	// what is the id of your left hip y?
	public Object visitJointPropertyQuestion(SpeechSyntaxParser.JointPropertyQuestionContext ctx) {
		bottle.assignRequestType(RequestType.GET_MOTOR_PROPERTY);
		String property = ctx.Property().getText().toUpperCase();
		
		try {
			JointProperty jp =  determineJointProperty(property);
			bottle.setProperty(BottleConstants.PROPERTY_NAME,jp.name());
			// If side or axis were set previously, use those jointValues as defaults
			String side = sharedDictionary.get(SharedKey.SIDE).toString();
			if( ctx.Side()!=null ) side = determineSide(ctx.Side().getText(),sharedDictionary);
			sharedDictionary.put(SharedKey.SIDE, side);
			String axis = sharedDictionary.get(SharedKey.AXIS).toString();
			if( ctx.Axis()!=null ) axis = ctx.Axis().getText();
			sharedDictionary.put(SharedKey.AXIS, axis);
			Joint joint = determineJoint(ctx.Joint().getText(),axis,side);
			bottle.setProperty(BottleConstants.JOINT_NAME,joint.name());
			if( joint.equals(Joint.UNKNOWN) ) {
				String msg = String.format("I don't have a joint %s, that I know of",ctx.Joint().getText());
				bottle.assignError(msg);
			}
			else {
				sharedDictionary.put(SharedKey.JOINT, joint);
				sharedDictionary.put(SharedKey.IT,SharedKey.JOINT);
			}
		}
		catch(IllegalArgumentException iae) {
			String msg = String.format("I don't have a property %s, that I know of",property);
			bottle.assignError(msg);
		}
		return null;
	}
	@Override
	// where is your left ear
	public Object visitLimbLocationQuestion(SpeechSyntaxParser.LimbLocationQuestionContext ctx) {
		// If axis was set previously, use it as default
		String axis = sharedDictionary.get(SharedKey.AXIS).toString();
		if( ctx.Axis()!=null ) axis = ctx.Axis().getText();
		sharedDictionary.put(SharedKey.AXIS, axis);
		// If side was set previously, use it as default
		String side = sharedDictionary.get(SharedKey.SIDE).toString();
		if( ctx.Side()!=null ) side = determineSide(ctx.Side().getText(),sharedDictionary);
		sharedDictionary.put(SharedKey.SIDE, side);
		if( ctx.Appendage()==null) {
			bottle.assignRequestType(RequestType.GET_JOINT_LOCATION);
			Joint joint = (Joint)sharedDictionary.get(SharedKey.JOINT);
			if( ctx.Joint()!=null ) joint = determineJoint(ctx.Joint().getText(),axis,side);
			bottle.setProperty(BottleConstants.JOINT_NAME,joint.name());
			if( joint.equals(Joint.UNKNOWN) ) {
				String msg = String.format("I don't have a joint like that");
				bottle.assignError(msg);
			}
			else {
				sharedDictionary.put(SharedKey.JOINT, joint);
				sharedDictionary.put(SharedKey.IT,SharedKey.JOINT);
			}
		}
		else {
			bottle.assignRequestType(RequestType.GET_APPENDAGE_LOCATION);
			Appendage appendage = (Appendage)sharedDictionary.get(SharedKey.APPENDAGE);
			if( ctx.Appendage()!=null )appendage = determineAppendage(ctx.Appendage().getText(),side);
			bottle.setProperty(BottleConstants.APPENDAGE_NAME,appendage.name());
			if( appendage.equals(Appendage.UNKNOWN) ) {
				String msg = String.format("I don't have an appendage %s, that I know of",ctx.Appendage().getText());
				bottle.assignError(msg);
			}
			else {
				sharedDictionary.put(SharedKey.APPENDAGE, appendage);
			}
		}
		return null;
	}
	@Override 
	// What is your duty cycle?
	public Object visitMetricsQuestion(SpeechSyntaxParser.MetricsQuestionContext ctx) {
		bottle.assignRequestType(RequestType.GET_METRIC);
		String metric = ctx.Metric().getText().toUpperCase();
		if( metric.equalsIgnoreCase("cycle time") ) {
			bottle.setProperty(BottleConstants.METRIC_NAME,MetricType.CYCLETIME.name());
		}
		else if(metric.equalsIgnoreCase("duty cycle") ) {
			bottle.setProperty(BottleConstants.METRIC_NAME,MetricType.DUTYCYCLE.name());
		}
		else {
			try {
				bottle.setProperty(BottleConstants.METRIC_NAME,MetricType.valueOf(metric).name());
			}
			catch(IllegalArgumentException iae) {
				String msg = String.format("I did't know that I had a %s",metric);
				bottle.assignError(msg);
			}
		}
		return null;
	}
	@Override 
	// Map a command to holding a pose
	// to stand means to take the pose standing
	public Object visitMapPoseToCommand1(SpeechSyntaxParser.MapPoseToCommand1Context ctx) {
		bottle.assignRequestType(RequestType.MAP_POSE);
		if( ctx.phrase().size()>1 ) {
			bottle.setProperty(BottleConstants.COMMAND_NAME,visit(ctx.phrase(0)).toString());
			bottle.setProperty(BottleConstants.POSE_NAME,visit(ctx.phrase(1)).toString());
		}
		else {
			String msg = String.format("I need both a pose name and associated command");
			bottle.assignError(msg);
		}
		return null;
	}
	@Override 
	// to climb means you are climbing
	public Object visitMapPoseToCommand2(SpeechSyntaxParser.MapPoseToCommand2Context ctx) {
		bottle.assignRequestType(RequestType.MAP_POSE);
		if( ctx.phrase().size()>1 ) {
			bottle.setProperty(BottleConstants.COMMAND_NAME,visit(ctx.phrase(0)).toString());
			bottle.setProperty(BottleConstants.POSE_NAME,visit(ctx.phrase(1)).toString());
		}
		else {
			String msg = String.format("I need both a pose name and associated command");
			bottle.assignError(msg);
		}
		return null;
	}
	@Override 
	// to eat is to become eating
	public Object visitMapPoseToCommand3(SpeechSyntaxParser.MapPoseToCommand3Context ctx) {
		bottle.assignRequestType(RequestType.MAP_POSE);
		if( ctx.phrase().size()>1 ) {
			bottle.setProperty(BottleConstants.COMMAND_NAME,visit(ctx.phrase(0)).toString());
			bottle.setProperty(BottleConstants.POSE_NAME,visit(ctx.phrase(1)).toString());
		}
		else {
			String msg = String.format("I need both a pose name and associated command");
			bottle.assignError(msg);
		}
		return null;
	}
	@Override 
	// when i say climb take the pose climbing
	public Object visitMapPoseToCommand4(SpeechSyntaxParser.MapPoseToCommand4Context ctx) {
		bottle.assignRequestType(RequestType.MAP_POSE);
		if( ctx.phrase().size()>1 ) {
			String command = visit(ctx.phrase(0)).toString();
			String pose    = visit(ctx.phrase(1)).toString();
			bottle.setProperty(BottleConstants.COMMAND_NAME,command);
			bottle.setProperty(BottleConstants.POSE_NAME,pose);
		}
		else {
			String msg = String.format("This mapping requires both a pose name and associated command");
			bottle.assignError(msg);
		}
		return null;
	}
	@Override 
	// when you climb then you are climbing
	public Object visitMapPoseToCommand5(SpeechSyntaxParser.MapPoseToCommand5Context ctx) {
		bottle.assignRequestType(RequestType.MAP_POSE);
		if( ctx.phrase().size()>1 ) {
			String command = visit(ctx.phrase(0)).toString();
			String pose    = visit(ctx.phrase(1)).toString();
			bottle.setProperty(BottleConstants.COMMAND_NAME,command);
			bottle.setProperty(BottleConstants.POSE_NAME,pose);
		}
		else {
			String msg = String.format("I need both a pose name and associated command");
			bottle.assignError(msg);
		}
		return null;
	}
	@Override 
	// what is the z position of your left hip?
	// Identical to JointPropertyQuestion, but different word order
	public Object visitMotorPropertyQuestion1(SpeechSyntaxParser.MotorPropertyQuestion1Context ctx) {
		bottle.assignRequestType(RequestType.GET_MOTOR_PROPERTY);
		String property = ctx.Property().getText().toUpperCase();
		
		try {
			JointProperty jp = determineJointProperty(property);
			bottle.setProperty(BottleConstants.PROPERTY_NAME,jp.name());
			// If side or axis were set previously, use those jointValues as defaults
			String side = sharedDictionary.get(SharedKey.SIDE).toString();
			if( ctx.Side()!=null ) side = determineSide(ctx.Side().getText(),sharedDictionary);
			sharedDictionary.put(SharedKey.SIDE, side);
			String axis = sharedDictionary.get(SharedKey.AXIS).toString();
			if( ctx.Axis()!=null ) axis = ctx.Axis().getText();
			sharedDictionary.put(SharedKey.AXIS, axis);
			Joint joint = (Joint)sharedDictionary.get(SharedKey.JOINT);
			if( ctx.Joint()!=null ) joint = determineJoint(ctx.Joint().getText(),axis,side);
			bottle.setProperty(BottleConstants.JOINT_NAME,joint.name());
			if( joint.equals(Joint.UNKNOWN) ) {
				String msg = "You must specify a legal joint";
				if( ctx.Joint()!=null )  msg = String.format("I don't have a joint %s, that I know of",ctx.Joint().getText());
				bottle.assignError(msg);
			}
			else {
				sharedDictionary.put(SharedKey.JOINT, joint);
				sharedDictionary.put(SharedKey.IT,SharedKey.JOINT);
			}
		}
		catch(IllegalArgumentException iae) {
			String msg = String.format("I don't have a property %s, that I know of",property);
			bottle.assignError(msg);
		}
		return null;
	}
	@Override 
	// what is the speed of your left hip x?
	// Identical to MotorPropertyQuestion1, but different word order
	public Object visitMotorPropertyQuestion2(SpeechSyntaxParser.MotorPropertyQuestion2Context ctx) {
		bottle.assignRequestType(RequestType.GET_MOTOR_PROPERTY);
		String property = ctx.Property().getText().toUpperCase();
		
		try {
			JointProperty jp = determineJointProperty(property);
			bottle.setProperty(BottleConstants.PROPERTY_NAME,jp.name());
			// If side or axis were set previously, use those jointValues as defaults
			String side = sharedDictionary.get(SharedKey.SIDE).toString();
			if( ctx.Side()!=null ) side = determineSide(ctx.Side().getText(),sharedDictionary);
			sharedDictionary.put(SharedKey.SIDE, side);
			String axis = sharedDictionary.get(SharedKey.AXIS).toString();
			if( ctx.Axis()!=null ) axis = ctx.Axis().getText();
			sharedDictionary.put(SharedKey.AXIS, axis);
			Joint joint = (Joint)sharedDictionary.get(SharedKey.JOINT);
			if( ctx.Joint()!=null ) joint = determineJoint(ctx.Joint().getText(),axis,side);
			bottle.setProperty(BottleConstants.JOINT_NAME,joint.name());
			if( joint.equals(Joint.UNKNOWN) ) {
				String msg = "You must specify a legal joint";
				if( ctx.Joint()!=null )  msg = String.format("I don't have a joint %s, that I know of",ctx.Joint().getText());
				bottle.assignError(msg);
			}
			else {
				sharedDictionary.put(SharedKey.JOINT, joint);
				sharedDictionary.put(SharedKey.IT,SharedKey.JOINT);
			}
		}
		catch(IllegalArgumentException iae) {
			String msg = String.format("I don't have a property %s, that I know of",property);
			bottle.assignError(msg);
		}
		return null;
	}
	@Override 
	// move your left hip y to 45 degrees
	public Object visitMoveMotor(SpeechSyntaxParser.MoveMotorContext ctx) {
		bottle.assignRequestType(RequestType.SET_MOTOR_PROPERTY);

		// If side or axis were set previously, use those jointValues as defaults
		String side = sharedDictionary.get(SharedKey.SIDE).toString();
		if( ctx.Side()!=null ) side = determineSide(ctx.Side().getText(),sharedDictionary);
		sharedDictionary.put(SharedKey.SIDE, side);
		String axis = sharedDictionary.get(SharedKey.AXIS).toString();
		if( ctx.Axis()!=null ) axis = ctx.Axis().getText();
		sharedDictionary.put(SharedKey.AXIS, axis);
		Joint joint = Joint.UNKNOWN;
		if(ctx.It()!=null ) {
			joint = (Joint)sharedDictionary.get(SharedKey.JOINT);
		}
		else if( ctx.Joint() != null ) {
			joint = determineJoint(ctx.Joint().getText(),axis,side);
		}
		
		if( joint.equals(Joint.UNKNOWN) ) {
			String msg = String.format("I don't have a joint like that");
			bottle.assignError(msg);
		}
		else {
			sharedDictionary.put(SharedKey.JOINT, joint);
			sharedDictionary.put(SharedKey.IT,SharedKey.JOINT);
			bottle.setProperty(BottleConstants.JOINT_NAME,joint.name());
		}
		bottle.setProperty(BottleConstants.PROPERTY_NAME,JointProperty.POSITION.name());
		bottle.setProperty(JointProperty.POSITION.name(),ctx.Value().getText());
		return null;
	}
	@Override 
	// move slowly
	public Object visitMoveSpeed(SpeechSyntaxParser.MoveSpeedContext ctx) {
		bottle.assignRequestType(RequestType.SET_POSE);

		String pose = poseForAdverb(ctx.Adverb().getText());
		if( pose!=null ) {
			bottle.setProperty(BottleConstants.POSE_NAME,pose );
			bottle.assignText(String.format("I am moving %s", ctx.Adverb().getText()) );
		}
		return null;
	}
	@Override 
	// What is your current pose?
	public Object visitPoseQuestion(SpeechSyntaxParser.PoseQuestionContext ctx) {
		String pose = sharedDictionary.get(SharedKey.POSE).toString();
		bottle.assignRequestType(RequestType.NOTIFICATION);
		bottle.setProperty(BottleConstants.POSE_NAME,pose);
		bottle.assignText(String.format("My current pose is %s", pose));
		return null;
	}
	// set your left hip y to 45 degrees
	// set your left elbow torque to 1.2
	@Override
	public Object visitSetMotorPosition(SpeechSyntaxParser.SetMotorPositionContext ctx) {
		bottle.assignRequestType(RequestType.SET_MOTOR_PROPERTY);
		// Property defaults to position
		JointProperty property = JointProperty.POSITION;
		if( ctx.Property()!=null )  property = determineJointProperty(ctx.Property().getText());

		// If side or axis were set previously, use those jointValues as defaults
		String side = sharedDictionary.get(SharedKey.SIDE).toString();
		if( ctx.Side()!=null ) side = determineSide(ctx.Side().getText(),sharedDictionary);
		sharedDictionary.put(SharedKey.SIDE, side);
		String axis = sharedDictionary.get(SharedKey.AXIS).toString();
		if( ctx.Axis()!=null ) axis = ctx.Axis().getText();
		sharedDictionary.put(SharedKey.AXIS, axis);
		Joint joint = Joint.UNKNOWN;
		if( ctx.Joint() != null ) {
			joint = determineJoint(ctx.Joint().getText(),axis,side);
			bottle.setProperty(BottleConstants.JOINT_NAME,joint.name());
		}
		if( joint.equals(Joint.UNKNOWN) ) {
			String msg = String.format("I don't have a joint like that");
			bottle.assignError(msg);
		}
		bottle.setProperty(BottleConstants.PROPERTY_NAME,property.name());
		bottle.setProperty(property.name(),ctx.Value().getText());
		if( !property.equals(JointProperty.POSITION) &&
			!property.equals(JointProperty.SPEED)    &&
			!property.equals(JointProperty.STATE)    &&
			!property.equals(JointProperty.TORQUE)  ) {
				bottle.assignError("Only position, speed, torque and state are settable for a joint");
		}
		sharedDictionary.put(SharedKey.JOINT, joint);
		sharedDictionary.put(SharedKey.IT,SharedKey.JOINT);
		return null;
	}
	// set the position of your left hip y to 45 degrees
	public Object visitSetMotorProperty(SpeechSyntaxParser.SetMotorPropertyContext ctx) {
		bottle.assignRequestType(RequestType.SET_MOTOR_PROPERTY);
		// Get the property
		JointProperty property = determineJointProperty(ctx.Property().getText());
		
		// If side or axis were set previously, use those jointValues as defaults
		String side = sharedDictionary.get(SharedKey.SIDE).toString();
		if( ctx.Side()!=null ) side = determineSide(ctx.Side().getText(),sharedDictionary);
		sharedDictionary.put(SharedKey.SIDE, side);
		String axis = sharedDictionary.get(SharedKey.AXIS).toString();
		if( ctx.Axis()!=null ) axis = ctx.Axis().getText();
		sharedDictionary.put(SharedKey.AXIS, axis);
		Joint joint = Joint.UNKNOWN;
		if( ctx.Joint() != null ) {
			joint = determineJoint(ctx.Joint().getText(),axis,side);
			bottle.setProperty(BottleConstants.JOINT_NAME,joint.name());
		}
		if( joint.equals(Joint.UNKNOWN) ) {
			String msg = String.format("I don't have a joint like that");
			bottle.assignError(msg);
		}
		else {
			sharedDictionary.put(SharedKey.JOINT, joint);
			sharedDictionary.put(SharedKey.IT,SharedKey.JOINT);
		}
		bottle.setProperty(BottleConstants.PROPERTY_NAME,property.name());
		if(ctx.Value()!=null )   bottle.setProperty(property.name(),ctx.Value().getText());
		else if(ctx.On()!=null ) bottle.setProperty(property.name(),BottleConstants.ON_VALUE);
		else if(ctx.Off()!=null )bottle.setProperty(property.name(),BottleConstants.OFF_VALUE);
		if( !property.equals(JointProperty.POSITION) &&
			!property.equals(JointProperty.SPEED)    &&
			!property.equals(JointProperty.STATE)    &&
			!property.equals(JointProperty.TORQUE)  ) {
			bottle.assignError("Only position, speed, torque and state are settable for a joint");
		}
		return null;
	}

	@Override
	// If the joint is not specified, then straighten the entire body
	// straighten your left elbow.
	public Object visitStraightenJoint(SpeechSyntaxParser.StraightenJointContext ctx) {
		// A real joint
		bottle.assignRequestType(RequestType.SET_MOTOR_PROPERTY);
		// Get the property
		JointProperty property = JointProperty.POSITION;

		// If side or axis were set previously, use those jointValues as defaults
		String side = sharedDictionary.get(SharedKey.SIDE).toString();
		if( ctx.Side()!=null ) side = determineSide(ctx.Side().getText(),sharedDictionary);
		sharedDictionary.put(SharedKey.SIDE, side);
		String axis = sharedDictionary.get(SharedKey.AXIS).toString();
		if( ctx.Axis()!=null ) axis = ctx.Axis().getText();
		sharedDictionary.put(SharedKey.AXIS, axis);
		Joint joint = Joint.UNKNOWN;
		if(ctx.It()!=null && sharedDictionary.get(SharedKey.IT).equals(SharedKey.JOINT) ) {
			joint = (Joint)sharedDictionary.get(SharedKey.JOINT);
		}
		if( ctx.Joint() != null ) {
			joint = determineJoint(ctx.Joint().getText(),axis,side);
			bottle.setProperty(BottleConstants.JOINT_NAME,joint.name());
		}
		if( joint.equals(Joint.UNKNOWN) ) {
			String msg = String.format("Which joint am i supposed to straighten?");
			bottle.assignError(msg);
		}
		else if(joint.equals(Joint.LEFT_ELBOW_Y)  ||
				joint.equals(Joint.RIGHT_ELBOW_Y) ||
				joint.equals(Joint.LEFT_KNEE_Y)   ||
				joint.equals(Joint.RIGHT_KNEE_Y)  ||
				joint.equals(Joint.LEFT_HIP_Y)   ||
				joint.equals(Joint.RIGHT_HIP_Y )   ) {
			// Straighten means 180 degrees
			double value = 180.;
			bottle.setProperty(BottleConstants.JOINT_NAME,joint.name());
			bottle.setProperty(BottleConstants.PROPERTY_NAME,property.name());
			bottle.setProperty(JointProperty.POSITION.name(),String.valueOf(value));
			sharedDictionary.put(SharedKey.JOINT, joint);
			sharedDictionary.put(SharedKey.IT,SharedKey.JOINT);
		}
		else if(joint.equals(Joint.NECK_Y)  ||
				joint.equals(Joint.NECK_Z) ||
				joint.equals(Joint.LEFT_HIP_Z)   ||
				joint.equals(Joint.RIGHT_HIP_Z)   ) {
			// Straighten means 0 degrees
			double value = 0.;
			bottle.setProperty(BottleConstants.JOINT_NAME,joint.name());
			bottle.setProperty(BottleConstants.PROPERTY_NAME,property.name());
			bottle.setProperty(JointProperty.POSITION.name(),String.valueOf(value));
			sharedDictionary.put(SharedKey.JOINT, joint);
			sharedDictionary.put(SharedKey.IT,SharedKey.JOINT);
		}
		return null;
	}
	@Override
	// why do you wear mittens
	public Object visitWhyMittens(SpeechSyntaxParser.WhyMittensContext ctx) {
		bottle.assignRequestType(RequestType.GET_METRIC);
		bottle.setProperty(BottleConstants.METRIC_NAME,MetricType.MITTENS.name());
		return null;
	}
	@Override
	// a phrase. Return space-separated words
	public Object visitWordList(SpeechSyntaxParser.WordListContext ctx) {
		StringBuffer text = new StringBuffer();
		boolean needsSpace = false;
		for(ParseTree token:ctx.children) {
			if( token==null ) continue;
			if( needsSpace ) {
				text.append(" ");
			}
			needsSpace = true;
			text.append(token.getText());
		}
		return text.toString();
	}
	//===================================== Helper Methods ======================================
	// Determine the specific appendage from the body part and side. (Side is not always needed).
	private Appendage determineAppendage(String bodyPart,String side) {
		Appendage result = Appendage.UNKNOWN;
		
		if( bodyPart.equalsIgnoreCase("EAR")) {
			if(side!=null) {
				if( side.equalsIgnoreCase("left"))     result = Appendage.LEFT_EAR;
				else                                   result = Appendage.RIGHT_EAR;
			}
		}
		else if( bodyPart.equalsIgnoreCase("EYE") || bodyPart.equalsIgnoreCase("EYES")) {
			if(side!=null) {
				if( side.equalsIgnoreCase("left"))     result = Appendage.LEFT_EYE;
				else                                   result = Appendage.RIGHT_EYE;
			}
		}
		else if( bodyPart.equalsIgnoreCase("FINGER") || bodyPart.equalsIgnoreCase("HAND")) {
			if(side!=null) {
				if( side.equalsIgnoreCase("left"))     result = Appendage.LEFT_FINGER;
				else                                   result = Appendage.RIGHT_FINGER;
			}
		}
		else if( bodyPart.equalsIgnoreCase("FOOT") || bodyPart.equalsIgnoreCase("TOE")) {
			if(side!=null) {
				if( side.equalsIgnoreCase("left"))     result = Appendage.LEFT_TOE;
				else                                   result = Appendage.RIGHT_TOE;
			}
		}
		else if( bodyPart.equalsIgnoreCase("HEEL") ) {
			if(side!=null) {
				if( side.equalsIgnoreCase("left"))     result = Appendage.LEFT_HEEL;
				else                                   result = Appendage.RIGHT_HEEL;
			}
		}
		else if( bodyPart.equalsIgnoreCase("NOSE")) 	result = Appendage.NOSE;

		if( result.equals(Limb.UNKNOWN)) {
			LOGGER.info(String.format("WARNING: StatementTranslator.determineLimb did not find a match for %s",bodyPart));
		}
		return result;
	}
	// Return TRUE if the phrase should be interpreted as one of the fixed commands. If so, update the 
	// request bottle appropriately.
	private boolean determineCommandFromPhrase(String phrase) {
		boolean success = true;
	
		if( phrase.equals("die")  || phrase.equals("exit") || phrase.equals("halt") || 
			phrase.equals("quit") || phrase.equals("stop") ) {
				bottle.assignRequestType(RequestType.COMMAND);
				bottle.setProperty(BottleConstants.COMMAND_NAME,BottleConstants.COMMAND_HALT);
		}
		else if( phrase.equals(BottleConstants.COMMAND_RELAX) || phrase.startsWith("go limp") ) {
			bottle.assignRequestType(RequestType.COMMAND);
			bottle.setProperty(BottleConstants.COMMAND_NAME,BottleConstants.COMMAND_RELAX);
		}
		else if(phrase.equals(BottleConstants.COMMAND_FREEZE) || phrase.startsWith("go rigid") ) {
			bottle.assignRequestType(RequestType.COMMAND);
			bottle.setProperty(BottleConstants.COMMAND_NAME,BottleConstants.COMMAND_FREEZE);
		}
		else if( phrase.startsWith("ignore")||phrase.equalsIgnoreCase("go to sleep") || phrase.startsWith("sleep")) {
			sharedDictionary.put(SharedKey.ASLEEP,"true");
			bottle.assignRequestType(RequestType.COMMAND);
			bottle.setProperty(BottleConstants.COMMAND_NAME,BottleConstants.COMMAND_SLEEP);
		}
		else if( phrase.startsWith("pay attention")||phrase.equalsIgnoreCase("wake up") ) {
			sharedDictionary.put(SharedKey.ASLEEP,"false");
			bottle.assignRequestType(RequestType.COMMAND);
			bottle.setProperty(BottleConstants.COMMAND_NAME,BottleConstants.COMMAND_WAKE);
		}
		else if( phrase.equals("power off") || phrase.equals("shut down") || phrase.equals("shutdown") ) {
			bottle.assignRequestType(RequestType.COMMAND);
			bottle.setProperty(BottleConstants.COMMAND_NAME,BottleConstants.COMMAND_SHUTDOWN);
		}
		else if( phrase.equals("reset")) {
			bottle.assignRequestType(RequestType.COMMAND);
			bottle.setProperty(BottleConstants.COMMAND_NAME,BottleConstants.COMMAND_RESET);
		}
		// straighten yourself up
		else if( phrase.startsWith("straighten")) {
			bottle.assignRequestType(RequestType.SET_POSE);
			bottle.setProperty(BottleConstants.POSE_NAME,BottleConstants.POSE_HOME);
		}
		else {
			success = false;
		}
		
		return success;
	}
	// Determine controller from the supplied string. 
	private String determineController(String text) throws IllegalArgumentException  {
		String controller = BottleConstants.CONTROLLER_UPPER;
		if( text.equalsIgnoreCase("lower")) controller = BottleConstants.CONTROLLER_LOWER;
		return controller;
	}
	// Determine the specific joint from the body part, side and axis. (The latter two are
	// not always needed).
	private Joint determineJoint(String bodyPart,String axis,String side) {
		Joint result = Joint.UNKNOWN;
		
		// Handle some synonyms
		if( axis!=null ) {
			if( axis.equalsIgnoreCase("horizontal")  ) axis="Z";
			else if( axis.equalsIgnoreCase("vertical") ||
					 axis.equalsIgnoreCase("why")    ) axis="Y";
			else if( axis.equalsIgnoreCase("ex")     ) axis="X";
		}

		
		if( bodyPart.equalsIgnoreCase("ABS")) {
			if(axis!=null) {
				if( axis.equalsIgnoreCase("X"))    		result = Joint.ABS_X;
				else if( axis.equalsIgnoreCase("Y"))    result = Joint.ABS_Y;
				else                                    result = Joint.ABS_Z;
			}
		}
		else if( bodyPart.equalsIgnoreCase("ANKLE")) {
			if(side!=null) {
				if( side.equalsIgnoreCase("left"))         result = Joint.LEFT_ANKLE_Y;
				else                                       result = Joint.RIGHT_ANKLE_Y;
			}
		}
		else if( bodyPart.equalsIgnoreCase("BUST") || bodyPart.equalsIgnoreCase("CHEST") ) {
			if(axis!=null) {
				if( axis.equalsIgnoreCase("X"))    		result = Joint.BUST_X;
				else                                    result = Joint.BUST_Y;
			}
		}
		else if( bodyPart.equalsIgnoreCase("ELBOW")) {
			if(side!=null) {
				if( side.equalsIgnoreCase("left"))         result = Joint.LEFT_ELBOW_Y;
				else                                       result = Joint.RIGHT_ELBOW_Y;
			}
		}
		else if( bodyPart.equalsIgnoreCase("NECK")) {
			if(axis!=null) {
				if( axis.equalsIgnoreCase("Y"))         result = Joint.NECK_Y;
				else                                    result = Joint.NECK_Z;
			}
		}
		else if( bodyPart.equalsIgnoreCase("HIP") || bodyPart.equalsIgnoreCase("THIGH")) {
			if(axis!=null && side!=null) {
				if( side.equalsIgnoreCase("left")) {
					if( axis.equalsIgnoreCase("X"))    		result = Joint.LEFT_HIP_X;
					else if( axis.equalsIgnoreCase("Y"))    result = Joint.LEFT_HIP_Y;
					else                                    result = Joint.LEFT_HIP_Z;
				}
				else if( side.equalsIgnoreCase("right")) {
					if( axis.equalsIgnoreCase("X"))    		result = Joint.RIGHT_HIP_X;
					else if( axis.equalsIgnoreCase("Y"))    result = Joint.RIGHT_HIP_Y;
					else                                    result = Joint.RIGHT_HIP_Z;
				}
			}
		}
		else if( bodyPart.equalsIgnoreCase("KNEE")) {
			if(side!=null) {
				if( side.equalsIgnoreCase("left"))         result = Joint.LEFT_KNEE_Y;
				else                                       result = Joint.RIGHT_KNEE_Y;
			}
		}
		else if( bodyPart.equalsIgnoreCase("SHOULDER") || bodyPart.equalsIgnoreCase("ARM")) {
			if(axis!=null && side!=null) {
				if( side.equalsIgnoreCase("left")) {
					if( axis.equalsIgnoreCase("X"))    		result = Joint.LEFT_SHOULDER_X;
					else if( axis.equalsIgnoreCase("Y"))    result = Joint.LEFT_SHOULDER_Y;
					else                                    result = Joint.LEFT_ARM_Z;
				}
				else if( side.equalsIgnoreCase("right")) {
					if( axis.equalsIgnoreCase("X"))    		result = Joint.RIGHT_SHOULDER_X;
					else if( axis.equalsIgnoreCase("Y"))    result = Joint.RIGHT_SHOULDER_Y;
					else                                    result = Joint.RIGHT_ARM_Z;
				}
			}
		}
		if( result.equals(Joint.UNKNOWN)) {
			LOGGER.info(String.format("WARNING: StatementTranslator.determineJoint did not find a match for %s",bodyPart));
		}
		return result;
	}
	
	// Determine a joint property from the supplied string. Take care of recognized
	// aliases in one place. The name may be plural in some settings.
	private JointProperty determineJointProperty(String pname) throws IllegalArgumentException  {
		JointProperty result = JointProperty.UNRECOGNIZED;
		if( pname.endsWith("s") || pname.endsWith("S")) {
			pname = pname.substring(0, pname.length()-1).toUpperCase();
		}
		if( pname.equalsIgnoreCase("angle")) pname = "POSITION";
		else if( pname.equalsIgnoreCase("load")) pname = "TORQUE";
		else if( pname.equalsIgnoreCase("max angle")) pname = "MAXIMUMANGLE";
		else if( pname.equalsIgnoreCase("min angle")) pname = "MINIMUMANGLE";
		else if( pname.equalsIgnoreCase("maximum angle")) pname = "MAXIMUMANGLE";
		else if( pname.equalsIgnoreCase("minimum angle")) pname = "MINIMUMANGLE";
		else if( pname.equalsIgnoreCase("motor type")) pname = "MOTORTYPE";
		else if( pname.equalsIgnoreCase("speed"))  pname = "SPEED";
		else if( pname.equalsIgnoreCase("state"))  pname = "STATE";
		else if( pname.equalsIgnoreCase("torque"))  pname = "TORQUE";
		else if( pname.equalsIgnoreCase("velocity"))  pname = "SPEED";
		else if( pname.equalsIgnoreCase("velocitie")) pname = "SPEED";
		result = JointProperty.valueOf(pname.toUpperCase());
		return result;
	}
	// Determine the specific limb from the body part and side. (Side is not always needed).
	// A limb is a grouping of joints, e.g. "arm" includes elbow and shoulder.
	private Limb determineLimb(String bodyPart,String side) {
		Limb result = Limb.UNKNOWN;

		if( bodyPart.equalsIgnoreCase("arm")) {
			if(side!=null) {
				if( side.equalsIgnoreCase("left"))     result = Limb.LEFT_ARM;
				else                                   result = Limb.RIGHT_ARM;
			}
		}
		else if( bodyPart.equalsIgnoreCase("leg")) {
			if(side!=null) {
				if( side.equalsIgnoreCase("left"))     result = Limb.LEFT_LEG;
				else                                   result = Limb.RIGHT_LEG;
			}
		}
		else if( bodyPart.equalsIgnoreCase("back") || bodyPart.equalsIgnoreCase("torso")) {
			result = Limb.TORSO;
		}
		else if( bodyPart.equalsIgnoreCase("head") ) {
			result = Limb.HEAD;
		}

		if( result.equals(Limb.UNKNOWN)) {
			LOGGER.info(String.format("WARNING: StatementTranslator.determineLimb did not find a match for %s",bodyPart));
		}
		return result;
	}

	// Determine side from the supplied string. If the string is "other", return
	// the side different from the last used.
	private String determineSide(String text,HashMap<SharedKey,Object> dict) throws IllegalArgumentException  {
		String side = "right";
		if( text.equalsIgnoreCase("left")) side = "left";
		else if(text.equalsIgnoreCase("other")) {
			String former = dict.get(SharedKey.SIDE).toString();
			if( former.equalsIgnoreCase("left")) side = "right";
			else side="left";
		}
		return side;
	}
	// The poses returned here are expected to exist in the Pose table of the database.
	private String poseForAdverb(String adverb)  {
		String pose = "";
		if( adverb.toLowerCase().contains("slow motion")) pose = "very slow speed";
		else if( adverb.toLowerCase().contains("slow")) {
			if( adverb.toLowerCase().contains("very") ) pose = "very slow speed";
			else pose = "slow speed";
		}
		else if( adverb.toLowerCase().contains("fast") || adverb.toLowerCase().contains("quick")) {
			if( adverb.toLowerCase().contains("very") ) pose = "very fast speed";
			else pose = "fast speed";
		}
		else pose = "normal speed";
		return pose;
	}
}
