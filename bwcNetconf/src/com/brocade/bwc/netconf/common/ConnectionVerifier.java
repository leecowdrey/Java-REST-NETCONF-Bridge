/*======================================================*/
// Module: Inbound Connection Verifier
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0	  Initial Version
//
// Notes: quick and dirty
//
/*======================================================*/
package com.brocade.bwc.netconf.common;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;

public class ConnectionVerifier {

    private InetAddress remoteAddr = null;
    private Boolean allowRemote = false;
    private String remoteHostHA = null;
    private String url = null;

    public ConnectionVerifier(HttpServletRequest request, Boolean allowRemote, String remoteHostHA) {
        try {
            this.url = request.getRequestURI();
            this.remoteAddr = InetAddress.getByName(request.getRemoteAddr());
            this.allowRemote = allowRemote;
            if (remoteHostHA != null) {
                if (!remoteHostHA.isEmpty()) {
                    this.remoteHostHA = remoteHostHA;
                }
            }
        } catch (UnknownHostException ex) {
            this.remoteAddr = null;
        }
    }

    public ConnectionVerifier(InetAddress inetAddress, Boolean allowRemote, String remoteHostHA) {
        this.remoteAddr = inetAddress;
        this.allowRemote = allowRemote;
        if (remoteHostHA != null) {
            if (!remoteHostHA.isEmpty()) {
                this.remoteHostHA = remoteHostHA;
            }
        }
    }

    private boolean isLocal() {
        boolean result = false;
        if (remoteAddr.isLoopbackAddress()) {
            result = true;
        } else {
            boolean nicIpMatch = false;
            try {
                // scan all local interfaces
                for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                    for (InetAddress nicIpAddress : Collections.list(nic.getInetAddresses())) {
                        if (remoteAddr.getHostAddress().equalsIgnoreCase(nicIpAddress.getHostAddress())) {
                            result = true;
                            nicIpMatch = true;
                            break;
                        }
                    }
                    if (nicIpMatch) {
                        break;
                    }
                }
            } catch (SocketException se) {
                result = false;
            }
        }
        return result;
    }

    private boolean isRemote() {
        return !isLocal();
    }

    private boolean isRemoteHA() {
        boolean result;
        if (this.remoteHostHA != null) {
            if (!this.remoteHostHA.isEmpty()) {
                try {
                    if (!this.remoteHostHA.equalsIgnoreCase("0.0.0.0")) {
                        InetAddress remoteHost = InetAddress.getByName(this.remoteHostHA);
                        result = remoteAddr.getHostAddress().equalsIgnoreCase(remoteHost.getHostAddress());
                    } else {
                        result = false;
                    }
                } catch (UnknownHostException ex) {
                    result = false;
                }
            } else {
                result = false;
            }
        } else {
            result = false;
        }
        return result;
    }

    public boolean allowConnection() {
        boolean result = false;
        System.out.println(Constants.s_module_name + ": connection from " + this.remoteAddr.getHostAddress() + " requesting " + this.url);
        if (isRemote()) {
            if (allowRemote) {
                result = true;
                System.out.println(Constants.s_module_name + ": connection from remote host " + this.remoteAddr.getHostAddress() + " permitted");
            }
            if (isRemoteHA()) {
                System.out.println(Constants.s_module_name + ": connection permitted from defined HA host " + this.remoteAddr.getHostAddress());
                result = true;
            }
        } else if (isLocal()) {
            System.out.println(Constants.s_module_name + ": connection from local host permitted");
            result = true;
        }
        if (!result) {
            System.out.println(Constants.s_module_name + ": connection from remote host " + this.remoteAddr.getHostAddress() + " is not permitted");
        }
        return result;
    }

}
