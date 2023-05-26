/*======================================================*/
// Module: XMLMerge
// Author: Lee Cowdrey
// Version: 1.0
// History:
// 1.0	  Initial Version
//
// Notes: quick and dirty
//
// Usage:
//        String newXML = new XMLMerge("<base/>","<base><delta>1</delta></base>").toString();
//
/*======================================================*/
package com.brocade.bwc.netconf.common;

import java.io.StringReader;
import java.util.Iterator;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class XMLMerge {

    private Document baseDoc = null;
    private Document deltaDoc = null;
    private Document mergedDoc = null;

    public XMLMerge(String base, String delta) {
        try {
            SAXReader saxReader = new SAXReader();
            this.baseDoc = saxReader.read(new StringReader(base));
            this.deltaDoc = saxReader.read(new StringReader(delta));
            this.mergedDoc = xml(baseDoc.asXML(), deltaDoc.asXML());
        } catch (DocumentException de) {
            throw new RuntimeException(Constants.s_module_name + ": XMLMerge exception", de);
        }
    }

    @Override
    public String toString() {
        String asXml = "";
        if (mergedDoc != null) {
            if (mergedDoc.hasContent()) {
                asXml = mergedDoc.asXML();
            }
        }
        return asXml;
    }

    public Document toDocument() {
        return mergedDoc;
    }

    private Document xml(String mergex, String mergement) throws DocumentException {
        Document docMain = DocumentHelper.parseText(mergex);
        Document docVice = DocumentHelper.parseText(mergement);

        Element rootVice = docVice.getRootElement();
        Iterator iter = rootVice.elementIterator();

        while (iter.hasNext()) {
            Element messageItem = (Element) iter.next();
            doc(docMain, messageItem, "insert");
        }

        return docMain;
    }

    private boolean doc(Document docDup, Element son, String value) throws DocumentException {
        boolean isDone = false;
        String parent = son.getParent().getPath();
        Element parentElement = (Element) docDup.getRootElement().selectSingleNode(parent);

        if (parentElement != null) {
            if (parentElement.selectSingleNode(son.getPath()) == null) {
                son.setParent(null);
                parentElement.add(son);
            } else if ((parentElement.attribute("action") != null)
                    && parentElement.attribute("action").getStringValue().equalsIgnoreCase(value)) {
                son.setParent(null);
                parentElement.add(son);
            }
        }

        Iterator iter = son.elementIterator();

        while (iter.hasNext()) {
            Element subMessageItem = (Element) iter.next();
            isDone = doc(docDup, subMessageItem, value);
        }

        return isDone;
    }

}
