/**
 * @author chuckc
 *
 */
module chuckcoughlin.common {
	requires transitive java.xml;
	requires org.slf4j;
	provides java.lang.System.LoggerFinder
		with chuckcoughlin.bert.logging.SyslogLoggerFinder;
	exports chuckcoughlin.bert.common;
	exports chuckcoughlin.bert.logging;
	exports chuckcoughlin.bert.model;
	exports chuckcoughlin.bert.xml;
}