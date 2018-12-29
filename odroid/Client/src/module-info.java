/**
 *  NOTE: "requires" are module names, "exports" are packages
 */
module bert.client {
	requires bert.share;
	requires bert.speech;
	exports bert.client.model;
	exports bert.client.main;

}