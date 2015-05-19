/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.xq;

import java.util.regex.Pattern;
import javax.xml.xpath.XPathExpressionException;
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
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.project.CorpusResearchProject.ProjType;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.tools.FileIO;
import nl.ru.util.ByRef;
import nl.ru.xmltools.XmlDocument;
import nl.ru.xmltools.XmlNode;
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
  private static ProjType loc_intProjType;
  private static String loc_sNodeNameSnt;
  private static ErrHandle errHandle;
  // ============== Local constants ============================================
  private static final QName loc_qnName = new QName("", "", "name");
  private static final QName loc_qnValue = new QName("", "", "value");
  private static final QName loc_qnFs = new QName("", "", "fs");
  private static final QName loc_qnF = new QName("", "", "f");

  // ============== CLASS initialization =======================================
  public Extensions(CorpusResearchProject objPrj) {
    // Make sure the class I extend is initialized
    super(objPrj);
    // Set my own error handler
    errHandle = new ErrHandle(Extensions.class);
    // Initialize a list of string arrays to help ru:matches()
    objStore = new PatternStore(errHandle);
    // Set my base
    // objBase = new RuBase(objPrj);
  }
// <editor-fold defaultstate="collapsed" desc="ru:back">
  // ------------------------------------------------------------------------------------
  // Name:   back
  // Goal:   Return the ancestor <forest>
  // History:
  // 26-04-2012  ERK Created for .NET
  // 29/apr/2015 ERK Adapted for Java
  // ------------------------------------------------------------------------------------
  public static XmlNode back(XPathContext objXp, XdmValue valSax) {
    XdmNode ndSax;                            // The actual node
    XmlNode ndxFor = null;                    // The new forest node we make
    XmlDocument pdxThis;                      // Where we provide the feedback
    ByRef<String> strLoc = new ByRef("");     // Location feature
    ByRef<String> strFile =new ByRef("");     // File feature
    ByRef<String> strTreeId =new ByRef("");   // TreeId feature
    ByRef<String> strForestId =new ByRef(""); // ForestId feature
    
    try {
      // Validate
      ndSax = getNodeValue(valSax);
      if (ndSax == null) return null;
      // ProjPsdx preparations: get appropriate values for each of the <forest> elements
      if (!PrepareBack(objXp, ndSax,strLoc, strFile, strForestId, strTreeId)) return null;
      // Add a <forest> node
      String sNode = "<forest Location=\"" + strLoc.argValue + "\"" + 
              " File=\"" + strFile.argValue + "\"" + 
              " forestId=\"" + strForestId.argValue + "\"" + 
              " TreeId=\"" + strTreeId.argValue + "\"" + 
              "/>";
      
      pdxThis = new XmlDocument(objSaxDoc, objSaxon);
      pdxThis.LoadXml(sNode);
      ndxFor = pdxThis.SelectSingleNode("//forest");

      // Return positively
      return ndxFor;
    } catch (RuntimeException | XPathExpressionException | SaxonApiException ex) {
      // Warn user
      errHandle.DoError("Extensions/back() error", ex, Extensions.class);
      // Return failure
      return null;
    }
  }
  public static XmlNode back(XPathContext objXp, XdmValue valSax, AtomicValue strMsg) {
    return back(objXp, valSax, strMsg.getStringValue());
  }
  public static XmlNode back(XPathContext objXp, XdmValue valSax, String strMsg) {
    XdmNode ndSax;                            // The actual node
    XmlNode ndxFor = null;                    // The new forest node we make
    ByRef<String> strLoc = new ByRef("");     // Location feature
    ByRef<String> strFile =new ByRef("");     // File feature
    ByRef<String> strTreeId =new ByRef("");   // TreeId feature
    ByRef<String> strForestId =new ByRef(""); // ForestId feature
    XmlDocument pdxThis;                      // Where we provide the feedback
    
    try {
      // Validate
      ndSax = getNodeValue(valSax);
      if (ndSax == null) return null;
      // ProjPsdx preparations: get appropriate values for each of the <forest> elements
      if (!PrepareBack(objXp, ndSax,strLoc, strFile, strForestId, strTreeId)) return null;
      // Make sure the message is okay
      strMsg = strMsg.replace("'", "''");
      // Add a <forest> node
      String sNode = "<forest Location=\"" + strLoc.argValue + "\"" + 
              " File=\"" + strFile.argValue + "\"" + 
              " forestId=\"" + strForestId.argValue + "\"" + 
              " TreeId=\"" + strTreeId.argValue + "\"" + 
              " Msg=\"" + strMsg + "\"" + 
              "/>";
      pdxThis = new XmlDocument(objSaxDoc, objSaxon);
      pdxThis.LoadXml(sNode);
      ndxFor = pdxThis.SelectSingleNode("//forest");
      
      // Return positively
      return ndxFor;
    } catch (RuntimeException | XPathExpressionException | SaxonApiException ex) {
      // Warn user
      errHandle.DoError("Extensions/back() error", ex, Extensions.class);
      // Return failure
      return null;
    }
  }
  public static XmlNode back(XPathContext objXp, XdmValue valSax, AtomicValue strMsg, AtomicValue strCat) {
    return back(objXp, valSax, strMsg.getStringValue(), strCat.getStringValue());
  }
  public static XmlNode back(XPathContext objXp, XdmValue valSax, String strMsg, String strCat) {
    XdmNode ndSax;                            // The actual node
    XmlNode ndxFor = null;                    // The new forest node we make
    ByRef<String> strLoc = new ByRef("");     // Location feature
    ByRef<String> strFile =new ByRef("");     // File feature
    ByRef<String> strTreeId =new ByRef("");   // TreeId feature
    ByRef<String> strForestId =new ByRef(""); // ForestId feature
    XmlDocument pdxThis;                      // Where we provide the feedback
    
    try {
      // Validate
      ndSax = getNodeValue(valSax);
      if (ndSax == null) return null;
      // ProjPsdx preparations: get appropriate values for each of the <forest> elements
      if (!PrepareBack(objXp, ndSax,strLoc, strFile, strForestId, strTreeId)) return null;
      // Make sure the message is okay
      strMsg = strMsg.replace("'", "''");
      // Make sure the CAT is okay
      strCat = strCat.replace("'", "''");
      // Add a <forest> node
      String sNode = "<forest Location=\"" + strLoc.argValue + "\"" + 
              " File=\"" + strFile.argValue + "\"" + 
              " forestId=\"" + strForestId.argValue + "\"" + 
              " TreeId=\"" + strTreeId.argValue + "\"" + 
              " Msg=\"" + strMsg + "\"" + 
              " Cat=\"" + strCat + "\"" + 
              "/>";
      pdxThis = new XmlDocument(objSaxDoc, objSaxon);
      pdxThis.LoadXml(sNode);
      ndxFor = pdxThis.SelectSingleNode("//forest");
      
      // Return positively
      return ndxFor;
    } catch (RuntimeException | XPathExpressionException | SaxonApiException ex) {
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
  public static String conv(XPathContext objXp, AtomicValue strText, String strType) {
    // Call the main ru:conv() handling function
    return conv(objXp, strText.getStringValue(), strType);
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
      strText = objBase.RuConv(strText, strType);
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
  public static String feature(XdmValue valSax, AtomicValue strFeatName, AtomicValue strType) {
    String strFval = "";

    try {
      // Get feature value from the function ru:feature()
      strFval = feature(valSax, strFeatName);
      // Perform conversion if needed
      strFval = objBase.RuConv(strFval, strType.getStringValue());
      // Return the result
      return strFval;
    } catch (RuntimeException ex) {
      // Show error
      errHandle.DoError("Extensions/feature error", ex, Extensions.class );
      // Return failure
      return "";
    }
  }
  public static String feature(XdmValue valSax, AtomicValue strFeatName) {
    XdmNode ndSax = null;             // Myself, if I am a proper node
    XdmNode ndFS = null;              // The FS nodes
    XdmNode ndF = null;               // Potential F nodes
    XdmSequenceIterator colFS = null; // Iterate through <fs>
    XdmSequenceIterator colF = null;  // Iterate through <f>

    try {
      // Get node and validate it
      ndSax = getNodeValue(valSax); if (ndSax == null) return "";
      // Initialise depending on project type
      switch (loc_intProjType) {
        case ProjPsdx:
          // We can only get features for an [eTree] node
          if (!ndSax.getNodeName().getLocalName().equals("eTree")) return "";
          break;
        default:
          // This method doesn't work for other projects
          errHandle.DoError("ru:feature() is not implemented for this project", Extensions.class);
          return "";
      }
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
      // Return failure
      return "";
    } catch (SaxonApiUncheckedException ex) {
      // Show error
      errHandle.DoError("Extensions/feature error", ex, Extensions.class );
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
    return objBase.RuAddLex(objXp, strText, strPos);
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
  /*
  public static boolean matches(XdmValue varText, String strPattern) {
    String strText;   // The string value we are matching
    
    // Validate: look what kind of Xdm we receive
    if (!hasStringValue(varText)) return false;
    strText = getStringValue(varText);
    // Execute the main matching function
    return matches(strText, strPattern);
  }
  */
  public static boolean matches(AtomicValue varText, AtomicValue strPattern) {
    return matches(varText.getStringValue(), strPattern.getStringValue());
  } 
  private static boolean matches(String strText, String strPattern) {
    Pattern[] arPatt; // Array of patterns to be matched
    
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
  }
  /*
  public static boolean matches(XdmValue varText, String strPattern, String strPatternNo) {
    String strText;       // The string value we are matching
    
    // Validate: look what kind of Xdm we receive
    if (!hasStringValue(varText)) return false;
    strText = getStringValue(varText);
    // Execute the main function
    return matches(strText, strPattern, strPatternNo);
  } */
  public static boolean matches(AtomicValue varText, AtomicValue strPattern, AtomicValue strPatternNo) {
    // Execute the main function
    return matches(varText.getStringValue(), strPattern.getStringValue(), strPatternNo.getStringValue());
  }
  private static boolean matches(String strText, String strPattern, String strPatternNo) {
    Pattern[] arPattYes;  // Array of patterns to be matched
    Pattern[] arPattNo;   // Array of patterns that must *not* be matched
    
    // Validate: empty strings
    if (strText.isEmpty()) return false;
    // Make sure to be case-insensitive
    strText = strText.toLowerCase();
    // Reduce the [strPattern]
    strPattern = strPattern.toLowerCase().trim();
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
    private static String NodeText(XPathContext objXp, NodeInfo node, String strType) {
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
    } catch (IllegalArgumentException ex) {
      // Show error
      logger.error("Extensions/NodeText error: " + ex.getMessage() + "\r\n");
      // Return failure
      return "";
    }
  }

  /*
  public static XdmValue NodeText(XPathContext objXp, XdmValue valSax, AtomicValue strType) {
    return NodeText(objXp, valSax, strType.getStringValue());
  }
  private static XdmValue NodeText(XPathContext objXp, XdmValue valSax, String strType) {
    // XmlDocument docThis = new XmlDocument(); // Where we store it
    XdmNode ndSax = null; // Myself, if I am a proper node
    String sResult = "";  // Resulting value
    XdmValue oBack;

    try {
      // Validate
      ndSax = getNodeValue(valSax); 
      if (ndSax==null) return loc_EmptyString;
      // Transform to XML document
      // docThis.LoadXml(ndSax.OuterXml);
      
      // Convert to text
      // return GetNodeText(docThis.FirstChild, strType);
      sResult = objBase.RuNodeText(objXp, ndSax, strType);
      oBack = new XdmAtomicValue(sResult);
      return oBack;
    } catch (RuntimeException ex) {
      // Show error
      logger.error("Extensions/NodeText error: " + ex.getMessage() + "\r\n");
      // Return failure
      return loc_EmptyString;
    }
  } */
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
  //         This function is only called from the three variants of ru:back()
  // History:
  // 12-06-2012  ERK Created for .NET
  // 29/apr/2015 ERK Ported to Java
  // ------------------------------------------------------------------------------------
  private static boolean PrepareBack(XPathContext objXp, XdmNode ndSax, ByRef<String> strLoc, ByRef<String> strFile,
          ByRef<String> strForestId, ByRef<String> strTreeId) {
    XdmNode ndFor;    // The forest I am part of
    XmlNode ndxFor;   // The <forest> node in the document
    
    try {
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
      // Get the id value
      strTreeId.argValue = objBase.GetRefId(objXp, ndSax);
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
          // Determine the forest I am part of
          ndFor = ndSax; strForestId.argValue = ""; strFile.argValue = ""; strLoc.argValue = "";
          while (!ndSax.getClass().getName().equals("XdmEmptySequence")) {
            String sNodeName = ndSax.getNodeName().getLocalName();
            // Make sure this is not the highest element that can occur, given the proejct
            if (sNodeName.equals(loc_sNodeNameSnt)) break;
            // Step to the parent
            ndFor = ndFor.getParent();
          }
          // Check the result
          if (ndFor == null || ndFor.getClass().getName().equals("XdmEmptySequence")) return false;
          // Get the values of all the features we need
          switch (loc_intProjType) {
            case ProjPsdx:
              strLoc.argValue = ndSax.getAttributeValue(RuBase.ru_qnLoc);
              strFile.argValue = ndSax.getAttributeValue(RuBase.ru_qnFile);
              strForestId.argValue = ndSax.getAttributeValue(RuBase.ru_qnForId);
              break;
            case ProjFolia:
              // TODO: check and implement correctly for folia
              strLoc.argValue = ndSax.getAttributeValue(RuBase.ru_qnFoliaId);
              strFile.argValue = ndSax.getAttributeValue(RuBase.ru_qnFile);
              strForestId.argValue = ndSax.getAttributeValue(RuBase.ru_qnFoliaId);
              break;
            case ProjNegra:
              // TODO: check and implement correctly for negra
              
              break;
            case ProjAlp:
              // TODO: check and implement correctly for alpino
              
              break;
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
