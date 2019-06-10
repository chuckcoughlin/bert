/**
 * 
 */
module bert.server {
	requires transitive bert.share;
	requires transitive bert.motor;
	requires transitive bert.control;
	exports bert.server.main;
	exports bert.server.model;
}