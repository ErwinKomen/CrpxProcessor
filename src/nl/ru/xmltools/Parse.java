/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathExpressionException;
import net.sf.saxon.Configuration;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.s9api.DOMDestination;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XdmDestination;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.project.Query;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.xq.CrpFile;
import nl.ru.util.json.JSONArray;
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
  private static final QName loc_xq_File = new QName("", "", "File");
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
  public boolean DoParseXq(Query qThis, XQueryEvaluator qEval, DocumentBuilder objSaxDoc, Configuration xconfig, CrpFile oCrpThis,
        XmlNode ndxThis, JSONArray colBackJson, boolean bReset) {
    String strQname = "(empty)";    // Initialize the query name
    XQueryEvaluator objQuery;
    XdmDestination oDest;           // Query output object
    
    try {
      // Validate
      if (ndxThis == null) return false;
      // Take over the right values
      strQname = qThis.Name;
      // objQuery = qThis.Qeval;
      objQuery = qEval;
      
      // Debugging
      String sFile = ndxThis.getAttributeValue(loc_xq_File);
      
      // Create a new DOM destination for the results
      Document pdxDoc = dbuilder.newDocument();
      
      // There is no need for *isolated* execution, since every Query / File
      //   combination has its own XQueryEvaluator, and therefore its own context
      /* synchronized (objQuery) */
      // Set the context for the query: the node in [ndxThis]
      objQuery.setContextItem(ndxThis);
      // Set the dynamic context: a pointer to the CrpFile
      DynamicQueryContext dqc = objQuery.getUnderlyingQueryContext();
      dqc.setParameter("crpfile", oCrpThis);
      // Additional parameters to identify the query
      dqc.setParameter("qfile", qThis.QueryFile);
      dqc.setParameter("sentid", ndxThis.getAttributeValue(loc_xq_forestId));
      // Execute the query with the set context items
      try {
        objQuery.run(new DOMDestination(pdxDoc));
      } catch (SaxonApiException ex) {
        return errHandle.DoError("Runtime error while executing [" + strQname + "]: ", ex, Parse.class);
      }
      /* } */
      // Get all the <forest> results from the [pdxDoc] answer
      NodeList ndList = pdxDoc.getElementsByTagName("forest");
      for (int i=0; i< ndList.getLength(); i++) {
        // Get access to that node
        Node ndDeep = ndList.item(i);
        NamedNodeMap attrList = ndDeep.getAttributes();

        // Creeer een JSONObject
        JSONObject jsBack = new JSONObject();
        /* ==== oude methode (teveel ruimte) =====
        jsBack.put("file", attrList.getNamedItem("File").getNodeValue());
        jsBack.put("forestId", attrList.getNamedItem("forestId").getNodeValue());
        jsBack.put("eTreeId", attrList.getNamedItem("eTreeId").getNodeValue());
        jsBack.put("Loc", attrList.getNamedItem("Location").getNodeValue());
        // The following are only added if they actually are defined
        if (attrList.getNamedItem("Cat") != null)
          jsBack.put("Cat", attrList.getNamedItem("Cat").getNodeValue());
        if (attrList.getNamedItem("Msg") != null)
          jsBack.put("Msg", attrList.getNamedItem("Msg").getNodeValue());
          ================================== */
        // New method, economizing on space!
        /*
        jsBack.put("loc", attrList.getNamedItem("forestId").getNodeValue() + 
                "." + attrList.getNamedItem("eTreeId").getNodeValue());
        */
        /*  == Debugging == 
        jsBack.put("file", attrList.getNamedItem("File").getNodeValue());
                       */
        
        // Store the details needed for each hit: location, category, message
        jsBack.put("locs", attrList.getNamedItem("forestId").getNodeValue());
        jsBack.put("locw", attrList.getNamedItem("eTreeId").getNodeValue());
        // The following are only added if they actually are defined
        if (attrList.getNamedItem("Cat") != null)
          jsBack.put("cat", attrList.getNamedItem("Cat").getNodeValue());
        if (attrList.getNamedItem("Msg") != null)
          jsBack.put("msg", attrList.getNamedItem("Msg").getNodeValue());
        // Add the object to what we return
        colBackJson.put(jsBack);
      }
      // Return positively if we have found anything
      return (colBackJson.length() > 0);

    } catch ( RuntimeException ex) {
      return errHandle.DoError("Runtime error while executing [" + strQname + "]: ", ex, Parse.class);
      // TODO: provide a visualization of the node this happens to
    } 
  }

}

