/*======================================================*/
// Module: XMLLoadFromDisk
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

import java.io.File;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import java.io.*;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

public class XMLLoadFromDisk {

    private Document baseDoc = null;

    public XMLLoadFromDisk(String filename) throws FileNotFoundException, UnsupportedEncodingException, IOException, ParserConfigurationException, SAXException {

        final StringBuilder xmlStringBuilder = new StringBuilder();

        File file = new File(filename);
        if (file.exists() && file.canRead()) {
            FileInputStream fis = new FileInputStream(filename);
            InputStreamReader isr = new InputStreamReader(fis, Constants.CHARACTER_SET);
            try (Reader in = new BufferedReader(isr)) {
                int ch;
                while ((ch = in.read()) > -1) {
                    xmlStringBuilder.append(ch);
                }
            }
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            ByteArrayInputStream input = new ByteArrayInputStream(xmlStringBuilder.toString().getBytes(Constants.CHARACTER_SET));
            this.baseDoc = builder.parse(input);
            System.out.println(Constants.s_module_name + ": load from XML disk file: " + filename + " [" + Integer.toString(this.baseDoc.toString().length()) + "]");
        } else {
            System.out.println(Constants.s_module_name + ": does not exist or can not read from XML disk file: " + filename);
            xmlStringBuilder.append(Constants.XML_ENCODING);
        }
    }

    @Override
    public String toString() {
        DOMImplementationLS domImplementationLS = (DOMImplementationLS) this.baseDoc.getImplementation();
        LSSerializer lsSerializer = domImplementationLS.createLSSerializer();
        return lsSerializer.writeToString(this.baseDoc).trim();
    }

    public Document toDocument() {
        return this.baseDoc;
    }

}
