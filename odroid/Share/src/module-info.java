/**
 * @author chuckc
 *
 */
module bert.share {
	requires transitive java.logging;
	requires transitive java.xml;
	requires com.fasterxml.jackson.databind;
	requires transitive jssc;
	requires jdk.unsupported;
	exports bert.share.message;
	exports bert.share.common;
	exports bert.share.controller;
	exports bert.share.model;
	exports bert.share.util;
	exports bert.share.xml;
}