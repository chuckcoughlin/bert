/**
 *  NOTE: "requires" are module names, "exports" are packages
 */
module bert.command {
	requires bert.share;
	requires bert.speech;
	requires bert.sql;
	exports bert.command.model;
	exports bert.command.main;

}