/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import net.sf.saxon.Configuration;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DOMDestination;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.SaxonApiUncheckedException;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.project.Query;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.xq.CrpFile;
import nl.ru.util.Json;
import nl.ru.util.json.JSONArray;
import nl.ru.util.json.JSONException;
import nl.ru.util.json.JSONObject;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
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
  DocumentBuilderFactory dfactory;  // Dom document factory
  javax.xml.parsers.DocumentBuilder dbuilder;         // DOM document builder
  // ========== Class initialisation ===========================================
  public Parse(CorpusResearchProject objPrj, ErrHandle objEh) {
    try {
      // Set the correct CRP and error handle
      this.crpThis = objPrj;
      this.errHandle = objEh;
      // Initialize the DOM handling
      dfactory = DocumentBuilderFactory.newInstance();
      dbuilder = dfactory.newDocumentBuilder();
    } catch (Exception ex) {
      errHandle.DoError("Cannot initialize the [Parse] class: ", ex, Parse.class);
    }
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
  public boolean DoParseXq(Query qThis, DocumentBuilder objSaxDoc, Configuration xconfig, CrpFile oCrpThis,
        XmlNode ndxThis, JSONArray colBackJson, boolean bReset) throws SaxonApiException {
    String strQname = "(empty)";    // Initialize the query name
    XQueryEvaluator objQuery;
    XdmDestination oDest;           // Query output object
    
    try {
      // Validate
      if (ndxThis == null) return false;
      // Take over the right values
      strQname = qThis.Name;
      objQuery = qThis.Qeval;
      
      /* NO!!! discontinue this...
      // Possibly initialize the [colBackJson] object
      if (bReset) {
        colBackJson = new JSONArray();
      }
      */
      
      // The context for the query is the node in [ndxThis]
      objQuery.setContextItem(ndxThis);
      // The dynamic context contains a pointer to the CrpFile
      DynamicQueryContext dqc = objQuery.getUnderlyingQueryContext();
      dqc.setParameter("crpfile", oCrpThis);
      
      boolean bMethodJSON = false;
      if (bMethodJSON) {
        // Alternative: try out receiving JSONObject as string back
        oDest = new XdmDestination();
        objQuery.run(oDest);
        String sRes = oDest.getXdmNode().getStringValue();
        // Check the result
        if (sRes.isEmpty())
          return false;
        else {
          // Add the string into a return object [colBackJson]
          colBackJson.put(Json.read(new StringReader(sRes)));
          // Return positively
          return true;
        }
      } else {
        // Try out DOM destination
        Document pdxDoc = dbuilder.newDocument();
        objQuery.run(new DOMDestination(pdxDoc));
        // Convert the *lowest* node into a JSONObject
        NodeList ndList = pdxDoc.getElementsByTagName("forest");
        for (int i=0; i< ndList.getLength(); i++) {
          // Get access to that node
          Node ndDeep = ndList.item(i);
          NamedNodeMap attrList = ndDeep.getAttributes();
          // Creeer een JSONObject
          JSONObject jsBack = new JSONObject();
          jsBack.put("file", attrList.getNamedItem("File").getNodeValue());
          jsBack.put("forestId", attrList.getNamedItem("forestId").getNodeValue());
          jsBack.put("eTreeId", attrList.getNamedItem("eTreeId").getNodeValue());
          jsBack.put("Loc", attrList.getNamedItem("Location").getNodeValue());
          // The following are only added if they actually are defined
          if (attrList.getNamedItem("Cat") != null)
            jsBack.put("Cat", attrList.getNamedItem("Cat").getNodeValue());
          if (attrList.getNamedItem("Msg") != null)
            jsBack.put("Msg", attrList.getNamedItem("Msg").getNodeValue());
          // Add the object to what we return
          colBackJson.put(jsBack);
        }
        return (colBackJson.length() > 0);

        /*
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
            // The @forestId can be extracted from the calling [ndxThis]
            strForestId = ndxThis.getAttributeValue(loc_xq_forestId);
            // Walk the resulting DOM structure
            if (pdxDoc != null && pdxDoc.hasChildNodes()) {
              // Main child should be TEI (for psdx)
              Node ndChild = pdxDoc.getFirstChild();
              if (ndChild != null && ndChild.getNodeName().equals("TEI")) {
                Node attrThis;  // Attribute node

                // Now we should have a <forest> child
                ndChild = ndChild.getFirstChild();
                if (ndChild != null && ndChild.getNodeName().equals("forest")) {
                  // Right, now get the attribute values
                  strCat=""; strMsg="";strDb="";strEtreeId="";
                  // Get the @Id of the <eTree>
                  attrThis = ndChild.getAttributes().getNamedItem("TreeId");
                  if (attrThis != null) 
                    strEtreeId = attrThis.getNodeValue();
                  else {
                    errHandle.DoError("Cannot find @TreeId value in forest [" + 
                            strForestId + "]:\n" +
                            ndChild.toString()); 
                    errHandle.bInterrupt = true; 
                    return false;
                  }
                  // Get the other nodes: Msg, Cat, Db
                  attrThis = ndChild.getAttributes().getNamedItem("Msg");
                  if (attrThis != null) strMsg = attrThis.getNodeValue();
                  attrThis = ndChild.getAttributes().getNamedItem("Cat");
                  if (attrThis != null) strCat = attrThis.getNodeValue();
                  attrThis = ndChild.getAttributes().getNamedItem("Db");
                  if (attrThis != null) strDb = attrThis.getNodeValue();
                  // Add the results to the lists
                  colBack.add(new ParseResult(strEtreeId, strForestId, strCat, strMsg, strDb));
                }
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
        */
  // </editor-fold>       
      }
      /* SaxonApiException | SaxonApiUncheckedException | ParserConfigurationException | */
    } catch ( RuntimeException | IOException ex) {
      return errHandle.DoError("Runtime error while executing [" + strQname + "]: ", ex, Parse.class);
      // TODO: provide a visualization of the node this happens to
    } 
  }

}

