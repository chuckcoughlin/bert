/***
 * Excerpted from "The Definitive ANTLR 4 Reference",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
***/
package com.ils.tf.gateway.command;

import java.util.HashMap;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

import com.ils.tf.common.TestFrameProperties;
import com.inductiveautomation.ignition.common.util.LogUtil;
import com.inductiveautomation.ignition.common.util.LoggerEx;

/** Instead of recovering from exceptions, log the information to
 * a .
 */
public class ExpressionErrorStrategy extends DefaultErrorStrategy {
	private static final String TAG = "ExpressionErrorStrategy: ";
	private final LoggerEx log;
	private final HashMap<String,Object> sharedDictionary;
	
	public ExpressionErrorStrategy(HashMap<String,Object> table) {
		log = LogUtil.getLogger(getClass().getPackage().getName());
		this.sharedDictionary = table;
	}
	
	/**
	 * This appears to be a top-level view of things...
	 */
    @Override
    public void recover(Parser recognizer, RecognitionException e) {
    	super.recover(recognizer,e);
    	log.trace(TAG+": RECOVER");
    	//recordError(recognizer,e);  // Moved to reportError() override
    }

    /** Make sure we don't attempt to recover inline; if the parser
     *  successfully recovers, it won't throw an exception.
     */
    @Override
    public Token recoverInline(Parser recognizer)  {
    	log.trace(TAG+": RECOVER-INLINE");
    	recordError(recognizer,new InputMismatchException(recognizer));
    	return super.recoverInline(recognizer);
    }
    
    /**
	 * This appears to be a top-level view of things...
	 */
    @Override
    public void reportError(Parser recognizer, RecognitionException e) {
    	log.trace(TAG+": REPORT-ERROR");
    	recordError(recognizer,e);
    }


    /** Make sure we don't attempt to recover from problems in sub-rules. */
    @Override
    public void sync(Parser recognizer) { }
    
    protected void recordError(Recognizer<?,?> recognizer, RecognitionException re) {
    	// In each case the expected tokens are an expression. Don't bother to list
    	
    	Token offender = re.getOffendingToken();
    	if( offender != null ) {
    		String msg = String.format("Mismatch col %d: expecting an expression, got \'%s\'",offender.getStartIndex(),offender.getText());
    		log.info(TAG+msg);
    		sharedDictionary.put(TestFrameProperties.EXPR_ERR_MESSAGE, msg);
    		sharedDictionary.put(TestFrameProperties.EXPR_ERR_LINE, Integer.toString(offender.getLine()));
    		sharedDictionary.put(TestFrameProperties.EXPR_ERR_POSITION,Integer.toString(offender.getCharPositionInLine()));
    		sharedDictionary.put(TestFrameProperties.EXPR_ERR_TOKEN, offender.getText());
    	}
    	else {
    		String msg = "SYTNTAX ERROR: No information";
    		log.debug(TAG+msg);
    		sharedDictionary.put(TestFrameProperties.EXPR_ERR_MESSAGE, msg);
    	}
    }
}

