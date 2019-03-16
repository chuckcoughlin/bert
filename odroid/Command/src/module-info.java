/**
 *  NOTE: "requires" are module names, "exports" are packages
 */
module bert.command {
	requires transitive  bert.share;
	requires transitive bluez.library;
	requires bert.speech;
	requires bert.sql;
	exports bert.command.model;
	exports bert.command.main;

}