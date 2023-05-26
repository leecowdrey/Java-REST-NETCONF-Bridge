/*======================================================*/
// Module: CopyConfig (Get,Edit,Update,Delete,Copy)
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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;

public class CopyConfig extends HttpServlet implements javax.servlet.Servlet {

    @Override
    // COPY_CONFIG
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        CommonRPC rpc = new CommonRPC(getServletContext(), request, response) {
            @Override
            public int rpcTask(NetconfSession ncs, RequestResponse rr, int t) throws ServletException, IOException, JNCException {
                int httpStatusCode = 0;
                // the actual COPY_CONFIG
                String reply = "";
                if (!rr.getURL().isEmpty()) {
                    ncs.copyConfig(rr.getDataStore(), rr.getURL());
                    rr.setReply(reply);
                    rr.setStatusOk();
                } else {
                    rr.setStatusFail();
                }
                //System.out.println("copyConfig rpc-reply:\n" + getXMLResponse);
                httpStatusCode = 200;
                return httpStatusCode;
            }
        };

    }

}
