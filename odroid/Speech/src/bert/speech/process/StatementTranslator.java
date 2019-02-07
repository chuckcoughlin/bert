/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.speech.process;

import java.util.HashMap;
import java.util.logging.Logger;

import bert.share.message.BottleConstants;
import bert.share.message.MessageBottle;
import bert.share.message.MetricType;
import bert.share.message.RequestType;
import bert.share.motor.Joint;
import bert.share.motor.JointProperty;
import bert.speech.antlr.SpeechSyntaxBaseVisitor;
import bert.speech.antlr.SpeechSyntaxParser;


/**
 *  This translator takes spoken lines of text and converts them into
 *  "Request Bottles".
 */
public class StatementTranslator extends SpeechSyntaxBaseVisitor<Object>  {
	private static final String CLSS = "StatementTranslator";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private final HashMap<String,String> sharedDictionary;
	private final MessageBottle bottle;
	
	/**
	 * Constructor.
	 * @param bot a request container supplied by the framework. It is our job 
	 *            to fully configure it.
	 * @param shared a parameter dictionary used to communicate between invocations
	 */
	public StatementTranslator(MessageBottle bot,HashMap<String,String> shared) {
		this.sharedDictionary = shared;
		this.bottle = bot;
	}
	
	// ================================= Overridden Methods =====================================
	// These do the actual translations. Text->RequestBottle.
	// NOTE: Any action, state or pose names require database access to fill in the details.
	@Override 
	public Object visitHandleSingleWordCommand(SpeechSyntaxParser.HandleSingleWordCommandContext ctx) {
		if( ctx.Command()!=null) {
		String cmd = ctx.Command().getText();
		if( cmd.equalsIgnoreCase("relax") ) {
			bottle.setRequestType(RequestType.SET_STATE);
			//bottle.setProperty(BottleConstants.STATE_NAME,"relax");
		}
		else if( cmd.equalsIgnoreCase("freeze") ||
				 cmd.equalsIgnoreCase("stop") ) {
			bottle.setRequestType(RequestType.SET_STATE);
			bottle.setProperty(BottleConstants.POSE_NAME,"freeze");
		}
		else if( cmd.equalsIgnoreCase("attention") ||
				 cmd.equalsIgnoreCase("wake up") ) {
			bottle.setRequestType(RequestType.SET_POSE);
			bottle.setProperty(BottleConstants.POSE_NAME,"attention");
			
		}
		else {
			String msg = String.format("I do not know how to %s",cmd);
			bottle.setError(msg);
		}
		}
		else if(ctx.Halt()!=null) {
			bottle.setRequestType(RequestType.COMMAND);
			bottle.setProperty(BottleConstants.COMMAND_NAME,"shutdown");
		}
		return null;
	}
	@Override 
	// How tall are you?
	public Object visitAttributeQuestion(SpeechSyntaxParser.AttributeQuestionContext ctx) {
		bottle.setRequestType(RequestType.GET_METRIC);
		String attribute = ctx.Adjective().getText();
		if( attribute.equalsIgnoreCase("old") ) {
			bottle.setProperty(BottleConstants.PROPERTY_METRIC,MetricType.AGE.name());
		}
		else if(	attribute.equalsIgnoreCase("tall")   ) {
			bottle.setProperty(BottleConstants.PROPERTY_METRIC,MetricType.HEIGHT.name());
		}
		else {
			String msg = String.format("I don't know what %s means",attribute);
			bottle.setError(msg);
		}
		return null;
	}
	
	@Override 
	// what is the id of your left hip y?
	public Object visitJointPropertyQuestion(SpeechSyntaxParser.JointPropertyQuestionContext ctx) {
		bottle.setRequestType(RequestType.GET_CONFIGURATION);
		String property = ctx.Property().getText().toUpperCase();
		if( property.equalsIgnoreCase("maximum angle")) property = "MAXIMUMANGLE";
		else if( property.equalsIgnoreCase("minimum angle")) property = "MINIMUMANGLE";
		else if( property.equalsIgnoreCase("motor type")) property = "MOTORTYPE";
		try {
			bottle.setProperty(BottleConstants.PROPERTY_PROPERTY,JointProperty.valueOf(property).name());
			// If side or axis were set previously, use those values as defaults
			String side = sharedDictionary.get(SharedKey.SIDE.name()).toString();
			if( ctx.Side()!=null ) side = ctx.Side().getText();
			sharedDictionary.put(SharedKey.SIDE.name(), side);
			String axis = sharedDictionary.get(SharedKey.AXIS.name()).toString();
			if( ctx.Axis()!=null ) axis = ctx.Axis().getText();
			sharedDictionary.put(SharedKey.AXIS.name(), axis);
			Joint joint = determineJoint(ctx.Joint().getText(),axis,side);
			bottle.setProperty(BottleConstants.PROPERTY_JOINT,joint.name());
		}
		catch(IllegalArgumentException iae) {
			String msg = String.format("I don't have a property %s, that I know of",property);
			bottle.setError(msg);
		}
		return null;
	}
	
	@Override 
	// What is your duty cycle?
	public Object visitMetricsQuestion(SpeechSyntaxParser.MetricsQuestionContext ctx) {
		bottle.setRequestType(RequestType.GET_METRIC);
		String metric = ctx.Metric().getText().toUpperCase();
		if( metric.equalsIgnoreCase("cycle time") ) {
			bottle.setProperty(BottleConstants.PROPERTY_METRIC,MetricType.CYCLETIME.name());
		}
		else if(metric.equalsIgnoreCase("duty cycle") ) {
			bottle.setProperty(BottleConstants.PROPERTY_METRIC,MetricType.DUTYCYCLE.name());
		}
		else {
			try {
				bottle.setProperty(BottleConstants.PROPERTY_METRIC,MetricType.valueOf(metric).name());
			}
			catch(IllegalArgumentException iae) {
				String msg = String.format("I did't know that I had a %s",metric);
				bottle.setError(msg);
			}
		}
		return null;
	}
	@Override 
	// what is the z position of your left hip?
	// Identical to JointPropertyQuestion, but different word order
	public Object visitPositionQuestion(SpeechSyntaxParser.PositionQuestionContext ctx) {
		bottle.setRequestType(RequestType.GET_CONFIGURATION);
		String property = ctx.Property().getText().toUpperCase();
		if( property.equalsIgnoreCase("maximum angle")) property = "MAXIMUMANGLE";
		else if( property.equalsIgnoreCase("minimum angle")) property = "MINIMUMANGLE";
		else if( property.equalsIgnoreCase("motor type")) property = "MOTORTYPE";
		try {
			bottle.setProperty(BottleConstants.PROPERTY_PROPERTY,JointProperty.valueOf(property).name());
			// If side or axis were set previously, use those values as defaults
			String side = sharedDictionary.get(SharedKey.SIDE.name()).toString();
			if( ctx.Side()!=null ) side = ctx.Side().getText();
			sharedDictionary.put(SharedKey.SIDE.name(), side);
			String axis = sharedDictionary.get(SharedKey.AXIS.name()).toString();
			if( ctx.Axis()!=null ) axis = ctx.Axis().getText();
			sharedDictionary.put(SharedKey.AXIS.name(), axis);
			Joint joint = determineJoint(ctx.Joint().getText(),axis,side);
			bottle.setProperty(BottleConstants.PROPERTY_JOINT,joint.name());
		}
		catch(IllegalArgumentException iae) {
			String msg = String.format("I don't have a property %s, that I know of",property);
			bottle.setError(msg);
		}
		return null;
	}
	//===================================== Helper Methods ======================================
	// Determine the specific joint from the body part, side and axis. (The latter two are
	// not always needed.
	private Joint determineJoint(String bodyPart,String axis,String side) {
		Joint result = Joint.UNKNOWN;
		if( bodyPart.equalsIgnoreCase("ABS")) {
			if(axis!=null) {
				if( axis.equalsIgnoreCase("X"))    		result = Joint.ABS_X;
				else if( axis.equalsIgnoreCase("Y"))    result = Joint.ABS_Y;
				else if( axis.equalsIgnoreCase("Z"))    result = Joint.ABS_Z;
			}
		}
		else if( bodyPart.equalsIgnoreCase("ANKLE")) {
			if(side!=null) {
				if( side.equalsIgnoreCase("left"))         result = Joint.LEFT_ANKLE_Y;
				else if( side.equalsIgnoreCase("right"))   result = Joint.RIGHT_ANKLE_Y;
			}
		}
		else if( bodyPart.equalsIgnoreCase("BUST")) {
			if(axis!=null) {
				if( axis.equalsIgnoreCase("X"))    		result = Joint.BUST_X;
				else if( axis.equalsIgnoreCase("Y"))    result = Joint.BUST_Y;
			}
		}
		else if( bodyPart.equalsIgnoreCase("ELBOW")) {
			if(side!=null) {
				if( side.equalsIgnoreCase("left"))         result = Joint.LEFT_ELBOW_Y;
				else if( side.equalsIgnoreCase("right"))   result = Joint.RIGHT_ELBOW_Y;
			}
		}
		else if( bodyPart.equalsIgnoreCase("HEAD") || bodyPart.equalsIgnoreCase("NECK")) {
			if(axis!=null) {
				if( axis.equalsIgnoreCase("Y"))         result = Joint.HEAD_Y;
				else if( axis.equalsIgnoreCase("Z"))    result = Joint.HEAD_Z;
			}
		}
		else if( bodyPart.equalsIgnoreCase("HIP")) {
			if(axis!=null && side!=null) {
				if( side.equalsIgnoreCase("left")) {
					if( axis.equalsIgnoreCase("X"))    		result = Joint.LEFT_HIP_X;
					else if( axis.equalsIgnoreCase("Y"))    result = Joint.LEFT_HIP_Y;
					else if( axis.equalsIgnoreCase("Z"))    result = Joint.LEFT_HIP_Z;
				}
				else if( side.equalsIgnoreCase("right")) {
					if( axis.equalsIgnoreCase("X"))    		result = Joint.RIGHT_HIP_X;
					else if( axis.equalsIgnoreCase("Y"))    result = Joint.RIGHT_HIP_Y;
					else if( axis.equalsIgnoreCase("Z"))    result = Joint.RIGHT_HIP_Z;
				}
			}
		}
		else if( bodyPart.equalsIgnoreCase("KNEE")) {
			if(side!=null) {
				if( side.equalsIgnoreCase("left"))         result = Joint.LEFT_KNEE_Y;
				else if( side.equalsIgnoreCase("right"))   result = Joint.RIGHT_KNEE_Y;
			}
		}
		else if( bodyPart.equalsIgnoreCase("SHOULDER") || bodyPart.equalsIgnoreCase("ARM")) {
			if(axis!=null && side!=null) {
				if( side.equalsIgnoreCase("left")) {
					if( axis.equalsIgnoreCase("X"))    		result = Joint.LEFT_SHOULDER_X;
					else if( axis.equalsIgnoreCase("Y"))    result = Joint.LEFT_SHOULDER_Y;
					else if( axis.equalsIgnoreCase("Z"))    result = Joint.LEFT_ARM_Z;
				}
				else if( side.equalsIgnoreCase("right")) {
					if( axis.equalsIgnoreCase("X"))    		result = Joint.RIGHT_SHOULDER_X;
					else if( axis.equalsIgnoreCase("Y"))    result = Joint.RIGHT_SHOULDER_Y;
					else if( axis.equalsIgnoreCase("Z"))    result = Joint.RIGHT_ARM_Z;
				}
			}
		}
		return result;
	}
}
