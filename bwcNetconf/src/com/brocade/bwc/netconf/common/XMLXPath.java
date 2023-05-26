/*======================================================*/
// Module: XMLXPath
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0	  Initial Version
//
// Notes: quick and dirty
//
// Usage:
//       
/*======================================================*/
package com.brocade.bwc.netconf.common;

import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.*;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

public class XMLXPath {

    private String docRootTag = Constants.BROCADE_XML_DOCROOT_TAG;
    private Document baseDoc = null;
    private Document newDoc = null;
    private String xpathFilter = Constants.BROCADE_XML_DOCROOT_XPATH;

    public XMLXPath(String base, String xpath, String docRootTag) throws ParserConfigurationException, UnsupportedEncodingException, SAXException, IOException, XPathExpressionException, TransformerConfigurationException, TransformerException {

        final XPath xPath = XPathFactory.newInstance().newXPath();
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();

        if (!docRootTag.isEmpty()) {
            this.docRootTag = docRootTag;
        }
        // xPath mangle to support temporary new document root wrapper
        if (!xpath.isEmpty()) {
            if (xpath.contentEquals(Constants.BROCADE_XML_DOCROOT_XPATH)) {
                this.xpathFilter = xpath;
            } else if (xpath.contains(Constants.BROCADE_XML_DOCROOT_TAG)) {
                this.xpathFilter = xpath;
            } else if (xpath.contentEquals("//*")) {
                this.xpathFilter = xpath;
            } else if (xpath.contains("//")) {
                this.xpathFilter = xpath;
            } else if (xpath.startsWith("/")) {
                this.xpathFilter = "/" + Constants.BROCADE_XML_DOCROOT_TAG + xpath;
            } else {
                this.xpathFilter = "/" + Constants.BROCADE_XML_DOCROOT_TAG + "/" + xpath;
            }
        }

        final StringBuilder xmlStringBuilder = new StringBuilder();
        xmlStringBuilder.append(Constants.XML_ENCODING);
        xmlStringBuilder.append("<");
        xmlStringBuilder.append(this.docRootTag);
        xmlStringBuilder.append(">");
        if (!base.isEmpty()) {
            xmlStringBuilder.append(base);
        }
        xmlStringBuilder.append("</");
        xmlStringBuilder.append(this.docRootTag);
        xmlStringBuilder.append(">");

        ByteArrayInputStream input = new ByteArrayInputStream(xmlStringBuilder.toString().getBytes(Constants.CHARACTER_SET));
        this.baseDoc = builder.parse(input);
        NodeList subNodeList = (NodeList) xPath.compile(this.xpathFilter).evaluate(this.baseDoc, XPathConstants.NODESET);

        this.newDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element newElement = this.newDoc.createElement(this.docRootTag);
        this.newDoc.appendChild(newElement);
        for (int i = 0; i < subNodeList.getLength(); i++) {
            Node node = subNodeList.item(i);
            Node copyNode = this.newDoc.importNode(node, true);
            newElement.appendChild(copyNode);
        }
    }

    @Override
    public String toString() {
        DOMImplementationLS domImplementationLS = (DOMImplementationLS) this.newDoc.getImplementation();
        LSSerializer lsSerializer = domImplementationLS.createLSSerializer();
        return lsSerializer.writeToString(this.newDoc).replace("<" + this.docRootTag + ">", "").replace("</" + this.docRootTag + ">", "").replace("<" + this.docRootTag + "/>", "").replaceAll("\\<\\?xml(.+?)\\?\\>", "").trim();
    }

    public Document baseDocument() {
        return this.baseDoc;
    }

    public Document toDocument() {
        return this.newDoc;
    }

}
