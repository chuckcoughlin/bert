/***
 * Excerpted from "The Definitive ANTLR 4 Reference",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
***/
package bert.speech.process;

import java.lang.System.Logger.Level;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

import bert.share.bottle.MessageBottle;




/** Instead of recovering from exceptions, log the information to
 * a logfile.
 */
public class SpeechErrorStrategy extends DefaultErrorStrategy {
	private static final String CLSS = "SpeechErrorStrategy: ";
	private static final System.Logger LOGGER = System.getLogger(CLSS);
	private final MessageBottle bottle;
	
	public SpeechErrorStrategy(MessageBottle bot) {
		this.bottle = bot;
	}
	
	/**
	 * This appears to be a top-level view of things...
	 */
    @Override
    public void recover(Parser recognizer, RecognitionException e) {
    	super.recover(recognizer,e);
    	LOGGER.log(Level.WARNING, CLSS+": RECOVER");
    	//recordError(recognizer,e);  // Moved to reportError() override
    }

    /** Make sure we don't attempt to recover inline; if the parser
     *  successfully recovers, it won't throw an exception.
     */
    @Override
    public Token recoverInline(Parser recognizer)  {
    	LOGGER.log(Level.WARNING, CLSS+": RECOVER-INLINE");
    	recordError(recognizer,new InputMismatchException(recognizer));
    	return super.recoverInline(recognizer);
    }
    
    /**
	 * This appears to be a top-level view of things...
	 */
    @Override
    public void reportError(Parser recognizer, RecognitionException e) {
    	LOGGER.log(Level.WARNING, CLSS+": REPORT-ERROR");
    	recordError(recognizer,e);
    }


    /** Make sure we don't attempt to recover from problems in sub-rules. */
    @Override
    public void sync(Parser recognizer) { }
    
    protected void recordError(Recognizer<?,?> recognizer, RecognitionException re) {
    	// In each case the expected tokens are an expression. Don't bother to list
    	
    	Token offender = re.getOffendingToken();
    	if( offender != null ) {
    		String msg = String.format("I misunderstood the statement following %s",offender.getText());
    		LOGGER.log(Level.WARNING, CLSS+msg);
    		bottle.setError(msg);
    	}
    	else {
    		String msg = "I don't understand";
			LOGGER.log(Level.INFO,CLSS+msg);
			bottle.setError(msg);
    	}
    }
}

