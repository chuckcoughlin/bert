/**
 * Based on examples from "The Definitive ANTLR 4 Reference",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
**/
package bert.speech.process;

import java.util.logging.Logger;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

import bert.share.message.MessageBottle;


/**
 * As errors are detected, add their parameters to the dictionary.
 */
public class SpeechErrorListener extends BaseErrorListener {
	private static String CLSS = "SpeechErrorListener: ";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private final MessageBottle bottle;
	
	public SpeechErrorListener(MessageBottle bot) {
		this.bottle = bot;
	}
	@Override
	public void syntaxError(Recognizer<?,?> recognizer,
				Object offendingSymbol,
				int line, int charPositionInLine,
				String msg,
				RecognitionException e) {
		recordError(recognizer,(Token)offendingSymbol,
                       line, charPositionInLine);
    }

    protected void recordError(Recognizer<?,?> recognizer,
                                  Token offendingToken, int line,
                                  int charPositionInLine) {
    	// Defer to the parser.
    	if( bottle.fetchError()==null ) {
    		if( offendingToken != null ) {
    			String msg = String.format("I didn't understand what came after %s",offendingToken.getStartIndex(),offendingToken.getText());
    			LOGGER.info(CLSS+msg);
    			bottle.assignError(msg);
    		}
    		// We get here if we're listening to the lexer - which we are not
    		else {
    			String msg = "I don't understand";
    			LOGGER.info(CLSS+msg);
    			bottle.assignError(msg);
    		}
    	}
    }
}
