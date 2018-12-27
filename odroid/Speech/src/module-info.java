/**
 *
 */
module bert.speech {
	requires transitive java.logging;
	requires antlr.runtime;
	requires transitive bert.share;
	
	exports bert.speech.antlr;
	exports bert.speech.process;

}