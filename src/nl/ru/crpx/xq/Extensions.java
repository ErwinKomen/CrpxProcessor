/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.xq;

import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.SaxonApiUncheckedException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Value;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.tools.FileIO;
import nl.ru.util.ByRef;
import nl.ru.xmltools.XmlNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author Erwin R. Komen
 */
public class Extensions extends RuBase {
  // This class uses a logger
  private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Extensions.class);
  // ============== Variables associated with the *class* "Extensions" =========
  private static PatternStore objStore;
  private static RuBase objBase;
  private static final XdmValue loc_EmptyString = new XdmAtomicValue("");
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
      
      // Nothing -- attempt: objXp.getController().setParameter(strCat, node);
      
      /*
      // N.B: voor dit alternatief moet de return-value van
      //      ru:back() veranderd worden naar String.
      //
      // Alternatief #4: creeer meteen een JSONObject
      JSONObject jsBack = new JSONObject();
      jsBack.put("file", strFile.argValue);
      jsBack.put("forestId", strForestId.argValue);
      jsBack.put("eTreeId", strTreeId.argValue);
      jsBack.put("Cat", strCat);
      jsBack.put("Msg", strMsg);
      */
      
      // Return the <forest> element, which will be packed into <TEI></TEI>
      return mainRootElement.cloneNode(true);
      // return jsBack.toString();
    } catch (RuntimeException ex) {
      // Warn user
      errHandle.DoError("Extensions/back() error", ex, Extensions.class);
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
  /*
  public static String conv(XdmValue varText, String strType) {
    // Calculate using the main ru:conv() function
    return conv(getStringValue(varText), strType);
  } */
  public static String conv(XPathContext objXp, Value strText, Value strType) {
    try {
      // Call the main ru:conv() handling function
      return conv(objXp, strText.getStringValue(), strType.getStringValue());
    } catch (XPathException ex) {
      // Show error
      errHandle.DoError("Extensions/conv error", ex, Extensions.class);
      // Return failure
      return "";
    }
  }
  /*
  public static String conv(XPathContext objXp, AtomicValue strText, String strType) {
    // Try to get user-data
    XdmNode nThis = (XdmNode) objXp.getController().getUserData("aap", "type");
    // Continue...
    return conv(strText.getStringValue(), strType);
  } */
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
      // Return failure
      return "";
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
      // Return failure
      return "";
    }
  }
  public static String feature(XPathContext objXp, NodeInfo node, Value strFeatName) {
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
              if (strFeatName.getStringValue().equals(ndF.getAttributeValue(loc_qnName))) {
                // Return the @value of the feature
                return ndF.getAttributeValue(loc_qnValue);
              }
            }
          }
          break;
        case ProjFolia:
          // TODO: implement this for the FoLiA type processing
          
        default:
          // This method doesn't work for other projects
          errHandle.DoError("ru:feature() is not implemented for this project", Extensions.class);
          return "";
      }
      // Return failure
      return "";
    } catch (SaxonApiUncheckedException | XPathException ex) {
      // Show error
      errHandle.DoError("Extensions/feature saxon error", ex, Extensions.class );
      // Return failure
      return "";
    } catch (RuntimeException ex) {
      // Show error
      errHandle.DoError("Extensions/feature runtime error", ex, Extensions.class );
      // Return failure
      return "";
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
  /*
  public static boolean lex(XdmValue varText, XdmValue varPos) {
    String strText;   // The string value we are matching
    String strPos;    // The part-of-speech (or other grouping value)
    
    // Validate: look what kind of Xdm we receive
    if (!hasStringValue(varText)) return false;
    strText = getStringValue(varText);
    // Validate: check the POS we received
    if (!hasStringValue(varPos)) return false;
    strPos = getStringValue(varPos);
    // call the main lex() function
    return lex(strText, strPos);
  } */
  public static boolean lex(XPathContext objXp, AtomicValue strText, AtomicValue strPos) {
    return lex(objXp, strText.getStringValue(), strPos.getStringValue());
  }
  private static boolean lex(XPathContext objXp, String strText, String strPos) {
    // If the text to compare is empty, then we return false
    if (strText.isEmpty()) return false;
    // Note: [strPos] may be empty!!
    // Process further using the "AddLex" function defined in the RuBase class
    return RuAddLex(objXp, strText, strPos);
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
  public static boolean matches(Value varText, Value strPattern) {
    try {
      return matches(varText.getStringValue(), strPattern.getStringValue());
    } catch (Exception ex) {
      // Show error
      logger.error("Extensions/matches error: " + ex.getMessage() + "\r\n");
      return false;
    }
  } 
  private static boolean matches(String strText, String strPattern) {
    Pattern[] arPatt; // Array of patterns to be matched
    
    try {
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
        if (patThis.matcher(strText).matches()) return true;
      }
      // No match has happened, so return false
      return false;
    } catch (Exception ex) {
      // Show error
      logger.error("Extensions/matches error: " + ex.getMessage() + "\r\n");
      return false;
    }
  }
  public static boolean matches(Value varText, Value strPattern, Value strPatternNo) {
    try {
      // Execute the main function
      return matches(varText.getStringValue(), strPattern.getStringValue(), strPatternNo.getStringValue());
    } catch (Exception ex) {
      // Show error
      logger.error("Extensions/matches error: " + ex.getMessage() + "\r\n");
      return false;
    }
  }
  private static boolean matches(String strText, String strPattern, String strPatternNo) {
    Pattern[] arPattYes;  // Array of patterns to be matched
    Pattern[] arPattNo;   // Array of patterns that must *not* be matched
    
    try {
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
      logger.error("Extensions/matches error: " + ex.getMessage() + "\r\n");
      return false;
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
  public static String NodeText(XPathContext objXp, NodeInfo node) {
    return NodeText(objXp, node, "");
  }
  public static String NodeText(XPathContext objXp, NodeInfo node, String strType) {
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
      logger.error("Extensions/NodeText error: " + ex.getMessage() + "\r\n");
      // Return failure
      return "";
    }
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
    public static Node line(XPathContext objXp, NodeInfo node, int intLines) {
      XdmNode ndSax;    // Myself, if I am a proper node
      int nodeKind;     // The kind of object getting passed as argument

      try {
        // Validate
        if (node == null) return null;
        nodeKind = node.getNodeKind();
        if (nodeKind != Type.ELEMENT) return null;
        // Get the XdmNode representation of the node
        ndSax = objSaxDoc.wrap(node);      
        
        return null;
      } catch (Exception ex) {
        // Show error
        logger.error("Extensions/line: " + ex.getMessage());
        // Return failure
        return null;
      }
    }
    public static Node line(XPathContext objXp, int intLines) {

      try {
        // Validate
        
        return null;
      } catch (Exception ex) {
        // Show error
        logger.error("Extensions/line: " + ex.getMessage());
        // Return failure
        return null;
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
        case "eTree": case "nt": case "t": case "Result":
          // Okay, perfect
          break;
        case "Feature": case "Text": case "Psd":
          // Return the <Result> element above (this is a CrpOview type)
          ndSax = ndSax.getParent();
          break;
        case "eLeaf":
          // Return the <eTree> element above
          ndSax = ndSax.getParent();
          break;
        case "edge":
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
              strLoc.argValue = ndFor.getAttributeValue(RuBase.ru_qnFoliaId);
              strFile.argValue = ndFor.getAttributeValue(RuBase.ru_qnFile);
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
