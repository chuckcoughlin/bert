/***
 * Excerpted from "The Definitive ANTLR 4 Reference",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
***/
package bert.speech.process;


import java.util.logging.Logger;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

import bert.share.message.MessageBottle;




/** Instead of recovering from exceptions, log the information to
 * a logfile.
 */
public class SpeechErrorStrategy extends DefaultErrorStrategy {
	private static final String CLSS = "SpeechErrorStrategy: ";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private final MessageBottle bottle;
	
	public SpeechErrorStrategy(MessageBottle bot) {
		this.bottle = bot;
	}
	
	/**
	 * This appears to be a top-level view of things 
	 */
    @Override
    public void recover(Parser recognizer, RecognitionException e) {
    	super.recover(recognizer,e);
    	//LOGGER.warning(CLSS+": RECOVER");
    	//recordError(recognizer,e);  // Moved to reportError() override
    }

    /** Make sure we don't attempt to recover inline; if the parser
     *  successfully recovers, it won't throw an exception.
     */
    @Override
    public Token recoverInline(Parser recognizer)  {
    	// LOGGER.warning(CLSS+": RECOVER-INLINE");
    	recordError(recognizer,new InputMismatchException(recognizer));
    	return super.recoverInline(recognizer);
    }
    
    /**
	 * This appears to be a top-level view of things...
	 */
    @Override
    public void reportError(Parser recognizer, RecognitionException e) {
    	//LOGGER.warning(CLSS+":reportError ...");
    	recordError(recognizer,e);
    }


    /** Make sure we don't attempt to recover from problems in sub-rules. */
    @Override
    public void sync(Parser recognizer) { }
    
    protected void recordError(Recognizer<?,?> recognizer, RecognitionException re) {
    	// In each case the expected tokens are an expression. Don't bother to list
    	
    	Token offender = re.getOffendingToken();
    	String msg = "";
    	if( offender != null && offender.getText()!=null && !offender.getText().isEmpty() ) {
    		msg = String.format("I misunderstood the word \"%s\"",offender.getText());
    	}
    	else {
    		msg = "I don't understand";
    	}
    	LOGGER.info(String.format("WARNING: %s: %s",CLSS,msg));
		bottle.assignError(msg);
    }
}

