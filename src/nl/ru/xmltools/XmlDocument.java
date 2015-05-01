/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import static nl.ru.crpx.project.CrpGlobal.DoError;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
// import org.w3c.dom.Node;
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
    this.parser = null;
    this.factory = DocumentBuilderFactory.newInstance();
    // Set up factory
    this.factory.setIgnoringComments(true);  // Ignore comments
    this.factory.setCoalescing(true);        // Convert CDATA to Text nodes
    this.factory.setNamespaceAware(false);   // No namespace (default)
    this.factory.setValidating(false);       // Don't validate DTD
    try {
      this.parser = this.factory.newDocumentBuilder();
    } catch (ParserConfigurationException ex) {
      logger.error("XmlDocument: could not create documentbuilder", ex);
      DoError("Could not create XmlDocument");
    }
  }  
  public void LoadXml(String sXmlText) throws IOException, SAXException {
    // parse the string
    InputSource is = new InputSource();
    is.setCharacterStream(new StringReader(sXmlText));
    doc = parser.parse(is);
  }
  public XmlNode SelectSingleNode(String sXpath) throws XPathExpressionException {
    XmlNode ndThis = (XmlNode) xpath.compile(sXpath).evaluate(this.doc, XPathConstants.NODE);
    return ndThis;
  }
  public XmlNode CreateNode(XmlNodeType ndType, String sName, String sNameSpaceURI) {
    return null;
  }
}
