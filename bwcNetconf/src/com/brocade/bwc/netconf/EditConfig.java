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
import com.brocade.bwc.netconf.jnc.NodeSet;
import com.brocade.bwc.netconf.jnc.XMLParser;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;

public class EditConfig extends HttpServlet implements javax.servlet.Servlet {

    @Override
    // EDIT_CONFIG
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        CommonRPC rpc = new CommonRPC(getServletContext(), request, response) {
            @Override
            public int rpcTask(NetconfSession ncs, RequestResponse rr, int t) throws ServletException, IOException, JNCException {
                int httpStatusCode = 0;
                // the actual EDIT_CONFIG
                String reply = "";

                if (!rr.getRequest().isEmpty()) {
                    Element r = new XMLParser().parse(rr.getRequest());
                    if (ncs.hasCapability(Capabilities.VALIDATE_CAPABILITY)) {
                        try {
                            if (r != null) {
                                ncs.validate(t);
                                rr.setOK("datastore " + rr.getDataStoreName() + " valid");
                            }
                        } catch (JNCException jnce) {
                            if (jnce.errorCode == JNCException.SESSION_ERROR) {
                                rr.setOK("capability config validation not fully supported");
                            } else {
                                rr.setWarning("datastore " + rr.getDataStoreName() + " invalid");
                            }
                        }
                    }
                    NodeSet configTrees = new NodeSet();
                    Boolean xmlAdded = configTrees.add(r);
                    if (xmlAdded) {
                        if (ncs.hasCapability(Capabilities.ROLLBACK_ON_ERROR_CAPABILITY)) {
                            ncs.setErrorOption(NetconfSession.ROLLBACK_ON_ERROR);
                            rr.setOK("error option ROLLBACK_ON_ERROR requested");
                        } else {
                            rr.setOK("capability rollback on error not supported");
                        }

                        ncs.setTestOption(NetconfSession.TEST_THEN_SET);
                        rr.setOK("test option TEST_THEN_SET requested");

                        // check for Base:1.1 capabilitiy if REMOVE was specified
                        if (rr.getOperation() == NetconfSession.REMOVE) {
                            if (!ncs.hasCapability(Capabilities.NETCONF_BASE_CAPABILITY_11)) {
                                rr.setOK("capability NETCONF base 1.1 not supported, changing operation from REMOVE to DELETE");
                                rr.setOperation(NetconfSession.DELETE);
                            }
                        }

                        ncs.setDefaultOperation(rr.getOperation());
                        rr.setOK("operation " + rr.getOperationName() + " requested");

                        if (ncs.hasCapability(Capabilities.CANDIDATE_CAPABILITY)) {
                            ncs.lock(rr.getDataStore());
                            rr.setOK("datastore " + rr.getDataStoreName() + " now locked");
                            ncs.editConfig(rr.getDataStore(), configTrees);
                            rr.setOK("datastore " + rr.getDataStoreName() + " changes submitted");
                        } else {
                            rr.setOK("capability candidate not supported");
                            ncs.editConfig(configTrees);
                            rr.setOK("datastore running changes submitted");
                        }
                        if (ncs.hasCapability(Capabilities.CONFIRMED_COMMIT_CAPABILITY)) {
                            try {
                                ncs.confirmedCommit(60);
                                rr.setOK("capability confirmed commit set for 60");
                            } catch (JNCException jnce) {
                                if (jnce.errorCode == JNCException.SESSION_ERROR) {
                                    rr.setOK("capability confirmed commit not fully supported");
                                }
                            }
                        }
                        try {
                            if (ncs.hasCapability(Capabilities.CANDIDATE_CAPABILITY)) {
                                ncs.commit();
                                rr.setOK("datastore " + rr.getDataStoreName() + " changes saved");
                                ncs.unlock(rr.getDataStore());
                                rr.setOK("datastore " + rr.getDataStoreName() + " now unlocked");
                            }
                            rr.setStatusOk();
                        } catch (JNCException jnce) {
                            rr.setError("config rejected, discarding changes");
                            if (ncs.hasCapability(Capabilities.CANDIDATE_CAPABILITY)) {
                                ncs.discardChanges();
                                rr.setOK("datastore " + rr.getDataStoreName() + " discarded changes");
                                ncs.unlock(rr.getDataStore());
                                rr.setOK("datastore " + rr.getDataStoreName() + " now unlocked");
                            }
                            rr.setStatusFail();
                            rr.setException(jnce.toString());
                        }
                    } else {
                        rr.setError("Invalid XML config, skipping");
                        rr.setStatusFail();
                    }
                } else {
                    rr.setStatusFail();
                }
                //System.out.println("getConfig rpc-reply:\n" + getXMLResponse);
                httpStatusCode = 200;
                return httpStatusCode;
            }
        };

    }

}
