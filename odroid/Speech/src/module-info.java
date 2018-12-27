/**
 *
 */
module bert.speech {
	requires transitive java.logging;
	requires antlr.runtime;
	requires bert.share;
	
	exports bert.speech.antlr;
	exports bert.speech.process;

}