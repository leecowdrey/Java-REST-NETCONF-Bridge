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
import com.brocade.bwc.netconf.common.Constants;
import com.brocade.bwc.netconf.common.RequestResponse;
import com.brocade.bwc.netconf.common.XMLXPath;
import com.brocade.bwc.netconf.jnc.Capabilities;
import com.brocade.bwc.netconf.jnc.JNCException;
import com.brocade.bwc.netconf.jnc.NetconfSession;
import com.brocade.bwc.netconf.jnc.NodeSet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import java.io.UnsupportedEncodingException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.xml.sax.SAXException;

public class GetConfig extends HttpServlet implements javax.servlet.Servlet {

    @Override
    // GET_CONFIG
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        CommonRPC rpc = new CommonRPC(getServletContext(), request, response) {

            @Override
            public int rpcTask(NetconfSession ncs, RequestResponse rr, int t) throws ServletException, IOException, JNCException {
                int httpStatusCode = 0;
                // the actual GET_CONFIG
                String reply = "";
                if (!rr.getConfig().isEmpty()) {
                    if (ncs.hasCapability(Capabilities.XPATH_CAPABILITY)) {
                        NodeSet result = ncs.getConfig(rr.getDataStore(), rr.getConfig());
                        reply = result.toXMLString();
                    } else {
                        rr.setWarning("host " + rr.getHost(t) + " does not provide capability " + Capabilities.XPATH_CAPABILITY + ", performing XPath in-stream");
                        NodeSet result = ncs.getConfig(rr.getDataStore());
                        XMLXPath xmlPath;
                        try {
                            xmlPath = new XMLXPath(result.toXMLString(), rr.getConfig(), Constants.BROCADE_XML_DOCROOT_TAG);
                            reply = xmlPath.toString();
                        } catch (ParserConfigurationException ex) {
                            httpStatusCode = 504;
                            rr.setException(ex.toString());
                        } catch (UnsupportedEncodingException ex) {
                            httpStatusCode = 504;
                            rr.setException(ex.toString());
                        } catch (SAXException ex) {
                            httpStatusCode = 504;
                            rr.setException(ex.toString());
                        } catch (XPathExpressionException ex) {
                            httpStatusCode = 504;
                            rr.setException(ex.toString());
                        } catch (TransformerConfigurationException ex) {
                            httpStatusCode = 504;
                            rr.setException(ex.toString());
                        } catch (TransformerException ex) {
                            httpStatusCode = 504;
                            rr.setException(ex.toString());
                        }
                    }
                } else {
                    NodeSet result = ncs.getConfig(rr.getDataStore());
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
