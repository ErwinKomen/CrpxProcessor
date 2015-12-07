/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;

import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathExpressionException;
import net.sf.saxon.Configuration;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DOMDestination;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.project.CorpusResearchProject.ProjType;
import nl.ru.crpx.project.Query;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.xq.CrpFile;
import static nl.ru.crpx.xq.RuBase.ru_qnFoliaId;
import nl.ru.util.ByRef;
import nl.ru.util.StringUtil;
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
  private static final QName loc_xq_EtreeId = new QName("", "", "eTreeId");
  private static final QName loc_xq_Msg = new QName("", "", "Msg");  
  private static final QName loc_xq_Cat = new QName("", "", "Cat");  
  private static final QName loc_xq_Label = new QName("", "", "Label");
  private static final QName loc_xq_Type = new QName("", "", "Type");
  private static final QName loc_xq_Text = new QName("", "", "Text");
  private static final QName loc_xq_Loc = new QName("", "", "Loc");  
  private static final QName loc_xq_Db = new QName("", "", "Db");  
  private static final QName loc_xq_File = new QName("", "", "File");
  private static final QName loc_xq_forestId = new QName("", "", "forestId");  
  private static final QName loc_xq_FoliaT = new QName("", "", "t");
  private static final QName loc_xq_FoliaClass = new QName("", "", "class");
  private static XPathSelector ru_xpeNodeText_Folia; // Path to all <w> elements in a FoLiA <s> element
  private XPathCompiler xpComp;             // My own xpath compiler (Xdm, saxon)
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
      // Set up the compiler
      this.xpComp = this.crpThis.getSaxProc().newXPathCompiler();
      // NOTE: this Xpath line should find all the <w> elements that contain text or vernacular
      //       it should exclude classes Zero and Star
      //       Checking w/@class works for anything transformed by Cesax from Psdx > FoLiA (e.g. CGN)
      //       The 'native' FoLiA texts do *not* have the @class marker at the <w> level.
      //       They do not have Star or Zero nodes on the <w> level, 
      ru_xpeNodeText_Folia = xpComp.compile("./descendant-or-self::s[1]/child::w[not(@class) or @class='Punct' or @class='Vern']").load();
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
    String sSeg = "";
    
    try {
      // Validate: Does the node exist?
      if (ndThis == null) return "";
      // Action depends on project type
      switch(crpThis.intProjType) {
        case ProjPsdx:
          // Try find the 'org' segment
          ndxOrg = ndThis.SelectSingleNode("./child::div[@lang='org']/child::seg");
          // Check the reply
          if (ndxOrg == null) return "";
          // The node has been found, so return the innertext
          sSeg = ndxOrg.getNodeValue();
          break;
        case ProjFolia:
          // Try find the 'org' segment
          ndxOrg = ndThis.SelectSingleNode("./child::t[@class='original']");
          // Check the reply
          if (ndxOrg != null) {
            // The node has been found, so return the innertext
            sSeg = ndxOrg.getNodeValue();
          }
          break;
        case ProjAlp:
        case ProjNegra:
        default:
          // Cannot handle this
          errHandle.DoError("modParse/GetSeg: Can not handle project type " + crpThis.getProjectType(), Parse.class);
          sSeg = "";
      }
      return sSeg;
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
  // Name :  GetPde
  // Goal :  Get the "PDE" (modern English) text of the current sentence node
  //         Psdx:   Get the stuff that is between <seg>...</seg> 
  //         Negra:  Combine all the terminal nodes in the correct order
  // History:
  // 20-02-2010   ERK Created for .NET
  // 23/apr/2015  ERK Adapted for Java
  // ----------------------------------------------------------------------------------------------------------
  public String GetPde(XmlNode ndThis) {
    String sSeg = "";
    XmlNode ndxOrg;   // The div/seg node we are looking for
    NodeList ndxTrm;  // List of terminal nodes
    String strProjType; // The project type we are dealing with
    
    try {
      // Validate: Does the node exist?
      if (ndThis == null) return "";
      // Action depends on project type
      switch(crpThis.intProjType) {
        case ProjPsdx:
          // Try find the 'org' segment
          ndxOrg = ndThis.SelectSingleNode("./child::div[@lang='eng']/child::seg");
          // Check the reply
          if (ndxOrg == null) return "";
          // The node has been found, so return the innertext
          sSeg = ndxOrg.getNodeValue();
          break;
        case ProjFolia:
          // Try find the 'org' segment
          ndxOrg = ndThis.SelectSingleNode("./child::t[@class='eng']");
          // Check the reply
          if (ndxOrg != null) {
            // The node has been found, so return the innertext
            sSeg = ndxOrg.getNodeValue();
          }
          break;
        case ProjAlp:
        case ProjNegra:
        default:
          // Cannot handle this
          errHandle.DoError("modParse/GetPde: Can not handle project type " + crpThis.getProjectType(), Parse.class);
          sSeg = "";
      }
      return "";
    } catch (DOMException ex) {
      // Warn user
      errHandle.DoError("Parse/GetPde DOM error", ex, Parse.class);
      // Return failure
      return "";
    } catch (XPathExpressionException ex) {
      errHandle.DoError("Parse/GetPde xpath error", ex, Parse.class);
      return "";
    }
  }
  
  /**
   * TravTree -- Travel a tree to derive bracketed labelling
   * 
   * @param ptThis
   * @param ndxThis
   * @param iLevel
   * @param sbBack
   * @return 
   */
  public boolean TravTree(ProjType ptThis, XdmNode ndxThis, int iLevel, ByRef<StringBuilder> sbBack) {
    try {
      // Validate
      if (ndxThis == null || ndxThis.getNodeKind() != XdmNodeKind.ELEMENT) return false;
      // Action prior to going down
      switch(ptThis) {
        case ProjPsdx:
          switch (ndxThis.getNodeName().toString()) {
            case "forest":
              // Output location
              String sLoc = ndxThis.getAttributeValue(loc_xq_Loc);
              if (sLoc != null && !sLoc.isEmpty()) sbBack.argValue.append( ": " + sLoc);
              break;
            case "eTree":
              // Action depends on kind of label
              String sLabel = ndxThis.getAttributeValue(loc_xq_Label);
              if (sLabel.startsWith("CP") || sLabel.startsWith("IP")) 
                sbBack.argValue.append( "\n" + StringUtil.repeatChar(iLevel, ' ') +
                        "(" + sLabel + " ");                // Start a new line
              else if (sLabel.equals("CODE")) return true;  // Jump out of here
              else sbBack.argValue.append(" (" + sLabel + " ");      // Continue
              break;
            case "eLeaf":
              // Action depends on the type of eLeaf
              String sType = ndxThis.getAttributeValue(loc_xq_Type);
              switch (sType) {
                case "Vern": case "Punct": case "Star": case "Zero":
                  sbBack.argValue.append(ndxThis.getAttributeValue(loc_xq_Text));
                  break;
                default: // no action
                  break;
              }
              break;
          }
          break;
        case ProjFolia:
          switch (ndxThis.getNodeName().toString()) {
            case "s":
              // Output location
              /*
              String sLoc = ndxThis.getAttributeValue(loc_xq_Loc);
              if (!sLoc.isEmpty()) sbBack.append( ": " + sLoc);
              */
              break;
            case "su":
              // Action depends on kind of label
              String sLabel = ndxThis.getAttributeValue(loc_xq_FoliaClass);
              if (sLabel.startsWith("CP") || sLabel.startsWith("IP")) 
                sbBack.argValue.append( "\n" + StringUtil.repeatChar(iLevel, ' ') +
                        "(" + sLabel + " ");                // Start a new line
              else if (sLabel.equals("CODE")) return true;  // Jump out of here
              else sbBack.argValue.append(" (" + sLabel + " ");      // Continue
              break;
            case "wref":
              // Any wref gets taken by us
              sbBack.argValue.append(ndxThis.getAttributeValue(loc_xq_FoliaT));
              break;
          }
          break;
      }
      // Now process all children, going one level down
      XdmSequenceIterator iter = ndxThis.axisIterator(Axis.CHILD);
      while (iter.hasNext()) {
        // Go to the following siblinb
        XdmNode ndxNext = (XdmNode) iter.next();
        if (ndxNext.getNodeKind() == XdmNodeKind.ELEMENT) {
          // Process this node
          if (!TravTree(ptThis, ndxNext, iLevel+1, sbBack)) return false;
        }
      }
      // Any action after having gone down
      switch(ptThis) {
        case ProjPsdx:
          switch (ndxThis.getNodeName().toString()) {
            case "eTree":
              // Close a bracket
              sbBack.argValue.append(")");
              break;
          }
          break;
        case ProjFolia:
          switch (ndxThis.getNodeName().toString()) {
            case "su":
              // Close a bracket
              sbBack.argValue.append(")");
              break;
          }
          break;
      }
      
      // Return positively
      return true;
    } catch (Exception ex) {
      errHandle.DoError("Parse/TravTree xpath error", ex, Parse.class);
      return false;
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
  public boolean DoParseXq(Query qThis, XQueryEvaluator qEval, DocumentBuilder objSaxDoc, Configuration xconfig, 
          CrpFile oCrpThis, XmlNode ndxThis, JSONArray colBackJson, boolean bReset) {
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
      dqc.setParameter("sentid", ndxThis.getAttributeValue(crpThis.getAttrLineId() /*loc_xq_forestId */));
      // If this is FoLiA, then we need to add some more to the dynamic context
      if (oCrpThis.crpThis.intProjType == ProjType.ProjFolia) {
        // Make a list of all <t> nodes that have a <w> parent
        XPathSelector selectXp = ru_xpeNodeText_Folia;
        try {
          selectXp.setContextItem(ndxThis);
        } catch (SaxonApiException ex) {
          return errHandle.DoError("Runtime error while retrieving FoLiA context [" + strQname + "]: ", ex, Parse.class);
        }
        // Go through all the items and add them to a new list
        List<String> lSuId = new ArrayList<>();
        for (XdmItem item : selectXp) {
          String sValue = ((XdmNode) item).getAttributeValue(ru_qnFoliaId);
          // =================================
          // NOTE: this is an ad-hoc measurement because of wrongly made .folia.xml files
          // sValue = sValue.replace(".w.", ".");
          // =================================
          lSuId.add(sValue);
        }
        // Add this 'valid words' context variable to this line
        dqc.setParameter("words", lSuId);
      }
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

