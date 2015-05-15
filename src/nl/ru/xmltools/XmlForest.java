/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;
// <editor-fold defaultstate="collapsed" desc="Imports">

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;
import nl.ru.crpx.project.CorpusResearchProject;
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
public class XmlForest {
  // This class uses a logger
  private final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XmlForest.class);
// <editor-fold defaultstate="collapsed" desc="Header">
  private final  class Context {
    public String Seg;    // Text of this line
    public String TxtId;  // TextId of this line
    public String Loc;    // Location for this line
  }

  public enum ForType {
    PsdxWholeFile(1),   // Each file in one go
    PsdxPerForest(2),   // Each <forest> element
    PsdxPerForgrp(3),   // Each <forestGrp> element
    FoliaWholeFile(10), // Each file in one go
    FoliaPerDiv(11),    // Each <div> element
    FoliaPerPara(12),   // Each <p> element
    FoliaPerS(13);      // Each <s> element

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
  }
  // ========================================== LOCAL VARIABLE ================================================
  private String loc_strCurrent = "";   // XML code of current forest
  private String loc_strCombi = "";     // Combined XML context, from where current node is taken
  private int loc_intCurrent;           // Position of current node within [loc_arContext]
  private List<String> loc_colStack;    // Stack for the context
  private List<String> loc_colCombi;    // Where we combine the context
  private XmlDocument loc_pdxThis;      // Current one
  private XmlDocument[] loc_arPrec;     // Preceding lines as Xml document
  private XmlDocument[] loc_arFoll;     // Following lines as Xml document
  private Context[] loc_arPrecCnt;      // Preceding context
  private Context[] loc_arFollCnt;      // Following context
  private Context loc_cntThis;          // Current context
  private XmlChunkReader loc_xrdFile;   // Local copy of XML chunk reader
  private ForType loc_Type;             // The type of treatment expected
  private ErrHandle objErr;               // Local access to the general object with global variables
  private JobXq objJob;                   // Access to the job that is being executed
  private CorpusResearchProject crpThis;  // The corpus research project for which I am created
  private Parse objParse;                 // Object to use my own version of the "GetSeg()" function
  // private XmlReaderSettings loc_xrdSet; // Special arrangements for the reader --> already done in XmlDocument()
  // ==========================================================================================================
  // Class instantiation
  public XmlForest(CorpusResearchProject oCrp, JobXq oJob, ErrHandle oErr) {
    loc_pdxThis = new XmlDocument();
    // loc_xrdSet = new XmlReaderSettings(); // This is already done in XmlDocument()
    loc_colStack = new ArrayList<>();
    loc_cntThis  = new Context();
    loc_Type  = ForType.PsdxWholeFile;
    objErr = oErr;
    crpThis = oCrp;
    objParse = new Parse(oCrp, oErr);
    this.objJob = oJob;
  }
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Main">
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

  // ----------------------------------------------------------------------------------------------------------
  // Name :  FirstForest
  // Goal :  Load the first forest - depending on method 
  // History:
  // 14-04-2014  ERK Created
  // 23/apr/2015  ERK Adapted for Java
  // ----------------------------------------------------------------------------------------------------------
  public final boolean FirstForest(ByRef<XmlNode> ndxForest, ByRef<XmlNode> ndxHeader, String strFile) {
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
  public final boolean GetForestId(ByRef<XmlNode> ndxForest, ByRef<Integer> intForestId) {
    Node attrThis;  // Room for one attribute
    
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
          attrThis = ndxForest.argValue.Attributes("forestId") ;
          if (attrThis == null ) return objErr.DoError("<forest> does not have @forestId");
          // Get the @forestId value
          intForestId.argValue = Integer.parseInt(attrThis.getNodeValue());
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
  public final boolean NextForest(ByRef<XmlNode> ndxForest) {
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
  public final boolean IsEnd() {
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
  public final boolean Percentage(ByRef<Integer> intPtc) {
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
  public final String GetContext() {
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
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="PsdxPerForest">
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
        loc_arPrec[intI] = new XmlDocument();
        loc_arPrecCnt[intI] = new Context();
      }
      for (intI = 0; intI < loc_arFoll.length; intI++) {
        loc_arFoll[intI] = new XmlDocument();
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
      Node ndxFirst = loc_pdxThis.getDocument().getFirstChild();
      ndxWork = loc_pdxThis.SelectSingleNode("//teiHeader");
      // ndxWork = loc_pdxThis.SelectSingleNode("./descendant-or-self::teiHeader[1]");
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
          ndxForest.argValue = loc_pdxThis.SelectSingleNode("./descendant::forest[1]");
          loc_cntThis.Seg = objParse.GetSeg(ndxForest.argValue);
          loc_cntThis.Loc = ndxForest.argValue.Attributes("Location").Value;
          loc_cntThis.TxtId = ndxForest.argValue.Attributes("TextId").Value;
        } else {
          // Fill the following context XmlDocument
          loc_arFoll[intI - 1].LoadXml(loc_xrdFile.ReadOuterXml());
          // Fill the following context @seg, @txtid and @loc
          ndxWork = loc_arFoll[intI - 1].SelectSingleNode("./descendant::forest[1]");
          loc_arFollCnt[intI - 1].Seg = objParse.GetSeg(ndxWork);
          loc_arFollCnt[intI - 1].Loc = ndxWork.Attributes("Location").Value;
          loc_arFollCnt[intI - 1].TxtId = ndxWork.Attributes("TextId").Value;
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
      // Return failure
      return false;
    } catch (IOException | SAXException ex) {
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
        //If (InStr(loc_cntThis.Loc, "." & loc_pdxThis.SelectSingleNode("./descendant::forest[1]").Attributes("forestId").Value) = 0) Then
        //  Stop
        //End If
        //If (loc_pdxThis.SelectSingleNode("./descendant::forest[1]").Attributes("forestId").Value = 2) Then Stop
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
        loc_arFoll[objJob.intFollNum - 1] = new XmlDocument();
        loc_arFoll[objJob.intFollNum - 1].LoadXml(strNext);
        // Get working node <forest>
        ndxWork = loc_arFoll[objJob.intFollNum - 1].SelectSingleNode("./descendant::forest[1]");
        // Calculate the correct context
        loc_arFollCnt[objJob.intFollNum - 1].Seg = objParse.GetSeg(ndxWork);
        loc_arFollCnt[objJob.intFollNum - 1].Loc = ndxWork.Attributes("Location").Value;
        loc_arFollCnt[objJob.intFollNum - 1].TxtId = ndxWork.Attributes("TextId").Value;
      } else {
        // No following context...
        loc_pdxThis.LoadXml(strNext);
        // Get this forest
        ndxWork = loc_pdxThis.SelectSingleNode("./descendant::forest[1]");
        // Calculate the correct context
        loc_cntThis.Seg = objParse.GetSeg(ndxWork);
        loc_cntThis.Loc = ndxWork.Attributes("Location").Value;
        loc_cntThis.TxtId = ndxWork.Attributes("TextId").Value;
      }
      // Double check for EOF
      if (loc_pdxThis == null) {
        ndxForest.argValue = null;
        return true;
      }
      // Find the current forest
      ndxForest.argValue = loc_pdxThis.SelectSingleNode("./descendant::forest[1]");
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
    } catch (IOException | SAXException | XPathExpressionException ex) {
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
//VB TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
///#End Region
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="PsdxPerFile">
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
        loc_arPrec[intI] = new XmlDocument();
        loc_arPrecCnt[intI] = new Context();
      }
      for (intI = 0; intI < loc_arFoll.length; intI++) {
        loc_arFoll[intI] = new XmlDocument();
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
          ndxForest.argValue = loc_pdxThis.SelectSingleNode("./descendant::forest[1]");
          loc_cntThis.Seg = objParse.GetSeg(ndxForest.argValue);
          loc_cntThis.Loc = ndxForest.argValue.Attributes("Location").Value;
          loc_cntThis.TxtId = ndxForest.argValue.Attributes("TextId").Value;
        } else {
          // Fill the following context XmlDocument
          loc_arFoll[intI - 1].LoadXml(loc_xrdFile.ReadOuterXml());
          // Fill the following context @seg, @txtid and @loc
          ndxWork = loc_arFoll[intI - 1].SelectSingleNode("./descendant::forest[1]");
          loc_arFollCnt[intI - 1].Seg = objParse.GetSeg(ndxWork);
          loc_arFollCnt[intI - 1].Loc = ndxWork.Attributes("Location").Value;
          loc_arFollCnt[intI - 1].TxtId = ndxWork.Attributes("TextId").Value;
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
    } catch (XPathExpressionException | IOException | SAXException ex) {
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
        //If (InStr(loc_cntThis.Loc, "." & loc_pdxThis.SelectSingleNode("./descendant::forest[1]").Attributes("forestId").Value) = 0) Then
        //  Stop
        //End If
        //If (loc_pdxThis.SelectSingleNode("./descendant::forest[1]").Attributes("forestId").Value = 2) Then Stop
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
        loc_arFoll[objJob.intFollNum - 1] = new XmlDocument();
        loc_arFoll[objJob.intFollNum - 1].LoadXml(strNext);
        // Get working node <forest>
        ndxWork = loc_arFoll[objJob.intFollNum - 1].SelectSingleNode("./descendant::forest[1]");
        // Calculate the correct context
        loc_arFollCnt[objJob.intFollNum - 1].Seg = objParse.GetSeg(ndxWork);
        loc_arFollCnt[objJob.intFollNum - 1].Loc = ndxWork.Attributes("Location").Value;
        loc_arFollCnt[objJob.intFollNum - 1].TxtId = ndxWork.Attributes("TextId").Value;
      } else {
        // No following context...
        loc_pdxThis.LoadXml(strNext);
        // Get this forest
        ndxWork = loc_pdxThis.SelectSingleNode("./descendant::forest[1]");
        // Calculate the correct context
        loc_cntThis.Seg = objParse.GetSeg(ndxWork);
        loc_cntThis.Loc = ndxWork.Attributes("Location").Value;
        loc_cntThis.TxtId = ndxWork.Attributes("TextId").Value;
      }
      // Double check for EOF
      if (loc_pdxThis == null) {
        ndxForest.argValue = null;
        return true;
      }
      // Find the current forest
      ndxForest.argValue = loc_pdxThis.SelectSingleNode("./descendant::forest[1]");
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
    } catch (XPathExpressionException | IOException | SAXException ex) {
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
// </editor-fold>

}
