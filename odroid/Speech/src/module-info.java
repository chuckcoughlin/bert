/**
 *
 */
module bert.speech {
	requires transitive java.logging;
	requires transitive org.antlr.antlr4.runtime;
	requires transitive bert.share;
	
	exports bert.speech.antlr;
	exports bert.speech.process;

}