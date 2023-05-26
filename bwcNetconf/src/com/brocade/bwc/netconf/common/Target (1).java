/*======================================================*/
// Module: Slave Structure
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0	  Initial Version
//
// Notes: quick and dirty
//
/*======================================================*/
package com.brocade.bwc.netconf.common;

public class Target {

    private String host = "";
    private int port = Constants.BROCADE_NETCONF_SSH_PORT;

    public Target(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public Target(String host) {
        this.host = host;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

}
