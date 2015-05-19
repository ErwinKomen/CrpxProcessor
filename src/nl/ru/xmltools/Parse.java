/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;

import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathExpressionException;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DOMDestination;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.SaxonApiUncheckedException;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.xmltools.XmlNode;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Parse: all the routines and functions that are essential for the actual
 *        parsing of documents using Xquery 
 * 
 * @author Erwin R. Komen
 */
public class Parse {
  // My error handling function: this should be passed on from the caller
  // private static final ErrHandle errHandle = new ErrHandle(Parse.class);
  ErrHandle errHandle;
  // ========== constants ======================================================
  private static final QName loc_xq_EtreeId = new QName("", "", "TreeId");
  private static final QName loc_xq_Msg = new QName("", "", "Msg");  
  private static final QName loc_xq_Cat = new QName("", "", "Cat");  
  private static final QName loc_xq_Db = new QName("", "", "Db");  
  private static final QName loc_xq_forestId = new QName("", "", "forestId");  
  // ========== Variables for this class =======================================
  CorpusResearchProject crpThis;    // Which CRP are we working with?
  // ========== Class initialisation ===========================================
  public Parse(CorpusResearchProject objPrj, ErrHandle objEh) {
    this.crpThis = objPrj;
    this.errHandle = objEh;
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  GetSeg
  // Goal :  Get the "original" text of the current sentence node
  //         Psdx:   Get the stuff that is between <seg>...</seg> 
  //         Negra:  Combine all the terminal nodes in the correct order
  // History:
  // 20-02-2010   ERK Created for .NET
  // 23/apr/2015  ERK Adapted for Java
  // ----------------------------------------------------------------------------------------------------------
  public String GetSeg(XmlNode ndThis) {
    XmlNode ndxOrg;   // The div/seg node we are looking for
    NodeList ndxTrm;  // List of terminal nodes
    String strProjType; // The project type we are dealing with
    
    try {
      // Validate: Does the node exist?
      if (ndThis == null) return "";
      // Action depends on project type
      strProjType = crpThis.getProjectType();
      switch(strProjType.toLowerCase()) {
        case "xquery-psdx":
          // Try find the 'org' segment
          ndxOrg = ndThis.SelectSingleNode("./child::div[@lang='org']/child::seg");
          // Check the reply
          if (ndxOrg == null) return "";
          // The node has been found, so return the innertext
          return ndxOrg.getNodeValue();
        case "folia-psdx":
          break;
        case "negra-tig":
          break;
        default:
          // Cannot handle this
          errHandle.DoError("modParse/GetSeg: Can not handle project type " + strProjType, Parse.class);
          return "";
      }
      
      return "";
    } catch (DOMException ex) {
      // Warn user
      errHandle.DoError("Parse/GetSeg DOM error", ex, Parse.class);
      // Return failure
      return "";
    } catch (XPathExpressionException ex) {
      errHandle.DoError("Parse/GetSeg xpath error", ex, Parse.class);
      return "";
    }
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  DoParse
  // Goal :  Perform one Xquery on one XML node
  // History:
  // 09-08-2010  ERK Derived from [OneXquery()] for .NET
  // 14-09-2010  ERK Added return of [intId]
  // 13/may/2015 ERK Re-written for JAVA
  // ----------------------------------------------------------------------------------------------------------
  public boolean DoParseXq(String strQname, XQueryEvaluator objQuery, DocumentBuilder objSaxDoc,
        String sQfile,  String sQstring,   XdmNode ndxThis, List<ParseResult> colBack, boolean bReset) {
    XdmDestination oDest;           // Query output object
    XdmSequenceIterator objForest;  // To iterate through the children
    DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
    Document dom;
    DOMDestination dDest;
    
    try {
      // Validate
      if (ndxThis == null) return false;
      // Possibly initialize
      if (bReset) {colBack.clear();}
      // Work with the query
      objQuery.setContextItem(ndxThis);
      // Test: try getting a DOM result
      // dom = dfactory.newDocumentBuilder().newDocument();
      // dDest = new DOMDestination(dom);
      // objQuery.run(dDest);
      // TEST: perform the query and store it in an XdmNode
      XdmNode ndResult = (XdmNode) objQuery.evaluate();
      // ndxThis.toString();
      // ndResult.itemAt(0).getStringValue();
      // Run the query to this output
      oDest = new XdmDestination();
      objQuery.run(oDest);
      // Check the results for something valuable
      switch (this.crpThis.intProjType) {
        case ProjAlp:
          // Not yet implemented
          break;
        case ProjNegra:
          // Not yet implemented
          break;
        case ProjFolia:
          // TODO: implement
          break;
        case ProjPsdx:
          // Walk the resulting XdmNode
          XdmNode ndForDest = oDest.getXdmNode();
          if (ndForDest != null) {
            objForest = ndForDest.axisIterator(Axis.CHILD);
            while (objForest.hasNext()) {
              // Get this node
              XdmNode ndF = (XdmNode) objForest.next();
              // Extract the correct attribute values
              // (1) the @TreeId value: this must be present
              String strEtreeId = ndF.getAttributeValue(loc_xq_EtreeId);
              // Validate reply
              // TODO: if we unite the different project replies, then at least
              //       differentiate here at the error-handling level.
              String sForestId = ndxThis.getAttributeValue(loc_xq_forestId);
              if (strEtreeId.isEmpty()) { 
                errHandle.DoError("Cannot find @TreeId value in forest [" + 
                        sForestId + "]:\n" +
                        objForest.toString()); 
                errHandle.bInterrupt = true; 
                return false;
              }        
              // (2) the @Msg value - optional
              String strMsg = ndF.getAttributeValue(loc_xq_Msg);
              strMsg = strMsg.replace("''", "'");
              // (3) the @Cat value - also optional
              String strCat = ndF.getAttributeValue(loc_xq_Cat);
              strCat = strCat.replace("''", "'");
              // (4) the @Db value - also optional
              String strDb = ndF.getAttributeValue(loc_xq_Db);
              strDb = strDb.replace("''", "'");
              // Add the results to the lists
              colBack.add(new ParseResult(strEtreeId, strCat, strMsg, strDb));
           }            
          }
          break;
        case ProjPsd:
          // This should not run
          break;
        default:
          // Provide warning?
          break;
      }
      
      // Return positively - provided there are results
      return (colBack.size() > 0);
    } catch (SaxonApiException  ex) {
      return errHandle.DoError("Runtime error while executing [" + strQname + "]: ", ex, Parse.class);
      // TODO: provide a visualization of the node this happens to
    } catch ( SaxonApiUncheckedException ex) {
      return errHandle.DoError("Runtime error while executing [" + strQname + "]: ", ex, Parse.class);
      // TODO: provide a visualization of the node this happens to
    }
      
  }
  public boolean DoParseXq_OLD(String strQname, XQueryEvaluator objQuery, DocumentBuilder objSaxDoc,
          XmlNode ndxThis, List<ParseResult> colBack, boolean bReset) {
    XdmItem oContext;               // The context for this query
    XdmDestination oDest;           // Query output object
    XdmSequenceIterator objForest;  // To iterate through the children
    
    try {
      // Validate
      if (ndxThis == null) return false;
      // Possibly initialize
      if (bReset) {colBack.clear();}
      // Work with the query
      oContext = objSaxDoc.wrap(ndxThis);
      objQuery.setContextItem(oContext);
      // Run the query to this output
      oDest = new XdmDestination();
      objQuery.run(oDest);
      // Check the results for something valuable
      switch (this.crpThis.intProjType) {
        case ProjAlp:
          // Not yet implemented
          break;
        case ProjNegra:
          // Not yet implemented
          break;
        case ProjFolia:
          // TODO: implement
          break;
        case ProjPsdx:
          // Walk the resulting XdmNode
          XdmNode ndForDest = oDest.getXdmNode();
          if (ndForDest != null) {
            objForest = ndForDest.axisIterator(Axis.CHILD);
            while (objForest.hasNext()) {
              // Get this node
              XdmNode ndF = (XdmNode) objForest.next();
              // Extract the correct attribute values
              // (1) the @TreeId value: this must be present
              String strEtreeId = ndF.getAttributeValue(loc_xq_EtreeId);
              // Validate reply
              // TODO: if we unite the different project replies, then at least
              //       differentiate here at the error-handling level.
              if (strEtreeId.isEmpty()) {
                /*
                errHandle.DoError("Cannot find @TreeId value in forest [" + 
                        ndxThis.Attributes("forestId").getNodeValue() + "]:\n" +
                        objForest.toString());  */
                errHandle.DoError("Cannot find @TreeId value in forest [" + 
                        ndxThis.getAttributeValue(loc_xq_forestId) + "]:\n" +
                        objForest.toString()); 
                errHandle.bInterrupt = true; 
                return false;
              }        
              // (2) the @Msg value - optional
              String strMsg = ndF.getAttributeValue(loc_xq_Msg);
              strMsg = strMsg.replace("''", "'");
              // (3) the @Cat value - also optional
              String strCat = ndF.getAttributeValue(loc_xq_Cat);
              strCat = strCat.replace("''", "'");
              // (4) the @Db value - also optional
              String strDb = ndF.getAttributeValue(loc_xq_Db);
              strDb = strDb.replace("''", "'");
              // Add the results to the lists
              colBack.add(new ParseResult(strEtreeId, strCat, strMsg, strDb));
           }            
          }
          break;
        case ProjPsd:
          // This should not run
          break;
        default:
          // Provide warning?
          break;
      }
      
      // Return positively - provided there are results
      return (colBack.size() > 0);
    } catch (Exception ex) {
      return errHandle.DoError("Runtime error while executing [" + strQname + "]: ", ex, Parse.class);
      // TODO: provide a visualization of the node this happens to
    }
      
  }
}

