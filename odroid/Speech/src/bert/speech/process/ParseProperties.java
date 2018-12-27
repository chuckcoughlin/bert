/**
 *   (c) 2014-2015  ILS Automation. All rights reserved.
 *  
 */
package bert.speech.process;


/**
 *  Define properties that are common to all scopes.
 */
public interface ParseProperties   {
	// These are keys used in the shared dictionary for parsing errors
	public final static String EXPR_ERR_MESSAGE    = "message";
	public final static String EXPR_ERR_LINE       = "lineno";
	public final static String EXPR_ERR_POSITION   = "position";   // Character position
	public final static String EXPR_ERR_TOKEN      = "token";
}
