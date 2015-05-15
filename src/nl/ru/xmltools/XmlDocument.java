/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Erwin
 */
public class XmlDocument {
  // This class uses a logger
  private static final Logger logger = Logger.getLogger(XmlDocument.class);
  // ======================== variables for internal use
  private DocumentBuilderFactory factory;
  private DocumentBuilder parser;
  private Document doc;
  private XPath xpath = XPathFactory.newInstance().newXPath();
  public enum XmlNodeType {
    Attribute, Comment, Document, Element, EndElement
  }
  // Instantiation of the class
  public XmlDocument() {
    try {
      this.parser = null;
      this.factory = DocumentBuilderFactory.newInstance();
      // Set up factory
      this.factory.setIgnoringComments(true);  // Ignore comments
      this.factory.setCoalescing(true);        // Convert CDATA to Text nodes
      this.factory.setNamespaceAware(false);   // No namespace (default)
      this.factory.setValidating(false);       // Don't validate DTD
    } catch (RuntimeException ex) {
      logger.error("XmlDocument class instantiation failed", ex);
    }
    try {
      this.parser = this.factory.newDocumentBuilder();
    } catch (ParserConfigurationException ex) {
      logger.error("XmlDocument: could not create documentbuilder", ex);
    }
  }  
  public void LoadXml(String sXmlText) throws IOException, SAXException {
    try {
      // Convert the string into a DOM document object
      InputSource is = new InputSource();
      String sWrap = "<wrap>" + sXmlText + "</wrap>";
      is.setCharacterStream(new StringReader(sWrap));
      this.doc = this.parser.parse(is);
      // this.doc = this.parser.parse(new InputSource(new StringReader(sWrap)));
      // logger.debug("LoadXml: " + doc.getFirstChild());
    }  catch (SAXException | IOException ex) {
      logger.error("Problem attempting LoadXml", ex);
    }
  }
  
  public Document getDocument() {
    return this.doc;
  }
  public XmlNode SelectSingleNode(String sXpath) throws XPathExpressionException {
    try {
      // Check for null
      if (this.doc == null) {logger.debug("SelectSingleNode on empty doc"); return null;}
      // Now try to find what is needed
      org.w3c.dom.Node ndResult = (org.w3c.dom.Node) xpath.compile(sXpath).evaluate(this.doc, XPathConstants.NODE);
      // XmlNode ndThis = new XmlNode();
      XmlNode ndThis = (XmlNode) ndResult.cloneNode(true);
      logger.debug(ndResult.getNodeType());
      return ndThis;
    } catch (XPathExpressionException | RuntimeException ex) {
      logger.error("Problem attempting SelectSingleNode", ex);
      // Return falt
      return null;
    }
  }
  
  public XmlNode CreateNode(XmlNodeType ndType, String sName, String sNameSpaceURI) {
    return null;
  }
}
