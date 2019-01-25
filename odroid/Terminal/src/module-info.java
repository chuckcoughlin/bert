/**
 *
 */
module bert.terminal {
	requires transitive bert.share;
	requires bert.speech;
	requires bert.sql;	
	exports bert.term.main;
	exports bert.term.model;

}