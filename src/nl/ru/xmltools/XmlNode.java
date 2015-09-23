/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;

import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathVariableResolver;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.s9api.Axis;
// import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;
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
   * @param ndThis  -- the NodeInfo associated with me (Interface)
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
  
  // Get the value of a particular attribute
  public String getAttributeValue(String sAttrName) {
    try {
      // Walk the attributes
      XdmSequenceIterator iter = this.ndThis.axisIterator(Axis.ATTRIBUTE);
      while (iter.hasNext()) {
        XdmNode attrThis = (XdmNode) iter.next();
        if (attrThis.getNodeName().toString().equals(sAttrName)) return attrThis.getStringValue();
      }
      return "";
    } catch (Exception ex) {
      errHandle.DoError("getAttributeValue", ex, XmlNode.class);
      return null;
    }
  }
  
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
      if (ndResult != null && ndResult.isAtomicValue()) {
        // The result is an atomic value, but we were expecting a node
        ndResult = null;
      } 
      if (ndResult == null) {
        return null;
      }
      // Create an XmlNode with the result
      XmlNode ndBack = new XmlNode((XdmNode) ndResult, oProc);
      return ndBack;
    } catch (Exception ex) {
      errHandle.DoError("XmlNode - SelectSingleNode", ex, XmlNode.class);
      return null;
    }
  }
  
    // Get a list of underlying nodes using an Xpath expression
  public List<XmlNode> SelectNodes(String sPath) throws XPathExpressionException {
    List<XmlNode> arBack = new ArrayList<>();
    
    try {
      // Get to the indicated path
      XPathExecutable oXpath = xpath.compile(sPath);
      // Execute the path and return a result
      XPathSelector oSelector = oXpath.load();
      // Set the context
      oSelector.setContextItem(this.ndThis);
      // Find the nodes
      XdmValue ndResults = oSelector.evaluate();
      for (XdmItem ndResult : ndResults ) {
        if (ndResult.isAtomicValue()) {
          // Add as nothing
          arBack.add(null);
        } else {
          // Add this item to the array as XmlNode
          arBack.add(new XmlNode((XdmNode) ndResult, oProc));
        }
      }
      // Return the results
      return arBack;
    } catch (Exception ex) {
      errHandle.DoError("XmlNode - SelectNodes", ex, XmlNode.class);
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

