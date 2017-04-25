/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

package nl.ru.xmltools;
// <editor-fold defaultstate="collapsed" desc="Imports">

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPathExpressionException;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.project.ExecuteXml;
import nl.ru.crpx.search.JobXq;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.util.ByRef;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
// </editor-fold>
/**
 *
 * @author Erwin R. Komen
 */
public abstract class XmlForest {
  // This class uses a logger
  protected final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XmlForest.class);
// <editor-fold defaultstate="collapsed" desc="Header">
  protected final  class Context {
    public String Seg = "";    // Text of this line
    public String TxtId = "";  // TextId of this line
    public String Loc = "";    // Location for this line
  }
// <editor-fold defaultstate="collapsed" desc="Enumeration ForType">
  public enum ForType {
    PsdxWholeFile(1),   // Each file in one go
    PsdxPerForest(2),   // Each <forest> element
    PsdxPerForgrp(3),   // Each <forestGrp> element
    PsdxIndex(4),       // Random-access PSDX through indexing
    FoliaWholeFile(10), // Each file in one go
    FoliaPerDiv(11),    // Each <div> element
    FoliaPerPara(12),   // Each <p> element
    FoliaPerS(13),      // Each <s> element
    FoliaIndex(14),     // Random-access FoLiA through indexing
    AlpWholeFile(20),   // Alpino: whole file
    AlpPerS(21),        // Alpino: per sentence
    NegraWholeFile(30), // Negra: whole file
    NegraPerS(31),      // Negra: per sentence
    PsdWholeFile(40),   // Treebank: whole file
    PsdPerS(41),        // Treebank: per sentence
    Unknown(50);        // Unknown value

    private int intValue;
    private static java.util.HashMap<Integer, ForType> mappings;
    private static java.util.HashMap<Integer, ForType> getMappings() {
      if (mappings == null) {
        synchronized (ForType.class) {
          if (mappings == null) {
            mappings = new java.util.HashMap<Integer, ForType>();
          }
        }
      }
      return mappings;
    }
    private ForType(int value) {
      intValue = value;
      getMappings().put(value, this);
    }
    public int getValue() {
      return intValue;
    }
    public static ForType forValue(int value) {
      return getMappings().get(value);
    }
    public static ForType forValue(String sValue) {
      switch (sValue.toLowerCase()) {
        case "psdxwholefile": return PsdxWholeFile;
        case "psdxperforest": return PsdxPerForest;
        case "psdxperforgrp": return PsdxPerForgrp;
        case "psdxindex": return PsdxIndex;
        case "foliawholefile": return FoliaWholeFile;
        case "foliaperdiv": return FoliaPerDiv;
        case "foliaperpara": return FoliaPerPara;
        case "foliapers": return FoliaPerS;
        case "alpwholefile": return AlpWholeFile;
        case "alppers": return AlpPerS;
        case "negrawholefile": return NegraWholeFile;
        case "negrapers": return NegraPerS;
        case "psdwholefile": return PsdWholeFile;
        case "psdpers": return PsdPerS;
        default: return Unknown;
      }
    }
  }
// </editor-fold>
  // ========================================== Constants ======================
  protected static final QName loc_xq_forestId = new QName("", "", "forestId");  
  protected static final QName loc_xq_Location = new QName("", "", "Location");  
  protected static final QName loc_xq_TextId = new QName("", "", "TextId");  
  protected static final QName loc_xq_Folia_Id = new QName("xml", "http://www.w3.org/XML/1998/namespace", "id");  
  protected static final String loc_path_PsdxSent = "./descendant-or-self::forest[1]";
  protected static final String loc_path_PsdxHeader = "./descendant-or-self::teiHeader[1]";
  protected static final String loc_path_FoliaSent = "./descendant-or-self::s[1]";
  protected static final String loc_path_FoliaHeader = "./descendant-or-self::metadata[1]";
  // ========================================== LOCAL VARIABLE ================================================
  protected String loc_strCurrent = "";   // XML code of current forest
  protected String loc_strCombi = "";     // Combined XML context, from where current node is taken
  protected int loc_intCurrent;           // Position of current node within [loc_arContext]
  protected List<String> loc_colStack;    // Stack for the context
  protected List<String> loc_colCombi;    // Where we combine the context
  protected XmlDocument loc_pdxThis;      // Current one
  protected XmlDocument loc_pdxMdi;       // MDI document (if existing)
  protected XmlDocument[] loc_arPrec;     // Preceding lines as Xml document
  protected XmlDocument[] loc_arFoll;     // Following lines as Xml document
  protected Context[] loc_arPrecCnt;      // Preceding context
  protected Context[] loc_arFollCnt;      // Following context
  protected Context loc_cntThis;          // Current context
  protected int iPrecNum = 2; // Default
  protected int iFollNum = 1; // Default
  protected ForType loc_Type;             // The type of treatment expected
  protected ErrHandle objErr;               // Local access to the general object with global variables
  protected JobXq objJob;                   // Access to the job that is being executed
  protected CorpusResearchProject crpThis;  // The corpus research project for which I am created
  protected Parse objParse;                 // Object to use my own version of the "GetSeg()" function
  protected Processor objSaxon;             // Local access to the processor
  protected DocumentBuilder objSaxDoc;      // My own document-builder
  protected boolean bUseRa = true;          // Use the Random-Access reader
  // private XmlReaderSettings loc_xrdSet;  // Special arrangements for the reader --> already done in XmlDocument()
  // ==========================================================================================================
  // Class instantiation
  public XmlForest(CorpusResearchProject oCrp, JobXq oJob, ErrHandle oErr) {
    try {
      // Get the processor
      this.objSaxon = oCrp.getSaxProc();
      // Create a document builder
      this.objSaxDoc = this.objSaxon.newDocumentBuilder();
      // Set a new XML document
      loc_pdxThis = new XmlDocument(this.objSaxDoc, this.objSaxon);
      // Set a new XML document for MDI
      loc_pdxMdi = new XmlDocument(this.objSaxDoc, this.objSaxon);
      // Other initialisations
      loc_colStack = new ArrayList<>();
      loc_cntThis  = new Context();
      loc_Type  = ForType.PsdxWholeFile;
      objErr = oErr;
      crpThis = oCrp;
      objParse = new Parse(oCrp, oErr);
      this.objJob = oJob;
    } catch (Exception ex) {
      logger.error("XmlForest initialisation error", ex);
    }
  }
// </editor-fold>
  // ----------------------------------------------------------------------------------------------------------
  // Name :  ProcType
  // Goal :  The type of procedure that needs to be used
  // History:
  // 14-04-2014  ERK Created for .NET
  // 23/apr/2015  ERK Adapted for Java
  // ----------------------------------------------------------------------------------------------------------
  public final int getProcType() {return loc_Type.getValue();}
  public final void setProcType(int value) {loc_Type = ForType.forValue(value);  }
  public final void setProcType(ForType value) {loc_Type = value;  }

  // Methods that are overridden by the classes that extend XmlForest:
  public abstract boolean FirstForest(ByRef<XmlNode> ndxForest, ByRef<XmlNode> ndxHeader, ByRef<XmlNode> ndxMdi, String strFile);
  public abstract boolean GetForestId(ByRef<XmlNode> ndxForest, ByRef<Integer> intForestId);
  public abstract String getSentenceId(ByRef<XmlNode> ndxForest);
  public abstract boolean NextForest(ByRef<XmlNode> ndxForest);
  public abstract boolean OneForest(ByRef<XmlNode> ndxForest, String sSentId);
  public abstract boolean IsEnd();
  public abstract boolean Percentage(ByRef<Integer> intPtc);
  public abstract String GetContext();
  public abstract String GetSyntax(ByRef<XmlNode> ndxForest);
  public abstract String GetPde(ByRef<XmlNode> ndxForest);
  public abstract int GetSize();
  public abstract void close();
  
  // Public methods for all
  public String getCurrentLoc() { return this.loc_cntThis.Loc; }
  public String getCurrentSeg() { return this.loc_cntThis.Seg; }
  public String getCurrentTxtId() { return this.loc_cntThis.TxtId; }
}
