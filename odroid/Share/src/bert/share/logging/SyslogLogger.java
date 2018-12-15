/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 * MIT License.
 * See tutorial at: https://www.baeldung.com/java-9-logging-api
 */
package bert.share.logging;


import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement a SLF4j custom logger that directs everything to syslog.
 * @author chuckc
 *
 */
public class SyslogLogger implements System.Logger {
	private final Logger logger;
	private final String name;

	public SyslogLogger(String name) {
		this.name = name;
		logger = LoggerFactory.getLogger(name);
	}

	@Override
	public String getName() {
		return "SyslogLogger";
	}

	@Override
	public boolean isLoggable(Level level) {
		switch (level) {
		case OFF:
			return false;
		case TRACE:
			return logger.isTraceEnabled();
		case DEBUG:
			return logger.isDebugEnabled();
		case INFO:
			return logger.isInfoEnabled();
		case WARNING:
			return logger.isWarnEnabled();
		case ERROR:
			return logger.isErrorEnabled();
		case ALL:
		default:
			return true;
		}
	}

	@Override
	public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
		if (!isLoggable(level)) {
			return;
		}

		switch (level) {
		case TRACE:
			logger.trace(msg, thrown);
			break;
		case DEBUG:
			logger.debug(msg, thrown);
			break;
		case INFO:
			logger.info(msg, thrown);
			break;
		case WARNING:
			logger.warn(msg, thrown);
			break;
		case ERROR:
			logger.error(msg, thrown);
			break;
		case ALL:
		default:
			logger.info(msg, thrown);
		}
	}

	@Override
	public void log(Level level, ResourceBundle bundle, String format, Object... params) {
		if (!isLoggable(level)) {
			return;
		}

		switch (level) {
		case TRACE:
			logger.trace(format, params);
			break;
		case DEBUG:
			logger.debug(format, params);
			break;
		case INFO:
			logger.info(format, params);
			break;
		case WARNING:
			logger.warn(format, params);
			break;
		case ERROR:
			logger.error(format, params);
			break;
		case ALL:
		default:
			logger.info(format, params);

		}
	}
}
