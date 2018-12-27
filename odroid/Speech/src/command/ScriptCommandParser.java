/**
 *   (c) 2014  ILS Automation. All rights reserved.
 */
package com.ils.tf.gateway.command;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import com.ils.tf.common.TestFrameProperties;
import com.ils.tf.gateway.expression.ScriptSyntaxLexer;
import com.ils.tf.gateway.expression.ScriptSyntaxParser;

/**
 *  The expression converter uses ANTLR classes to parse lines in
 *  an Application Test Module script. A context dictionary is passed
 *  between invocations of the parser. We assume that all tags are 
 *  controlled by the same provider.
 */
public class ScriptCommandParser  {
	private final HashMap<String,Object> sharedDictionary;

	/**
	 * Constructor provides parameters specific to the project. The
	 * shared dictionary is intended for communication between the
	 * translator and command prototype. 
	 */
	public ScriptCommandParser() {
		sharedDictionary = new HashMap<String,Object>();
	}
	public void setSharedProperty(String key,Object value) { sharedDictionary.put(key, value); }
	/**
	 * This is the method to create a command - one line of the script. It uses the visitor pattern to
	 * traverse the parse tree and generate the returned command prototype. This method parses one line.
	 * 
	 * @param cmd user-entered command string
	 * @return executable Ignition expression
	 */
	public CommandPrototype parseCommand(String cmd) throws Exception {
		// The NOT field is a marker, delete it each cycle.
		sharedDictionary.remove(TestFrameProperties.CP_NOT);
		// Zero the count, it is only used during commands with repeats
		sharedDictionary.put(TestFrameProperties.CP_COUNT,new Integer(0));
		CommandPrototype prototype = new CommandPrototype(sharedDictionary);
		ByteArrayInputStream bais = new ByteArrayInputStream(cmd.getBytes());
		ANTLRInputStream in = new ANTLRInputStream(bais);
		ScriptSyntaxLexer lexer = new QuietLexer(in);
		lexer.removeErrorListeners();  // Quiet lexer gripes
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		ScriptSyntaxParser parser = new ScriptSyntaxParser(tokens);
		parser.removeErrorListeners(); // remove default error listener
	    parser.addErrorListener(new ExpressionErrorListener(sharedDictionary));
		parser.setErrorHandler(new ExpressionErrorStrategy(sharedDictionary));
		ParseTree tree = parser.line();   // Start with a line
		CommandTranslator visitor = new CommandTranslator(prototype,sharedDictionary);
		visitor.visit(tree);
		return prototype;
	}
	
}
