/**
 * @author chuckc
 *
 */
module chuckcoughlin.common {
	requires java.xml;
	requires org.slf4j;
	provides java.lang.System.LoggerFinder
		with chuckcoughlin.bert.logging.SyslogLoggerFinder;
	exports chuckcoughlin.bert.common;
	exports chuckcoughlin.bert.logging;
}