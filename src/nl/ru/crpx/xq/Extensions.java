/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.xq;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiUncheckedException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.Value;
import net.sf.saxon.value.AtomicValue;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.tools.FileIO;
import nl.ru.util.ByRef;
import nl.ru.util.StringUtil;
import static nl.ru.util.StringUtil.isNumeric;
import nl.ru.xmltools.XmlNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author Erwin R. Komen
 */
public class Extensions extends RuBase {
  // This class uses a logger
  private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Extensions.class);
  // ============== Variables associated with the *class* "Extensions" =========
  private static PatternStore objStore;
  // private static RuBase objBase;
  // private static final XdmValue loc_EmptyString = new XdmAtomicValue("");
  // ============== Variables local to me ======================================
  // private static CorpusResearchProject prjThis;
  private static ErrHandle errHandle;
  // ============== DOM document building for ru:back() ========================
  private static DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
  private static DocumentBuilder dbuilder;
  // ============== Local constants ============================================
  private static final QName loc_qnName = new QName("", "", "name");
  private static final QName loc_qnValue = new QName("", "", "value");
  private static final QName loc_qnFs = new QName("", "", "fs");
  private static final QName loc_qnF = new QName("", "", "f");
  private static final QName loc_qnFeat = new QName("", "", "feat");
  private static final QName loc_qnPos = new QName("", "", "pos");
  private static final QName loc_qnSubset = new QName("", "", "subset");
  private static final QName loc_qnClass = new QName("", "", "class");
  private static final QName loc_qnEleaf = new QName("", "", "eLeaf");
  private static final QName loc_qnPsdxConstId = new QName("", "", "Id");
  private static final QName loc_qnRef = new QName("", "", "ref");
  private static final QName loc_qnAref = new QName("", "", "aref");
  private static final QName loc_qnLabel = new QName("", "", "Label");
  private static final QName loc_qnText = new QName("", "", "Text");
  private static final QName loc_qnT = new QName("", "", "t");
  private static final QName loc_qnWref = new QName("", "", "wref");
  private static final QName loc_qnS = new QName("", "", "s");
  private static final QName loc_qnW = new QName("", "", "w");
  private static final QName loc_qnForest = new QName("", "", "forestId");
  private static final QName ru_qnFoliaId = new QName("xml", "http://www.w3.org/XML/1998/namespace", "id");
  private static final QName ru_qnFoliaWrefId = new QName("", "", "id");     // Simple identifier for <wref> element
  private static final QName ru_qnPsdxTarget = new QName("", "", "target");

  // ============== CLASS initialization =======================================
  public Extensions(CorpusResearchProject objPrj) {
    // Make sure the class I extend is initialized
    super(objPrj);
    try {
      // Set my own error handler
      errHandle = new ErrHandle(Extensions.class);
      // Initialize a list of string arrays to help ru:matches()
      objStore = new PatternStore(errHandle);
      // Initialise the document builder stuff (for DOM creation)
      dbuilder = dfactory.newDocumentBuilder();
    } catch (ParserConfigurationException ex) {
      errHandle.DoError("Could not initialise Extensions: ", ex, Extensions.class);
    }
  }
  
// <editor-fold desc="ru:ant">
    // ------------------------------------------------------------------------------------
    // Name:   ant
    // Goal:   Return the antecedent of this node
    // History:
    // 14/may/2018 ERK Started adaptation for Java
    // ------------------------------------------------------------------------------------
    public static NodeInfo ant(XPathContext objXp, SequenceIterator sIt) {
        // Get the node we are considering
        NodeInfo node = getOneNode(objXp, "ant", sIt);
        return ant(objXp, node);
    }
    public static NodeInfo ant(XPathContext objXp, NodeInfo node) {
      try {
        // Get the constituent id of the antecedent
        String sConstId = get_antConstId(objXp, node);
        return get_antnode(objXp, node, sConstId);
      } catch (Exception ex) {
        // Show error
        errHandle.DoError("Extensions/ant", ex);
        setRtError(objXp, "ant", ex.getMessage());
        // Return failure
        return null;
      }
    }
    // Given current node, get the constituent with identifier [sConstId],
    //    which may occur in a completely different forest
    public static NodeInfo get_antnode(XPathContext objXp, NodeInfo node, String sConstId) {
      int nodeKind;
      int iDist = 0;
      String sBack = "";
      XdmNode ndSax;             // Myself, if I am a proper node
      String sNodeName = "";
      NodeInfo ndxBack = null;
      XmlNode ndxRes = null;
      ByRef<XmlNode> ndxSent = new ByRef(null);

      try {
        // Validate
        if (node == null) return null;
        nodeKind = node.getNodeKind();
        if (nodeKind != Type.ELEMENT) return null;
        if (sConstId.isEmpty()) { return null; }
        // Get the XdmNode representation of the node
        ndSax = objSaxDoc.wrap(node);   
        // Determine which CRP this is
        CrpFile oCF = getCrpFile(objXp);
        
        switch(oCF.crpThis.intProjType) {
          case ProjPsdx:
            sNodeName = ndSax.getNodeName().getLocalName();
            switch (sNodeName) {
              case "eTree": // Find a child <ref> and take that one's id
                  // Make sure we get the SENTENCE in which this node is
                  if (oCF.getSentence(ndxSent, sConstId)) {
                    // The correct sentence is now in [ndxSent]: 
                    //   retrieve the correct constituent
                    ndxRes = ndxSent.argValue.SelectSingleNode("./descendant::eTree[@Id='"+sConstId+"']");

                    // This method only works, because the [ndxRes] is contained
                    ndxBack = ndxRes.getNode().getUnderlyingNode();

                    // NOTE: these methods do NOT work:
                    // (1) getting the external node
                    // ndxBack = (Node) ndxRes.getNode().getExternalNode();
                    // (2) just getting the XdmNode
                    // ndxBack = ndxRes.getNode();
                  }
                  break;
            }
            break;
          case ProjFolia:
            sNodeName = ndSax.getNodeName().getLocalName();
            switch (sNodeName) {
              case "su": // Find a child <ref> and take that one's id
                  // Make sure we get the SENTENCE in which this node is
                  if (oCF.getSentence(ndxSent, sConstId)) {
                    // The correct sentence is now in [ndxSent]: 
                    //   retrieve the correct constituent
                    ndxRes = ndxSent.argValue.SelectSingleNode("./descendant::su[@xml:id='"+sConstId+"']");

                    // This method only works, because the [ndxRes] is contained
                    ndxBack = ndxRes.getNode().getUnderlyingNode();
                  }
                break;
            }            
            break;
        }
        
        // Return the result
        return ndxBack;
      } catch (Exception ex) {
        // Show error
        errHandle.DoError("Extensions/get_antnode", ex);
        setRtError(objXp, "get_antnode", ex.getMessage());
        // Return failure
        return null;
      }
    }
// </editor-fold>
  
  
// <editor-fold desc="ru:antdist">
    // ------------------------------------------------------------------------------------
    // Name:   antdist
    // Goal:   Return the distance to the antecedent of this node
    // History:
    // 14/may/2018 ERK Started adaptation for Java
    // ------------------------------------------------------------------------------------
    public static int antdist(XPathContext objXp, SequenceIterator sIt) {
        // Get the node we are considering
        NodeInfo node = getOneNode(objXp, "antdist", sIt);
        return antdist(objXp, node);
    }
    public static int antdist(XPathContext objXp, NodeInfo node) {
      try {
        // Get the constituent id of the antecedent
        String sConstId = get_antConstId(objXp, node);
        
        if (sConstId.isEmpty()) {
          return 0;
        } else {     
          // Get the NdDist value
          String sNdDist = feature(objXp, node, "NdDist");
          if (!sNdDist.isEmpty()) {
            int iNdDist = Integer.parseInt(sNdDist);
            
            if (iNdDist <0) {
              return -1;
            } else {
              // If there is a constituent antecedent, then get the distance to it        
              return get_antdist(objXp, node, sConstId);        
            }
          } else {
            return 0;
          }
        }
      } catch (Exception ex) {
        // Show error
        errHandle.DoError("Extensions/get_antConstId", ex);
        setRtError(objXp, "get_antConstId", ex.getMessage());
        // Return failure
        return 0;
      }
    }
    public static String get_antConstId(XPathContext objXp, NodeInfo node) {
      int nodeKind;
      String sBack = "";
      String sNodeName = "";
      XdmNode ndSax;             // Myself, if I am a proper node
      XdmNode ndFS = null;              // The FS nodes
      XdmSequenceIterator colFS = null; // Iterate through <fs>

      try {
        // Validate
        if (node == null) return "";
        nodeKind = node.getNodeKind();
        if (nodeKind != Type.ELEMENT) return "";
        // Get the XdmNode representation of the node
        ndSax = objSaxDoc.wrap(node);   
        // Determine which CRP this is
        CrpFile oCF = getCrpFile(objXp);
        switch(oCF.crpThis.intProjType) {
          case ProjPsdx:
            sNodeName = ndSax.getNodeName().getLocalName();
            switch (sNodeName) {
              case "eTree": // Find a child <ref> and take that one's id
                colFS = ndSax.axisIterator(Axis.CHILD, loc_qnRef);
                // Only consider the *FIRST* <ref> child of an <eTree>
                if (colFS.hasNext()) {
                  // Get this <ref> node
                  ndFS = (XdmNode) colFS.next();
                  // Take the @target from this one
                  sBack = ndFS.getAttributeValue(ru_qnPsdxTarget);
                }
                break;
            }
            break;
          case ProjFolia:
            sNodeName = ndSax.getNodeName().getLocalName();
            switch (sNodeName) {
              case "su": // Find a child <aref> and take that one's id
                colFS = ndSax.axisIterator(Axis.CHILD, loc_qnAref);
                if (colFS.hasNext()) {
                  // Get the <aref> node
                  ndFS = (XdmNode) colFS.next();
                  // Take the @id from this one
                  sBack = ndFS.getAttributeValue(ru_qnFoliaWrefId);
                }
                break;
            }            
        }
        // Return the result
        return sBack;
      } catch (Exception ex) {
        // Show error
        errHandle.DoError("Extensions/get_antConstId", ex);
        setRtError(objXp, "get_antConstId", ex.getMessage());
        // Return failure
        return "";
      }
    }
    // Get the distance between the line of [node] and the line of [sConstId]
    public static int get_antdist(XPathContext objXp, NodeInfo node, String sConstId) {
      int nodeKind;
      int iDist = 0;
      String sNodeName="";
      XdmNode ndSax;             // Myself, if I am a proper node
      XmlNode ndxRes = null;

      try {
        // Validate
        if (node == null) return 0;
        nodeKind = node.getNodeKind();
        if (nodeKind != Type.ELEMENT) return 0;
        if (sConstId.isEmpty()) { return 0; }
        // Get the XdmNode representation of the node
        ndSax = objSaxDoc.wrap(node);   
        // Determine which CRP this is
        CrpFile oCF = getCrpFile(objXp);
        switch(oCF.crpThis.intProjType) {
          case ProjPsdx:
            sNodeName = ndSax.getNodeName().getLocalName();
            switch (sNodeName) {
              case "eTree": 
                // Get the current node's ID
                String sCurrentId = ndSax.getAttributeValue(loc_qnPsdxConstId);
                iDist = oCF.objProcType.NodeDist(sCurrentId, sConstId, "lines");
                break;
              default:
                // Cannot handle any other kind
                return 0;
            }
            break;
          case ProjFolia:
            sNodeName = ndSax.getNodeName().getLocalName();
            switch (sNodeName) {
              case "su": 
                // Get the current node's ID
                String sCurrentId = ndSax.getAttributeValue(ru_qnFoliaId);
                iDist = oCF.objProcType.NodeDist(sCurrentId, sConstId, "lines");
                break;
            }
            break;
        }
        
        if (iDist > 0) {
          int iStop = 1;
        }
        
        // Return the result
        return iDist;
      } catch (Exception ex) {
        // Show error
        errHandle.DoError("Extensions/get_antdist", ex);
        setRtError(objXp, "get_antdist", ex.getMessage());
        // Return failure
        return 0;
      }
    }
// </editor-fold>
    
// <editor-fold defaultstate="collapsed" desc="ru:back">
  // ------------------------------------------------------------------------------------
  // Name:  back
  // Goal:  Return a node <forest> representing this *hit*
  //        The <forest> node contains the hit information in the attributes:
  //        @forestId   -- string identifier of the sentence
  //        @eTreeId    -- string identifier of the hit node (syntactic)
  //        @Location   -- string identifier of the line (more prose)
  //        @File       -- short name of the document file of the hit
  //        @Msg        -- (optional) user-supplied message string
  //        @Cat        -- (optional) user-supplied subcategory of the hit
  //        
  // Note:  The node may be packed into a document XML node like <TEI></TEI>)
  //
  // History:
  // 26-04-2012  ERK Created for .NET
  // 29/apr/2015 ERK Adapted for Java
  // 21/may/2015 ERK Final method creating a DOM node 
  // ------------------------------------------------------------------------------------
  public static Node back(XPathContext objXp, SequenceIterator sIt) {
    return back(objXp, sIt, null, null);
  }
  public static Node back(XPathContext objXp, SequenceIterator sIt, String strMsg) {
    return back(objXp, sIt, strMsg, null);
  }
  public static Node back(XPathContext objXp, SequenceIterator sIt, String strMsg, String strCat) {
    NodeInfo node = getOneNode(objXp, "back", sIt);
    return back(objXp, node, strMsg, strCat);
  }
  public static Node back(XPathContext objXp, NodeInfo node) {
    return back(objXp, node, null, null);
  }
  public static Node back(XPathContext objXp, NodeInfo node, String strMsg) {
    return back(objXp, node, strMsg, null);
  }
  public static Node back(XPathContext objXp, NodeInfo node, String strMsg, String strCat) {
    XdmNode ndSax;                            // The actual node
    ByRef<String> strLoc = new ByRef("");     // Location feature
    ByRef<String> strFile =new ByRef("");     // File feature
    ByRef<String> strTreeId =new ByRef("");   // TreeId feature
    ByRef<String> strForestId =new ByRef(""); // ForestId feature
    int nodeKind;                             // The kind of object getting passed as argument
    
    try {
      // Validate
      if (node == null) return null;
      nodeKind = node.getNodeKind();
      if (nodeKind != Type.ELEMENT) return null;
      // Get the XdmNode representation of the node
      ndSax = objSaxDoc.wrap(node);      

      // ProjPsdx preparations: get appropriate values for each of the <forest> elements
      if (!PrepareBack(objXp, ndSax,strLoc, strFile, strForestId, strTreeId)) return null;
      
      // Alternative way: via the DOM model
      Document doc = dbuilder.newDocument();
      Element mainRootElement = doc.createElement("forest");
      doc.appendChild(mainRootElement);
      // Create and set attributes
      mainRootElement.setAttribute("forestId", strForestId.argValue);
      mainRootElement.setAttribute("eTreeId", strTreeId.argValue);
      mainRootElement.setAttribute("Location", strLoc.argValue);
      mainRootElement.setAttribute("File", strFile.argValue);
      // The following attributes are only added if they are not NULL
      if (strMsg != null) mainRootElement.setAttribute("Msg", strMsg);
      if (strCat != null) mainRootElement.setAttribute("Cat", strCat);
      
      // Return the <forest> element, which will be packed into <TEI></TEI>
      return mainRootElement.cloneNode(true);
      // return jsBack.toString();
    } catch (RuntimeException ex) {
      // Warn user
      String sErrMsg = "Extensions/back() error";
      errHandle.DoError(sErrMsg, ex, Extensions.class);
      setRtError(objXp, "back", ex.getMessage());
      // Return failure
      return null;
    }
  }
  /*
  static NodeInfo getOneNode(XPathContext objXp, XdmSequenceIterator sIt, String sFunction) {
    ErrHandle errCaller = getCrpFile(objXp).crpThis.errHandle;
    int iCheck = 0;
    
    try {
      // Get the first node
      NodeInfo ndFirst = (NodeInfo) sIt.next();
      // Check how many there are
      while (sIt.next() != null) { iCheck++; }
      // Action depends on the length
      if (iCheck > 1) {
        String sErrMsg = "The function ["+sFunction+
                "] function must be called with only 1 (one) node in the first argument. It now receives a sequence of "+iCheck;

        synchronized(errCaller) {
          errCaller.DoError(sErrMsg, sFunction, objXp);
          errCaller.bInterrupt = true;
        }
        // This is the local error handler
        errHandle.DoError(sErrMsg , sFunction,objXp);
        ndFirst = null;
      } 
      return ndFirst;
    }  catch (Exception ex) {
      errHandle.DoError("Extensions/getOneNode error: " , ex, Extensions.class);
      errHandle.bInterrupt = true;
    }
      
    return null;
  } */
// </editor-fold>
  
// <editor-fold defaultstate="collapsed" desc="ru:backgroup">
  // ------------------------------------------------------------------------------------
  // Name:  backgroup
  // Goal:  Return a node <group> representing the information collected for this group
  //        
  //
  // History:
  // 01-12-2015  ERK Created for Java
  // ------------------------------------------------------------------------------------
  public static Node backgroup(XPathContext objXp, Value valGroupName) {
    
    try {
      // Get the name of the group
      String sGroupName = valGroupName.getStringValue();
      // Get the name of the file
      String sFileName = textname(objXp);
      
      // Alternative way: via the DOM model
      Document doc = dbuilder.newDocument();
      Element mainRootElement = doc.createElement("group");
      doc.appendChild(mainRootElement);
      // Create and set attributes
      mainRootElement.setAttribute("fileName", sFileName);
      mainRootElement.setAttribute("groupName", sGroupName);
      
      // Return the <group> element, which will be packed into <TEI></TEI>
      return mainRootElement.cloneNode(true);
      // return jsBack.toString();
    } catch (Exception ex) {
      // Warn user
      errHandle.DoError("Extensions/backgroup() error", ex, Extensions.class);
      setRtError(objXp, "backgroup", ex.getMessage());
      // Return failure
      return null;
    }
  }  
// </editor-fold>
  
// <editor-fold defaultstate="collapsed" desc="ru:backfilter">
  // ------------------------------------------------------------------------------------
  // Name:  backfilter
  // Goal:  Return a node <filter> representing the information provided
  //          by a metadata input filter
  //        
  //
  // History:
  // 10-12-2015  ERK Created for Java
  // ------------------------------------------------------------------------------------
  public static Node backfilter(XPathContext objXp, Value valCondition) {
    
    try {
      // Get the name of the group
      String sCondition = valCondition.getStringValue();
      
      // Alternative way: via the DOM model
      Document doc = dbuilder.newDocument();
      Element mainRootElement = doc.createElement("filter");
      doc.appendChild(mainRootElement);
      // Create and set attributes
      mainRootElement.setAttribute("condition", sCondition);
      
      // Return the <filter> element, which will be packed into ...?
      return mainRootElement.cloneNode(true);
      // return jsBack.toString();
    } catch (Exception ex) {
      // Warn user
      errHandle.DoError("Extensions/backfilter() error", ex, Extensions.class);
      setRtError(objXp, "backfilter", ex.getMessage());
      // Return failure
      return null;
    }
  }  
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="ru:conv">
  // ----------------------------------------------------------------------------------------------------------
  // Name :  conv
  // Goal :  Convert the string according to [strType]
  // History:
  // 10-01-2013  ERK Created for .NET 
  // 30/apr/2015 ERK Ported to Java
  // ----------------------------------------------------------------------------------------------------------
  public static String conv(XPathContext objXp, Value strText, Value strType) {
    try {
      // Call the main ru:conv() handling function
      return conv(objXp, strText.getStringValue(), strType.getStringValue());
    } catch (XPathException ex) {
      // Show error
      errHandle.DoError("Extensions/conv error", ex, Extensions.class);
      setRtError(objXp, "conv", ex.getMessage());
      // Return failure
      return "";
    }
  }
  private static String conv(XPathContext objXp, String strText, String strType) {
    try {
      // Validate
      if (strText.isEmpty()) return "";
      // Do the actual conversion in RuBase
      strText = RuConv(strText, strType);
      // Return the result
      return strText;
    } catch (RuntimeException ex) {
      // Show error
      errHandle.DoError("Extensions/conv error", ex, Extensions.class);
      setRtError(objXp, "conv", ex.getMessage());
      // Return failure
      return "";
    }
  }

// </editor-fold>
  
// <editor-fold defaultstate="collapsed" desc="ru:docroot">
  /**
   * docroot
   *    Return a pointer to the document's root 
   *    For psdx texts it is the <TEI> node
   *    This function only 'makes sense' when parsing is such
   *      that whole files are kept in memory
   * 
   * @param objXp - Context providing information
   * @return      - Node pointing to the document's root
   */
  public static Node docroot(XPathContext objXp) {
    try {
      // Get the CrpFile associated with me
      CrpFile oCrpFile = getCrpFile(objXp);
      // Get the root node (if available)
      XmlNode ndxRoot = oCrpFile.ndxRoot;
      // Return the node that we found
      return xmlNodeToNode(oCrpFile, ndxRoot);
      // return jsBack.toString();
    } catch (RuntimeException ex) {
      // Warn user
      errHandle.DoError("Extensions/docroot() error", ex, Extensions.class);
      setRtError(objXp, "docroot", ex.getMessage());
      // Return failure
      return null;
    }
  }  
// </editor-fold>
  
// <editor-fold defaultstate="collapsed" desc="ru:feature">
  // ----------------------------------------------------------------------------------------------------------
  // Name :  feature
  // Goal :  Get the value of the feature indicated by [strFeatName]
  // History:
  // 19-04-2012  ERK Created for .NET
  // 30/apr/2015 ERK Adapted for Java
  // ----------------------------------------------------------------------------------------------------------
  public static String feature(XPathContext objXp, SequenceIterator sIt, Value strFeatName, Value strType) {
    NodeInfo node = getOneNode(objXp, "feature", sIt);
    return feature(objXp, node, strFeatName, strType);
  }
  public static String feature(XPathContext objXp, NodeInfo node, Value strFeatName, Value strType) {
    String strFval = "";

    try {
      // Get feature value from the function ru:feature()
      strFval = feature(objXp, node, strFeatName);
      // Perform conversion if needed
      strFval = RuConv(strFval, strType.getStringValue());
      // Return the result
      return strFval;
    } catch (RuntimeException | XPathException ex) {
      // Show error
      errHandle.DoError("Extensions/feature error", ex, Extensions.class );
      setRtError(objXp, "feature", ex.getMessage());
      // Return failure
      return "";
    }
  }
  public static String feature(XPathContext objXp, SequenceIterator sIt, Value strFeatName) {
    NodeInfo node = getOneNode(objXp, "feature", sIt);
    return feature(objXp, node, strFeatName);
  }
  public static String feature(XPathContext objXp, NodeInfo node, Value strFeatName) {
    try {
      String sFeatName = strFeatName.getStringValue();
      return feature(objXp, node, sFeatName);
    } catch (XPathException ex) {
      // Show error
      errHandle.DoError("Extensions/feature saxon error", ex, Extensions.class );
      setRtError(objXp, "feature", ex.getMessage());
      // Return failure
      return "";
    }
  }  
  public static String feature(XPathContext objXp, NodeInfo node, String sFeatName) {
    XdmNode ndSax;             // Myself, if I am a proper node
    XdmNode ndFS = null;              // The FS nodes
    XdmNode ndF = null;               // Potential F nodes
    int nodeKind;
    XdmSequenceIterator colFS = null; // Iterate through <fs>
    XdmSequenceIterator colF = null;  // Iterate through <f>

    try {
      // Validate
      if (node == null) return null;
      nodeKind = node.getNodeKind();
      if (nodeKind != Type.ELEMENT) return null;
      // Get the XdmNode representation of the node
      ndSax = objSaxDoc.wrap(node);   
      // Get the feature string
      // String sFeatName = strFeatName.getStringValue();
      // Get the CrpFile associated with me
      CrpFile oCrpFile = getCrpFile(objXp);
      // Initialise depending on project type
      switch (oCrpFile.crpThis.intProjType) {
        case ProjPsdx:
          // We can only get features for an [eTree] node
          if (!ndSax.getNodeName().getLocalName().equals("eTree")) return "";
          // Visit all the <fs> children of [ndSax]
          colFS = ndSax.axisIterator(Axis.CHILD, loc_qnFs);
          while (colFS.hasNext()) {
            // Get this <fs> node
            ndFS = (XdmNode) colFS.next();
            // Visit all the <f> children of [ndFS]
            colF = ndFS.axisIterator(Axis.CHILD, loc_qnF);
            while (colF.hasNext()) {
              // Get this <f> node
              ndF = (XdmNode) colF.next();
              // Get the @name attribute of the feature
              if (sFeatName.equals(ndF.getAttributeValue(loc_qnName))) {
                // Return the @value of the feature
                return ndF.getAttributeValue(loc_qnValue);
              }
            }
          }
          break;
        case ProjFolia:
          // TODO: implement this for the FoLiA type processing
          // We can get features for a <su> node and for a <w> node
          String sNodeName = ndSax.getNodeName().getLocalName();
          switch (sNodeName) {
            case "su":  // The <su> elements may have features directly under them
            case "s":   // The <s> elements too may have features directly under them
              // Visit all the <feat> children of [ndSax]
              colF = ndSax.axisIterator(Axis.CHILD, loc_qnFeat);
              while (colF.hasNext()) {
                // Get this <feat> node
                ndF = (XdmNode) colF.next();
                // Get the @name attribute of the feature
                String sSubset = ndF.getAttributeValue(loc_qnSubset);
                if (sFeatName.equals(sSubset)) {
                  // Return the @value of the feature
                  return ndF.getAttributeValue(loc_qnClass);
                }
                if (!sFeatName.contains("/")) {
                  // Try taking it from past a slash
                  int iSlash = sSubset.indexOf("/");
                  if (iSlash>0) {
                    sSubset = sSubset.substring(iSlash+1);
                    if (sFeatName.equals(sSubset)) {
                      // Return the @value of the feature
                      return ndF.getAttributeValue(loc_qnClass);
                    }
                  }
                }
              }
              break;
            case "w":
              // Visit all the <pos> children of [ndSax]
              colFS = ndSax.axisIterator(Axis.CHILD, loc_qnPos);
              while (colFS.hasNext()) {
                // Get this <pos> node
                ndFS = (XdmNode) colFS.next();
                // Visit all the <feat> children of [ndFS]
                colF = ndFS.axisIterator(Axis.CHILD, loc_qnFeat);
                while (colF.hasNext()) {
                  // Get this <feat> node
                  ndF = (XdmNode) colF.next();
                  // Get the @name attribute of the feature
                  String sSubset = ndF.getAttributeValue(loc_qnSubset);
                  if (sFeatName.equals(sSubset)) {
                    // Return the @value of the feature
                    return ndF.getAttributeValue(loc_qnClass);
                  }
                  if (!sFeatName.contains("/")) {
                    // Try taking it from past a slash
                    int iSlash = sSubset.indexOf("/");
                    if (iSlash>0) {
                      sSubset = sSubset.substring(iSlash+1);
                      if (sFeatName.equals(sSubset)) {
                        // Return the @value of the feature
                        return ndF.getAttributeValue(loc_qnClass);
                      }
                    }
                  }
                }
              }
              break;
            default:
              return "";
          }
          break;          
        default:
          // This method doesn't work for other projects
          errHandle.DoError("ru:feature() is not implemented for this project", Extensions.class);
          return "";
      }
      // Return failure
      return "";
    } catch (SaxonApiUncheckedException ex) {
      // Show error
      errHandle.DoError("Extensions/feature saxon error", ex, Extensions.class );
      setRtError(objXp, "feature", ex.getMessage());
      // Return failure
      return "";
    } catch (RuntimeException ex) {
      // Show error
      errHandle.DoError("Extensions/feature runtime error", ex, Extensions.class );
      setRtError(objXp, "feature", ex.getMessage());
      // Return failure
      return "";
    }
  }
// </editor-fold>
  
// <editor-fold desc="ru:foliaw">
  public static Node foliaw(XPathContext objXp, SequenceIterator sIt) {
      // Get the node we are considering
      NodeInfo node = getOneNode(objXp, "foliaw", sIt);   
      return get_foliaw(objXp, node);
  }
  public static Node get_foliaw(XPathContext objXp, NodeInfo node) {
    XdmNode ndSax;             // Myself, if I am a proper node
    XdmNode ndFS = null;              // The FS nodes
    XdmNode ndW = null;
    XdmSequenceIterator colFS = null; // Iterate through <fs>
    XdmSequenceIterator colW = null;  // Iterate through <w>
    int nodeKind;
    String sId = "";      // Standard <wref> id
    String sIdw = "";     // Additional <wref> id, but with .w.
    String sBack = "";
    Node ndxBack = null;

    try {
      // Validate
      if (node == null) return null;
      nodeKind = node.getNodeKind();
      if (nodeKind != Type.ELEMENT) return null;
      // Get the XdmNode representation of the node
      ndSax = objSaxDoc.wrap(node);   
      // Determine which CRP this is
      CrpFile oCF = getCrpFile(objXp);
      // Initialise depending on project type
      switch (oCF.crpThis.intProjType) {
        case ProjPsdx:
          break;
        case ProjFolia:
          String sNodeName = ndSax.getNodeName().getLocalName();
          switch (sNodeName) {
            case "su":      // Find a child <wref> and take that one's id
              colFS = ndSax.axisIterator(Axis.CHILD, loc_qnWref);
              // Only consider the *FIRST* <wref> child of an <su>
              if (colFS.hasNext()) {
                // Get this <pos> node
                ndFS = (XdmNode) colFS.next();
                // Teks the id from this one
                sId = ndFS.getAttributeValue(ru_qnFoliaWrefId);
              }
              break;
            case "wref":    // Take this one's id
              sId = ndSax.getAttributeValue(ru_qnFoliaWrefId);
              // Check for the wrong <wref> id, the one that doesn't have .w.
              if (!sId.contains(".w.")) {
                // Replace the last .n by .w.n
                int iLastDot = sId.lastIndexOf(".");
                sIdw = sId.substring(0,iLastDot) + ".w" + sId.substring(iLastDot);
              }
              break;
          }
          // Do we have an id?
          if (!sId.isEmpty()) {
            // We have an id: get the first ancestor <s>
            colFS = ndSax.axisIterator(Axis.ANCESTOR, loc_qnS);
            if (colFS.hasNext()) {
              ndFS = (XdmNode) colFS.next();
              // From here start looking at the children
              colW = ndFS.axisIterator(Axis.DESCENDANT, loc_qnW);
              while (colW.hasNext()) {
                // Get this one's id
                ndW = (XdmNode) colW.next();
                String sWid = ndW.getAttributeValue(ru_qnFoliaId);
                if (sId.equals(sWid) || sIdw.equals(sWid)) {
                  // Found the correct <w>
                  sBack = ndW.toString();
                  
                  // Convert this <w> to a proper <Node>
                  // Attempt #1...
                  ndxBack = oCF.oDocFac.newDocumentBuilder().parse(
                          new InputSource(new StringReader(sBack))).getDocumentElement();
                  
                  // Attempt #2: outdated
                  /*
                  Node ndxFound = oCF.oDocFac.newDocumentBuilder().parse(new InputSource(new StringReader(sBack)));
                  ndxBack = ndxFound.getFirstChild();
                  */
                  return ndxBack;
                }
              }
            }
          }
          break;
      }
      // Return the result
      return ndxBack;
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("Extensions/foliaw", ex);
      setRtError(objXp, "foliaw", ex.getMessage());
      // Return failure
      return null;
    }
  }
  
// </editor-fold>
  
// <editor-fold defaultstate="collapsed" desc="ru:header">
  /**
   * header
   *    Return a pointer to the document's header node 
   *    For psdx texts it is the <teiHeader> node
   * 
   * @param objXp - Context providing information
   * @return      - Node pointing to the document's root
   */
  public static Node header(XPathContext objXp) {
    try {
      // Get the CrpFile associated with me
      CrpFile oCrpFile = getCrpFile(objXp);
      // Get the root node (if available)
      XmlNode ndxHeader = oCrpFile.ndxHeader;
      // Return the node that we found
      return xmlNodeToNode(oCrpFile, ndxHeader);
      // return jsBack.toString();
    } catch (RuntimeException ex) {
      // Warn user
      errHandle.DoError("Extensions/header() error", ex, Extensions.class);
      setRtError(objXp, "header", ex.getMessage());
      // Return failure
      return null;
    }
  }  
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="ru:lex">
  // ------------------------------------------------------------------------------------
  // Name:   lex
  // Goal:   Add the word to the lexicon
  // History:
  // 03-10-2013  ERK Created
  // ------------------------------------------------------------------------------------
  public static boolean lex(XPathContext objXp, Value strText, Value strPos) {
    try {
      return lex(objXp, strText.getStringValue(), strPos.getStringValue());
    } catch (XPathException ex) {
      // Show error
      errHandle.DoError("Extensions/lex saxon error", ex, Extensions.class );
      setRtError(objXp, "lex", ex.getMessage());
      // Return failure
      return false;
    }
  }
  private static boolean lex(XPathContext objXp, String strText, String strPos) {
    // =========== DEBUG ===================
    // errHandle.debug("lex [" + strText + "] [" + strPos + "]");
    // If the text to compare is empty, then we return false
    if (strText.isEmpty()) return false;
    // Note: [strPos] may be empty!!
    // Process further using the "AddLex" function defined in the RuBase class
    return RuAddLex(objXp, strText, strPos);
  }
// </editor-fold>
    
// <editor-fold desc="ru:line">
    // ------------------------------------------------------------------------------------
    // Name:   line
    // Goal:   Return a following or preceding line
    // History:
    // 15-05-2012  ERK Created for .NET
    // 24-09-2013  ERK Added line with only 1 argument
    // 21/May/2015 ERK Started adaptation for Java
    // ------------------------------------------------------------------------------------
    public static Node line(XPathContext objXp, int intLines) {
      XmlNode ndxRes = null;
      Node ndxBack = null;
      boolean bFirstChild = true;    // Don't return the document root but the first real child
      
      try {
        // Determine which CRP this is
        CrpFile oCF = getCrpFile(objXp);
        if (intLines == 0) 
          ndxRes = oCF.ndxCurrentForest;
        else {
          ByRef<XmlNode> ndxNew = new ByRef(null);
          String sNewSentId;
          switch(oCF.crpThis.intProjType) {
            case ProjPsdx:
              sNewSentId = String.valueOf(Integer.parseInt(oCF.currentSentId) + intLines);
              if (oCF.objProcType.OneForest(ndxNew, sNewSentId)) {
                ndxRes = ndxNew.argValue;
              }
              break;
            case ProjFolia:
              // sNewSentId = String.valueOf(Integer.parseInt(oCF.currentSentId) + intLines);
              // Get to [intLines] with respect to the currentSentId
              if (oCF.objProcType.OneForest(ndxNew, oCF.currentSentId, intLines)) {
                ndxRes = ndxNew.argValue;
              }
              break;
          }
        }
        
        // Convert Xdm or Xml node to Node
        if (ndxRes == null)
          ndxBack = null;
        else {
          //ndxBack = (Node) ndxRes.getUnderlyingNode(); // dbuilder.parse(ndxRes.toString());
          //ndxBack = (Node) ndxRes.getExternalNode();
          // ndxBack = dbuilder.parse(new InputSource(new StringReader(ndxRes.toString())));
          ndxBack = oCF.oDocFac.newDocumentBuilder().parse(new InputSource(new StringReader(ndxRes.toString())));
          // ndxBack = oCF.oSaxDoc.build(new InputSource(new StringReader(ndxRes.toString())));
          // ndxBack = (Node) oCF.oSaxDoc.build(new StreamSource(new StringReader(ndxRes.toString()))).getUnderlyingNode();
        }

        if (bFirstChild) {
          // Possible extension: make sure we don't return the document root, but the actual child, the <s> node
          if (ndxBack != null && ndxBack.hasChildNodes()) { ndxBack = ndxBack.getFirstChild(); }
        }

        // Return the result
        return ndxBack;
      } catch (Exception ex) {
        // Show error
        errHandle.DoError("Extensions/line", ex);
        setRtError(objXp, "line", ex.getMessage());
        // Return failure
        return null;
      }
    }
// </editor-fold>
    
// <editor-fold defaultstate="collapsed" desc="ru:matches">
  // ------------------------------------------------------------------------------------
  // Name:   matches
  // Goal:   Perform the "Like" function using the pattern (or patterns) stored in [strPattern]
  //         There can be more than 1 pattern in [strPattern], which must be separated
  //         by a vertical bar: |
  // History:
  // 17-06-2010  ERK Created as "Like()" for .NET
  // 22-05-2012  ERK Assigned a new name: "matches", which coincides with a built-in
  //                 Xquery function with approximately the same functionality
  // 29/apr/2015 ERK Transformed to Java
  // ------------------------------------------------------------------------------------
  public static boolean matches(XPathContext objXp, Value varText, Value strPattern) {
    try {
      return matches(objXp, varText.getStringValue(), strPattern.getStringValue());
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("Extensions/matches error: ", ex, Extensions.class);
      setRtError(objXp, "matches", ex.getMessage());
      return false;
    }
  } 
  private static boolean matches(XPathContext objXp, String strText, String strPattern) {
    Pattern[] arPatt; // Array of patterns to be matched
    
    try {
      // =========== DEBUG ===================
      // errHandle.debug("matches [" + strText + "] [" + strPattern + "]");
      // Validate: empty strings
      if (strText.isEmpty()) return false;
      // Make sure to be case-insensitive
      strText = strText.toLowerCase();
      // Reduce the [strPattern]
      strPattern = strPattern.toLowerCase().trim();
      // Convert the pattern string into an array of RegEx patterns
      arPatt = objStore.getMatchHelp(strPattern);
      // Perform pattern matching for each pattern
      for (Pattern patThis : arPatt) {
        if (patThis.matcher(strText).matches()) {
          return true;
        }
      }
      // No match has happened, so return false
      return false;
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("Extensions/matches error: " , ex, Extensions.class);
      setRtError(objXp, "matches", ex.getMessage());
      return false;
    }
  }
  public static boolean matches(XPathContext objXp, Value varText, Value strPattern, Value strPatternNo) {
    try {
      // Execute the main function
      return matches(objXp, varText.getStringValue(), strPattern.getStringValue(), strPatternNo.getStringValue());
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("Extensions/matches error: " , ex, Extensions.class);
      setRtError(objXp, "matches", ex.getMessage());
      return false;
    }
  }
  private static boolean matches(XPathContext objXp, String strText, String strPattern, String strPatternNo) {
    Pattern[] arPattYes;  // Array of patterns to be matched
    Pattern[] arPattNo;   // Array of patterns that must *not* be matched
    
    try {
      // =========== DEBUG ===================
      // errHandle.debug("matches2 [" + strText + "] [" + strPattern + "]");
      // Validate: empty strings
      if (strText.isEmpty()) return false;
      // Make sure to be case-insensitive
      strText = strText.toLowerCase();
      // Reduce the [strPattern]
      strPattern = strPattern.toLowerCase().trim();
      // The same with the *no* pattern
      strPatternNo = strPatternNo.toLowerCase().trim();
      // Convert the pattern string into an array of RegEx patterns
      arPattYes = objStore.getMatchHelp(strPattern);
      arPattNo = objStore.getMatchHelp(strPatternNo);
      // Perform pattern matching for each pattern
      for (Pattern patThis : arPattYes) {
        if (patThis.matcher(strText).matches()) {
          // Make sure it does not match any of the NO patterns
          for (Pattern patNo : arPattNo) {
            // If there is a match on the non-allowed pattern: return failure
            if (patNo.matcher(strText).matches()) return false;
          }
          // There is a positive match on arPattYes, and no negative on arPattNo
          return true;
        }
      }
      // No match has happened, so return false
      return false;
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("Extensions/matches error: " , ex, Extensions.class);
      setRtError(objXp, "matches", ex.getMessage());
      return false;
    }
  }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="ru:mdi">
  /**
   * header
   *    Return a pointer to a possible accompanying .cmdi or .imdi file
   * 
   * @param objXp - Context providing information
   * @return      - Node pointing to the document's root
   */
  public static Node mdi(XPathContext objXp) {
    try {
      // Get the CrpFile associated with me
      CrpFile oCrpFile = getCrpFile(objXp);
      // Get the root node (if available)
      XmlNode ndxMdi = oCrpFile.ndxMdi;
      // Return the node that we found
      return xmlNodeToNode(oCrpFile, ndxMdi);
      // return jsBack.toString();
    } catch (RuntimeException ex) {
      // Warn user
      errHandle.DoError("Extensions/mdi() error", ex, Extensions.class);
      setRtError(objXp, "mdi", ex.getMessage());
      // Return failure
      return null;
    }
  }  
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="ru:message">
  // ------------------------------------------------------------------------------------
  // Name:   message
  // Goal:   show a message box or what is appropriate with the indicated message
  //         Within a web service, the action only shows up in log files
  // History:
  // 03-10-2013  ERK Created
  // 22/oct/2015 ERK Adapted for Java
  // ------------------------------------------------------------------------------------
  public static boolean message(XPathContext objXp, Value strText) {
    try {
      return Message(objXp, strText.getStringValue());
    } catch (XPathException ex) {
      // Show error
      errHandle.DoError("Extensions/message saxon error", ex, Extensions.class );
      setRtError(objXp, "message", ex.getMessage());
      // Return failure
      return false;
    }
  }
  public static boolean Message(XPathContext objXp, Value strText) {
    try {
      return Message(objXp, strText.getStringValue());
    } catch (XPathException ex) {
      // Show error
      errHandle.DoError("Extensions/Message saxon error", ex, Extensions.class );
      setRtError(objXp, "message", ex.getMessage());
      // Return failure
      return false;
    }
  }
  private static boolean Message(XPathContext objXp, String strText) {
    // How can we 'show' a message from a web service? 
    // Two ways: 
    // (1) Show the message 'real-time' in the logging
    errHandle.debug("Message: [" + strText + "]");
    // (2) Gather all the messages and make them available for the caller
    CrpFile oCF = getCrpFile(objXp);
    oCF.lstMessage.add(strText);
    // Always return positively
    return true;
  }
// </editor-fold>
    
// <editor-fold desc="ru:periodgrp">
  // ------------------------------------------------------------------------------------
  // Name:   periodgrp
  // Goal:   Get the period-group name depending on the division [strDiv] selected by the user
  // History:
  // 19-02-2013  ERK Created for .NET
  // 01/sep/2015 ERK Transformed to Java
  // ------------------------------------------------------------------------------------
  public static String periodgrp(XPathContext objXp,Value varText) {
    try {
      return periodgrp(objXp, varText.getStringValue());
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("Extensions/periodgrp error: " , ex, Extensions.class);
      setRtError(objXp, "periodgrp", ex.getMessage());
      // Return failure
      return "";
    }
  }
  public static String periodgrp(XPathContext objXp,String strDiv) {
    try {
      // Validate
      if (strDiv.isEmpty()) return "";
      // use RuBase function
      String sRes = RuPeriodGrp(objXp, strDiv);
      // Return the result
      return sRes;
  
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("Extensions/periodgrp error: " , ex, Extensions.class);
      setRtError(objXp, "periodgrp", ex.getMessage());
      // Return failure
      return "";
    }
  }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="ru:NodeText">
  // ----------------------------------------------------------------------------------------------------------
  // Name :  NodeText
  // Goal :  Get the node's text (without label)
  // History:
  // 01-11-2010  ERK Created for .NET 
  // 12-02-2014  ERK Added variant with[strType]
  // 29/apr/2015 ERK Ported to Java
  // ----------------------------------------------------------------------------------------------------------
  public static String NodeText(XPathContext objXp, SequenceIterator sIt) {
    return NodeText(objXp, sIt, "");
  }
  public static String NodeText(XPathContext objXp, SequenceIterator sIt, String strType) {
    try {
      // Call the actual function, but first check if there is only one node
      NodeInfo node = getOneNode(objXp, "nodetext", sIt);
      if (node == null)
        return "";
      else
        return NodeTextOne(objXp, node, strType);
    } catch (Exception ex) {
      // Show error
      logger.error("Extensions/NodeText[a] error: ",ex);
      setRtError(objXp, "NodeText", ex.getMessage());
      // Return failure
      return "";
    }
  }
  public static String NodeTextOne(XPathContext objXp, NodeInfo node) {
    return NodeTextOne(objXp, node, "");
  }
  public static String NodeTextOne(XPathContext objXp, NodeInfo node, String strType) {
    // XmlDocument docThis = new XmlDocument(); // Where we store it
    XdmNode ndSax;    // Myself, if I am a proper node
    String sResult;   // Resulting value
    int nodeKind;     // The kind of object getting passed as argument

    try {
      // Validate
      if (node == null) return "";
      nodeKind = node.getNodeKind();
      if (nodeKind != Type.ELEMENT) return "";
      // Get the XdmNode representation of the node
      ndSax = objSaxDoc.wrap(node);      
      // Convert to text
      sResult = RuNodeText(objXp, ndSax, strType);
      // Return the result
      return sResult;
    } catch (Exception ex) {
      // Show error
      logger.error("Extensions/NodeTextOne[b] error: ",ex);
      setRtError(objXp, "NodeText", ex.getMessage());
      // Return failure
      return "";
    }
  }
  public static String NodesText(XPathContext objXp, SequenceIterator sIt, String sCombi) {
    return NodesText(objXp, sIt, sCombi,"");
  }
  public static String NodesText(XPathContext objXp, SequenceIterator sIt, String sCombi, String strType) {
    List<String> lCombi = new ArrayList<>();
    
    try {
      // Assuming that there may be more than one node passed on
      while (sIt.next() != null) {
        // Get this node
        NodeInfo node = (NodeInfo) sIt.current();
        lCombi.add(NodeTextOne(objXp, node, strType));
      }
      // Combine the result using spaces
      String sBack = StringUtil.join(lCombi, sCombi);
      return sBack;
    } catch (Exception ex) {
      // Show error
      logger.error("Extensions/NodesText[a] error: ",ex);
      setRtError(objXp, "NodesText", ex.getMessage());
      // Return failure
      return "";
    }
  }
// </editor-fold>
    
// <editor-fold desc="ru:refnum">
  // ------------------------------------------------------------------------------------
  // Name:   refnum
  // Goal:   Get the "internal coreference" number of this node (it must be a node)
  // History:
  // 18-01-2013  ERK Created for .NET
  // 01/sep/2015 ERK Transformed to Java
  // ------------------------------------------------------------------------------------
  public static String refnum(XPathContext objXp, SequenceIterator sIt) {
    // Call the actual function, but first check if there is only one node
    NodeInfo node = getOneNode(objXp, "refnum", sIt);
    // What we return depends on the node we have (or not)
    return refnum(objXp, node);
  }
  public static String refnum(XPathContext objXp, NodeInfo node) {
    // NodeInfo node;
    XdmNode ndSax;    // Myself, if I am a proper node
    int nodeKind;     // The kind of object getting passed as argument

    try {
      // Validate
      if (node == null) return "";
      nodeKind = node.getNodeKind();
      if (nodeKind != Type.ELEMENT) return "";
      // Get the XdmNode representation of the node
      ndSax = objSaxDoc.wrap(node);      
      return refnum(objXp, ndSax);   
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("Extensions/refnum error: " , ex, Extensions.class);
      setRtError(objXp, "refnum", ex.getMessage());
      // Return failure
      return "";
    }
  }
  private static String refnum(XPathContext objXp, XdmNode ndSax) {
    XdmSequenceIterator colEleaf = null;  // Iterate through the children of <eTree>
    XdmNode ndChild = null;
    String sRes = "";
    String sLabel = "";
    String sLast = "";
    String[] arParts;
    
    try {
      // Validate
      if (ndSax == null || !ndSax.getNodeName().toString().equals("eTree")) return "";
            // Get the CrpFile associated with me
      CrpFile oCrpFile = getCrpFile(objXp);
      // Initialise depending on project type
      switch (oCrpFile.crpThis.intProjType) {
        case ProjPsdx:
          // Get the label of this node
          sLabel = ndSax.getAttributeValue(loc_qnLabel);
          break;
        case ProjFolia:
          sLabel = ndSax.getAttributeValue(loc_qnClass);
          break;
        default:
          return "";
      }
      // (1) Try get the number of the label
      arParts = sLabel.split("-");
      if (arParts.length>0) {
        // Get the last part
        sLast = arParts[arParts.length-1];
        // Check if this is numeric
        if (isNumeric(sLast)) return sLast;
      }
      // (2) the refnum is not part of the label
      // Handling depends on project type
      switch (oCrpFile.crpThis.intProjType) {
        case ProjPsdx:
          // Go to first <eLeaf> child
          colEleaf = ndSax.axisIterator(Axis.CHILD, loc_qnEleaf);
          while (colEleaf.hasNext()) {
            // Get this child
            ndChild = (XdmNode) colEleaf.next();
            if (ndChild.getNodeName().toString().equals("eLeaf")) {
              // Get the value of the @Text attribute
              sLabel = ndChild.getAttributeValue(loc_qnText);
              arParts = sLabel.split("-");
              if (sLabel.startsWith("*")) {
                // Get the string following the last hyphen
                sLast = arParts[arParts.length-1];
                // Check if this is numeric
                if (isNumeric(sLast)) return sLast;
              }
            }
          }    
          break;
        case ProjFolia:
          // Go to first <wref> child
          colEleaf = ndSax.axisIterator(Axis.CHILD, loc_qnWref);
          while (colEleaf.hasNext()) {
            // Get this child
            ndChild = (XdmNode) colEleaf.next();
            if (ndChild.getNodeName().toString().equals("wref")) {
              // Get the value of the @Text attribute
              sLabel = ndChild.getAttributeValue(loc_qnT);
              arParts = sLabel.split("-");
              if (sLabel.startsWith("*")) {
                // Get the string following the last hyphen
                sLast = arParts[arParts.length-1];
                // Check if this is numeric
                if (isNumeric(sLast)) return sLast;
              }
            }
          }    
          break;
        default:
          return "";
      }

      // Return the result
      return sRes;
  
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("Extensions/refnum error: " , ex, Extensions.class);
      setRtError(objXp, "refnum", ex.getMessage());
      // Return failure
      return "";
    }
  }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="ru:relates">
  // ----------------------------------------------------------------------------------------------------------
  // Name :  relates
  // Goal :  Check if the node [ndSax1] stands in a relation of [strType] to [ndSax2]
  // History:
  // 12-07-2011  ERK Created for .NET 
  // 29/oct/2015 ERK Ported to Java
  // ----------------------------------------------------------------------------------------------------------
  public static boolean relates(XPathContext objXp, SequenceIterator sIt1, SequenceIterator sIt2, Value varType) {
    try {
    /*
      // Call the actual function, but first check if there is only one node
      NodeInfo node1 = getOneNode(objXp, "refnum", sIt1);
      NodeInfo node2 = getOneNode(objXp, "refnum", sIt2);
      return relates(objXp, node1, node2, varType);
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("Extensions/relates", ex);
      setRtError(objXp, "relates", ex.getMessage());
      // Return failure
      return false;
    }
  }
  public static boolean relates(XPathContext objXp, NodeInfo node1, NodeInfo node2, Value varType) {
    try {
    */
      String sType = varType.getStringValue();
      // return relates(objXp, node1, node2, sType);
      return relates(objXp, sIt1, sIt2, sType);
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("Extensions/relates", ex);
      setRtError(objXp, "relates", ex.getMessage());
      // Return failure
      return false;
    }
  }
  public static boolean relates(XPathContext objXp, SequenceIterator sIt1, SequenceIterator sIt2, String sType) {
    XdmNode ndSax1;   // Myself, if I am a proper node
    XdmNode ndSax2;   // Myself, if I am a proper node
    boolean bResult;      // Resulting value

    try {
      // Convert nodes
      NodeInfo node1 = getOneNode(objXp, "refnum", sIt1);
      NodeInfo node2 = getOneNode(objXp, "refnum", sIt2);
      // Validate
      if (node1 == null || node2 == null) return false;
      if (node1.getNodeKind() != Type.ELEMENT) return false;
      if (node2.getNodeKind() != Type.ELEMENT) return false;
      // Get the XdmNode representation of the node
      ndSax1 = objSaxDoc.wrap(node1);      
      ndSax2 = objSaxDoc.wrap(node2);      
      // Convert to text
      bResult = RuRelates(objXp, ndSax1, ndSax2, sType);
      // Return the result
      return bResult;
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("Extensions/relates", ex);
      setRtError(objXp, "relates", ex.getMessage());
      // Return failure
      return false;
    }
  }
  /*
  public static boolean relates(XPathContext objXp, NodeInfo node1, NodeInfo node2, String sType) {
    XdmNode ndSax1;   // Myself, if I am a proper node
    XdmNode ndSax2;   // Myself, if I am a proper node
    boolean bResult;      // Resulting value

    try {
      // Validate
      if (node1 == null || node2 == null) return false;
      if (node1.getNodeKind() != Type.ELEMENT) return false;
      if (node2.getNodeKind() != Type.ELEMENT) return false;
      // Get the XdmNode representation of the node
      ndSax1 = objSaxDoc.wrap(node1);      
      ndSax2 = objSaxDoc.wrap(node2);      
      // Convert to text
      bResult = RuRelates(objXp, ndSax1, ndSax2, sType);
      // Return the result
      return bResult;
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("Extensions/relates", ex);
      setRtError(objXp, "relates", ex.getMessage());
      // Return failure
      return false;
    }
  } */
// </editor-fold>
  
// <editor-fold defaultstate="collapsed" desc="ru:strchoose">
  // ----------------------------------------------------------------------------------------------------------
  // Name :  strchoose
  // Goal :  Get the first non-empty string from [strArg1] and [strArg2]
  //         If both are empty, return empty
  // History:
  // 19-01-2016  ERK Created for Java
  // ----------------------------------------------------------------------------------------------------------
  public static String strchoose(XPathContext objXp, Value strArg1, Value strArg2) {
    String sBack = "";
    
    try {
      // Get the string values
      String sArg1 = strArg1.getStringValue();
      String sArg2 = strArg2.getStringValue();
      // Return the first non-empty one
      sBack = (sArg1.isEmpty()) ? sArg2 : sArg1;
      return sBack;
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("Extensions/strchoose error", ex, Extensions.class );
      setRtError(objXp, "strchoose", ex.getMessage());
      // Return failure
      return "";
    }
  }
// </editor-fold>
  
// <editor-fold defaultstate="collapsed" desc="ru:stringpart">
  // ----------------------------------------------------------------------------------------------------------
  // Name :  stringpart
  // Goal :  Get the part of [strArg] before or after the delimiter in [strDelim]
  //         This depends on the type specified in [strType]
  // History:
  // 01-12-2015  ERK Created for Java
  // ----------------------------------------------------------------------------------------------------------
  public static String stringpart(XPathContext objXp, Value strArg, Value strDelim, Value strType) {
    String sBack = "";
    
    try {
      // Get the string values
      String sArg = strArg.getStringValue();
      String sDelim = strDelim.getStringValue();
      String sType = strType.getStringValue();
      // Find delimiter
      int iPos = sArg.indexOf(sDelim);
      // Find the string we are looking for
      switch (sType) {
        case "before":
          sBack = (iPos <0) ? sArg : sArg.substring(0, iPos-1);
          break;
        case "after":
          sBack = (iPos <0) ? sArg : sArg.substring(iPos+1);
          break;
      }
      // Return the result
      return sBack;
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("Extensions/stringpart error", ex, Extensions.class );
      setRtError(objXp, "stringpart", ex.getMessage());
      // Return failure
      return "";
    }
  }
// </editor-fold>
  
// <editor-fold defaultstate="collapsed" desc="ru:textname">
  // ----------------------------------------------------------------------------------------------------------
  // Name :  textname
  // Goal :  Get the name of this text
  // History:
  // 01-12-2015  ERK Created for Java
  // ----------------------------------------------------------------------------------------------------------
  public static String textname(XPathContext objXp) {
    try {
      // Get the CrpFile associated with me
      CrpFile oCrpFile = getCrpFile(objXp);
      // Return the name of this text
      String sFileName = oCrpFile.flThis.getName();
      String sBack = FileIO.getFileNameWithoutExtension(sFileName);
      // Return the result
      return sBack;
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("Extensions/textname error", ex, Extensions.class );
      setRtError(objXp, "textname", ex.getMessage());
      // Return failure
      return "";
    }
  }
// </editor-fold>  
  
// <editor-fold defaultstate="collapsed" desc="ru:word">
  // ------------------------------------------------------------------------------------
  // Name:  word
  // Goal:  Get the word (if exists) of node [sIt]        
  //
  // History:
  // 09-10-2017  ERK Created for Java
  // ------------------------------------------------------------------------------------
  public static String word(XPathContext objXp, SequenceIterator sIt) {
    try {
      // Get the node we are considering
      NodeInfo node = getOneNode(objXp, "word", sIt);
      // Call the actual function with the correct parameters
      return get_current_word(objXp, node);
    } catch (Exception ex) {
      // Warn user
      errHandle.DoError("Extensions/word() error", ex, Extensions.class);
      setRtError(objXp, "word", ex.getMessage());
      // Return failure
      return "";
    }
  }
  public static String get_current_word(XPathContext objXp, NodeInfo node) {
    String sResult;  // The result we are going to return
    XdmNode ndSax;    // Myself, if I am a proper node
    int nodeKind;     // The kind of object getting passed as argument
    
    try {
      // Validate
      if (node == null) return "";
      nodeKind = node.getNodeKind();
      if (nodeKind != Type.ELEMENT) return "";
      // Get the XdmNode representation of the node
      ndSax = objSaxDoc.wrap(node);    
      // Now call the function in [RuBase]
      sResult = RuCurrentWord(objXp, ndSax);
      // Return the result
      return sResult;
    } catch (Exception ex) {
      // Warn user
      errHandle.DoError("Extensions/get_current_word() error", ex, Extensions.class);
      setRtError(objXp, "word", ex.getMessage());
      // Return failure
      return "";
    }
  }
// </editor-fold>
  
// <editor-fold defaultstate="collapsed" desc="ru:words">
  // ----------------------------------------------------------------------------------------------------------
  // Name :  words
  // Goal :  Count the number of 'words' under the indicated node
  // History:
  // 23-01-2013  ERK Created for .NET 
  // 29/oct/2015 ERK Ported to Java
  // ----------------------------------------------------------------------------------------------------------
  
  public static int words(XPathContext objXp, SequenceIterator sIt) {
    int iCount = 0;
    
    try {
      // Assuming that there may be more than one node passed on
      while (sIt.next() != null) {
        // Get this node
        NodeInfo node = (NodeInfo) sIt.current();
        // Count the words in this node
        iCount += get_words(objXp, node);
      }
      // Return the total
      return iCount;
    } catch(Exception ex) {      
      // Show error
      logger.error("Extensions/words error: ", ex);
      setRtError(objXp, "words", ex.getMessage());
      // Return failure
      return -1;
    }
    // Call the actual function, but first check if there is only one node
    // NodeInfo node = getOneNode(objXp, "refnum", sIt);
    // return words(objXp, node);
  }  
  public static int get_words(XPathContext objXp, NodeInfo node) {
    XdmNode ndSax;    // Myself, if I am a proper node
    int iResult;      // Resulting value
    int nodeKind;     // The kind of object getting passed as argument

    try {
      // Validate
      if (node == null) return -1;
      nodeKind = node.getNodeKind();
      if (nodeKind != Type.ELEMENT) return -1;
      // Get the XdmNode representation of the node
      ndSax = objSaxDoc.wrap(node);      
      // Convert to text
      iResult = RuWords(objXp, ndSax);
      // Return the result
      return iResult;
    } catch (Exception ex) {
      // Show error
      logger.error("Extensions/get_words error: ", ex);
      setRtError(objXp, "get_words", ex.getMessage());
      // Return failure
      return -1;
    }
  }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="ru:wordsmatch">
  // ------------------------------------------------------------------------------------
  // Name:  wordsmatch
  // Goal:  Check if the sequence of words in [sMulti] matches with the 
  //        words starting at ndThis  //        
  //
  // History:
  // 09-10-2017  ERK Created for Java
  // ------------------------------------------------------------------------------------
  public static boolean wordsmatch(XPathContext objXp, SequenceIterator sIt, Value valMulti) {
    try {
      // Get the node we are considering
      NodeInfo node = getOneNode(objXp, "wordsmatch", sIt);
      // Get the list of words
      String sMulti = valMulti.getStringValue();
      // Call the actual function with the correct parameters
      return wordsmatch2(objXp, node, sMulti);
    } catch (Exception ex) {
      // Warn user
      errHandle.DoError("Extensions/word_sequence() error", ex, Extensions.class);
      setRtError(objXp, "wordsmatch", ex.getMessage());
      // Return failure
      return false;
    }
  }
  public static boolean wordsmatch2(XPathContext objXp, NodeInfo node, String sMulti) {
    boolean bResult;  // The result we are going to return
    XdmNode ndSax;    // Myself, if I am a proper node
    int nodeKind;     // The kind of object getting passed as argument
    
    try {
      // Validate
      if (node == null) return false;
      nodeKind = node.getNodeKind();
      if (nodeKind != Type.ELEMENT) return false;
      // If [sMulti] is empty, there cannot be a match
      if (sMulti.isEmpty()) return false;
      // Get the XdmNode representation of the node
      ndSax = objSaxDoc.wrap(node);    
      // Now call the function in [RuBase]
      bResult = RuWordsMatch(objXp, ndSax, sMulti);
      // Return the result
      return bResult;
    } catch (Exception ex) {
      // Warn user
      errHandle.DoError("Extensions/word_sequence() error", ex, Extensions.class);
      setRtError(objXp, "wordsmatch", ex.getMessage());
      // Return failure
      return false;
    }
  }
// </editor-fold>
  
// <editor-fold defaultstate="collapsed" desc="Private functions">
  private static boolean hasStringValue(XdmValue varText) {
    String strType = varText.getClass().getName();
    return (strType == "XdmAtomicValue" || strType == "XdmNode");
  }
  private static String getStringValue(XdmValue varText) {
    XdmNode ndThis;
    String strText;
    
    // Validate: look what kind of Xdm we receive
    switch(varText.getClass().getName()) {
      case "XdmAtomicValue":
        // Get the string value
        strText = varText.toString();
        break;
      case "XdmNode":
        ndThis = (XdmNode) varText;
        // Get the string value of this node
        strText = ndThis.getStringValue();
        break;
      default:
        // Cannot handle this
        return "";
    }
    // Return the result
    return strText;
  }
  private static XdmNode getNodeValue(XdmValue valSax) {
    if (valSax == null) return null;
    if (valSax.getClass().getName().equals("XdmNode")) {
      return (XdmNode) valSax;
    } else return null;
  }
  /**
   * getOneNode
   *    Make sure only one (1) node is returned from the potential sequence
   *    passed on in @sIt.
   *    If the sequence @sIt contains more than one node, give a runtime error
   *    and set the interrupt to 'true'.
   * 
   * @param objXp
   * @param sFname
   * @param sIt
   * @return 
   */
  private static NodeInfo getOneNode(XPathContext objXp, String sFname, SequenceIterator sIt) {
    ErrHandle errCaller = getCrpFile(objXp).crpThis.errHandle;
    ErrHandle errParse = getErrHandle(objXp);
    int iCheck = 0;
    NodeInfo node = null;
    
    try {
      while (sIt.next() != null) {
        // Always try to take the first node
        if (node == null) {
          node = (NodeInfo) sIt.current();
          // node = (NodeInfo) ( (AtomicValue) sIt.current()).;
        }
        iCheck++;
      }
      // Check the number of arguments
      if (iCheck > 1) {
        // This is actually okay! Proceed...
        //        node = (NodeInfo) sIt.current();
        //      } else {
        logger.debug(sFname+" length = " + iCheck);
        String sMsg = "The ru:"+sFname+"() function must be called with only 1 (one) node. It now receives a sequence of "+iCheck;
        if (node != null) {
          XdmNode ndThis = (XdmNode) objSaxDoc.wrap(node);
          if (ndThis != null) {
            QName qnAttrConstId = getCrpFile(objXp).crpThis.getAttrConstId();
            String sId = ndThis.getAttributeValue( qnAttrConstId);
            if (!sId.isEmpty()) {
              sMsg += ". Constituent=["+sId+"]";
            }
          }
        }
        // Local error
        errHandle.DoError(sMsg , sFname,objXp);
        // DO NOT interrupt this static handler: errHandle.bInterrupt = true;
        // Pass on message upwards to the PARSE error handler
        errParse.bInterrupt = true;
        errParse.DoError(sMsg , sFname,objXp);
      }
      return node;
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("Extensions/getOneNode error: " , ex, Extensions.class);
      // Return failure
      return null;
    }
  }
  /**
   * setRtError -- Set a runtimer error in the function calling me
   * 
   * @param objXp
   * @param sFname
   * @param sMsg 
   */
  private static void setRtError(XPathContext objXp, String sFname, String sMsg) {
    // Get the correct error handler of the caller
    ErrHandle errParse = getErrHandle(objXp);
    // Set that handler to interrupt and pass on a message
    errParse.bInterrupt = true;
    errParse.DoError(sMsg , sFname,objXp);
  }
  // ------------------------------------------------------------------------------------
  // Name:   PrepareBack
  // Goal:   Perform general preparation before returning a result
  //         This function is only called from ru:back()
  // History:
  // 12-06-2012  ERK Created for .NET
  // 29/apr/2015 ERK Ported to Java
  // ------------------------------------------------------------------------------------
  private static boolean PrepareBack(XPathContext objXp, XdmNode ndSax, ByRef<String> strLoc, ByRef<String> strFile,
          ByRef<String> strForestId, ByRef<String> strTreeId) {
    XdmNode ndFor;        // The forest I am part of
    XmlNode ndxFor;       // The <forest> node in the document
    String sNodeNameSnt;  // The name of the sentence node (Psdx: <forest>)
    CrpFile oCrpFile;     // Current crp file
    
    try {
      // Determine which CrpFile I am
      oCrpFile = getCrpFile(objXp);
      sNodeNameSnt = oCrpFile.crpThis.sNodeNameSnt;
      // Action depends on the element we get element
      switch(ndSax.getNodeName().getLocalName()) {
        case "eTree": case "nt": case "Result": // ProjPsdx
        case "t":                               // ProjNegra
        case "su":                              // ProjFolia
          // Okay, perfect
          break;
        case "Feature": case "Text": case "Psd":
          // Return the <Result> element above (this is a CrpOview type)
          ndSax = ndSax.getParent();
          break;
        case "eLeaf":                           // ProjPsdx
          // Return the <eTree> element above
          ndSax = ndSax.getParent();
          break;
        case "edge":                            // ProjNegra
          // Return the <nt> element above me
          ndSax = ndSax.getParent();
          break;
        default:
          // Unable to return anything else
          return false;
      }
      // Double check on the [ndSax] element we have now
      if (ndSax == null) return false; 
      // Get the id value
      strTreeId.argValue = GetRefId(objXp, ndSax);
      // Action depends on the kind of node I am
      switch(ndSax.getNodeName().getLocalName()) {
        case "Result":
          // Determine the forest I am part of
          strForestId.argValue = ndSax.getAttributeValue(RuBase.ru_qnForId);
          // The full file is in @File -- should this be reduced or not?? --> yes
          strFile.argValue = FileIO.getFileNameWithoutExtension(ndSax.getAttributeValue(RuBase.ru_qnFile));
          // The location is part of the @Search value
          strLoc.argValue = ndSax.getAttributeValue(RuBase.ru_qnResultLoc);
          break;
        default:
          // Determine the Sentence I am part of (this is project-dependant through [sNodeNameSnt])
          ndFor = ndSax; strForestId.argValue = ""; strFile.argValue = ""; strLoc.argValue = "";
          while (!ndFor.getClass().getName().contains("XdmEmptySequence")) {
            String sNodeName = ndFor.getNodeName().getLocalName();
            // Make sure this is NOT the highest element that can occur, given the proejct
            if (sNodeName.equals(sNodeNameSnt)) break;
            // Step to the parent
            ndFor = ndFor.getParent();
          }
          // Check the result
          if (ndFor == null || ndFor.getClass().getName().contains("XdmEmptySequence")) return false;
          // Get the values of all the features we need
          switch (oCrpFile.crpThis.intProjType) {
            case ProjPsdx:
              strLoc.argValue = ndFor.getAttributeValue(RuBase.ru_qnLoc);
              strFile.argValue = ndFor.getAttributeValue(RuBase.ru_qnFile);
              strForestId.argValue = ndFor.getAttributeValue(RuBase.ru_qnForId);
              break;
            case ProjFolia:
              // TODO: check and implement correctly for folia
              
              strLoc.argValue = oCrpFile.sTextId;
              strFile.argValue = oCrpFile.flThis.getName();
              strForestId.argValue = ndFor.getAttributeValue(RuBase.ru_qnFoliaId);
              break;
            case ProjNegra:
              // TODO: check and implement correctly for negra
              errHandle.DoError("ru:back is not implemented for Negra", Extensions.class);
              return false;
            case ProjAlp:
              // TODO: check and implement correctly for alpino
              errHandle.DoError("ru:back is not implemented for Alpino", Extensions.class);
              return false;
            default:
              // There is a problem
              errHandle.DoError("ru:back error: uknown project type ", Extensions.class);
              return false;
          }
          break;
      }
      // Return positively
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      errHandle.DoError("Extensions/PrepareBack() error", ex, Extensions.class);
      // Return failure
      return false;
    }
    
  }
// </editor-fold>

}
