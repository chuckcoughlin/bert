/**
 * 
 */
module bert.server {
	requires transitive bert.share;
	requires transitive bert.motor;
	exports bert.server.main;
	exports bert.server.model;
	exports bert.server.timer;

}