/*======================================================*/
// Module: Common NETCONF operation code
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0	  Initial Version
//
// Notes: quick and dirty
//
/*======================================================*/
package com.brocade.bwc.netconf.common;

import com.brocade.bwc.netconf.jnc.JNCException;
import com.brocade.bwc.netconf.jnc.NetconfSession;
import com.brocade.bwc.netconf.jnc.SSHConnection;
import com.brocade.bwc.netconf.jnc.SSHSession;
import javax.servlet.http.HttpServlet;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import com.brocade.bwc.netconf.json.JSONArray;
import com.brocade.bwc.netconf.json.JSONException;
import com.brocade.bwc.netconf.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import javax.servlet.ServletContext;

public class CommonRPC extends HttpServlet implements javax.servlet.Servlet {

    private int sessionId = 1;
    private int messageId = 1;
    private Boolean DEBUG = false;

    // the specific code behind each support NETCONF operation is provided by class override
    public int rpcTask(NetconfSession ncs, RequestResponse rr, int t) throws ServletException, IOException, JNCException {
        int httpStatusCode = 0;

        return httpStatusCode;
    }

    private int runRPC(ServletContext context, HttpServletRequest request, HttpServletResponse response, RequestResponse rr) throws ServletException, IOException {
        int httpStatusCode = 0;
        SSHConnection c = null;
        SSHSession ssh = null;
        NetconfSession ncs = null;

        if (!rr.bypassEnabled()) {
            boolean targetHostFailed = false;
            for (int t = 0; t < rr.getTargetsSize(); t++) {
                if (!rr.getHost(t).isEmpty()) {
                    IoSubscriber ios = new IoSubscriber(rr, true, rr.getHost(t));
                    try {
                        c = new SSHConnection(rr.getHost(t), rr.getPort(t), rr.getSshStricHosts());
                        if (rr.getSshPrivateKey().isEmpty()) {
                            //progressUpdate(progressIdx++, "SSH Plaintext Authentication: " + targetUsername + "@" + targetHost + ":" + Integer.toString(targetPort));
                            c.authenticateWithPassword(rr.getUsername(), rr.getPassword());
                        } else {
                            //progressUpdate(progressIdx++, "SSH Public Key Authentication: " + targetUsername + "@" + targetHost + ":" + Integer.toString(targetPort));
                            c.authenticateWithPublicKey(rr.getUsername(), rr.getSshPrivateKey().toCharArray(), rr.getSshPassphrase());
                        }

                        //progressUpdate(progressIdx++, "SSH Opening session: " + targetHost + ":" + Integer.toString(targetPort));
                        ssh = new SSHSession(c);

                        //progressUpdate(progressIdx++, "SSH Requesting netconf subsystem");
                        if (ssh != null) {
                            if (!ssh.serverSideClosed()) {
                                ssh.setReadTimeout(rr.getSshReadTimeOut());
                                rr.setOK("target host " + rr.getHost(t) + ":" + Integer.toString(rr.getPort(t)) + " connected");
                                if (rr.isSshDebugEnabled()) {
                                    ssh.addSubscriber(ios);
                                }
                                ncs = new NetconfSession(ssh);
                                ncs.message_id = messageId;
                                rr.setOK("target host " + rr.getHost(t) + ":" + Integer.toString(rr.getPort(t)) + " NETCONF established");
                                targetHostFailed = false;
                                context.setAttribute("bwcNetconf.session.id", Integer.toString(ncs.sessionId));
                                rr.setSessionId(ncs.sessionId);

                                // specific operation override
                                httpStatusCode = rpcTask(ncs, rr, t);

                                if (rr.isSshDebugEnabled()) {
                                    ssh.delSubscriber(ios);
                                }
                            } else {
                                //progressUpdate(progressIdx++, "SSH Failed Authentication: " + targetUsername + "@" + targetHost + ":" + Integer.toString(targetPort));
                                rr.setStatusFail();
                                httpStatusCode = 511;
                                targetHostFailed = true;
                            }

                        } else {
                            //progressUpdate(progressIdx++, "SSH Failed Authentication: " + targetUsername + "@" + targetHost + ":" + Integer.toString(targetPort));
                            rr.setStatusFail();
                            httpStatusCode = 503;
                            targetHostFailed = true;
                        }

                    } catch (IOException ioe) {
                        httpStatusCode = 504;
                        rr.setException(ioe.toString());
                    } catch (JNCException jnce) {
                        if (jnce.errorCode == JNCException.TIMEOUT_ERROR) {
                            httpStatusCode = 504;
                            rr.setException(jnce);
                        } else if (jnce.errorCode == JNCException.SSH_TARGET_NOT_AVAILABLE) {
                            targetHostFailed = true;
                        } else if (jnce.errorCode == JNCException.SSH_HOSTKEY_INVALID) {
                            targetHostFailed = true;
                        } else {
                            httpStatusCode = 502;
                            rr.setException(jnce);
                        }
                    } finally {
                        if (ncs != null) {
                            try {
                                ncs.closeSession();
                                rr.setOK("target host " + rr.getHost(t) + ":" + Integer.toString(rr.getPort(t)) + " NETCONF terminated");
                            } catch (JNCException ex) {

                            }
                        }
                        if (ssh != null) {
                            ssh.close();
                            rr.setOK("target host " + rr.getHost(t) + ":" + Integer.toString(rr.getPort(t)) + " disconnected");
                        }
                        if (c != null) {
                            c.close();
                        }
                    }

                } else {
                    rr.setStatusFail();
                    httpStatusCode = 411;
                    System.out.println(Constants.s_module_name + ": client " + request.getRemoteAddr() + " no host specified");
                }
                if (targetHostFailed) {
                    rr.setWarning("target host " + rr.getHost(t) + ":" + Integer.toString(rr.getPort(t)) + " not available, trying next slave");
                } else {
                    break;
                }
            }
        } else {
            rr.setOK("bypass in operation");
            rr.setStatusOk();
        }
        rr.updateResponse();
        return httpStatusCode;
    }

    public CommonRPC(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        final RequestResponse rr = new RequestResponse(context);
        final HttpAuth authorization = new HttpAuth();
        String acceptContentType = "";

        BufferedReader input = request.getReader();
        StringBuilder content = new StringBuilder();
        String buffer = "";

        DEBUG = (Boolean) context.getAttribute("bwcNetconf.debug");

        ConnectionVerifier cv = new ConnectionVerifier(request, Boolean.parseBoolean((String) context.getAttribute("bwcNetconf.http.permit.remote.requests")), (String) context.getAttribute("bwcNetconf.ha.remote.host"));
        if (cv.allowConnection()) {
            authorization.initialize(context);

            if (request.getContentType().contains(Constants.s_mime_application_json)) {
                //System.out.println(Constants.s_module_name+": connection from " + request.getRemoteAddr() + " Content-Type=" + request.getContentType());

                // get HTTP BODY contents
                while ((buffer = input.readLine()) != null) {
                    content.append(buffer);
                }
                if (DEBUG) {
                    System.out.println(Constants.s_module_name + ": request body follows:");
                    System.out.println(content.toString());
                }

                try {
                    Enumeration headerNames = request.getHeaderNames();
                    while (headerNames.hasMoreElements()) {
                        String headerName = (String) headerNames.nextElement();
                        String headerValue = request.getHeader(headerName);
                        if (DEBUG) {
                            System.out.println(Constants.s_module_name + ": request HTTP header: " + headerName + "=" + headerValue);
                        }
                        if (headerName.equalsIgnoreCase(Constants.s_header_authorization)) {
                            authorization.validate(headerName, headerValue);
                        } else if (headerName.equalsIgnoreCase(Constants.s_header_accept)) {
                            acceptContentType = headerValue;
                        }
                    }

                    if (authorization.isAuthorized()) {
                        System.out.println(Constants.s_module_name + ": authorized connection from " + request.getRemoteAddr());
                        //payload:
                        if (content.length() > 0) {
                            try {
                                try {
                                    JSONObject jsonRequest = new JSONObject(content.toString().trim());
                                    if (jsonRequest.has("request")) {
                                        JSONArray requestsJSON = jsonRequest.getJSONArray("request");
                                        messageId = Integer.parseInt((String) context.getAttribute("bwcNetconf.message.id"));
                                        for (int i = 0; i < requestsJSON.length(); i++) {
                                            JSONObject requestJSON = requestsJSON.getJSONObject(i);
                                            rr.prepare();
                                            rr.parse(requestJSON);
                                            messageId++;
                                            rr.setMessageId(messageId);
                                            context.setAttribute("bwcNetconf.message.id", Integer.toString(messageId));

                                            if (rr.isCallbackRequired()) {
                                                if (DEBUG) {
                                                    System.out.println(Constants.s_module_name + ": callback requested from " + request.getRemoteAddr() + " for [" + rr.getcallbackIdentifier() + "] to " + rr.getCallbackURL().toString());
                                                }
                                                rr.setOK("callback requested");
                                                rr.setStatusOk();
                                                response.setStatus(202);
                                                //drag in custom async authorization HTTP header and value if specified
                                                rr.setAsyncAuthorizationHeaderName((String) context.getAttribute("bwcNetconf.http.callback.authorization.header.name"));
                                                rr.setAsyncAuthorizationHeaderValue((String) context.getAttribute("bwcNetconf.http.callback.authorization.header.value"));
                                                //
                                                ExecutorService es = (ExecutorService) context.getAttribute("bwcNetconf.http.callback.thread.pool");
                                                try {
                                                    es.submit(() -> {
                                                        try {
                                                            int threadhHtpStatusCode = runRPC(context, request, response, rr);
                                                        } catch (ServletException ex) {
                                                            response.setStatus(400);
                                                            rr.setException(ex.toString());
                                                        } catch (IOException ex) {
                                                            response.setStatus(400);
                                                            rr.setException(ex.toString());
                                                        } finally {
                                                            HttpCallback httpCallback = new HttpCallback(rr, DEBUG);
                                                            httpCallback.post();
                                                            httpCallback.close();
                                                        }
                                                    });
                                                } catch (RejectedExecutionException ex) {
                                                    // cant add request to executor service so switch back to sync and continue
                                                    System.out.println(Constants.s_module_name + ": async pool operation failed " + request.getRemoteAddr() + " for [" + rr.getcallbackIdentifier() + "] to " + rr.getCallbackURL().toString() + ", exception: " + ex.toString());
                                                    System.out.println(Constants.s_module_name + ": async operation " + request.getRemoteAddr() + " for [" + rr.getcallbackIdentifier() + "] to " + rr.getCallbackURL().toString() + ", running synchronously");
                                                    int httpStatusCode = runRPC(context, request, response, rr);
                                                    response.setStatus(httpStatusCode);
                                                }
                                            } else {
                                                int httpStatusCode = runRPC(context, request, response, rr);
                                                response.setStatus(httpStatusCode);
                                            }
                                        }
                                    }
                                } catch (JSONException jse) {
                                    response.setStatus(400);
                                    rr.setException(jse.toString());
                                }

                            } catch (JSONException jse) {
                                response.setStatus(400);
                                rr.setException(jse.toString());
                            } finally {

                            }
                        } else {
                            rr.setStatusFail();
                            response.setStatus(411);
                        }
                    } else {
                        rr.setStatusFail();
                        response.setStatus(401);
                        response.addHeader(Constants.s_header_www_authenticate, authorization.getAuthType() + " realm=\"" + authorization.getRealm() + "\"");
                        System.out.println(Constants.s_module_name + ": unauthorized connection from " + request.getRemoteAddr());
                    }
                } finally {

                }

            } else {
                response.setStatus(400);
                System.out.println(Constants.s_module_name + ": client " + request.getRemoteAddr() + " bad body Content-Type=" + request.getContentType());
            }

        } else {
            response.setStatus(403);
            //System.out.println(Constants.s_module_name+": client " + request.getRemoteAddr() + " is remote and not permitted");
        }

        if (authorization.isAuthorized()) {
            PrintWriter out = response.getWriter();
            try {
                if (acceptContentType.equalsIgnoreCase(Constants.s_mime_application_json)) {
                    response.setContentType(Constants.s_mime_application_json);
                    out.println(rr.getResponseJSON().toString());
                    if (DEBUG) {
                        System.out.println(Constants.s_module_name + ": response body follows:");
                        System.out.println(rr.getResponseJSON().toString());
                    }
                } else if (acceptContentType.equalsIgnoreCase(Constants.s_mime_application_xml)) {
                    response.setContentType(Constants.s_mime_application_xml);
                    out.println(rr.getResponseXML());
                    if (DEBUG) {
                        System.out.println(Constants.s_module_name + ": response body follows:");
                        System.out.println(rr.getResponseXML());
                    }
                } else {
                    response.setStatus(406);
                    if (DEBUG) {
                        System.out.println(Constants.s_module_name + ": response HTTP status code 406");
                    }

                }
            } finally {
                out.close();
            }
            System.out.println(Constants.s_module_name + ": client " + request.getRemoteAddr() + " request completed");
        }
    }
}
