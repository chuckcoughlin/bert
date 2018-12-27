/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.speech.process;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import bert.share.bottle.RequestBottle;
import bert.speech.antlr.SpeechSyntaxLexer;
import bert.speech.antlr.SpeechSyntaxParser;

/**
 *  Parse spoken text using ANTLR classes. A context dictionary is passed
 *  between invocations of the parser. 
 */
public class StatementParser  {
	private final HashMap<String,Object> sharedDictionary;

	/**
	 * Constructor provides parameters specific to the robot. The
	 * shared dictionary is intended for communication between the
	 * invocations of the translator. 
	 */
	public StatementParser() {
		sharedDictionary = new HashMap<String,Object>();
	}
	
	public void setSharedProperty(String key,Object value) { sharedDictionary.put(key, value); }
	/**
	 * This is the method to parse a statement - one line of text. It uses the visitor pattern to
	 * traverse the parse tree and generate the returned statement prototype. This method parses one line.
	 * 
	 * @param cmd user-entered command string
	 * @return a request bottle to be sent to the server
	 */
	public RequestBottle parseCommand(String text) throws Exception {
		
		ByteArrayInputStream bais = new ByteArrayInputStream(text.getBytes());
		ANTLRInputStream in = new ANTLRInputStream(bais);
		SpeechSyntaxLexer lexer = new QuietLexer(in);
		lexer.removeErrorListeners();  // Quiet lexer gripes
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		SpeechSyntaxParser parser = new SpeechSyntaxParser(tokens);
		parser.removeErrorListeners(); // remove default error listener
	    parser.addErrorListener(new SpeechErrorListener(sharedDictionary));
		parser.setErrorHandler(new SpeechErrorStrategy(sharedDictionary));
		ParseTree tree = parser.line();   // Start with a line
		StatementTranslator visitor = new StatementTranslator(sharedDictionary);
		visitor.visit(tree);
		return visitor.getRequest();
	}
	
}
