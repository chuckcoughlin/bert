/**
 *
 */
module bert.speech {
	requires transitive java.logging;
	requires antlr.runtime;
	
	exports bert.speech.antlr;
	exports bert.speech.process;

}