/*======================================================*/
// Module: XMLSaveToDisk
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

public class XMLSaveToDisk {

    private Document baseDoc = null;

    public XMLSaveToDisk(String filename, String base, Boolean overwrite) throws ParserConfigurationException, UnsupportedEncodingException, SAXException, IOException {

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();

        final StringBuilder xmlStringBuilder = new StringBuilder();
        xmlStringBuilder.append(Constants.XML_ENCODING);
        if (!base.isEmpty()) {
            xmlStringBuilder.append(base);
        }

        ByteArrayInputStream input = new ByteArrayInputStream(xmlStringBuilder.toString().getBytes(Constants.CHARACTER_SET));
        this.baseDoc = builder.parse(input);

        // remove file if exists
        File file = new File(filename);
        if (file.exists()) {
            if (!overwrite) {
                System.out.println(Constants.s_module_name + ": XML disk file: " + filename + " already exists, removing prior to save");
                file.delete();
            }
        }
        Boolean fileCreated = file.createNewFile();
        System.out.println(Constants.s_module_name + ": XML disk file: " + filename + " created");
        if (file.canWrite()) {
            FileOutputStream fos = new FileOutputStream(filename);
            try (Writer out = new OutputStreamWriter(fos, Constants.CHARACTER_SET)) {
                out.append(this.baseDoc.toString());
                fos.flush();
                fos.close();
            }
            System.out.println(Constants.s_module_name + ": save to XML disk file: " + filename + " [" + Integer.toString(this.baseDoc.toString().length()) + "]");

        } else {
            System.out.println(Constants.s_module_name + ": can not write to XML disk file: " + filename);
        }
    }

}
