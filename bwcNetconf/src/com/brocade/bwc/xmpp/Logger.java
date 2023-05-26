/*======================================================*/
// Module: XMPP Driver Logging manager
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0	  Initial Version
//
// Notes: quick and dirty
//
/*======================================================*/
package com.brocade.bwc.xmpp;

import com.brocade.bwc.netconf.common.Constants;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 *
 * @author lcowdrey
 */
public class Logger {

    private java.util.logging.Logger logger = null;
    private Handler consoleHandler = null;

    public Logger(Object callingClass) {
        this.logger = java.util.logging.Logger.getLogger(callingClass.getClass().getName());
    }

    protected void destroy() {
        if (this.consoleHandler != null) {
            this.consoleHandler.close();
        }
        if (this.logger != null) {

        }
    }

    protected void configureLogging(Object callingClass) {

        LogManager.getLogManager().reset();

        // Log everything from the rocks.xmpp package with level FINE or above to the console.
        consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINE);
        //consoleHandler.setFormatter(new LogFormatter());

        logger = java.util.logging.Logger.getLogger(Constants.s_module_name);
        this.logger.setLevel(Level.FINE);
        this.logger.addHandler(consoleHandler);
    }

}
