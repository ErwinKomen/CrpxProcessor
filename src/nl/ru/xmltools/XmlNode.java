/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import static nl.ru.crpx.tools.General.DoError;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * @author Erwin
 */
public abstract class XmlNode implements Node {
  // This class uses a logger
  private static final Logger logger = Logger.getLogger(XmlNode.class);
  // ======================== variables for internal use
  private DocumentBuilderFactory factory;
  private DocumentBuilder parser;
  private Document doc;
  private XPath xpath = XPathFactory.newInstance().newXPath();
  // Instantiation of the class
  public XmlNode() {
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
      logger.error("XmlNode: could not create documentbuilder", ex);
      DoError("Could not create XmlDocument");
    }
  }

  public XmlAttribute Attributes(String sKey) {
    return (XmlAttribute) this.getAttributes().getNamedItem(sKey);
  }
  public XmlNode SelectSingleNode(String sXpath) throws XPathExpressionException {
    XmlNode ndThis = (XmlNode) xpath.compile(sXpath).evaluate(this.doc, XPathConstants.NODE);
    return ndThis;
  }

}

