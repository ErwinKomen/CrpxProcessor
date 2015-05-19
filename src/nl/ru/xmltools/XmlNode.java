/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;

import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathVariableResolver;
import net.sf.saxon.om.NodeInfo;
// import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import nl.ru.crpx.tools.ErrHandle;

/**
 *
 * @author Erwin R. Komen
 */

public class XmlNode extends XdmNode implements XPathVariableResolver {
  private ErrHandle errHandle;            // Own copy of more global error handler
  private final Processor oProc;          // The processor above the document builder
  private XPathCompiler xpath;            // My own xpath compiler
  private XdmNode ndThis;                 // Myself
  /**
   *
   * @param node    -- the NodeInfo associated with me (Interface)
   * @param objProc -- the Processor that is used overall for Saxon processing
   */
  public XmlNode(XdmNode ndThis, Processor objProc) {
    // Initialize the class I am extending: XdmNode
    super(ndThis.getUnderlyingNode());
    // Get my error handler
    this.errHandle = new ErrHandle(XmlNode.class);
    // Make sure I have the [node] copy
    this.ndThis = ndThis;
    // Set my document-builder
    //this.oSaxDoc = objSaxDoc;
    // Set my processor (same for all threads)
    this.oProc = objProc;
    // Create an xpath compiler
    xpath = oProc.newXPathCompiler();
  }

  // Get the string value of this node
  public String getNodeValue() {return this.ndThis.getStringValue();  }
  public XdmNode getNode() { return this.ndThis;}
  
  // Get an underlying node using an Xpath expression
  public XmlNode SelectSingleNode(String sPath) throws XPathExpressionException {
    try {
      // Get to the indicated path
      XPathExecutable oXpath = xpath.compile(sPath);
      // Execute the path and return a result
      XPathSelector oSelector = oXpath.load();
      // Set the context
      oSelector.setContextItem(this.ndThis);
      // Find the node
      XdmItem ndResult = oSelector.evaluateSingle();
      // Check what kind of item we have: atomic value or node
      if (ndResult.isAtomicValue()) {
        // The result is an atomic value, but we were expecting a node
        ndResult = null;
      } 
      // Create an XmlNode with the result
      XmlNode ndBack = new XmlNode((XdmNode) ndResult, oProc);
      return ndBack;
    } catch (Exception ex) {
      errHandle.DoError("XmlNode - SelectSingleNode", ex, XmlNode.class);
      return null;
    }
  }

  /**
   *
   * @param qName
   * @return
   */
  @Override
  public Object resolveVariable(javax.xml.namespace.QName qName) {
    if (qName.getLocalPart().equals("eTree")) {
        return this.ndThis;
    } else {
        return null;
    }
  }
}

