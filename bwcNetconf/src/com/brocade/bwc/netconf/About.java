/*======================================================*/
// Module: GetConfig (Get,Edit,Update,Delete,Copy)
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0    Initial Version
//
// Notes: quick and dirty
//
/*======================================================*/
package com.brocade.bwc.netconf;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.brocade.bwc.netconf.common.ConnectionVerifier;
import com.brocade.bwc.netconf.common.Constants;
import com.brocade.bwc.netconf.common.HttpAuth;

public class About extends HttpServlet implements javax.servlet.Servlet {

    private HttpAuth authorization = new HttpAuth();

    @Override
    // GET
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Ensures clients dont cache the results
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.

        ConnectionVerifier cv = new ConnectionVerifier(request, Boolean.parseBoolean((String) getServletContext().getAttribute("bwcNetconf.http.permit.remote.requests")), (String) getServletContext().getAttribute("bwcNetconf.ha.remote.host"));
        if (cv.allowConnection()) {
            authorization.initialize(getServletContext());
            try {
                Enumeration headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = (String) headerNames.nextElement();
                    String headerValue = request.getHeader(headerName);
                    if (headerName.equalsIgnoreCase(Constants.s_header_authorization)) {
                        authorization.validate(headerName, headerValue);
                    }
                }

                if (authorization.isAuthorized()) {
                    response.setContentType(com.brocade.bwc.netconf.common.Constants.HTML_FORMAT);
                    PrintWriter out = response.getWriter();
                    try {
                        out.println("<!DOCTYPE html>"
                                + "<html>"
                                + "<head>"
                                + "<title>"
                                + com.brocade.bwc.netconf.common.Constants.s_pkg_description
                                + "</title>"
                                + "</head>"
                                + "<body>"
                                + "<pre>"
                                + "\n"
                                + com.brocade.bwc.netconf.common.Constants.BROCADE_BANNER
                                + "\n"
                                + "                          "
                                + com.brocade.bwc.netconf.common.Constants.BROCADE_COPYRIGHT
                                + "\n"
                                + "</pre>"
                                + "</body>"
                                + "</html>");
                        response.setStatus(200);
                    } finally {
                        out.close();
                    }
                } else {
                    response.setStatus(401);
                    response.addHeader(Constants.s_header_www_authenticate, authorization.getAuthType() + " realm=\"" + authorization.getRealm() + "\"");
                    System.out.println(Constants.s_module_name + ": unauthorized connection from " + request.getRemoteAddr());
                }

            } finally {

            }
        } else {
            response.setStatus(403);
            //System.out.println(Constants.s_module_name+": client " + request.getRemoteAddr() + " is remote and not permitted");
        }
    }
}
