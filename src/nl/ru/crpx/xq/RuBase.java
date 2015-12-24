/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.xq;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.tools.ErrHandle;
import static nl.ru.crpx.xq.English.VernToEnglish;
import nl.ru.util.json.JSONObject;
import nl.ru.xmltools.XmlNode;
import org.xml.sax.InputSource;
import org.w3c.dom.Node;

/**
 *
 * @author Erwin R. Komen
 */
public class RuBase /* extends Job */ {
  // This class uses a logger
  private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RuBase.class);

// <editor-fold defaultstate="collapsed" desc="Local and global variables">
  // ======== attempt ============
  protected List<JSONObject> lAttempt;
  // ==================== Initialisation flag ==================================
  private static boolean bInit = false;
  // ==================== static list of all CRPs that are using me ============
  private static List<CrpFile> lCrpCaller;  // This is used by "Extensions"
  // ==================== local variables ======================================
  private String strRuLexFile;              // Location of the ru:lex file
  private XPath xpath;
  private XPathCompiler xpComp;             // My own xpath compiler (Xdm, saxon)
  static Processor objSaxon;                // The processor above the document builder
  static DocumentBuilder objSaxDoc;         // Internal copy of the document-builder to be used
  // ===================== local constant stuff ================================
  private static ErrHandle errHandle;
  private static XPathSelector ru_xpeNodeText_Psdx;   // Search expression for ProjPsdx
  private static XPathSelector ru_xpeNodeText_Folia;  // Search expression for ProjFolia
  private static XPathSelector ru_xpeNodeText_Folia2; // We need a second expression for ProjFolia
  private static XPathSelector ru_xpeNodeText_Negra;  // Search expression for ProjNegra
  private static XPathSelector ru_xpeNodeText_Alp;    // Search expression for ProjAlp  
  private static XPathSelector ru_xpeWords_Psdx;      // Search expression for ProjPsdx
  private static XPathSelector ru_xpeWords_Folia;     // Search expression for ProjFolia
  private static XPathSelector ru_xpeWords_Folia2;    // We need a second expression for ProjFolia
  private static XPathSelector ru_xpeWords_Negra;     // Search expression for ProjNegra
  private static XPathSelector ru_xpeWords_Alp;       // Search expression for ProjAlp  
  
  private static XPathSelector ru_xpPreceding_Psdx;   // List of preceding nodes for ProjPsdx
  private static XPathSelector ru_xpPreceding_Folia;  // List of preceding nodes for ProjFolia
  private static XPathSelector ru_xpPreceding_Alp;    // List of preceding nodes for ProjAlp

  private static XPathSelector ru_xpFollowing_Psdx;   // List of following nodes for ProjPsdx
  private static XPathSelector ru_xpFollowing_Folia;  // List of following nodes for ProjFolia
  private static XPathSelector ru_xpFollowing_Alp;    // List of following nodes for ProjAlp

  private static XPathSelector ru_xpDescendant_Psdx;  // List of descendant nodes for ProjPsdx
  private static XPathSelector ru_xpDescendant_Folia; // List of descendant nodes for ProjFolia
  private static XPathSelector ru_xpDescendant_Alp;   // List of descendant nodes for ProjAlp

  private static XPathSelector ru_xpSibling_Psdx;     // List of descendant nodes for ProjPsdx
  private static XPathSelector ru_xpSibling_Folia;    // List of descendant nodes for ProjFolia
  private static XPathSelector ru_xpSibling_Alp;      // List of descendant nodes for ProjAlp

  // ===================== public constants ====================================
  public static final QName ru_qnETreeId = new QName("", "", "Id");       // Id feature name of an <eTree> element
  public static final QName ru_qnTreeId = new QName("", "", "eTreeId");    // TreeId feature name
  // public static final QName ru_qnTreeId = new QName("", "", "TreeId");    // TreeId feature name
  public static final QName ru_qnMsg = new QName("", "", "Msg");          // Message feature name
  public static final QName ru_qnCat = new QName("", "", "Cat");          // Cat feature name
  public static final QName ru_qnFile = new QName("", "", "File");        // File feature name
  public static final QName ru_qnForId = new QName("", "", "forestId");   // forestId feature name
  public static final QName ru_qnText = new QName("", "", "Text");        // TextId feature name
  public static final QName ru_qnTextId = new QName("", "", "TextId");    // TextId feature name
  public static final QName ru_qnLoc = new QName("", "", "Location");     // Location feature name
  public static final QName ru_qnNegraLoc = new QName("", "", "id");      // location in negra <s> node
  public static final QName ru_qnNegraId = new QName("", "", "id");       // Id of negra <s> or <t> node
  public static final QName ru_qnNegraEdgeId = new QName("", "", "refid");// Id of negra <edge> node
  public static final QName ru_qnAlpinoId = new QName("", "", "id");      // Identifier for Alpinoa <node> elements
  public static final QName ru_qnResultLoc = new QName("", "", "Search");
  public static final QName ru_qnEleaf = new QName("", "", "eLeaf");      // Nodename for <eLeaf> nodes
  public static final QName ru_qnWord = new QName("", "", "word");        // The @word attribute
  // FoLiA processing: the xml:id of <su>, <s> and other elements
  public static final QName ru_qnFoliaId = new QName("xml", "http://www.w3.org/XML/1998/namespace", "id");
  public static final QName ru_qnFoliaWrefId = new QName("", "", "id");     // Simple identifier for <wref> element
  public static final QName ru_qnFoliaWrefT = new QName("", "", "t");       // Text in a <wref> element

  // =========================== Local constants ===============================
  private final String RU_LEX = "-Lex";
  private final String RU_OUT = "-Out.csv";
  private final String RU_DISTRI = "-Distri";
  private final String RU_TIMBL = "-Timbl";

// </editor-fold>

  // =========================== instantiate the class =========================
  public RuBase(CorpusResearchProject objPrj) {
    try {
      // this.xpath = XPathFactory.newInstance(NamespaceConstant.OBJECT_MODEL_SAXON).newXPath();
      this.xpath = XPathFactory.newInstance().newXPath();
      // Take over the Processor that has already been made
      this.objSaxon = objPrj.getSaxProc();
      // Every new project gets its own documentbuilder (??)
      this.objSaxDoc = this.objSaxon.newDocumentBuilder();
      errHandle = new ErrHandle(RuBase.class);
      // Only reset the caller list if it has not yet been initialized
      if (!bInit) {
         lCrpCaller = new ArrayList<>();
        // Set up the compiler
        this.xpComp = this.objSaxon.newXPathCompiler();
        // Create search expressions for NodeText
        ru_xpeNodeText_Psdx = xpComp.compile("./descendant-or-self::eLeaf[@Type = 'Vern' or @Type = 'Punct']").load();
        ru_xpeNodeText_Folia = xpComp.compile("./descendant-or-self::wref[count(ancestor::su[@class='CODE'])=0]").load();
        ru_xpeNodeText_Folia2 = xpComp.compile("./ancestor::s/child::w[not(@class) or @class='Punct' or @class='Vern']").load();
        ru_xpeNodeText_Alp = xpComp.compile("./descendant-or-self::node[count(@word)>0]").load();
        ru_xpeNodeText_Negra = xpComp.compile("./descendant-or-self::t").load();
        // Create search expressions for RuWords()
        ru_xpeWords_Psdx = xpComp.compile("./descendant-or-self::eLeaf[@Type = 'Vern' and count(ancestor::eTree[@Label='CODE'])=0]").load();
        ru_xpeWords_Folia = xpComp.compile("./descendant-or-self::wref[count(ancestor::su[@class='CODE'])=0]").load();
        ru_xpeWords_Folia2 = xpComp.compile("./ancestor::s/child::w[not(@class) or @class='Vern']").load();
        ru_xpeWords_Alp = xpComp.compile("./descendant-or-self::node[count(@word)>0]").load();
        ru_xpeWords_Negra = xpComp.compile("./descendant-or-self::t").load();
        // Create search expressions for RuRelates()
        // (1) PRECEDING
        ru_xpPreceding_Psdx = xpComp.compile("./following::eTree").load();
        ru_xpPreceding_Folia = xpComp.compile("./following::su").load();
        ru_xpPreceding_Alp = xpComp.compile("./following::node").load();
        // (2) FOLLOWING
        ru_xpPreceding_Psdx = xpComp.compile("./preceding::eTree").load();
        ru_xpPreceding_Folia = xpComp.compile("./preceding::su").load();
        ru_xpPreceding_Alp = xpComp.compile("./preceding::node").load();        
        // (3) DESCENDANT
        ru_xpPreceding_Psdx = xpComp.compile("./descendant::eTree").load();
        ru_xpPreceding_Folia = xpComp.compile("./descendant::su").load();
        ru_xpPreceding_Alp = xpComp.compile("./descendant::node").load();        
        // (4) SIBLING
        ru_xpPreceding_Psdx = xpComp.compile("./preceding-sibling::eTree | ./following-sibling::eTree").load();
        ru_xpPreceding_Folia = xpComp.compile("./preceding-sibling::su | ./following-sibling::su").load();
        ru_xpPreceding_Alp = xpComp.compile("./preceding-sibling::node | ./following-sibling::node").load();        
        // Indicate we are initialized
        bInit = true;
      }
    } catch (SaxonApiException ex) {
        logger.error("RuBase initialisation error", ex);
    }
  }
// <editor-fold defaultstate="collapsed" desc="Handling of the CrpCaller list">
  // ------------------------------------------------------------------------------------
  // Name:   setCrpCaller
  // Goal:   Add this crp from the list if it is not yet there
  // History:
  // 11/may/2015  ERK Created for Java
  // ------------------------------------------------------------------------------------
  public static boolean setCrpCaller(CrpFile oCrp) {
    try {
      // Check if it is not yet there
      if (!hasCrpCaller(oCrp)) {
        // The object is not yet on the stack
        //    So: we add it there
        synchronized(lCrpCaller) {
          lCrpCaller.add(oCrp);
        }
      }
      // Return positively
      return true;
    }  catch (RuntimeException ex) {
      // Warn user
      errHandle.DoError("RuBase/setCrpCaller error", ex, RuBase.class);
      // Return failure
      return false;
    }
  }
  // ------------------------------------------------------------------------------------
  // Name:   getCrpCaller
  // Goal:   Get the @id value of the CRP/File combination
  // History:
  // 11/may/2015  ERK Created for Java
  // ------------------------------------------------------------------------------------
  public static int getCrpCaller(CrpFile oCrp) {
    try {
      synchronized(lCrpCaller) {
        // Check if it is not yet there
        for (CrpFile oThis: lCrpCaller) {
          if (oThis.equals(oCrp)) {
            return lCrpCaller.indexOf(oThis);
          }
        }
      }
      // Return failure: negative index
      return -1;
    }  catch (RuntimeException ex) {
      // Warn user
      errHandle.DoError("RuBase/getCrpCaller error", ex, RuBase.class);
      // Return failure
      return -1;
    }
  }
  // ------------------------------------------------------------------------------------
  // Name:   getCrpFile
  // Goal:   Get the CrpFile of the entry with index @idx
  // History:
  // 11/may/2015  ERK Created for Java
  // ------------------------------------------------------------------------------------
  public static CrpFile getCrpFile(int idx) {
    CrpFile oBack = null;
    
    try {
      synchronized(lCrpCaller) {
        if (idx < 0 || lCrpCaller.size() <= idx) {
          // Cannot get the caller what he/she wants
          errHandle.debug("RuBase/getCrpFile: request for idx="+idx+", but size="+lCrpCaller.size());
          return null;
        }
        oBack = lCrpCaller.get(idx);
      }
      return oBack;
    } catch (RuntimeException ex) {
      // Warn user
      errHandle.DoError("RuBase/getCrpFile error", ex, RuBase.class);
      // Return failure
      return null;
    }
  }
  // ------------------------------------------------------------------------------------
  // Name:   removeCrpCaller
  // Goal:   Remove this crp from the list
  // History:
  // 11/may/2015  ERK Created for Java
  // ------------------------------------------------------------------------------------
  public static boolean removeCrpCaller(CrpFile oCrp) {
    try {
      synchronized(lCrpCaller) {
        // Check if it is not yet there
        for (CrpFile oThis: lCrpCaller) {
          if (oThis.equals(oCrp)) {
            // Remove it from the list
            lCrpCaller.remove(oCrp);
            // Return positively
            return true;
          }
        }
      }
      // Return positively
      return true;
    }  catch (RuntimeException ex) {
      // Warn user
      errHandle.DoError("RuBase/removeCrpCaller error", ex, RuBase.class);
      // Return failure
      return false;
    }
  }
  // ------------------------------------------------------------------------------------
  // Name:   hasCrpCaller
  // Goal:   Check if this crp is on the list
  // History:
  // 11/may/2015  ERK Created for Java
  // ------------------------------------------------------------------------------------
  public static boolean hasCrpCaller(CrpFile oCrp) {
    try {
      synchronized(lCrpCaller) {
        // Check if it is not yet there
        for (CrpFile oThis: lCrpCaller) {
          if (oThis.equals(oCrp)) return true;
        }
      }
      // We don't have it
      return false;
    } catch (RuntimeException ex) {
      // Warn user
      errHandle.DoError("RuBase/hasCrpCaller error", ex, RuBase.class);
      // Return failure
      return false;
    }
  }
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Initialisation for the Extensions class">

  // ------------------------------------------------------------------------------------
  // Name:   RuInitLex
  // Goal:   Initialise output file(s) for RU lexicon
  // History:
  // 03-10-2013  ERK Created for .NET
  // ??/??/2015  ERK Adapted for Java
  // ------------------------------------------------------------------------------------
  static boolean RuInitLex(boolean bForce) {

    try {
      // TODO: implement
      
      // Return positively
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      errHandle.DoError("RuBase/RuInitLex error", ex, RuBase.class);
      // Return failure
      return false;
    }
  }
  static boolean RuInitLex() {
    return RuInitLex(false);
  }
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Implementation of functions in the Extensions class">

  // ------------------------------------------------------------------------------------
  // Name:   RuAddLex
  // Goal:   Add the word to the lexicon
  // History:
  // 03-10-2013  ERK Created for .NET
  // 29/apr/2015 ERK Implemented for Java
  // ------------------------------------------------------------------------------------
  static boolean RuAddLex(XPathContext objXp, String strLexeme, String strPos, int intQC) {
    // String strFile;     // Which file to use
    // String strOut;      // What we append
    LexDict ldThis;     // Reference to my lexdict
    
    try {
      // Try initialize
      // if (!RuInitLex()) return false;
      // Determine file name
      // strFile = strRuLexFile + "_QC" + intQC + ".xml";
      // TODO:  Check if we need a preamble
      
      // Get the LexDict object for this CrpFile combination
      ldThis = getCrpFile(objXp).getLexDictQC(intQC);
      // Validate
      if (ldThis == null) {errHandle.DoError("RuAddLex: could not find lexdict for line " + intQC); return false;}
      // Add the word/pos combination to the lexdict for this QC line
      ldThis.Add(strLexeme, strPos);
      
      // Return positively
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      errHandle.DoError("RuBase/RuAddLex error", ex, RuBase.class);
      // Return failure
      return false;
    }
  }
  static boolean RuAddLex(XPathContext objXp, String strLexeme, String strPos) {
    // int intCurrentQCline = (int) objXp.getController().getUserData("QCline", "QCline");
    int intCurrentQCline = ((CrpFile) objXp.getController().getParameter("crpfile")).QCcurrentLine;
    return RuAddLex(objXp, strLexeme, strPos, intCurrentQCline);
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  RuConv
  // Goal :  Convert the string according to [strType]
  // History:
  // 21-04-2014  ERK Created for .NET 
  // 30/apr/2015 ERK Ported to Java
  // ----------------------------------------------------------------------------------------------------------
  static String RuConv(String strIn, String strType) {
    String strOut = strIn;  // What we return
    String[] arType;        // Array of types
    
    try {
      // Trim the result
      strOut = strOut.trim();
      // Get all operations
      arType = strType.split("\\+");
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
            errHandle.bInterrupt = true;
            return "";
        }
      }
      
      // Return the result
      return strOut;
    } catch (RuntimeException ex) {
      // Warn user
      errHandle.DoError("RuBase/RuConv error", ex, RuBase.class);
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
  static String RuNodeText(XPathContext objXp, XdmNode ndStart) {
    // Call the generalized NodeText function
    return RuNodeText(objXp, ndStart, "");
  }
  public static String RuNodeText(XPathContext objXp, XdmNode ndStart, String strType) {
    String sBack;           // Resulting string
    XPathSelector selectXp; // The actual selector we are using
    StringBuilder sBuild;   // Resulting string that is being built
    CorpusResearchProject crpThis;
    
    try {
      // Validate
      if (ndStart == null) return "";
      // Determine which CRP this is
      CrpFile oCF = getCrpFile(objXp);
      crpThis = oCF.crpThis;
      // Default value for array
      selectXp = null; sBuild = new StringBuilder();
      // Action depends on the kind of xml project we have
      switch(crpThis.intProjType) {
        case ProjPsdx:
          // Validate: this should be a <forest> or <eTree> node
          switch(ndStart.getNodeName().getLocalName()) {
            case "eTree": case "forest": case "eLeaf":
              // Make a list of all <eLeaf> nodes
              selectXp = ru_xpeNodeText_Psdx;
              selectXp.setContextItem(ndStart);
              // Go through all the items
              for (XdmItem item : selectXp) {
                // Get the @Text attribute values
                sBuild.append(((XdmNode) item).getAttributeValue(ru_qnText)).append(" ");
              }
              break;
            default:
              // Default behaviour: get the string value of this node
              String sValue = ndStart.getStringValue();
              // Only add it if it is not empty
              if (!sValue.isEmpty()) sBuild.append(sValue);
          }
          break;
        case ProjFolia:
          // Retrieve the list of words from the dynamic context
          List<String> lSuId = getWordList(objXp);
          // Make a list of all the <wref> nodes under me
          selectXp = ru_xpeNodeText_Folia;
          selectXp.setContextItem(ndStart);
          // Go through all the items
          for (XdmItem item : selectXp) {
            // Check if this @id is in the list
            String sId =((XdmNode) item).getAttributeValue(ru_qnFoliaWrefId);
            if (lSuId.contains(sId)) {
              // Get the text value of the node
              String sTvalue = ((XdmNode) item).getAttributeValue(ru_qnFoliaWrefT);
              // ===========================
              // TEST: should not contain *
              if (sTvalue.contains("*")) {
                sTvalue = sTvalue;
              }
              // ===========================
              sBuild.append(sTvalue).append(" ");
            }
          }
          break;
        case ProjAlp:
          // Make a list of all end nodes; Alpino only uses <node> tags
          selectXp = ru_xpeNodeText_Alp;
          selectXp.setContextItem(ndStart);
          // Go through all the items
          for (XdmItem item : selectXp) {
            // Get the @word attribute values
            sBuild.append(((XdmNode) item).getAttributeValue(ru_qnWord)).append(" ");
          }
          break;
        case ProjNegra:
          // Make a list of all end nodes; Negra has them under <terminals> as <t> items
          selectXp = ru_xpeNodeText_Negra;
          selectXp.setContextItem(ndStart);
          // Go through all the items
          for (XdmItem item : selectXp) {
            // Get the @word attribute values
            sBuild.append(((XdmNode) item).getAttributeValue(ru_qnWord)).append(" ");
          }
          break;
        default:
          errHandle.DoError("RuNodeText: cannot process type " + crpThis.getProjectType(), RuBase.class);
      }
      // Combine the result
      sBack = sBuild.toString();
      // Possibly apply filtering
      if (!strType.isEmpty()) sBack = RuConv(sBack, strType);
      // Build a string from the array
      return sBack;      
    } catch (Exception ex) {
      // Warn user
      errHandle.DoError("RuBase/RuNodeText error", ex, RuBase.class);
      // Return failure
      return "";
    }
  }
  public static String RuNodeText(CorpusResearchProject crpThis, XdmNode ndStart, String strType) {
    String sBack;           // Resulting string
    XPathSelector selectXp; // The actual selector we are using
    StringBuilder sBuild;   // Resulting string that is being built
    
    try {
      // Validate
      if (ndStart == null) return "";
      // Default value for array
      selectXp = null; sBuild = new StringBuilder();
      // Action depends on the kind of xml project we have
      switch(crpThis.intProjType) {
        case ProjPsdx:
          // Validate: this should be a <forest> or <eTree> node
          switch(ndStart.getNodeName().getLocalName()) {
            case "eTree": case "forest": case "eLeaf":
              // Make a list of all <eLeaf> nodes
              selectXp = ru_xpeNodeText_Psdx;
              selectXp.setContextItem(ndStart);
              // Go through all the items
              for (XdmItem item : selectXp) {
                // Get the @Text attribute values
                sBuild.append(((XdmNode) item).getAttributeValue(ru_qnText)).append(" ");
              }
              break;
            default:
              // Default behaviour: get the string value of this node
              String sValue = ndStart.getStringValue();
              // Only add it if it is not empty
              if (!sValue.isEmpty()) sBuild.append(sValue);
          }
          break;
        case ProjFolia:
          // Retrieve the list of words from the current node
          List<String> lSuId = getWordList(ndStart);
          // Make a list of all the <wref> nodes under me
          selectXp = ru_xpeNodeText_Folia;
          selectXp.setContextItem(ndStart);
          // Go through all the items
          for (XdmItem item : selectXp) {
            // Check if this @id is in the list
            String sId =((XdmNode) item).getAttributeValue(ru_qnFoliaWrefId);
            if (lSuId.contains(sId)) {
              // Get the text value of the node
              String sTvalue = ((XdmNode) item).getAttributeValue(ru_qnFoliaWrefT);
              // ===========================
              // TEST: should not contain *
              if (sTvalue.contains("*")) {
                sTvalue = sTvalue;
              }
              // ===========================
              sBuild.append(sTvalue).append(" ");
            }
          }
          break;
        case ProjAlp:
          // Make a list of all end nodes; Alpino only uses <node> tags
          selectXp = ru_xpeNodeText_Alp;
          selectXp.setContextItem(ndStart);
          // Go through all the items
          for (XdmItem item : selectXp) {
            // Get the @word attribute values
            sBuild.append(((XdmNode) item).getAttributeValue(ru_qnWord)).append(" ");
          }
          break;
        case ProjNegra:
          // Make a list of all end nodes; Negra has them under <terminals> as <t> items
          selectXp = ru_xpeNodeText_Negra;
          selectXp.setContextItem(ndStart);
          // Go through all the items
          for (XdmItem item : selectXp) {
            // Get the @word attribute values
            sBuild.append(((XdmNode) item).getAttributeValue(ru_qnWord)).append(" ");
          }
          break;
        default:
          errHandle.DoError("RuNodeText: cannot process type " + crpThis.getProjectType(), RuBase.class);
      }
      // Combine the result
      sBack = sBuild.toString();
      // Possibly apply filtering
      if (!strType.isEmpty()) sBack = RuConv(sBack, strType);
      // Build a string from the array
      return sBack;
    } catch (RuntimeException | SaxonApiException ex) {
      // Warn user
      errHandle.DoError("RuBase/RuNodeText error", ex, RuBase.class);
      // Return failure
      return "";
    }
  }
  // ------------------------------------------------------------------------------------
  // Name:   RuPeriodGrp
  // Goal:   Get the period-group name depending on the division [strDiv] selected by the user
  // History:
  // 19-02-2013  ERK Created for .NET
  // 01/sep/2015 ERK Transformed to Java
  // ------------------------------------------------------------------------------------
  static String RuPeriodGrp(XPathContext objXp,String strDiv) {
    String strGroup = "";
        
    try  {
      // Validate
      if (strDiv.isEmpty()) return "";
      // Determine which CRP this is
      CrpFile oCF = getCrpFile(objXp);
      CorpusResearchProject crpThis = oCF.crpThis;
      // Initialize, depending on project type
      switch(oCF.crpThis.intProjType) {
        case ProjFolia: // FoLiA: act as if we behave the same as Psdx
        case ProjPsdx:
          // Get the division id with this name
          List<JSONObject> lDiv = crpThis.getListDivision();
          for (JSONObject oOneDiv : lDiv) {
            // Is this the correct division?
            if (oOneDiv.getString("Name").equals(strDiv)) {
              // Get the id of this division
              int intDivId = oOneDiv.getInt("DivisionId");
              // Get the current period
              String strCurrentPeriod = oCF.currentPeriod;
              // Get the group name to which the current period belongs
              List<JSONObject> lMem = crpThis.getListMember();
              for (JSONObject oOneMem : lMem) {
                if (oOneMem.getInt("DivisionId") == intDivId && oOneMem.getString("Period").equals(strCurrentPeriod)) {
                  // Pick out the group name from here
                  strGroup = oOneMem.getString("Group");
                  break;
                }
              }
              break;
            }
          }
          break;
        case ProjNegra:
          return "";
        case ProjPsd:
          return "";
        default:
          return "";
      }
      // Return the result
      return strGroup;      
    } catch (Exception ex) {
      // Warn user
      errHandle.DoError("RuBase/RuPeriodGrp error", ex, RuBase.class);
      // Return failure
      return "";
    }      
  }
  
  /**
   * doOneRelation
   *    
   * 
   * @param selectXp  - The Xpath expression to use
   * @param ndStart   - the node we start from 
   * @param ndFind    - the node we are looking for
   * @param qId       - the @id attribute Qname
   * @param sType     - either 'first' or 'contains'
   * @return 
   */
  static private boolean doOneRelation(XPathSelector selectXp, XdmNode ndStart, XdmNode ndFind, QName qId, String sType) {
    try {
      // Validate
      if (selectXp == null || ndStart == null) return false;
      // Start the search
      selectXp.setContextItem(ndStart);
      // Are there any?
      if (!selectXp.iterator().hasNext()) return false;
      // Get the id we are looking for
      String sSearchedId = ndFind.getAttributeValue(qId);
      // Action depends on the type
      switch(sType) {
        case "first":
          // Get the first one
          XdmItem itFirst = selectXp.iterator().next();
          XdmNode ndFirst = (XdmNode) itFirst;
          return (ndFirst.getAttributeValue(qId).equals(sSearchedId));
        case "contains":
          // Go through all the items
          for (XdmItem item : selectXp) {
            if (((XdmNode) item).getAttributeValue(qId).equals(sSearchedId)) return true;
          }
          break;
      }
      // Getting here: failure
      return false;
    } catch (Exception ex) {
      
      return false;
    }
  }

  // ------------------------------------------------------------------------------------
  // Name:   RuRelates
  // Goal:   Check whether the relation holds betwee @id=strId1 and @id=strId2
  //         The relation types are as follows:
  //         precedes  -- ndId1 linearly precedes ndId2
  //         iPrecedes -- ndId1 is the preceding sibling of ndId2
  //         follows   -- ndId2 linearly follows ndId1
  //         iFollows  -- ndId2 is the following sibling of ndId1
  //         dominates -- ndId1 dominates ndId2 (and is not equal to ndId2)
  //         iDominates-- ndId2 is a child of ndId1
  //         Sibling   -- ndId1 is a sibling of ndId2
  // History:
  // 05-07-2011  ERK Created for .NET
  // 29/oct/2015 ERK Implemented for Java
  // ------------------------------------------------------------------------------------
  static boolean RuRelates(XPathContext objXp, XdmNode ndSax1, XdmNode ndSax2, String sType) {
    boolean bBack = false;
    CorpusResearchProject crpThis;

    try {
      // Validate
      if (ndSax1 == null || ndSax2 == null) return false;
      
      // Determine which CRP this is
      CrpFile oCF = getCrpFile(objXp);
      crpThis = oCF.crpThis;
      // Action depends on the kind of xml project we have
      switch(crpThis.intProjType) {
        case ProjPsdx: 
          switch(sType.toLowerCase()) {
            case "precedes": bBack = doOneRelation(ru_xpPreceding_Psdx, ndSax1, ndSax2, ru_qnETreeId, "contains"); break;
            case "iprecedes": bBack = doOneRelation(ru_xpPreceding_Psdx, ndSax1, ndSax2, ru_qnETreeId, "first"); break;
            case "follows": bBack = doOneRelation(ru_xpFollowing_Psdx, ndSax1, ndSax2, ru_qnETreeId, "contains"); break;
            case "ifollows": bBack = doOneRelation(ru_xpFollowing_Psdx, ndSax1, ndSax2, ru_qnETreeId, "first"); break;
            case "dominates":bBack = doOneRelation(ru_xpDescendant_Psdx, ndSax1, ndSax2, ru_qnETreeId, "contains"); break;
            case "idominates":bBack = doOneRelation(ru_xpDescendant_Psdx, ndSax1, ndSax2, ru_qnETreeId, "first"); break;
            case "sibling":bBack = doOneRelation(ru_xpSibling_Psdx, ndSax1, ndSax2, ru_qnETreeId, "contains"); break;
          }
          break;
        case ProjFolia: 
          switch(sType.toLowerCase()) {
            case "precedes": bBack = doOneRelation(ru_xpPreceding_Folia, ndSax1, ndSax2, ru_qnFoliaId, "contains"); break;
            case "iprecedes": bBack = doOneRelation(ru_xpPreceding_Folia, ndSax1, ndSax2, ru_qnFoliaId, "first"); break;
            case "follows": bBack = doOneRelation(ru_xpFollowing_Folia, ndSax1, ndSax2, ru_qnFoliaId, "contains"); break;
            case "ifollows": bBack = doOneRelation(ru_xpFollowing_Folia, ndSax1, ndSax2, ru_qnFoliaId, "first"); break;
            case "dominates":bBack = doOneRelation(ru_xpDescendant_Folia, ndSax1, ndSax2, ru_qnFoliaId, "contains"); break;
            case "idominates":bBack = doOneRelation(ru_xpDescendant_Folia, ndSax1, ndSax2, ru_qnFoliaId, "first"); break;
            case "sibling":bBack = doOneRelation(ru_xpSibling_Folia, ndSax1, ndSax2, ru_qnFoliaId, "contains"); break;
          }
          break;
        case ProjAlp: 
          switch(sType.toLowerCase()) {
            case "precedes": bBack = doOneRelation(ru_xpPreceding_Alp, ndSax1, ndSax2, ru_qnAlpinoId, "contains"); break;
            case "iprecedes": bBack = doOneRelation(ru_xpPreceding_Alp, ndSax1, ndSax2, ru_qnAlpinoId, "first"); break;
            case "follows": bBack = doOneRelation(ru_xpFollowing_Alp, ndSax1, ndSax2, ru_qnAlpinoId, "contains"); break;
            case "ifollows": bBack = doOneRelation(ru_xpFollowing_Alp, ndSax1, ndSax2, ru_qnAlpinoId, "first"); break;
            case "dominates":bBack = doOneRelation(ru_xpDescendant_Alp, ndSax1, ndSax2, ru_qnAlpinoId, "contains"); break;
            case "idominates":bBack = doOneRelation(ru_xpDescendant_Alp, ndSax1, ndSax2, ru_qnAlpinoId, "first"); break;
            case "sibling":bBack = doOneRelation(ru_xpSibling_Alp, ndSax1, ndSax2, ru_qnAlpinoId, "contains"); break;
          }
          break;
        case ProjNegra: 
          switch(sType.toLowerCase()) {
            case "precedes":
              break;
            case "iprecedes":
              break;
            case "follows":
              break;
            case "ifollows":
              break;
            case "dominates":
              break;
            case "idominates":
              break;
            case "sibling":
              break;
          }
          break;
      }
      // Return the result
      return bBack;
    } catch (Exception ex) {
       // Warn user
       errHandle.DoError("RuBase/RuRelates error", ex, RuBase.class);
       // Return failure
       return false;
    }
  }
  
  // ------------------------------------------------------------------------------------
  // Name:   RuWords
  // Goal:   Get the number of words in the constituent [ndStart]
  // History:
  // 23-01-2013  ERK Created for .NET
  // 29/oct/2015 ERK Implemented for Java
  // ------------------------------------------------------------------------------------
  static int RuWords(XPathContext objXp, XdmNode ndStart) {
    int iBack = 0;              // Resulting count
    XPathSelector selectXp; // The actual selector we are using
    CorpusResearchProject crpThis;
    
    try {
      // Validate
      if (ndStart == null) return -1;
      // Determine which CRP this is
      CrpFile oCF = getCrpFile(objXp);
      crpThis = oCF.crpThis;
      // Default value for array
      selectXp = null;
      // Action depends on the kind of xml project we have
      switch(crpThis.intProjType) {
        case ProjPsdx: selectXp = ru_xpeWords_Psdx; break;
        case ProjFolia: selectXp = ru_xpeWords_Folia; break;
        case ProjAlp: selectXp = ru_xpeWords_Alp; break;
        case ProjNegra: selectXp = ru_xpeWords_Negra; break;
      }
      // Satisfy Netbeans (which doesn't recognize this in the 'default' part of a switch)
      if (selectXp == null) {
          errHandle.DoError("RuWords: cannot process type " + crpThis.getProjectType(), RuBase.class);
          return -1;
      }
      // Count the words by taking the selection
      selectXp.setContextItem(ndStart);
      // Go through all the items
      for (XdmItem item : selectXp) iBack++;
      // Return what we found
      return iBack;
    } catch (Exception ex) {
      // Warn user
      errHandle.DoError("RuBase/RuWords error", ex, RuBase.class);
      // Return failure
      return -1;
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
  static String GetRefId(XPathContext objXp, XdmNode ndSax) {
    String strRef = "";   // The ID of the second node
    String sNodeName;     // Name of the node [ndSax]
    CrpFile oCrpFile;     // Current CRP/File object

    try {
      // Validate
      if (ndSax == null) return "";
      // Determine which CRP/File this is
      oCrpFile = getCrpFile(objXp);
      // Initialize values
      sNodeName = ndSax.getNodeName().getLocalName();
      switch (oCrpFile.crpThis.intProjType) {
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
              strRef = ndSax.getAttributeValue(ru_qnTreeId);
              // WAS: strRef = ndSax.getAttributeValue(ru_qnETreeId);
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
          // Get the ID of the node
          switch(sNodeName) {
            case "wref":
              strRef = ndSax.getAttributeValue(ru_qnFoliaWrefId);
              break;
            case "su": case "s": case "p": case "w": case "div": case "text":
              strRef = ndSax.getAttributeValue(ru_qnFoliaId);
              break;
            default:
              // Cannot handle this
              return "";
          }
          // Nodes in folia have the same kind of identifier: [xml:id]
          break;
        case ProjAlp:
          // The identifier for the alpino <node> elements is @id, but it is only unique within one sentence (apparently)
          strRef = ndSax.getAttributeValue(ru_qnAlpinoId);
          break;
        default:
      }
      // Return the reference string we found
      return strRef;
    } catch (RuntimeException ex) {
      // Show error
      errHandle.DoError("RuBase/GetRefId error", ex, RuBase.class);
      // Return failure
      return "";
    }
  }
  // ------------------------------------------------------------------------------------
  // Name:   getCrpFile
  // Goal:   Get the correct CrpFile object for the indicated context
  // History:
  // 19/may/2015  ERK Created for Java
  // ------------------------------------------------------------------------------------
  static CrpFile getCrpFile(XPathContext objXp) {
    try {
      return (CrpFile) objXp.getController().getParameter("crpfile");
    } catch (Exception ex) {
      errHandle.DoError("RuBase/getCrpFile error", ex, RuBase.class);
      return null;
    }
  }
  static ErrHandle getErrHandle(XPathContext objXp) {
    try {
      return (ErrHandle) objXp.getController().getParameter("errhandle");
    } catch (Exception ex) {
      errHandle.DoError("RuBase/getErrHandle error", ex, RuBase.class);
      return null;
    }
  }
  /**
   * getWordList - retrieve the list of words for this sentence from the context
   * 
   * @param objXp
   * @return 
   * @history
   *  24/sep/2015 ERK Created
   */
  static List<String> getWordList(XPathContext objXp) {
    try {
      return (List<String>) objXp.getController().getParameter("words");
    } catch (Exception ex) {
      errHandle.DoError("RuBase/getCrpFile error", ex, RuBase.class);
      return null;
    }
  }
  static List<String> getWordList(XdmNode ndxThis) {
    List<String> lSuId = new ArrayList<>();

    try {
      // Make a list of all <w> nodes under <s>
      XPathSelector selectXp = ru_xpeWords_Folia2;
      try {
        selectXp.setContextItem(ndxThis);
      } catch (SaxonApiException ex) {
        errHandle.DoError("RuBase/getWordList problem: ", ex, RuBase.class);
        return lSuId;
      }
      // Go through all the items and add them to a new list
      for (XdmItem item : selectXp) {
        String sValue = ((XdmNode) item).getAttributeValue(ru_qnFoliaId);
        // =================================
        // NOTE: this is an ad-hoc measurement because of wrongly made .folia.xml files
        // sValue = sValue.replace(".w.", ".");
        // =================================
        lSuId.add(sValue);
      }
      // Return the result
      return lSuId;
    } catch (Exception ex) {
      errHandle.DoError("RuBase/getWordList error", ex, RuBase.class);
      return null;
    }
  }
  
 
  /**
   * xmlNodeToNode
   *    Convert 'XmlNode' to interface 'Node'
   * 
   * @param oCF
   * @param ndxThis
   * @return 
   */
  static Node xmlNodeToNode(CrpFile oCF, XmlNode ndxThis) {
    Node ndxBack = null;
    try {
      // Convert Xdm or Xml node to Node
      if (ndxThis != null) {
        ndxBack = oCF.oDocFac.newDocumentBuilder().parse(new InputSource(new StringReader(ndxThis.toString())));
      }
      // Return the result
      return ndxBack;
    } catch (Exception ex) {
      errHandle.DoError("RuBase/xmlNodeToNode error", ex, RuBase.class);
      return null;
    }
  }
// </editor-fold>
}
