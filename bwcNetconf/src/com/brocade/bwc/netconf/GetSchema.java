/*======================================================*/
// Module: GetSchema
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0   Initial Version
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;

public class GetSchema extends HttpServlet implements javax.servlet.Servlet {

    @Override
    // GET_SCHEMA
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        CommonRPC rpc = new CommonRPC(getServletContext(), request, response) {

            private static final String s_y = "Y";
            private static final String s_n = "N";
            private static final String s_status_ok = "OK";
            private static final String s_status_fail = "FAIL";
            private static final String s_status_warning = "WARNING";
            private static final String s_status_exception = "EXCEPTION";

            String extractMatched(Pattern pattern, String tag, String stream, int occurence) {
                String matchedContent = null;
                int offset = -1;

                if (stream.length() > 0) {
                    Matcher matcher = pattern.matcher(stream);
                    int occurences = 0;
                    while (matcher.find()) {
                        occurences++;
                        if (occurences == occurence) {
                            offset = matcher.start();
                            break;
                        }
                    }

                    if (offset >= 0) {
                        matchedContent = "";
                        int brace = 0;
                        boolean quitLoop = false;
                        for (int i = offset; i < stream.length() && !quitLoop; i++) {
                            char piece = stream.charAt(i);
                            if (piece == '{') {
                                brace++;
                            } else if (piece == '}') {
                                brace--;
                                if (brace == 0) {
                                    quitLoop = true;
                                }
                            }
                            matchedContent += piece;
                        }
                        // if braces != 0 then empty what we may have extracted as not complete
                        if (brace != 0) {
                            matchedContent = null;
                        }
                    }
                }
                return matchedContent;
            }

            class capabilityRecord {

                String existed = s_n;
                String status = s_status_fail;
            }

            private capabilityRecord addCapability(Capabilities.Capa cap) throws JNCException {
                capabilityRecord cr = new capabilityRecord();
                return cr;
            }

            public String updateCapability(NetconfSession ncs, RequestResponse rr, Capabilities.Capa cap) throws JNCException, IOException {
                boolean proceed = true;
                String capabilityReply = "";

                if (!cap.hasIdentifier()) {
                    rr.setWarning("ignoring capability - missing identifier, URI:" + cap.getURI());
                    proceed = false;
                } else if (!cap.hasURI()) {
                    rr.setWarning("ignoring capability - missing URI, identifier:" + cap.getIdentifier());
                    proceed = false;
                }

                if (proceed) {
                    if (cap.getURI().equalsIgnoreCase("undefined")) {
                        rr.setWarning("ignoring capability - URI contains undefined tag, identifier:" + cap.getIdentifier() + " URI:" + cap.getURI());
                        proceed = false;
                    }
                }

                if (proceed) {
                    if (cap.hasRevision()) {
                        rr.setOK("capability:" + cap.getIdentifier() + ", URI:" + cap.getURI() + " revision:" + cap.getRevision());
                    } else {
                        rr.setOK("capability:" + cap.getIdentifier() + ", URI:" + cap.getURI());
                    }
                    capabilityRecord cr = new capabilityRecord();
                    // add or update capability

                    cr = addCapability(cap);

                    String yangModule = null;
                    int yangClobLength = 0;
                    String moduleName = null;
                    int moduleId = 0;
                    String yangDefinition = null;
                    String yinDefinition = null;

                    /*appendAttributeClob(rpcReplyObjectId, "CLOB_(NETCONF) RPC", "<get-schema xmlns=\"" + Capabilities.NS_MONITORING + "\">\n"
                     + " <identifier>" + cap.getIdentifier() + "</identifier>\n"
                     + " <format>yang</format>\n"
                     + "</get-schema>\n");
                     */
                    try {
                        yangDefinition = ncs.getSchema(cap.getIdentifier(), cap.getRevision(), "yang");
                        if (yangDefinition != null) {
                            if (yangDefinition.length() > 0) {
                                // process all modules BEFORE any SUBMODULES
                                boolean endOfModules = false;
                                int moduleIdx = 0;
                                while (!endOfModules) {
                                    moduleIdx++;
                                    String newModule = extractMatched(Pattern.compile("module(?!submodule)\\s{1,}([a-zA-Z0-9_\\\" -]){1,}\\s{0,}\\{"), "module", yangDefinition, moduleIdx);
                                    if (newModule == null) {
                                        endOfModules = true;
                                    } else {
                                        capabilityReply += "<module name=\"" + cap.getIdentifier() + "\"uri=\"" + cap.getURI() + "\" revision=\"" + cap.getRevision() + "\" format=\"yang\" length=\"" + Integer.toString(yangDefinition.length()) + "\"";
                                        capabilityReply += yangDefinition;
                                        capabilityReply += "</module";
                                    }
                                }
                            }
                            boolean endOfSubModules = false;
                            int subModuleIdx = 0;
                            while (!endOfSubModules) {
                                subModuleIdx++;
                                String newSubModule = extractMatched(Pattern.compile("submodule\\s{1,}([a-zA-Z0-9_\\\" -]){1,}\\s{0,}\\{"), "submodule", yangDefinition, subModuleIdx);
                                if (newSubModule == null) {
                                    endOfSubModules = true;
                                } else {
                                    capabilityReply += "<submodule name=\"" + cap.getIdentifier() + "\"uri=\"" + cap.getURI() + "\" revision=\"" + cap.getRevision() + "\" format=\"yang\" length=\"" + Integer.toString(yangDefinition.length()) + "\"";
                                    capabilityReply += yangDefinition;
                                    capabilityReply += "</submodule";
                                }
                            }
                        }
                    } catch (JNCException jnce) {
                        if (jnce.errorCode == JNCException.MODULE_DEFINITION_INVALID || jnce.errorCode == JNCException.MODULE_SCHEMA_NOT_FOUND) {
                            rr.setWarning("capability " + cap.getIdentifier() + " not available");
                        } else {
                            throw jnce;
                        }
                    }
                    /*
                     appendAttributeClob(rpcReplyObjectId, "CLOB_(NETCONF) RPC REPLY", "<data xmlns=\"" + com.tailf.jnc.Capabilities.NS_MONITORING + "\">\n"
                     + " <identifier>" + cap.getIdentifier() + "</identifier>\n"
                     + " <format>yang</format>\n"
                     + " <xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
                     + rpcReplyGetSchema
                     + " </xs:schema>\n"
                     + "</data>\n");
                     */
                    // need to relink modules if required
                }
                return capabilityReply;
            }

            @Override
            public int rpcTask(NetconfSession ncs, RequestResponse rr, int t) throws ServletException, IOException, JNCException {
                int httpStatusCode = 0;
                String xmlReply = "";
                // the actual GET_SCHEMA
                if (ncs.hasCapability(Capabilities.NS_MONITORING)) {
                    xmlReply += "<schema>";
                    Capabilities capabilities = ncs.getCapabilities();
                    for (Capabilities.Capa cap : capabilities.capas) {
                        if (cap.hasIdentifier() && cap.hasURI()) {
                            xmlReply += updateCapability(ncs, rr, cap);
                        }
                    }
                    for (Capabilities.Capa cap : capabilities.data_capas) {
                        if (cap.hasIdentifier() && cap.hasURI()) {
                            xmlReply += updateCapability(ncs, rr, cap);
                        }
                    }
                    xmlReply += "</schema>";
                    rr.setStatusOk();
                    rr.setReply(xmlReply);
                } else {
                    rr.setStatusFail();
                    rr.setException("host " + rr.getHost(t) + " does not provide capability " + Capabilities.NS_MONITORING);
                }
                httpStatusCode = 200;
                return httpStatusCode;
            }
        };
    }

}
