/*======================================================*/
// Module: IO Subscriber stream duplicate
// Author: TailF
// Version: 1.0
// History:
// 1.0	  Initial Version
//
// Notes: quick and dirty
//
/*======================================================*/
package com.brocade.bwc.netconf.common;

import com.brocade.bwc.netconf.jnc.IOSubscriber;

public class IoSubscriber extends IOSubscriber {

    String devName;
    RequestResponse rr;

    /**
     * Constructor.
     *
     * @param devName The name of the device.
     */
    public IoSubscriber(RequestResponse rr, String devName) {
        super(false); // rawmode = false
        this.rr = rr;
        this.devName = devName;
    }

    /**
     * Constructor.
     *
     * @param devName The name of the device.
     * @param rawmode If true 'raw' text will appear instead of pretty formatted
     * XML.
     */
    public IoSubscriber(RequestResponse rr, boolean rawmode, String devName) {
        super(rawmode);
        this.rr = rr;
        this.devName = devName;
    }

    /**
     * Will get called as soon as we have input (data which is received).
     *
     * @param s Text being received
     */
    @Override
    public void input(String s) {
        rr.sshSetDebug(s);
        //System.out.print(Constants.s_module_name+": " + devName + "<<< " + s);
    }

    /**
     * Will get called as soon as we have output (data which is being sent).
     *
     * @param s Text being sent
     */
    @Override
    public void output(String s) {
        rr.sshSetDebug(s);
        //System.out.print(Constants.s_module_name+": " + devName + ">>> " + s);
    }
}
