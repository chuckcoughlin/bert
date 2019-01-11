/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.speech.process;

import java.util.HashMap;

import bert.share.bottle.BottleConstants;
import bert.share.bottle.MessageBottle;
import bert.share.bottle.RequestType;
import bert.speech.antlr.SpeechSyntaxBaseVisitor;
import bert.speech.antlr.SpeechSyntaxParser;


/**
 *  This translator takes spoken lines of text and converts them into
 *  "Request Bottles".
 */
public class StatementTranslator extends SpeechSyntaxBaseVisitor<Object>  {
	private static final String CLSS = "StatementTranslator";
	private static final System.Logger LOGGER = System.getLogger(CLSS);
	private final HashMap<String,Object> sharedDictionary;
	private final MessageBottle bottle;
	
	/**
	 * Constructor.
	 * @param bot a request container supplied by the framework. It is our job 
	 *            to fully configure it.
	 * @param shared a parameter dictionary used to communicate between invocations
	 */
	public StatementTranslator(MessageBottle bot,HashMap<String,Object> shared) {
		this.sharedDictionary = shared;
		this.bottle = bot;
	}
	
	// ================================= Overridden Methods =====================================
	// These do the actual translations. Text->RequestBottle.
	// NOTE: Any action, state or pose names require database access to fill in the details.
	@Override 
	public Object visitHandleSingleWordCommand(SpeechSyntaxParser.HandleSingleWordCommandContext ctx) {
		String cmd = ctx.Command().getText();
		if( cmd.equalsIgnoreCase("relax") ) {
			bottle.setRequestType(RequestType.SET_STATE);
			bottle.setProperty(BottleConstants.STATE_NAME,"relax");
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
		return null;
	}
	@Override 
	public Object visitHowQuestion(SpeechSyntaxParser.HowQuestionContext ctx) {
		String property = ctx.HowAdjective().getText();
		if( property.equalsIgnoreCase("old") ||
			property.equalsIgnoreCase("tall")   ) {
			
			bottle.setRequestType(RequestType.GET_PROPERTY);
			bottle.setProperty(BottleConstants.PROPERTY_NAME,property);
			
		}
		else {
			String msg = String.format("I don't know what %s means",property);
			bottle.setError(msg);
		}
		return null;
	}
	@Override 
	public Object visitWhatQuestion(SpeechSyntaxParser.WhatQuestionContext ctx) {
		String property = ctx.Property().getText();
		if( property.equalsIgnoreCase("cadence") ||
			property.equalsIgnoreCase("cycle time") ||
			property.equalsIgnoreCase("duty cycle")   ) {
			
			bottle.setRequestType(RequestType.GET_PROPERTY);
			bottle.setProperty(BottleConstants.PROPERTY_NAME,property);	
		}
		else {
			String msg = String.format("I don't have a property %s, that I know of",property);
			bottle.setError(msg);
		}
		return null;
	}

}
