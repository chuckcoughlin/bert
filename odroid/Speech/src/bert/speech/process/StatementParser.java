/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.speech.process;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import bert.share.control.Appendage;
import bert.share.message.MessageBottle;
import bert.share.message.RequestType;
import bert.share.motor.Joint;
import bert.speech.antlr.SpeechSyntaxLexer;
import bert.speech.antlr.SpeechSyntaxParser;

/**
 *  Parse spoken text using ANTLR classes. A context dictionary is passed
 *  between invocations of the parser. 
 */
public class StatementParser  {
	private final HashMap<SharedKey,Object> context;

	/**
	 * Constructor provides parameters specific to the robot. The
	 * shared dictionary is intended for communication between the
	 * invocations of the translator. 
	 */
	public StatementParser() {
		context = new HashMap<>();
		initialize();
	}
	
	/**
	 * Initialize the shared dictionary
	 */
	private void initialize() {
		context.put(SharedKey.ASLEEP,"false");
		context.put(SharedKey.APPENDAGE,Appendage.UNKNOWN);
		context.put(SharedKey.AXIS,"x");
		context.put(SharedKey.JOINT,Joint.UNKNOWN);
		context.put(SharedKey.SIDE,"right");
	}
	
	public void setSharedProperty(SharedKey key,Object value) { context.put(key, value); }
	/**
	 * This is the method that parses a statement - one line of text. It uses the visitor pattern to
	 * traverse the parse tree and generate the returned statement prototype. This method parses one line.
	 * 
	 * Any partial text is saved off and prepended to the next string.
	 * 
	 * @param cmd user-entered english string
	 * @return a request bottle to be sent to the server
	 */
	public MessageBottle parseStatement(String text) throws Exception {
		MessageBottle bottle = new MessageBottle();
		if( text!=null) {
			if( context.get(SharedKey.PARTIAL)!=null ) {
				text = String.format("%s %s", context.get(SharedKey.PARTIAL),text);
				context.remove(SharedKey.PARTIAL);
			}
			ByteArrayInputStream bais = new ByteArrayInputStream(text.getBytes());
			CodePointCharStream stream = CharStreams.fromString(text);
			SpeechSyntaxLexer lexer = new QuietLexer(stream);
			lexer.removeErrorListeners();  // Quiet lexer gripes
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			SpeechSyntaxParser parser = new SpeechSyntaxParser(tokens);
			parser.removeErrorListeners(); // remove default error listener
			parser.addErrorListener(new SpeechErrorListener(bottle));
			parser.setErrorHandler(new SpeechErrorStrategy(bottle));
			ParseTree tree = parser.line();   // Start with a line
			StatementTranslator visitor = new StatementTranslator(bottle,context);
			visitor.visit(tree);
			
			if(bottle.fetchRequestType().equals(RequestType.PARTIAL) ) {
				context.put(SharedKey.PARTIAL,text);
			}
		}
		else {
			bottle.assignError("Empty");
		}
		return bottle;
	}
	
}
