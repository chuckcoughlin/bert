/**
 * 
 */
/**
 * @author chuckc
 *
 */
module chuckcoughlin.logging {
	requires org.slf4j;
	provides java.lang.System.LoggerFinder
		with chuckcoughlin.logging.SyslogLoggerFinder;
	exports chuckcoughlin.logging;
}