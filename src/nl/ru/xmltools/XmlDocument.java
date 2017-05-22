/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;

import java.io.StringReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import nl.ru.crpx.project.CorpusResearchProject;
import org.apache.log4j.Logger;

/**
 *
 * @author Erwin R. Komen
 */
public class XmlDocument {
  // This class uses a logger
  private static final Logger logger = Logger.getLogger(XmlDocument.class);
  // ======================== variables for internal use
  private Processor oProc;          // The processor above the document builder
  private DocumentBuilder builder;  // Internal copy of the document-builder to be used
  private XPathCompiler xpath;      // Local xpath compiler for searches
  private XdmNode docxdm;           // Entry to the document I hold
  private String sText;             // Text representation of this document

  // =============== Enumeration ===============================================
  public enum XmlNodeType {
    Attribute, Comment, Document, Element, EndElement
  }
  // Instantiation of the class
  public XmlDocument(CorpusResearchProject crpThis) {
    try {
      // Set my processor (same for all threads)
      this.oProc = crpThis.getSaxProc();
      // Set the internal document builder
      this.builder = this.oProc.newDocumentBuilder(); //  oSaxDoc;
      // Create an xpath compiler
      xpath = oProc.newXPathCompiler();
      this.sText = "";
    } catch (RuntimeException ex) {
      logger.error("XmlDocument class instantiation failed", ex);
    }
  }
  public XmlDocument(DocumentBuilder oSaxDoc, Processor objProc) {
    try {
      // Set the internal document builder
      this.builder = oSaxDoc;
      // Set my processor (same for all threads)
      this.oProc = objProc;
      // Create an xpath compiler
      xpath = oProc.newXPathCompiler();
      this.sText = "";
    } catch (RuntimeException ex) {
      logger.error("XmlDocument class instantiation failed", ex);
    }
  }  
  // ======= Access to the document we have loaded
  public XdmNode getNode() { return this.docxdm; }
  public String getDoc() { return this.sText; }
  public Processor getProcessor() { return this.oProc;}
  // ----------------------------------------------------------------------------------------------------------
  // Name :  LoadXml
  // Goal :  Load a string into an XdmNode 
  // History:
  // 18/may/2015  ERK Created for Java
  // ----------------------------------------------------------------------------------------------------------
  public boolean LoadXml(String sXmlText) throws SaxonApiException {
    try {
      // Do minimal validation for this text to see if it could be regarded as XML or not
      sXmlText = sXmlText.trim();
      if (sXmlText.isEmpty()) {
        logger.error("LoadXml: attempt to load EMPTY xml. ");
        return false;
      } else if (!sXmlText.startsWith("<") || !sXmlText.endsWith(">")) {
        int iLen = sXmlText.length();
        // It seems this is no XML, so don't load it
        logger.error("LoadXml: attempt to load non-xml. Length="+iLen+" Content:");
        if (iLen<80) {
          logger.error("["+sXmlText+"]");
        } else {
          logger.error("["+sXmlText.substring(0, 80)+"]");
        }
        return false;
      }
      // Load the string as an XdmNode
      this.docxdm = this.builder.build(new StreamSource(new StringReader(sXmlText)));
      this.sText = sXmlText;
      return true;
    }  catch (SaxonApiException ex) {
      logger.error("Problem attempting LoadXml", ex);
      return false;
    } catch (Exception ex) {
      logger.error("Problem attempting LoadXml", ex);
      return false;
    }
  }

  // ----------------------------------------------------------------------------------------------------------
  // Name :  SelectSingleNode
  // Goal :  Use the Xpath expression to get one single node
  //         Returns null if no node is found
  // History:
  // 18/may/2015  ERK Created for Java
  // ----------------------------------------------------------------------------------------------------------
  public XmlNode SelectSingleNode(String sXpath) throws XPathExpressionException, SaxonApiException {
    try {
      // Check for null
      if (this.docxdm == null) {logger.debug("SelectSingleNode on empty doc"); return null;}
      // Get to the indicated path
      XPathExecutable oXpath = xpath.compile(sXpath);
      // Execute the path and return a result
      XPathSelector oSelector = oXpath.load();
      // Set the context
      oSelector.setContextItem(docxdm);
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
    } catch (RuntimeException | SaxonApiException ex) {
      logger.error("Problem attempting SelectSingleNode", ex);
      // Return falt
      return null;
    }
  }
  
  // ----------------------------------------------------------------------------------------------------------
  // Name :  CreateNode
  // Goal :  Create a node within the current document
  // History:
  // 18/may/2015  ERK Bare bone
  // ----------------------------------------------------------------------------------------------------------
  public XmlNode CreateNode(XmlNodeType ndType, String sName, String sNameSpaceURI) {
    try {
      // Check if at least one node is there
      if (this.docxdm ==null) {
        /*
        // Create a root node
        NodeInfo node = new NodeInfo();
        this.docxdm = new XdmNode();
                */
        
      }
      return null;
    } catch (Exception ex) {
      logger.error("XmlDocument/CreateNode", ex);
      return null;
    }
  }
}
