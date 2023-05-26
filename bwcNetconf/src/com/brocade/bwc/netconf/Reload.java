/*======================================================*/
// Module: Reload 
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0    Initial Version
//
// Notes: reload all parameters from PF database
//
/*======================================================*/
package com.brocade.bwc.netconf;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.brocade.bwc.netconf.common.ConnectionVerifier;
import com.brocade.bwc.netconf.common.Constants;
import javax.servlet.ServletContext;

public class Reload extends HttpServlet implements javax.servlet.Servlet {

    private String xmlDump(ServletContext context) {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        xml += "<netconf>";
        xml += "<session>";
        xml += "<id>" + Integer.toString((Integer) context.getAttribute("bwcNetconf.session.id")) + "</id>";
        xml += "</session>";
        xml += "<message>";
        xml += "<id>" + Integer.toString((Integer) context.getAttribute("bwcNetconf.message.id")) + "</id>";
        xml += "</message>";
        xml += "<reload>";
        xml += "<parameters>";
        xml += "<http-auth>";
        xml += "<realm>" + (String) context.getAttribute("bwcNetconf.http.auth.realm") + "</realm>";
        xml += "<username>" + (String) context.getAttribute("bwcNetconf.http.auth.username") + "</username>";
        xml += "<password>" + (String) context.getAttribute("bwcNetconf.http.auth.password") + "</password>";
        xml += "</http-auth>";
        xml += "<remote>";
        xml += "<ha>";
        String remoteHost = (String) context.getAttribute("bwcNetconf.ha.remote.host");
        if (remoteHost != null) {
            xml += "<host>" + (String) context.getAttribute("bwcNetconf.ha.remote.host") + "</host>";
        } else {
            xml += "<host/>";
        }
        xml += "<port>" + Integer.toString((Integer) context.getAttribute("bwcNetconf.ha.remote.port")) + "</port>";
        xml += "</ha>";
        xml += "<requests>";
        xml += "<allow>" + (String) context.getAttribute("bwcNetconf.http.permit.remote.requests") + "</allow>";
        xml += "</requests>";
        xml += "</remote>";
        xml += "</parameters>";
        xml += "</reload>";
        xml += "</netconf>";
        return xml;
    }

    @Override
    // reload
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletContext context = getServletContext();

        ConnectionVerifier cv = new ConnectionVerifier(request, Boolean.parseBoolean((String) context.getAttribute("bwcNetconf.http.permit.remote.requests")), (String) context.getAttribute("bwcNetconf.ha.remote.host"));
        if (cv.allowConnection()) {

            response.setContentType(Constants.s_mime_application_xml);
            PrintWriter out = response.getWriter();
            try {
                String xml = xmlDump(context);
                if (!xml.isEmpty()) {
                    out.println(xml);
                }
                response.setStatus(200);
            } finally {
                out.close();
            }
        } else {
            response.setStatus(403);
        }
    }
}
