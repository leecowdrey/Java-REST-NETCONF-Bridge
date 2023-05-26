/*======================================================*/
// Module: GetConfig (Get,Edit,Update,Delete,Copy)
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
import com.brocade.bwc.netconf.jnc.JNCException;
import com.brocade.bwc.netconf.jnc.NetconfSession;
import com.brocade.bwc.netconf.jnc.NodeSet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;

public class Get extends HttpServlet implements javax.servlet.Servlet {

    @Override
    // GET
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        CommonRPC rpc = new CommonRPC(getServletContext(), request, response) {
            @Override
            public int rpcTask(NetconfSession ncs, RequestResponse rr, int t) throws ServletException, IOException, JNCException {
                int httpStatusCode = 0;
                // the actual GET
                String reply = "";
                if (!rr.getConfig().isEmpty()) {
                    NodeSet result = ncs.get(rr.getConfig());
                    reply = result.toXMLString();
                }
                rr.setReply(reply);
                rr.setStatusOk();
                httpStatusCode = 200;
                return httpStatusCode;
            }
        };
    }

}
