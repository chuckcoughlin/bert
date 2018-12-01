/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.logging;

public class SyslogLoggerFinder extends System.LoggerFinder {
	@Override
    public System.Logger getLogger(String name, Module module) {
        return new SyslogLogger(name);
    }
}
