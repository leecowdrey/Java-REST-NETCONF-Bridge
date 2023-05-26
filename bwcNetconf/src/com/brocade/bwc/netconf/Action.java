/*======================================================*/
// Module: EditConfig
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0	  Initial Version
//
// Notes: quick and dirty
//
/*======================================================*/
package com.brocade.bwc.netconf;

import com.brocade.bwc.netconf.common.CommonRPC;
import com.brocade.bwc.netconf.common.RequestResponse;
import com.brocade.bwc.netconf.jnc.Capabilities;
import com.brocade.bwc.netconf.jnc.Element;
import com.brocade.bwc.netconf.jnc.JNCException;
import com.brocade.bwc.netconf.jnc.NetconfSession;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;

public class Action extends HttpServlet implements javax.servlet.Servlet {

    @Override
    // EDIT_CONFIG
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        CommonRPC rpc = new CommonRPC(getServletContext(), request, response) {
            @Override
            public int rpcTask(NetconfSession ncs, RequestResponse rr, int t) throws ServletException, IOException, JNCException {
                int httpStatusCode = 0;
                // check server has actions capability
                if (ncs.hasCapability(Capabilities.ACTIONS_CAPABILITY)) {
                    if (!rr.getRequest().isEmpty()) {
                        try {
                            ncs.actionRpc(rr.getRequest());
                            rr.setOK("action RPC request sent");
                            try {
                                Element reply = ncs.readReply();
                                if (reply.toXMLString().toLowerCase().contains("<rpc-error>")) {
                                    rr.setError("action RPC error reported");
                                    rr.setReply(reply.toXMLString());
                                    rr.setStatusFail();
                                } else {
                                    rr.setOK("action RPC accepted");
                                    rr.setReply(reply.toXMLString());
                                    rr.setStatusOk();
                                }
                            } catch (IOException ioe) {
                                rr.setOK("NETCONF session closed by server, no RPC reply available");
                                rr.setStatusOk();
                            }
                        } catch (JNCException jnce) {
                            rr.setError(jnce.toString());
                            rr.setStatusFail();
                        }
                    } else {
                        rr.setError("Invalid XML config, skipping");
                        rr.setStatusFail();
                    }
                } else {
                    rr.setError("capability NETCONF actions 1.0 not supported");
                    rr.setStatusFail();
                }
                httpStatusCode = 200;
                return httpStatusCode;
            }
        };

    }

}
