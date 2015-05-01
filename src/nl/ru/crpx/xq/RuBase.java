/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.xq;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.project.CrpGlobal;
import static nl.ru.crpx.project.CrpGlobal.DoError;
import static nl.ru.crpx.xq.English.VernToEnglish;
import nl.ru.util.StringUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Erwin R. Komen
 */
public class RuBase extends CrpGlobal {
  // This class uses a logger
  private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RuBase.class);
// <editor-fold defaultstate="collapsed" desc="Local and global variables">
  // ==================== local variables ======================================
  // private CrpGlobal objGen;
  private String strRuLexFile;  // Location of the ru:lex file
  private XPath xpath = XPathFactory.newInstance().newXPath();
  private XPathExpression ru_xpNodeText_Psdx;   // Search expression for ProjPsdx
  private XPathExpression ru_xpNodeText_Folia;  // Search expression for ProjFolia
  private XPathExpression ru_xpNodeText_Negra;  // Search expression for ProjNegra
  private XPathExpression ru_xpNodeText_Alp;    // Search expression for ProjAlp
  // ===================== public constants ====================================
  public static final QName ru_qnETreeId = new QName("", "", "Id");      // Id feature name of an <eTree> element
  public static final QName ru_qnTreeId = new QName("", "", "TreeId");   // TreeId feature name
  public static final QName ru_qnMsg = new QName("", "", "Msg");         // Message feature name
  public static final QName ru_qnCat = new QName("", "", "Cat");         // Cat feature name
  public static final QName ru_qnFile = new QName("", "", "File");       // File feature name
  public static final QName ru_qnForId = new QName("", "", "forestId");  // forestId feature name
  public static final QName ru_qnTextId = new QName("", "", "TextId");   // TextId feature name
  public static final QName ru_qnLoc = new QName("", "", "Location");    // Location feature name
  public static final QName ru_qnNegraLoc = new QName("", "", "id");     // location in negra <s> node
  public static final QName ru_qnNegraId = new QName("", "", "id");      // Id of negra <s> or <t> node
  public static final QName ru_qnNegraEdgeId = new QName("", "", "refid");// Id of negra <edge> node
  public static final QName ru_qnFoliaId = new QName("", "", "id");      // Id of negra <s> or <t> node
  public static final QName ru_qnResultLoc = new QName("", "", "Search");
  public static final QName ru_qnEleaf = new QName("", "", "eLeaf");     // Nodename for <eLeaf> nodes

  // =========================== Local constants ===============================
  private final String RU_LEX = "-Lex";
  private final String RU_OUT = "-Out.csv";
  private final String RU_DISTRI = "-Distri";
  private final String RU_TIMBL = "-Timbl";

  /* ========= replaced by "public static final" ones above ===================
  private final QName qnIdField = new QName("", "", "id");
  private final QName qnIdRefField = new QName("", "", "idref");
  private final QName qnIdPsdx = new QName("", "", "Id");
  private final QName qnIdResult = new QName("", "", "eTreeId");
  private final QName qnForestIdPsdx = new QName("", "", "forestId");
  private final QName qnIdFoliaAll = new QName("", "", "id");
  */
  private static CorpusResearchProject prjThis;
// </editor-fold>

  // =========================== instantiate the class =========================
  public RuBase() {
    try {
      // objGen = new CrpGlobal();
      prjThis = crpThis;
      ru_xpNodeText_Psdx = xpath.compile("./descendant::eLeaf[@Type = 'Vern' or @Type = 'Punct']");
      ru_xpNodeText_Folia = xpath.compile("./descendant::w/child::t");
      ru_xpNodeText_Alp = xpath.compile("./descendant::node[count(@word)>0]");
    } catch (XPathExpressionException ex) {
      logger.error("RuBase initialisation error", ex);
    }
  }
// <editor-fold defaultstate="collapsed" desc="Initialisation for Extensions">

  // ------------------------------------------------------------------------------------
  // Name:   RuInitLex
  // Goal:   Initialise output file(s) for RU lexicon
  // History:
  // 03-10-2013  ERK Created for .NET
  // ??/??/2015  ERK Adapted for Java
  // ------------------------------------------------------------------------------------
  public boolean RuInitLex(boolean bForce) {

    try {
      // TODO: implement
      
      // Return positively
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      DoError("RuBase/RuInitLex error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
  public boolean RuInitLex() {
    return RuInitLex(false);
  }
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Implementation of Extensions">

  // ------------------------------------------------------------------------------------
  // Name:   RuAddLex
  // Goal:   Add the word to the lexicon
  // History:
  // 03-10-2013  ERK Created for .NET
  // 29/apr/2015 ERK Implemented for Java
  // ------------------------------------------------------------------------------------
  public boolean RuAddLex(String strLexeme, String strPos, int intQC) {
    String strFile;     // Which file to use
    String strOut;      // What we append
    
    try {
      // Try initialize
      if (!RuInitLex()) return false;
      // Determine file name
      strFile = strRuLexFile + "_QC" + intQC + ".xml";
      // TODO:  Check if we need a preamble
      
      // Return positively
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      DoError("RuBase/RuAddLex error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
  public boolean RuAddLex(String strLexeme, String strPos) {
    return RuAddLex(strLexeme, strPos, intCurrentQCline);
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  RuConv
  // Goal :  Convert the string according to [strType]
  // History:
  // 21-04-2014  ERK Created for .NET 
  // 30/apr/2015 ERK Ported to Java
  // ----------------------------------------------------------------------------------------------------------
  public String RuConv(String strIn, String strType) {
    String strOut = strIn;  // What we return
    String[] arType;        // Array of types
    
    try {
      // Trim the result
      strOut = strOut.trim();
      // Get all operations
      arType = strType.split("+");
      for (String sTypeThis : arType) {
        switch(sTypeThis.toLowerCase()) {
          case "clean":
            // Replace ";" with spaces and convert to lower case
            strOut = VernToEnglish(strOut.replace(";", " ").toLowerCase());
            // Remove "$" signs and single quotes "'"
            strOut = strOut.replace("$", "");
            strOut = strOut.replace("'", "");
            break;
          case "lcase":
            strOut = strOut.toLowerCase();
            break;
          case "ucase":
            strOut = strOut.toUpperCase();
            break;
          case "semi":
            strOut = strOut.replace(";", " ");
            break;
          case "oe":
            strOut = VernToEnglish(strOut);
            break;
          default:
            // Complain about this
            logger.error("ru:conv error - type unknown: " + sTypeThis);
            bInterrupt = true;
            return "";
        }
      }
      
      // Return the result
      return strOut;
    } catch (RuntimeException ex) {
      // Warn user
      DoError("RuBase/RuConv error: " + ex.getMessage() + "\r\n");
      // Return failure
      return "";
    }
  }

  // ------------------------------------------------------------------------------------
  // Name:   RuNodeText
  // Goal:   Convert the node provided into a labelled text
  // History:
  // 01-11-2010  ERK Created for .NET as GetNodeText()
  // 12-02-2014  ERK Added [strType]
  // 29/apr/2015 ERK Implemented for Java
  // ------------------------------------------------------------------------------------
  public String RuNodeText(XdmNode ndStart) {
    // Call the generalized NodeText function
    return RuNodeText(ndStart, "");
  }
  public String RuNodeText(XdmNode ndStart, String strType) {
    NodeList ndList;  // Result of looking for the end-nodes
    String[] arSent;  // The whole sentence
    
    try {
      // Validate
      if (ndStart == null) return "";
      // Default value for array
      arSent = null;
      // Action depends on the kind of xml project we have
      switch(prjThis.intProjType) {
        case ProjPsdx:
          // Make a list of all <eLeaf> nodes
          ndList = (NodeList) ru_xpNodeText_Psdx.evaluate(ndStart, XPathConstants.NODESET);
          arSent = new String[ndList.getLength()];
          for (int i=0;i<ndList.getLength();i++) {
            Node ndThis = ndList.item(i);
            arSent[i] = ndThis.getAttributes().getNamedItem("Text").getNodeValue();
          }
          break;
        case ProjFolia:
          // Make a list of all <t> nodes that have a <w> parent
          ndList = (NodeList) ru_xpNodeText_Folia.evaluate(ndStart, XPathConstants.NODESET);
          arSent = new String[ndList.getLength()];
          for (int i=0;i<ndList.getLength();i++) {
            Node ndThis = ndList.item(i);
            // Combine the innertext values of the <t> nodes
            arSent[i] = ndThis.getNodeValue();
          }
          break;
        case ProjAlp:
          // Make a list of all <eLeaf> nodes
          ndList = (NodeList) ru_xpNodeText_Alp.evaluate(ndStart, XPathConstants.NODESET);
          arSent = new String[ndList.getLength()];
          for (int i=0;i<ndList.getLength();i++) {
            Node ndThis = ndList.item(i);
            // combine the @word attribute values
            arSent[i] = ndThis.getAttributes().getNamedItem("word").getNodeValue();
          }
          break;
        case ProjNegra:
          // TODO: implement
          break;
        default:
          logger.error("RuNodeText: cannot process type " + prjThis.getProjectType());
      }
      // Build a string from the array
      return StringUtil.join(arSent, " ");
    } catch (RuntimeException | XPathExpressionException ex) {
      // Warn user
      logger.error("RuBase/RuNodeText error: " + ex.getMessage() + "\r\n");
      // Return failure
      return "";
    }
  }
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Support functions for Extensions">

  // ------------------------------------------------------------------------------------
  // Name:   GetRefId
  // Goal:   Retrieve the @id or @Id or @idref of this node
  // History:
  // 07-07-2011  ERK Created for .NET
  // 18-12-2012  ERK Added capabilities to process <CrpOview> --> <Result>
  // 29/apr/2015 ERK Ported to Java
  // ------------------------------------------------------------------------------------
  public final String GetRefId(XdmNode ndSax) {
    String strRef = ""; // The ID of the second node
    String sNodeName;

    try {
      // Validate
      if (ndSax == null) return "";
      // Initialize values
      sNodeName = ndSax.getNodeName().getLocalName();
      switch (prjThis.intProjType) {
        case ProjNegra:
          // Get the ID of the node in [ndSax]
          switch (sNodeName) {
            case "nt":
            case "t":
              // Get the @id field
              strRef = ndSax.getAttributeValue(ru_qnNegraId);
              break;
            case "edge":
              // Get the @id field
              strRef = ndSax.getAttributeValue(ru_qnNegraEdgeId);
              break;
            default:
              // Cannot handle this
              return "";
          }
          break;
        case ProjPsdx:
          // Get the ID of the node in [ndSax]
          switch (sNodeName) {
            case "Result":
              // Get the @eTreeId field within the <Result> element
              strRef = ndSax.getAttributeValue(ru_qnETreeId);
              break;
            case "eTree":
              // Get the Id of the <eTree> element
              strRef = ndSax.getAttributeValue(ru_qnETreeId);
              break;
            case "forest":
              // Get the Id of the <eTree> element
              strRef = ndSax.getAttributeValue(ru_qnForId);
              break;
            default:
              // Cannot handle this
              return "";
          }
          break;
        case ProjFolia:
          // Nodes in folia have the same kind of identifier: [xml:id]
          strRef = ndSax.getAttributeValue(ru_qnFoliaId);
          break;
        default:
      }
      // Return the reference string we found
      return strRef;
    } catch (RuntimeException ex) {
      // Show error
      DoError("RuBase/GetRefId error: " + ex.getMessage() + "\r\n");
      // Return failure
      return "";
    }
  }
// </editor-fold>
}
