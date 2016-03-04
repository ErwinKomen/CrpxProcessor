/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
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
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.project.CorpusResearchProject.ProjType;
import nl.ru.crpx.project.Qinfo;
import nl.ru.crpx.project.Query;
import nl.ru.crpx.search.Job;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.xq.CrpFile;
import static nl.ru.crpx.xq.RuBase.ru_qnFoliaId;
import nl.ru.crpx.xq.XqErrorListener;
import nl.ru.util.ByRef;
import nl.ru.util.FileUtil;
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
  // =============== local constants ===========================================
  // in .NET: String strRuDef = "declare namespace ru = 'clitype:CorpusStudio.RuXqExt.RU?asm=CorpusStudio';\r\n";
  private static final String strRuDef = "declare namespace ru = 'java:nl.ru.crpx.xq.Extensions';\r\n";
  private static final String strTbDef = "declare namespace tb = 'http://erwinkomen.ruhosting.nl/software/tb';\r\n";
  private static final String strFunctxDef = "declare namespace functx = 'http://www.functx.com';\r\n";
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
  public static String getDeclNmsp(String sType) { 
    switch (sType) {
      case "ru": return strRuDef;
      case "tb": return strTbDef;
      case "functx": return strFunctxDef;
    }
    return ""; 
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
  
  
  /**
   * getEvaluator
   *    Create and return an XQueryEvaluator object for the string
   * 
   * @param oCompiler
   * @param sQueryText
   * @return 
   */
  public XQueryEvaluator getEvaluator(XQueryCompiler oCompiler, String sQueryText) {
    try {
      // Transform the Xquery text into an Xquery executable
      XQueryExecutable oExe = oCompiler.compile(sQueryText);
      // Return an evaluator for this executable
      return oExe.load();
    } catch (Exception ex) {
      errHandle.DoError("Parse/getEvaluator error", ex, Parse.class);
      return null;
    }
  }
  
  /**
   * DoParseGroupXq
   *    Parse a group-determination query
   * 
   * @param qEval
   * @param oCrpThis
   * @param ndxThis
   * @return 
   */
  public String DoParseGroupXq(XQueryEvaluator qEval, CrpFile oCrpThis, XmlNode ndxThis) {
    try {
      String sBack = DoParseAnyXq(qEval, oCrpThis, ndxThis, "group", "groupName");
      return sBack;
    } catch (Exception ex) {
      errHandle.DoError("Parse/DoParseInputXq error", ex, Parse.class);
      return "";
    }
  }
  
  /**
   * DoParseInputXq
   *    Parse an input-restriction query
   * 
   * @param qEval
   * @param oCrpThis
   * @param ndxThis
   * @return 
   */
  public boolean DoParseInputXq(XQueryEvaluator qEval, CrpFile oCrpThis, XmlNode ndxThis) {
    try {
      String sBack = DoParseAnyXq(qEval, oCrpThis, ndxThis, "filter", "condition");
      boolean bBack = (sBack.isEmpty()) ? false : (sBack.equals("true"));
      return bBack;
    } catch (Exception ex) {
      errHandle.DoError("Parse/DoParseInputXq error", ex, Parse.class);
      return false;
    }
  }
  
  public String DoParseAnyXq(XQueryEvaluator qEval, CrpFile oCrpThis, XmlNode ndxThis, 
          String sElName, String sAttrName) {
    XQueryEvaluator objQuery;

    try {
      // Validate
      if (ndxThis == null) return "";
      // Take over the right values
      objQuery = qEval;
      // Create a new DOM destination for the results
      Document pdxDoc = dbuilder.newDocument();
      // Set the context for the query: the node in [ndxThis]
      objQuery.setContextItem(ndxThis);
      // Set the dynamic context: a pointer to the CrpFile
      DynamicQueryContext dqc = objQuery.getUnderlyingQueryContext();
      dqc.setParameter("crpfile", oCrpThis);
      // Additional parameters to identify the query
      dqc.setParameter("qfile", "" /*qThis.QueryFile */);
      dqc.setParameter("sentid", ndxThis.getAttributeValue(crpThis.getAttrLineId() /*loc_xq_forestId */));
      // Execute the query with the set context items
      try {
        objQuery.run(new DOMDestination(pdxDoc));
      } catch (SaxonApiException ex) {
        errHandle.DoError("Runtime error while executing [DoParseAnyXq]: ", ex, Parse.class);        
        return "";
      }
      // Get all the <forest> results from the [pdxDoc] answer
      NodeList ndList = pdxDoc.getElementsByTagName(sElName);
      // There should be just ONE result
      if (ndList.getLength() == 1) {
        // Get the resulting node
        Node ndDeep = ndList.item(0);
        NamedNodeMap attrList = ndDeep.getAttributes();
        // Check the presence of the named item
        Node ndBack = attrList.getNamedItem(sAttrName);
        if (ndBack != null) {
          // Get the value of the item
          String sBack = ndBack.getNodeValue();
          return sBack;
        }
      }
      
      // This is bad: no result 
      return "";
    } catch (Exception ex) {
      errHandle.DoError("Parse/DoParseAnyXq error", ex, Parse.class);
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
  public boolean DoParseXq(Job oJob, Qinfo[] arQinfo, Query qThis, XQueryEvaluator qEval, DocumentBuilder objSaxDoc, 
          Configuration xconfig, CrpFile oCrpThis, XmlNode ndxThis, JSONArray colBackJson, boolean bReset) {
    String strQname = "(empty)";    // Initialize the query name
    XQueryEvaluator objQuery;
    XdmDestination oDest;           // Query output object
    XqErrorListener listener = null;  // Keep track of errors
    XqErr oXq = new XqErr();
    List<Exception> errorList;        // List of errors
    
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
      // (1) Create error listener
      listener = new XqErrorListener();
      
      // There is no need for *isolated* execution, since every Query / File
      //   combination has its own XQueryEvaluator, and therefore its own context
      /* synchronized (objQuery) */
      // Set the context for the query: the node in [ndxThis]
      objQuery.setContextItem(ndxThis);
      // Set the dynamic context: a pointer to the CrpFile
      DynamicQueryContext dqc = objQuery.getUnderlyingQueryContext();
      dqc.setErrorListener(listener);
      dqc.setParameter("crpfile", oCrpThis);
      // Additional parameters to identify the query
      dqc.setParameter("qfile", qThis.QueryFile);
      dqc.setParameter("sentid", ndxThis.getAttributeValue(crpThis.getAttrLineId() /*loc_xq_forestId */));
      // Pass on the error handler too
      dqc.setParameter("errhandle", errHandle);
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
        /*
      } catch (SaxonApiException ex) {
        errHandle.bInterrupt = true;
        oJob.setJobErrors(errHandle.getErrList());
        return errHandle.DoError("Runtime error while executing [" + strQname + "]: ", ex, Parse.class);        
      } catch (ClassCastException ex) {
        // Do we have an errorlist?
        if (listener == null) return false;
        errorList = listener.getErrList();
        errHandle.debug("DoParseXq classcast error: ["+ex.getMessage()+"]");
        errHandle.bInterrupt = true;
        oJob.setJobErrors(errHandle.getErrList());
        return errHandle.DoError("ClassCast error while executing [" + strQname + "]: ", ex, Parse.class);   
        */
      } catch (Exception ex) {
        // Try to get runtime error
        if (listener != null && listener.processError(qThis.QueryFile, ex.getMessage(), arQinfo, oXq)) {
          oXq.XqErrShow();
          // Add the error to the error object
          errHandle.DoError(oXq.lQerr);
        }
        errHandle.bInterrupt = true;
        oJob.setJobErrors(errHandle.getErrList());
        oJob.setJobStatus("error");
        return errHandle.DoError("Runtime error while executing [" + strQname + "]: ", ex, Parse.class);        
      }
      // Check for interrupt
      if (errHandle.bInterrupt) {
        errHandle.debug("DoParseXq is interrupted");
        return false;
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
  
  /**
   * getGroupName
   *    Given an Xquery evaluator, calculate the name of the group the @sFileName
   *      belongs to.
   * 
   * @param qEval
   * @param oProj
   * @param sFileName
   * @return 
   */
  public String getGroupName(XQueryEvaluator qEval, CorpusResearchProject oProj, String sFileName) {
    ByRef<XmlNode> ndxForest;   // Forest we are working on
    ByRef<XmlNode> ndxHeader;   // Header of this file
    ByRef<XmlNode> ndxMdi;      // Access to corresponding .imdi or .cmdi file
    XmlForest objProcType;      // Access to the XmlForest object allocated to me
    CrpFile oCrpFile;
    String sGroup = "default";
    
    try {
      // Validate
      if (qEval == null || oProj == null || sFileName.isEmpty()) return "error";
      // Locate the file
      sFileName = FileUtil.findFileInDirectory(oProj.getSrcDir().getAbsolutePath(), sFileName);
      /*
      Path pStart = Paths.get(oProj.getSrcDir().getAbsolutePath());
      List<String> lInputFiles = new ArrayList<>();
      FileUtil.getFileNames(lInputFiles, pStart, sFileName);
      if (lInputFiles.isEmpty()) {
        return "error";
      }
      sFileName = lInputFiles.get(0); */
      
      // Create a CrpFile for this project/file combination
      File fThis = new File(sFileName);
      oCrpFile = new CrpFile(oProj, fThis, oProj.getSaxProc(), null);
      // Initialisations
      objProcType = oCrpFile.objProcType;
      ndxForest = new ByRef(null); 
      ndxHeader = new ByRef(null);
      ndxMdi = new ByRef(null);
      // (a) Read the first sentence (psdx: <forest>) as well as the header (psdx: <teiHeader>)
      if (!objProcType.FirstForest(ndxForest, ndxHeader, ndxMdi, fThis.getAbsolutePath())) {
        errHandle.DoError("hasInputRestr could not process first forest of " + fThis.getName());
        return "error";
      }
      // Pass on header information 
      oCrpFile.ndxHeader = ndxHeader.argValue;
      oCrpFile.ndxMdi = ndxMdi.argValue;
      oCrpFile.ndxCurrentForest = ndxForest.argValue;
      // Get the group information
      sGroup = DoParseGroupXq(qEval, oCrpFile, ndxForest.argValue);
      // Check
      if (sGroup.isEmpty()) sGroup = "default";
      // Make sure to close the Random-Access-Reader for this file
      oCrpFile.close();
      // Return the group we found
      return sGroup;
    } catch (Exception ex) {
      errHandle.DoError("Parse/getGroupName problem with [" + sFileName + "]: ", ex, Parse.class);
      return "error";
    }
  }
  
// <editor-fold defaultstate="collapsed" desc="ErrorListener class">
  public class MyErrorListener implements ErrorListener {
    // private ExecuteXml parent;
    List<Exception> errorList;  // List of all errors
    List<Integer> lineList;     // List of line numbers
    List<Integer> colList;      // List of column  numbers
    List<Boolean> fatalList;    // Fatal error lists

    private MyErrorListener() {
      // this.parent = parent;
      errorList = new ArrayList<>();
      lineList = new ArrayList<>();
      colList = new ArrayList<>();
      fatalList = new ArrayList<>();
    }
    @Override
    public void error(TransformerException exception) {
      errorList.add(exception);
      SourceLocator sl = exception.getLocator();
      if (sl == null) {
        lineList.add(-1);
        colList.add(-1);
      } else {
        lineList.add(sl.getLineNumber());
        colList.add(sl.getColumnNumber());
      }
      fatalList.add(false);
    }
    @Override
    public void fatalError(TransformerException exception)  {
      errorList.add(exception);
      SourceLocator sl = exception.getLocator();
      if (sl == null) {
        lineList.add(-1);
        colList.add(-1);
      } else {
        lineList.add(sl.getLineNumber());
        colList.add(sl.getColumnNumber());
      }
      
      // throw exception;
      fatalList.add(true);
    }
    @Override
    public void warning(TransformerException exception) {
      // no action
      lineList.add(-1);
      colList.add(-1);
      fatalList.add(false);
    }
    public int lineNum(int i) {return lineList.get(i);}
    public int colNum(int i) {return colList.get(i);}
    public boolean getFatal(int i) {return fatalList.get(i);}
    public String getMsg(int i) {return errorList.get(i).getMessage();}
    public List<Exception> getErrList() {return errorList;}
  }
// </editor-fold>  

}

