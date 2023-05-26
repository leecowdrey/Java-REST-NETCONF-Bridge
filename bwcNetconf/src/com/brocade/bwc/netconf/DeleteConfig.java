/*======================================================*/
// Module: DeleteConfig (Get,Edit,Update,Delete,Copy)
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
import com.brocade.bwc.netconf.jnc.JNCException;
import com.brocade.bwc.netconf.jnc.NetconfSession;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;

public class DeleteConfig extends HttpServlet implements javax.servlet.Servlet {

    @Override
    // DELETE_CONFIG
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        CommonRPC rpc = new CommonRPC(getServletContext(), request, response) {
            @Override
            public int rpcTask(NetconfSession ncs, RequestResponse rr, int t) throws ServletException, IOException, JNCException {
                int httpStatusCode = 0;
                // the actual DELETE_CONFIG
                if (ncs.hasCapability(Capabilities.CANDIDATE_CAPABILITY)) {
                    ncs.lock(rr.getDataStore());
                } else {
                    rr.setOK("capability candidate not supported");
                }
                ncs.deleteConfig(rr.getDataStore());
                if (ncs.hasCapability(Capabilities.CONFIRMED_COMMIT_CAPABILITY)) {
                    try {
                        ncs.confirmedCommit(60);
                    } catch (JNCException jnce) {
                        if (jnce.errorCode == JNCException.SESSION_ERROR) {
                            rr.setOK("capability confirmed commit not fully supported");
                        } else {
                            throw jnce;
                        }
                    }
                }
                ncs.commit();
                if (ncs.hasCapability(Capabilities.CANDIDATE_CAPABILITY)) {
                    ncs.unlock(rr.getDataStore());
                }
                rr.setStatusOk();
                httpStatusCode = 200;
                return httpStatusCode;
            }
        };

    }

}
