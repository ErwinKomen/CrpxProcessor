/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
    public String Seg;    // Text of this line
    public String TxtId;  // TextId of this line
    public String Loc;    // Location for this line
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
  protected static final String loc_path_Forest = "./descendant-or-self::forest[1]";
  protected static final String loc_path_TeiHeader = "./descendant-or-self::teiHeader[1]";
  // ========================================== LOCAL VARIABLE ================================================
  protected String loc_strCurrent = "";   // XML code of current forest
  protected String loc_strCombi = "";     // Combined XML context, from where current node is taken
  protected int loc_intCurrent;           // Position of current node within [loc_arContext]
  protected List<String> loc_colStack;    // Stack for the context
  protected List<String> loc_colCombi;    // Where we combine the context
  protected XmlDocument loc_pdxThis;      // Current one
  protected XmlDocument[] loc_arPrec;     // Preceding lines as Xml document
  protected XmlDocument[] loc_arFoll;     // Following lines as Xml document
  protected Context[] loc_arPrecCnt;      // Preceding context
  protected Context[] loc_arFollCnt;      // Following context
  protected Context loc_cntThis;          // Current context
  protected ForType loc_Type;             // The type of treatment expected
  protected ErrHandle objErr;               // Local access to the general object with global variables
  protected JobXq objJob;                   // Access to the job that is being executed
  protected CorpusResearchProject crpThis;  // The corpus research project for which I am created
  protected Parse objParse;                 // Object to use my own version of the "GetSeg()" function
  protected Processor objSaxon;             // Local access to the processor
  protected DocumentBuilder objSaxDoc;      // My own document-builder
  // private XmlReaderSettings loc_xrdSet; // Special arrangements for the reader --> already done in XmlDocument()
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
  public abstract boolean FirstForest(ByRef<XmlNode> ndxForest, ByRef<XmlNode> ndxHeader, String strFile);
  public abstract boolean GetForestId(ByRef<XmlNode> ndxForest, ByRef<Integer> intForestId);
  public abstract boolean NextForest(ByRef<XmlNode> ndxForest);
  public abstract boolean OneForest(ByRef<XmlNode> ndxForest, String sSentId);
  public abstract boolean IsEnd();
  public abstract boolean Percentage(ByRef<Integer> intPtc);
  public abstract String GetContext();
  public abstract String GetSyntax(ByRef<XmlNode> ndxForest);
  public abstract String GetPde(ByRef<XmlNode> ndxForest);
  public String getCurrentLoc() { return this.loc_cntThis.Loc; }
  public String getCurrentSeg() { return this.loc_cntThis.Seg; }
  public String getCurrentTxtId() { return this.loc_cntThis.TxtId; }
// <editor-fold defaultstate="collapsed" desc="Main">
  /*
  // ----------------------------------------------------------------------------------------------------------
  // Name :  FirstForest
  // Goal :  Load the first forest - depending on method 
  // History:
  // 14-04-2014  ERK Created
  // 23/apr/2015  ERK Adapted for Java
  // ----------------------------------------------------------------------------------------------------------
  public boolean FirstForest(ByRef<XmlNode> ndxForest, ByRef<XmlNode> ndxHeader, String strFile) {
    try {
      switch (loc_Type) {
        case FoliaPerDiv:
          break;
        case FoliaPerPara:
          break;
        case FoliaPerS:
          break;
        case FoliaWholeFile:
          break;
        case PsdxPerForest:
          return PsdxPerForest_FirstForest(ndxForest, ndxHeader, strFile);
        case PsdxPerForgrp:
          break;
        case PsdxWholeFile:
          break;
        default:
          return false;
      }
      // Return success
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/FirstForest error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  GetForestId
  // Goal :  Get the ID of the forest [ndxThis]
  // History:
  // 02-07-2011  ERK Created  for .NET
  // 13/may/2015 ERK Adapted for Java
  // ----------------------------------------------------------------------------------------------------------
  public boolean GetForestId(ByRef<XmlNode> ndxForest, ByRef<Integer> intForestId) {
    String sAttr;   // Attribute value
    // Node attrThis;  // Room for one attribute
    
    try {
      // Supply default value to indicate failure
      intForestId.argValue = -1;
      switch (loc_Type) {
        case FoliaPerDiv:
          break;
        case FoliaPerPara:
          break;
        case FoliaPerS:
          break;
        case FoliaWholeFile:
          break;
        case PsdxPerForest:
          // Check if there is a @forestId attribute
          sAttr = ndxForest.argValue.getAttributeValue(loc_xq_forestId);
          // attrThis = ndxForest.argValue.getAttributeValue(loc_xq_forestId);
          // if (attrThis == null ) return objErr.DoError("<forest> does not have @forestId");
          if (sAttr.isEmpty()) return objErr.DoError("<forest> does not have @forestId");
          // Get the @forestId value
          intForestId.argValue = Integer.parseInt(sAttr);
        case PsdxPerForgrp:
          break;
        case PsdxWholeFile:
          break;
        default:
          return false;
      }
      // Return success
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/GetForestId error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }

  // ----------------------------------------------------------------------------------------------------------
  // Name :  NextForest
  // Goal :  Load the next forest, depending on the particular method
  // History:
  // 14-04-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  public boolean NextForest(ByRef<XmlNode> ndxForest) {
    try {
      switch (loc_Type) {
        case FoliaPerDiv:
          break;
        case FoliaPerPara:
          break;
        case FoliaPerS:
          break;
        case FoliaWholeFile:
          break;
        case PsdxPerForest:
          return PsdxPerForest_NextForest(ndxForest);
        case PsdxPerForgrp:
          break;
        case PsdxWholeFile:
          break;
        default:
          return false;
      }
      // Return success
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/NextForest error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  IsEnd
  // Goal :  Check if the stream is at its end
  // History:
  // 14-04-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  public boolean IsEnd() {
    try {
      switch (loc_Type) {
        case FoliaPerDiv:
        break;
        case FoliaPerPara:
        break;
        case FoliaPerS:
        break;
        case FoliaWholeFile:
        break;
        case PsdxPerForest:
          return PsdxPerForest_IsEnd();
        case PsdxPerForgrp:
        break;
        case PsdxWholeFile:
        break;
        default:
          return false;
      }
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/IsEnd error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
    return false;
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  Percentage
  // Goal :  Show where we are in reading
  // History:
  // 14-04-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  public boolean Percentage(ByRef<Integer> intPtc) {
    try {
      switch (loc_Type) {
        case FoliaPerDiv:
          break;
        case FoliaPerPara:
          break;
        case FoliaPerS:
          break;
        case FoliaWholeFile:
          break;
        case PsdxPerForest:
          return PsdxPerForest_Percentage(intPtc);
        case PsdxPerForgrp:
          break;
        case PsdxWholeFile:
          break;
        default:
          return false;
      }
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/Percentage error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
    return false;
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  GetContext
  // Goal :  Get the complete context
  // History:
  // 08-04-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  public String GetContext() {
    try {
      switch (loc_Type) {
        case FoliaPerDiv:
          return "";
        case FoliaPerPara:
          return "";
        case FoliaPerS:
          return "";
        case FoliaWholeFile:
          return "";
        case PsdxPerForest:
          return PsdxPerForest_GetContext();
        case PsdxPerForgrp:
          return "";
        case PsdxWholeFile:
          return "";
        default:
          return "";
      }
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/GetContext error: " + ex.getMessage()  + "\r\n");
      // Return failure
      return "";
    }

  }
    */

// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="PsdxPerForest">
  /* ================ Taken over by all who "extend" me =====================
  // ----------------------------------------------------------------------------------------------------------
  // Name :  PsdxPerForest_FirstForest
  // Goal :  Load the first forest using an XmlReader 
  // History:
  // 18-03-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  private boolean PsdxPerForest_FirstForest(ByRef<XmlNode> ndxForest, ByRef<XmlNode> ndxHeader, String strFile) {
    XmlNode ndxWork = null;  // Working node
    File fThis;           // File object of [strFile]
    int intI = 0;         // Counter

    try {
      // Validate
      if (strFile == null || strFile.length() == 0) return false;
      fThis = new File(strFile);
      if (!fThis.exists()) return false;
      // Initialisations
      ndxForest.argValue = null;
      ndxHeader.argValue = null;
      // Fill the arrays
      loc_arPrec = new XmlDocument[objJob.intPrecNum];
      loc_arPrecCnt = new Context[objJob.intPrecNum];
      loc_arFoll = new XmlDocument[objJob.intFollNum];
      loc_arFollCnt = new Context[objJob.intFollNum];
      for (intI = 0; intI < loc_arPrec.length; intI++) {
        loc_arPrec[intI] = new XmlDocument(this.objSaxDoc, this.objSaxon);
        loc_arPrecCnt[intI] = new Context();
      }
      for (intI = 0; intI < loc_arFoll.length; intI++) {
        loc_arFoll[intI] = new XmlDocument(this.objSaxDoc, this.objSaxon);
        loc_arFollCnt[intI] = new Context();
      }
      // Start up the streamer
      loc_xrdFile = new XmlChunkReader(fThis);
      // First read the (obligatory) teiHeader
      if (! (loc_xrdFile.ReadToFollowing("teiHeader"))) {
        objErr.DoError("PsdxPerForest_FirstForest error: cannot find <teiHeader> in file [" + strFile + "]");
        return false;
      }
      // Load this obligatory teiHeader 
      loc_pdxThis.LoadXml(loc_xrdFile.ReadOuterXml());
      // Set the global parameter
      // Node ndxFirst = loc_pdxThis.getDocument().getFirstChild();
      
      ndxWork = loc_pdxThis.SelectSingleNode(loc_path_TeiHeader);
      ndxHeader.argValue =ndxWork;
      // Read the current node + following context
      for (intI = 0; intI <= objJob.intFollNum; intI++) {
        // Move to the first forest
        if (! (loc_xrdFile.ReadToFollowing("forest"))) {
          objErr.DoError("PsdxPerForest_FirstForest error: cannot find <forest> in file [" + strFile + "]");
          return false;
        }
        // Store it
        if (intI == 0) {
          // Get the current forest
          loc_pdxThis.LoadXml(loc_xrdFile.ReadOuterXml());
          // Get the current context
          ndxForest.argValue = loc_pdxThis.SelectSingleNode(loc_path_Forest);
          // Validate: do we have a result?
          if (ndxForest.argValue == null) {
            // This should not happen. Check what is the matter
            String sWork = loc_pdxThis.getDoc();
            logger.debug("XmlForest empty ndxForest.argValue: " + sWork);
          } else {
            loc_cntThis.Seg = objParse.GetSeg(ndxForest.argValue);
            // loc_cntThis.Loc = ndxForest.argValue.Attributes("Location").Value;
            // loc_cntThis.TxtId = ndxForest.argValue.Attributes("TextId").Value;
            loc_cntThis.Loc = ndxForest.argValue.getAttributeValue(loc_xq_Location);
            loc_cntThis.TxtId = ndxForest.argValue.getAttributeValue(loc_xq_TextId);
          }
        } else {
          // Fill the following context XmlDocument
          loc_arFoll[intI - 1].LoadXml(loc_xrdFile.ReadOuterXml());
          // Fill the following context @seg, @txtid and @loc
          ndxWork = loc_arFoll[intI - 1].SelectSingleNode(loc_path_Forest);
          // Validate: do we have a result?
          if (ndxWork == null) {
            // This should not happen. Check what is the matter
            String sWork = loc_arFoll[intI - 1].getDoc();
            logger.debug("XmlForest empty work: " + sWork);
          } else {
            loc_arFollCnt[intI - 1].Seg = objParse.GetSeg(ndxWork);
            // loc_arFollCnt[intI - 1].Loc = ndxWork.Attributes("Location").Value;
            // loc_arFollCnt[intI - 1].TxtId = ndxWork.Attributes("TextId").Value;
            loc_arFollCnt[intI - 1].Loc = ndxWork.getAttributeValue(loc_xq_Location);
            loc_arFollCnt[intI - 1].TxtId = ndxWork.getAttributeValue(loc_xq_TextId);
          }
        }
      }
      //' Construct the current context
      //If (Not StreamMakeContext()) Then Return False
      // Return success
      return true;
    } catch (XPathExpressionException ex) {
      // Warn user
      objErr.DoError("XmlForest/PsdxPerForest_FirstForest Xpath error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/PsdxPerForest_FirstForest Runtime error: " + ex.getMessage() + "\r\n");
      ex.printStackTrace();
      // Return failure
      return false;
    } catch (SaxonApiException | IOException ex) {
      objErr.DoError("XmlForest/PsdxPerForest_FirstForest IO/SAX error: " + ex.getMessage() + "\r\n");
      return false;
    }
  }

  // ----------------------------------------------------------------------------------------------------------
  // Name :  PsdxPerForest_IsEnd
  // Goal :  Check if the stream is at its end
  // History:
  // 08-04-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  private boolean PsdxPerForest_IsEnd() {
    try {
      return (loc_xrdFile.EOF) && (objJob.intFollNum == 0 || (loc_arFoll[0] == null));
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/PsdxPerForest_IsEnd error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  PsdxPerForest_NextForest
  // Goal :  Load the next forest using an XmlReader 
  // History:
  // 18-03-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  private boolean PsdxPerForest_NextForest(ByRef<XmlNode> ndxForest) {
    XmlNode ndxWork = null; // Working node
    String strNext = ""; // Another chunk of <forest>
    int intI = 0; // Counter

    try {
      // Validate
      if (loc_pdxThis == null) {
        return false;
      }
      // Check for end of stream
      if (PsdxPerFile_IsEnd()) {
        ndxForest.argValue = null;
        return true;
      }
      // More validateion
      if (loc_xrdFile==null) {
        return false;
      }
      // Try to read another piece of <forest> xml
      if (! loc_xrdFile.EOF) {
        // Read until  the following one
        loc_xrdFile.ReadToFollowing("forest");
        // Double check
        if (! loc_xrdFile.EOF) {
          // Read this <forest>
          strNext = loc_xrdFile.ReadOuterXml();
        }
      }
      // Check for end-of-file and file closing
      if (loc_xrdFile.EOF) {
        loc_xrdFile.Close();
        loc_xrdFile = null;
      }
      // Double check what we got
      if (strNext == null || strNext.length() == 0) {
        ndxForest.argValue = null;
        return true;
      }
      // Do we have any preceding context to take care of?
      if (objJob.intPrecNum > 0) {
        // Copy preceding context
        for (intI = 1; intI < objJob.intPrecNum; intI++) {
          // Copy context, so that loc_arPrec(0) contains the furthest-away-being context
          loc_arPrec[intI - 1] = loc_arPrec[intI];
          // loc_arPrecCnt(intI - 1) = loc_arPrecCnt(intI)
          loc_arPrecCnt[intI - 1].Loc = loc_arPrecCnt[intI].Loc;
          loc_arPrecCnt[intI - 1].Seg = loc_arPrecCnt[intI].Seg;
          loc_arPrecCnt[intI - 1].TxtId = loc_arPrecCnt[intI].TxtId;
        }
        // Copy current into preceding context
        loc_arPrec[objJob.intPrecNum - 1] = loc_pdxThis;
        // loc_arPrecCnt(objJob.intPrecNum - 1) = loc_cntThis
        loc_arPrecCnt[objJob.intPrecNum - 1].Loc = loc_cntThis.Loc;
        loc_arPrecCnt[objJob.intPrecNum - 1].Seg = loc_cntThis.Seg;
        loc_arPrecCnt[objJob.intPrecNum - 1].TxtId = loc_cntThis.TxtId;
      }
      // Do we have any following context to take care of?
      if (objJob.intFollNum > 0) {
        // Copy the first-following into the current
        loc_pdxThis = loc_arFoll[0];
        loc_cntThis.Loc = loc_arFollCnt[0].Loc;
        loc_cntThis.Seg = loc_arFollCnt[0].Seg;
        loc_cntThis.TxtId = loc_arFollCnt[0].TxtId;
        //' ============== DEBUG ===============
        //If (InStr(loc_cntThis.Loc, "." & loc_pdxThis.SelectSingleNode(loc_path_Forest).Attributes("forestId").Value) = 0) Then
        //  Stop
        //End If
        //If (loc_pdxThis.SelectSingleNode(loc_path_Forest).Attributes("forestId").Value = 2) Then Stop
        //' ====================================
        // Shift all the other elements
        for (intI = 1; intI < objJob.intFollNum; intI++) {
          // Shift XmlDocument
          loc_arFoll[intI - 1] = loc_arFoll[intI];
          // Shift Context element
          loc_arFollCnt[intI - 1].Loc = loc_arFollCnt[intI].Loc;
          loc_arFollCnt[intI - 1].Seg = loc_arFollCnt[intI].Seg;
          loc_arFollCnt[intI - 1].TxtId = loc_arFollCnt[intI].TxtId;
        }
        // The last element becomes what we have physically read
        loc_arFoll[objJob.intFollNum - 1] = new XmlDocument(this.objSaxDoc, this.objSaxon);
        loc_arFoll[objJob.intFollNum - 1].LoadXml(strNext);
        // Get working node <forest>
        ndxWork = loc_arFoll[objJob.intFollNum - 1].SelectSingleNode(loc_path_Forest);
        // Validate
        if (ndxWork == null) {
          // This should not happen. Check what is the matter
          String sWork = loc_arFoll[objJob.intFollNum - 1].getDoc();
          logger.debug("XmlForest empty work: " + sWork);
        } else {
          // Calculate the correct context
          loc_arFollCnt[objJob.intFollNum - 1].Seg = objParse.GetSeg(ndxWork);
          loc_arFollCnt[objJob.intFollNum - 1].Loc = ndxWork.getAttributeValue(loc_xq_Location);
          loc_arFollCnt[objJob.intFollNum - 1].TxtId = ndxWork.getAttributeValue(loc_xq_TextId);
        }
      } else {
        // No following context...
        loc_pdxThis.LoadXml(strNext);
        // Get this forest
        ndxWork = loc_pdxThis.SelectSingleNode(loc_path_Forest);
        // Validate
        if (ndxWork == null) {
          // This should not happen. Check what is the matter
          String sWork = loc_pdxThis.getDoc();
          logger.debug("XmlForest empty work: " + sWork);
        } else {
          // Calculate the correct context
          loc_cntThis.Seg = objParse.GetSeg(ndxWork);
          loc_cntThis.Loc = ndxWork.getAttributeValue(loc_xq_Location);
          loc_cntThis.TxtId = ndxWork.getAttributeValue(loc_xq_TextId);
        }
      }
      // Double check for EOF
      if (loc_pdxThis == null) {
        ndxForest.argValue = null;
        return true;
      }
      // Find the current forest
      ndxForest.argValue = loc_pdxThis.SelectSingleNode(loc_path_Forest);
      //' ================================
      //' TODO: improve...
      //If (Not StreamMakeContext()) Then Return False
      // ============== DEBUG ===============
      //If (InStr(loc_cntThis.Loc, "." & ndxForest.Attributes("forestId").Value) = 0) Then
      //  Stop
      //End If
      // Return success
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/PsdxPerForest_NextForest runtime error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    } catch (IOException | SaxonApiException | XPathExpressionException ex) {
      objErr.DoError("XmlForest/PsdxPerForest_NextForest IO/Sax/Xpath error: " + ex.getMessage() + ""
              + "\r\n");
      // Return failure
      return false;
    }
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  PsdxPerForest_Percentage
  // Goal :  Show where we are in reading
  // History:
  // 07-04-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  private boolean PsdxPerForest_Percentage(ByRef<Integer> intPtc) {
    try {
      // Validate
      if (loc_xrdFile == null) {
        intPtc.argValue = 0;
        return true;
      }
      if (loc_xrdFile.EOF) {
        intPtc.argValue = 100;
        return true;
      }
      // Find out what my position is
      intPtc.argValue = loc_xrdFile.getPtc();
      // Return success
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/PsdxPerForest_NextForest error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  PsdxPerForest_GetContext
  // Goal :  Get the complete context
  // History:
  // 08-04-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  private String PsdxPerForest_GetContext() {
    String strPrec = ""; // Preceding context
    String strFoll = ""; // Following context
    String strBack = ""; // What we return
    int intI = 0; // Counter

    try {
      // Start out with the TextId (name of the text)
      // Prepend the textid (name of the text)
      strPrec = "[<b>" + loc_cntThis.TxtId + "</b>]";
      // Add the preceding context
      if (objJob.intPrecNum > 0) {
        // Attempt to get the preceding context
        for (intI = 0; intI < objJob.intPrecNum; intI++) {
          // Only load existing context
          if (loc_arPrecCnt[intI].Seg != null && loc_arPrecCnt[intI].Seg.length() > 0) {
            strPrec += "[" + loc_arPrecCnt[intI].Loc + "]" + loc_arPrecCnt[intI].Seg;
          }
        }
      }
      // Add the following context
      if (objJob.intFollNum > 0) {
        // Attempt to get the preceding context
        for (intI = 0; intI < objJob.intFollNum; intI++) {
          if (loc_arFollCnt[intI].Seg != null && loc_arFollCnt[intI].Seg.length() > 0) {
            strFoll += "[" + loc_arFollCnt[intI].Loc + "]" + loc_arFollCnt[intI].Seg;
          }
        }
      }
      // Return the text of the actual node
      strBack = loc_cntThis.Seg;
      // Did we find anything?
      if (strBack == null || strBack.length() == 0) {
        // Make empty stuff in HTML
        strBack = "<div><b>[" + loc_cntThis.Loc + "\r\n" + "]</b></div>";
      } else {
        // Convert into html
        strBack = "<div>" + strPrec + "</div>" + "<div><b>[" + loc_cntThis.Loc + "]</b>" + "\r\n" + "<Font color=#0000ff>" + strBack + "</Font>" + "\r\n" + "</div>" + "<div>" + strFoll + "</div>" + "\r\n";
      }
      // Set the local context string
      loc_strCombi = strBack;
      // Return the context
      return strBack;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/PsdxPerForest_GetContext error: " + ex.getMessage() + "\r\n");
      // Return failure
      return "";
    }
  }
  ========================= */
//VB TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
///#End Region
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="PsdxPerFile">
  /* ====================== Taken over by all who extend me ====================
  // ----------------------------------------------------------------------------------------------------------
  // Name :  PsdxPerFile_FirstForest
  // Goal :  Load the first forest using an XmlReader 
  // History:
  // 18-03-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  private boolean PsdxPerFile_FirstForest(ByRef<XmlNode> ndxForest, String strFile) {
    XmlNode ndxWork = null; // Working node
    int intI = 0; // Counter
    File fThis;           // File object of [strFile]

    try {
      // Validate
      if (strFile == null || strFile.length() == 0) return false;
      fThis = new File(strFile);
      if (!fThis.exists()) return false;
      // Initialisations
      ndxForest.argValue = null;
      // Fill the arrays
      loc_arPrec = new XmlDocument[objJob.intPrecNum];
      loc_arPrecCnt = new Context[objJob.intPrecNum];
      loc_arFoll = new XmlDocument[objJob.intFollNum];
      loc_arFollCnt = new Context[objJob.intFollNum];
      for (intI = 0; intI < loc_arPrec.length; intI++) {
        loc_arPrec[intI] = new XmlDocument(this.objSaxDoc, this.objSaxon);
        loc_arPrecCnt[intI] = new Context();
      }
      for (intI = 0; intI < loc_arFoll.length; intI++) {
        loc_arFoll[intI] = new XmlDocument(this.objSaxDoc, this.objSaxon);
        loc_arFollCnt[intI] = new Context();
      }
      // Start up the chunk reader
      loc_xrdFile = new XmlChunkReader(fThis);
      // Read the current node + following context
      for (intI = 0; intI <= objJob.intFollNum; intI++) {
        // Move to the first forest
        if (! (loc_xrdFile.ReadToFollowing("forest"))) {
          objErr.DoError("StreamFirstForest error: cannot find <forest> in file [" + strFile + "]");
          return false;
        }
        // Store it
        if (intI == 0) {
          // Get the current forest
          loc_pdxThis.LoadXml(loc_xrdFile.ReadOuterXml());
          // Get the current context
          ndxForest.argValue = loc_pdxThis.SelectSingleNode(loc_path_Forest);
          loc_cntThis.Seg = objParse.GetSeg(ndxForest.argValue);
          loc_cntThis.Loc = ndxForest.argValue.getAttributeValue(loc_xq_Location);
          loc_cntThis.TxtId = ndxForest.argValue.getAttributeValue(loc_xq_TextId);
        } else {
          // Fill the following context XmlDocument
          loc_arFoll[intI - 1].LoadXml(loc_xrdFile.ReadOuterXml());
          // Fill the following context @seg, @txtid and @loc
          ndxWork = loc_arFoll[intI - 1].SelectSingleNode(loc_path_Forest);
          loc_arFollCnt[intI - 1].Seg = objParse.GetSeg(ndxWork);
          loc_arFollCnt[intI - 1].Loc = ndxWork.getAttributeValue(loc_xq_Location);
          loc_arFollCnt[intI - 1].TxtId = ndxWork.getAttributeValue(loc_xq_TextId);
        }
      }
      //' Construct the current context
      //If (Not StreamMakeContext()) Then Return False
      // Return success
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/PsdxPerFile_FirstForest runtime error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    } catch (XPathExpressionException | IOException | SaxonApiException ex) {
      // Warn user
      objErr.DoError("XmlForest/PsdxPerFile_FirstForest Xpath/IO/SAX error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }

  // ----------------------------------------------------------------------------------------------------------
  // Name :  PsdxPerFile_IsEnd
  // Goal :  Check if the stream is at its end
  // History:
  // 08-04-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  private boolean PsdxPerFile_IsEnd() {
    try {
      return (loc_xrdFile.EOF) && (objJob.intFollNum == 0 || (loc_arFoll[0] == null));
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/PsdxPerFile_IsEnd error: " + ex.getMessage()  + "\r\n");
      // Return failure
      return false;
    }
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  PsdxPerFile_NextForest
  // Goal :  Load the next forest using an XmlReader 
  // History:
  // 18-03-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  private boolean PsdxPerFile_NextForest(ByRef<XmlNode> ndxForest) {
    XmlNode ndxWork = null; // Working node
    String strNext = ""; // Another chunk of <forest>
    int intI = 0; // Counter

    try {
      // Validate
      if (loc_pdxThis == null) return false;
      // Check for end of stream
      if (StreamIsEnd()) {
        ndxForest.argValue = null;
        return true;
      }
      // Try to read another piece of <forest> xml
      if (! loc_xrdFile.EOF) {
        // Read until  the following one
        loc_xrdFile.ReadToFollowing("forest");
        // Double check
        if (! loc_xrdFile.EOF) {
          // Read this <forest>
          strNext = loc_xrdFile.ReadOuterXml();
        }
      }
      // Double check what we got
      if (strNext == null || strNext.length() == 0) {
        ndxForest.argValue = null;
        return true;
      }
      // Do we have any preceding context to take care of?
      if (objJob.intPrecNum > 0) {
        // Copy preceding context
        for (intI = 1; intI < objJob.intPrecNum; intI++) {
          // Copy context, so that loc_arPrec(0) contains the furthest-away-being context
          loc_arPrec[intI - 1] = loc_arPrec[intI];
          // loc_arPrecCnt(intI - 1) = loc_arPrecCnt(intI)
          loc_arPrecCnt[intI - 1].Loc = loc_arPrecCnt[intI].Loc;
          loc_arPrecCnt[intI - 1].Seg = loc_arPrecCnt[intI].Seg;
          loc_arPrecCnt[intI - 1].TxtId = loc_arPrecCnt[intI].TxtId;
        }
        // Copy current into preceding context
        loc_arPrec[objJob.intPrecNum - 1] = loc_pdxThis;
        // loc_arPrecCnt(objJob.intPrecNum - 1) = loc_cntThis
        loc_arPrecCnt[objJob.intPrecNum - 1].Loc = loc_cntThis.Loc;
        loc_arPrecCnt[objJob.intPrecNum - 1].Seg = loc_cntThis.Seg;
        loc_arPrecCnt[objJob.intPrecNum - 1].TxtId = loc_cntThis.TxtId;
      }
      // Do we have any following context to take care of?
      if (objJob.intFollNum > 0) {
        // Copy the first-following into the current
        loc_pdxThis = loc_arFoll[0];
        loc_cntThis.Loc = loc_arFollCnt[0].Loc;
        loc_cntThis.Seg = loc_arFollCnt[0].Seg;
        loc_cntThis.TxtId = loc_arFollCnt[0].TxtId;
        //' ============== DEBUG ===============
        //If (InStr(loc_cntThis.Loc, "." & loc_pdxThis.SelectSingleNode(loc_path_Forest).Attributes("forestId").Value) = 0) Then
        //  Stop
        //End If
        //If (loc_pdxThis.SelectSingleNode(loc_path_Forest).Attributes("forestId").Value = 2) Then Stop
        //' ====================================
        // Shift all the other elements
        for (intI = 1; intI < objJob.intFollNum; intI++) {
          // Shift XmlDocument
          loc_arFoll[intI - 1] = loc_arFoll[intI];
          // Shift Context element
          loc_arFollCnt[intI - 1].Loc = loc_arFollCnt[intI].Loc;
          loc_arFollCnt[intI - 1].Seg = loc_arFollCnt[intI].Seg;
          loc_arFollCnt[intI - 1].TxtId = loc_arFollCnt[intI].TxtId;
        }
        // The last element becomes what we have physically read
        loc_arFoll[objJob.intFollNum - 1] = new XmlDocument(this.objSaxDoc, this.objSaxon);
        loc_arFoll[objJob.intFollNum - 1].LoadXml(strNext);
        // Get working node <forest>
        ndxWork = loc_arFoll[objJob.intFollNum - 1].SelectSingleNode(loc_path_Forest);
        // Calculate the correct context
        loc_arFollCnt[objJob.intFollNum - 1].Seg = objParse.GetSeg(ndxWork);
        loc_arFollCnt[objJob.intFollNum - 1].Loc = ndxWork.getAttributeValue(loc_xq_Location);
        loc_arFollCnt[objJob.intFollNum - 1].TxtId = ndxWork.getAttributeValue(loc_xq_TextId);
      } else {
        // No following context...
        loc_pdxThis.LoadXml(strNext);
        // Get this forest
        ndxWork = loc_pdxThis.SelectSingleNode(loc_path_Forest);
        // Calculate the correct context
        loc_cntThis.Seg = objParse.GetSeg(ndxWork);
        loc_cntThis.Loc = ndxWork.getAttributeValue(loc_xq_Location);
        loc_cntThis.TxtId = ndxWork.getAttributeValue(loc_xq_TextId);
      }
      // Double check for EOF
      if (loc_pdxThis == null) {
        ndxForest.argValue = null;
        return true;
      }
      // Find the current forest
      ndxForest.argValue = loc_pdxThis.SelectSingleNode(loc_path_Forest);
      //' ================================
      //' TODO: improve...
      //If (Not StreamMakeContext()) Then Return False
      // ============== DEBUG ===============
      //If (InStr(loc_cntThis.Loc, "." & ndxForest.Attributes("forestId").Value) = 0) Then
      //  Stop
      //End If
      // Return success
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/PsdxPerFile_NextForest runtime error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    } catch (XPathExpressionException | IOException | SaxonApiException ex) {
      // Warn user
      objErr.DoError("XmlForest/PsdxPerFile_NextForest Xpath/IO/SAX error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  PsdxPerFile_Percentage
  // Goal :  Show where we are in reading
  // History:
  // 07-04-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  private boolean PsdxPerFile_Percentage(ByRef<Integer> intPtc) {
    try {
      // Validate
      if (loc_xrdFile == null) 
        intPtc.argValue = 0;
      else if (loc_xrdFile.EOF) 
        intPtc.argValue = 100;
      else
        // Find out what my position is
        intPtc.argValue = loc_xrdFile.getPtc();
      // Return success
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/PsdxPerFile_NextForest error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  PsdxPerFile_GetContext
  // Goal :  Get the complete context
  // History:
  // 08-04-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  private String PsdxPerFile_GetContext() {
    String strPrec = ""; // Preceding context
    String strFoll = ""; // Following context
    String strBack = ""; // What we return
    int intI = 0; // Counter

    try {
      // Start out with the TextId (name of the text)
      // Prepend the textid (name of the text)
      strPrec = "[<b>" + loc_cntThis.TxtId + "</b>]";
      // Add the preceding context
      if (objJob.intPrecNum > 0) {
        // Attempt to get the preceding context
        for (intI = 0; intI < objJob.intPrecNum; intI++) {
          // Only load existing context
          if (loc_arPrecCnt[intI].Seg != null && loc_arPrecCnt[intI].Seg.length() > 0) {
            strPrec += "[" + loc_arPrecCnt[intI].Loc + "]" + loc_arPrecCnt[intI].Seg;
          }
        }
      }
      // Add the following context
      if (objJob.intFollNum > 0) {
        // Attempt to get the preceding context
        for (intI = 0; intI < objJob.intFollNum; intI++) {
          if (loc_arFollCnt[intI].Seg != null && loc_arFollCnt[intI].Seg.length() > 0) {
            strFoll += "[" + loc_arFollCnt[intI].Loc + "]" + loc_arFollCnt[intI].Seg;
          }
        }
      }
      // Return the text of the actual node
      strBack = loc_cntThis.Seg;
      // Did we find anything?
      if (strBack == null || strBack.length() == 0) {
        // Make empty stuff in HTML
        strBack = "<div><b>[" + loc_cntThis.Loc + "\r\n" + "]</b></div>";
      } else {
        // Convert into html
        strBack = "<div>" + strPrec + "</div>" + "<div><b>[" + loc_cntThis.Loc + "]</b>" + "\r\n" + "<Font color=#0000ff>" + strBack + "</Font>" + "\r\n" + "</div>" + "<div>" + strFoll + "</div>" + "\r\n";
      }
      // Set the local context string
      loc_strCombi = strBack;
      // Return the context
      return strBack;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/PsdxPerFile_GetContext error: " + ex.getMessage() + "\r\n");
      // Return failure
      return "";
    }
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  StreamIsEnd
  // Goal :  Check if the stream is at its end
  // History:
  // 08-04-2014   ERK Created for .NET
  // 24/apr/2015  ERK Ported to Java
  // ----------------------------------------------------------------------------------------------------------
  private boolean StreamIsEnd() {
    return (loc_xrdFile.EOF && objJob.intFollNum == 0 || loc_arFoll[0] == null);
  }  
  ======================================== */
// </editor-fold>

}
