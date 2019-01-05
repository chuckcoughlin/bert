/**
 *
 */
module bert.speech {
	requires transitive java.logging;
	requires transitive antlr.runtime;
	requires transitive bert.share;
	
	exports bert.speech.antlr;
	exports bert.speech.process;

}