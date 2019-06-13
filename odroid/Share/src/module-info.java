/**
 * @author chuckc
 *
 */
module bert.share {
	requires transitive java.logging;
	requires transitive java.xml;
	requires com.fasterxml.jackson.databind;
	requires jdk.unsupported;
	
	exports bert.share.message;
	exports bert.share.common;
	exports bert.share.control;
	exports bert.share.controller;
	exports bert.share.logging;
	exports bert.share.model;
	exports bert.share.motor;
	exports bert.share.util;
	exports bert.share.xml;
}