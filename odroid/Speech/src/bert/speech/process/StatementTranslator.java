/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.speech.process;

import java.util.HashMap;

import bert.share.
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
	private final RequestBottle bottle;
	
	/**
	 * Constructor.
	 * @param shared is a parameter dictionary used to communicate between invocations
	 */
	public StatementTranslator(HashMap<String,Object> shared) {
		this.sharedDictionary = shared;
		this.bottle = new RequestBottle();
	}
	
	public RequestBottle getRequest() { return this.bottle; }
	
	// ================================= Overridden Methods =====================================
	// These do the actual translations
	@Override 
	Object visitHandleSingleWordCommand(SpeechSyntaxParser.HandleSingleWordCommandContext ctx) {
		return null;
	}

}
