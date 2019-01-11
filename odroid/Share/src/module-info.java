/**
 * @author chuckc
 *
 */
module bert.share {
	requires transitive java.logging;
	requires transitive java.xml;
	requires com.fasterxml.jackson.databind;
	
	exports bert.share.bottle;
	exports bert.share.common;
	exports bert.share.controller;
	exports bert.share.logging;
	exports bert.share.model;
	exports bert.share.motor;
	exports bert.share.util;
	exports bert.share.xml;
}