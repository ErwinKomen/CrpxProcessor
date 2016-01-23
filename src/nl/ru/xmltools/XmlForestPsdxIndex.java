/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */
package nl.ru.xmltools;

import java.io.File;
import java.io.IOException;
import javax.xml.xpath.XPathExpressionException;
import net.sf.saxon.s9api.SaxonApiException;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.search.JobXq;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.util.ByRef;

/**
 * Implement the "XmlForest" interface for .psdx files, providing 'random-access'
 *   by means of indexing the .psdx file
 * 
 * @author Erwin R. Komen
 */
public class XmlForestPsdxIndex extends XmlForest {
  // ============ Local variables ==============================================
  private XmlIndexTgReader loc_xrdFile;   // Indexed reader for xml files
  private XmlIndexRaReader loc_xrdRaFile; // Indexed reader -- RA variant
  // ============ Call the standard class initializer ==========================
  public XmlForestPsdxIndex(CorpusResearchProject oCrp, JobXq oJob, ErrHandle oErr) {
    super(oCrp,oJob,oErr);
  }
  
  // ----------------------------------------------------------------------------------------------------------
  // Name :  FirstForest
  // Goal :  Load the first forest using an XmlReader 
  // History:
  // 18-03-2014  ERK Created for .NET
  // 10/jun/2015 ERK Adapated for "PsdxIndex" in Java
  // ----------------------------------------------------------------------------------------------------------
  @Override
  public boolean FirstForest(ByRef<XmlNode> ndxForest, ByRef<XmlNode> ndxHeader, ByRef<XmlNode> ndxMdi, String strFile) {
    XmlNode ndxWork;  // Working node
    File fThis;       // File object of [strFile]
    int intI;         // Counter

    try {
      // Validate: existence of file
      if (strFile == null || strFile.length() == 0) return false;
      fThis = new File(strFile);
      if (!fThis.exists()) return false;
      // Initialisations
      ndxForest.argValue = null;
      ndxHeader.argValue = null;
      ndxMdi.argValue = null;
      // Try get mdi reader
      MdiReader rdMdi = new MdiReader(this.objErr, this.loc_pdxMdi);
      String sMdiFile = rdMdi.getMdi(strFile);
      if (!sMdiFile.isEmpty()) {
        // Read the MDI (.imdi/.cmdi) file as an XmlDocument
        loc_pdxMdi = rdMdi.Read(sMdiFile);
        // Check what has been returned
        if (loc_pdxMdi != null) {
          // Set the pointer in the mdi
          ndxMdi.argValue = loc_pdxMdi.SelectSingleNode("./descendant-or-self::Session");
          if (ndxMdi.argValue == null) {
            // Set point to the root document
            ndxMdi.argValue = loc_pdxMdi.SelectSingleNode("/");
          }
        }
      }
      // Check defaults
      if (objJob != null) {
        iPrecNum = objJob.intPrecNum;
        iFollNum = objJob.intFollNum;
      }
      
      // Fill the arrays
      loc_arPrec = new XmlDocument[iPrecNum];
      loc_arPrecCnt = new XmlForest.Context[iPrecNum];
      loc_arFoll = new XmlDocument[iFollNum];
      loc_arFollCnt = new XmlForest.Context[iFollNum];
      for (intI = 0; intI < loc_arPrec.length; intI++) {
        loc_arPrec[intI] = new XmlDocument(this.objSaxDoc, this.objSaxon);
        loc_arPrecCnt[intI] = new XmlForest.Context();
      }
      for (intI = 0; intI < loc_arFoll.length; intI++) {
        loc_arFoll[intI] = new XmlDocument(this.objSaxDoc, this.objSaxon);
        loc_arFollCnt[intI] = new XmlForest.Context();
      }
      // Start up the streamer
      if (bUseRa) {
        loc_xrdRaFile = new XmlIndexRaReader(fThis, crpThis, loc_pdxThis, crpThis.intProjType);
      } else {
        loc_xrdFile = new XmlIndexTgReader(fThis, crpThis, loc_pdxThis, crpThis.intProjType);
      }
      // Get the text id
      String sTextId = "";
      if (bUseRa) sTextId = loc_xrdRaFile.getTextId(); else sTextId = loc_xrdFile.getTextId();
      // Do we have a header?
      String sHeader = "";
      sHeader = (bUseRa) ? loc_xrdRaFile.getHeader() : loc_xrdFile.getHeader();
      if (sHeader.isEmpty()) {
        // Indicate that it is empty
        ndxHeader.argValue = null;
      } else {
        // Load this obligatory teiHeader 
        loc_pdxThis.LoadXml(sHeader);
        // Set the global parameter
        // Node ndxFirst = loc_pdxThis.getDocument().getFirstChild();

        ndxWork = loc_pdxThis.SelectSingleNode(loc_path_PsdxHeader);
        ndxHeader.argValue =ndxWork;
      }
      // Read the first node + following context
      for (intI = 0; intI <= iFollNum; intI++) {
        // Store it
        String sForest = "";
        if (intI == 0) {
          // Get the FIRST forest
          sForest = (bUseRa) ? loc_xrdRaFile.getFirstLine() : loc_xrdFile.getFirstLine();
          // TODO: what if this is empty??
          loc_pdxThis.LoadXml(sForest);
          // Get the current context
          ndxForest.argValue = loc_pdxThis.SelectSingleNode(loc_path_PsdxSent);
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
            /*
            // ===== DEBUG =======
            if (loc_cntThis.Loc == null) {
              boolean bStop = true;
            }
            // ===================
                    */
          }
        } else {
          // Fill the following context XmlDocument
          sForest = (bUseRa) ?  loc_xrdRaFile.getNextLine() : loc_xrdFile.getNextLine();
          if (sForest.isEmpty()) {
            // This is already empty...
            loc_arFoll[intI-1] = null;
            loc_arFollCnt[intI-1].Seg = "";
            loc_arFollCnt[intI-1].Loc = "";
            loc_arFollCnt[intI-1].TxtId = "";
            continue;
          }
          loc_arFoll[intI - 1].LoadXml(sForest);
          // Fill the following context @seg, @txtid and @loc
          ndxWork = loc_arFoll[intI - 1].SelectSingleNode(loc_path_PsdxSent);
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
            // ===== DEBUG =======
            /*
            if (loc_arFollCnt[intI - 1].Loc  == null) {
              boolean bStop = true;
            } */
            // ===================
          }
        }
      }
      //' Construct the current context
      //If (Not StreamMakeContext()) Then Return False
      // Return success
      return true;
    } catch (XPathExpressionException ex) {
      // Warn user
      objErr.DoError("XmlForest/FirstForest Xpath error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/FirstForest Runtime error: " + ex.getMessage() + "\r\n");
      // ex.printStackTrace();
      // Return failure
      return false;
    } catch (SaxonApiException | IOException ex) {
      objErr.DoError("XmlForest/FirstForest IO/SAX error: " + ex.getMessage() + "\r\n");
      return false;
    }
  }
  @Override
  // ----------------------------------------------------------------------------------------------------------
  // Name :  OneForest
  // Goal :  Load the sentence with the indicated [sSentId] using an XmlReader 
  // History:
  // 20/Jul/2015  ERK Created for Java
  // ----------------------------------------------------------------------------------------------------------
  public boolean OneForest(ByRef<XmlNode> ndxForest, String sSentId) {
    XmlNode ndxWork;      // Working node
    String strNext = "";  // Another chunk of <forest>

    try {
      // Validate
      if (loc_pdxThis == null)  return false;
      // Check for end of stream
      if (IsEnd()) {
        ndxForest.argValue = null;
        return true;
      }
      // More validateion
      if (bUseRa) {
        if (loc_xrdRaFile==null) return false;
      } else {
        if (loc_xrdFile==null) return false;
      }
      // Read this <forest>
      strNext = (bUseRa) ? loc_xrdRaFile.getOneLine(sSentId) : loc_xrdFile.getOneLine(sSentId);
      // Double check what we got
      if (strNext == null || strNext.length() == 0) {
        ndxForest.argValue = null;
        return true;
      }      
      // Load this line...
      loc_pdxThis.LoadXml(strNext);
      // Find and return the indicated sentence
      ndxForest.argValue = loc_pdxThis.SelectSingleNode(loc_path_PsdxSent);
      // Return positively
      return true;
    } catch (Exception ex) {
      // Warn user
      objErr.DoError("XmlForest/OneForest error: ",ex );
      // Return failure
      return false;
    }
  }

  @Override
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
      // Check if there is a @forestId attribute
      sAttr = ndxForest.argValue.getAttributeValue(loc_xq_forestId);
      // attrThis = ndxForest.argValue.getAttributeValue(loc_xq_forestId);
      // if (attrThis == null ) return objErr.DoError("<forest> does not have @forestId");
      if (sAttr.isEmpty()) return objErr.DoError("<forest> does not have @forestId");
      // Get the @forestId value
      intForestId.argValue = Integer.parseInt(sAttr);
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
  // Name :  IsEnd
  // Goal :  Check if the stream is at its end
  // History:
  // 08-04-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  @Override
  public boolean IsEnd() {
    try {
      boolean bEof = (bUseRa) ? loc_xrdRaFile.EOF : loc_xrdFile.EOF;
      return (bEof) && (iFollNum == 0 || (loc_arFoll[0] == null));
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/IsEnd error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  NextForest
  // Goal :  Load the next forest using an XmlReader 
  // History:
  // 18-03-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  @Override
  public boolean NextForest(ByRef<XmlNode> ndxForest) {
    XmlNode ndxWork;      // Working node
    String strNext = "";  // Another chunk of <forest>
    int intI;             // Counter

    try {
      // Validate
      if (loc_pdxThis == null)  return false;
      // Check for end of stream
      if (IsEnd()) {
        ndxForest.argValue = null;
        return true;
      }
      // More validation
      if (bUseRa) {
        if (loc_xrdRaFile==null) return false;
        // Try to read another piece of <forest> xml
        if (! loc_xrdRaFile.EOF) strNext = loc_xrdRaFile.getNextLine();
        // Check for end-of-file and file closing
        if (loc_xrdRaFile.EOF) {
          // CLosing should be done by the caller -- we just return empty || loc_xrdRaFile = null;
          ndxForest.argValue = null;
          return true;
        }
      } else {
        if (loc_xrdFile==null) return false;
        // Try to read another piece of <forest> xml
        if (! loc_xrdFile.EOF) strNext = loc_xrdFile.getNextLine();
        // Check for end-of-file and file closing
        if (loc_xrdFile.EOF) loc_xrdFile = null;
      }
      // Double check what we got
      if (strNext == null || strNext.length() == 0) {
        ndxForest.argValue = null;
        return true;
      }
      // Do we have any preceding context to take care of?
      if (iPrecNum > 0) {
        // Copy preceding context
        for (intI = 1; intI < iPrecNum; intI++) {
          // Copy context, so that loc_arPrec(0) contains the furthest-away-being context
          loc_arPrec[intI - 1] = loc_arPrec[intI];
          // loc_arPrecCnt(intI - 1) = loc_arPrecCnt(intI)
          loc_arPrecCnt[intI - 1].Loc = loc_arPrecCnt[intI].Loc;
          loc_arPrecCnt[intI - 1].Seg = loc_arPrecCnt[intI].Seg;
          loc_arPrecCnt[intI - 1].TxtId = loc_arPrecCnt[intI].TxtId;
        }
        // Copy current into preceding context
        loc_arPrec[iPrecNum - 1] = loc_pdxThis;
        // loc_arPrecCnt(iPrecNum - 1) = loc_cntThis
        loc_arPrecCnt[iPrecNum - 1].Loc = loc_cntThis.Loc;
        loc_arPrecCnt[iPrecNum - 1].Seg = loc_cntThis.Seg;
        loc_arPrecCnt[iPrecNum - 1].TxtId = loc_cntThis.TxtId;
      }
      // Do we have any following context to take care of?
      if (iFollNum > 0) {
        // Copy the first-following into the current
        loc_pdxThis = loc_arFoll[0];
        loc_cntThis.Loc = loc_arFollCnt[0].Loc;
        loc_cntThis.Seg = loc_arFollCnt[0].Seg;
        loc_cntThis.TxtId = loc_arFollCnt[0].TxtId;
        //' ============== DEBUG ===============
        //If (InStr(loc_cntThis.Loc, "." & loc_pdxThis.SelectSingleNode(loc_path_PsdxSent).Attributes("forestId").Value) = 0) Then
        //  Stop
        //End If
        //If (loc_pdxThis.SelectSingleNode(loc_path_PsdxSent).Attributes("forestId").Value = 2) Then Stop
        //' ====================================
        // Shift all the other elements
        for (intI = 1; intI < iFollNum; intI++) {
          // Shift XmlDocument
          loc_arFoll[intI - 1] = loc_arFoll[intI];
          // Shift Context element
          loc_arFollCnt[intI - 1].Loc = loc_arFollCnt[intI].Loc;
          loc_arFollCnt[intI - 1].Seg = loc_arFollCnt[intI].Seg;
          loc_arFollCnt[intI - 1].TxtId = loc_arFollCnt[intI].TxtId;
        }
        // The last element becomes what we have physically read
        loc_arFoll[iFollNum - 1] = new XmlDocument(this.objSaxDoc, this.objSaxon);
        loc_arFoll[iFollNum - 1].LoadXml(strNext);
        // Get working node <forest>
        ndxWork = loc_arFoll[iFollNum - 1].SelectSingleNode(loc_path_PsdxSent);
        // Validate
        if (ndxWork == null) {
          // This should not happen. Check what is the matter
          String sWork = loc_arFoll[iFollNum - 1].getDoc();
          logger.debug("XmlForest empty work: " + sWork);
        } else {
          // Calculate the correct context
          loc_arFollCnt[iFollNum - 1].Seg = objParse.GetSeg(ndxWork);
          loc_arFollCnt[iFollNum - 1].Loc = ndxWork.getAttributeValue(loc_xq_Location);
          loc_arFollCnt[iFollNum - 1].TxtId = ndxWork.getAttributeValue(loc_xq_TextId);
          // ======== DEBUG =========
          /*
          if (loc_arFollCnt[iFollNum - 1].Loc == null) {
            boolean bStop = true;
          } */
        }
      } else {
        // No following context...
        loc_pdxThis.LoadXml(strNext);
        // Get this forest
        ndxWork = loc_pdxThis.SelectSingleNode(loc_path_PsdxSent);
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
          // ======== DEBUG =========
          /*
          if (loc_cntThis.Loc == null) {
            boolean bStop = true;
          } */
        }
      }
      // Double check for EOF
      if (loc_pdxThis == null) {
        ndxForest.argValue = null;
        return true;
      }
      // Find the current forest
      ndxForest.argValue = loc_pdxThis.SelectSingleNode(loc_path_PsdxSent);
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
      objErr.DoError("XmlForest/NextForest runtime error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    } catch (SaxonApiException | XPathExpressionException ex) {
      objErr.DoError("XmlForest/NextForest IO/Sax/Xpath error: " + ex.getMessage() + ""
              + "\r\n");
      // Return failure
      return false;
    }
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  Percentage
  // Goal :  Show where we are in reading
  // History:
  // 07-04-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  @Override
  public boolean Percentage(ByRef<Integer> intPtc) {
    try {
      // Validate
      if (bUseRa) {
        if (loc_xrdRaFile == null) { intPtc.argValue = 0; return true; }
        if (loc_xrdRaFile.EOF)     { intPtc.argValue = 100; return true; }
        // Find out what my position is
        intPtc.argValue = loc_xrdRaFile.getPtc();
      } else {
        if (loc_xrdFile == null) { intPtc.argValue = 0; return true; }
        if (loc_xrdFile.EOF)     { intPtc.argValue = 100; return true; }
        // Find out what my position is
        intPtc.argValue = loc_xrdFile.getPtc();
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
  // Name :  GetContext
  // Goal :  Get the complete context
  // History:
  // 08-04-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  @Override
  public String GetContext() {
    String strPrec;   // Preceding context
    String strFoll;   // Following context
    String strBack;   // What we return
    //int intI;         // Counter

    try {
      // Initialisations
      strFoll = "";
      // Start out with the TextId (name of the text)
      // Prepend the textid (name of the text)
      strPrec = "[<b>" + loc_cntThis.TxtId + "</b>]";
      // Add the preceding context
      if (iPrecNum > 0) {
        // Attempt to get the preceding context
        for (int intI = 0; intI < iPrecNum; intI++) {
          // DEBUG
          /*
          if (loc_arPrecCnt[intI] == null) {
            boolean bStop = true;
          } */
          // =======
          String sLoc = (loc_arPrecCnt[intI].Loc.isEmpty()) ? "" : "[" + loc_arPrecCnt[intI].Loc + "]";
          // Only load existing context
          if (loc_arPrecCnt[intI].Seg != null && loc_arPrecCnt[intI].Seg.length() > 0) {
            strPrec += sLoc + loc_arPrecCnt[intI].Seg;
          }
        }
      }
      // Add the following context
      if (iFollNum > 0) {
        // Attempt to get the preceding context
        for (int intI = 0; intI < iFollNum; intI++) {
          // DEBUG
          /*
          if (loc_arPrecCnt[intI] == null) {
            boolean bStop = true;
          } */
          // =======
          String sLoc = (loc_arPrecCnt[intI].Loc.isEmpty()) ? "" : "[" + loc_arPrecCnt[intI].Loc + "]";
          if (loc_arFollCnt[intI].Seg != null && loc_arFollCnt[intI].Seg.length() > 0) {
            strFoll += sLoc + loc_arFollCnt[intI].Seg;
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
        String sLoc = (loc_cntThis.Loc.isEmpty()) ? "" : "[" + loc_cntThis.Loc + "]";
        strBack = "<div>" + strPrec + "</div>" + "<div><b>" + sLoc + "</b>" + "\r\n" + "<Font color=#0000ff>" + strBack + "</Font>" + "\r\n" + "</div>" + "<div>" + strFoll + "</div>" + "\r\n";
      }
      // Set the local context string
      loc_strCombi = strBack;
      // Return the context
      return strBack;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/GetContext error: " + ex.getMessage() + "\r\n");
      ex.printStackTrace();
      // Return failure
      return "";
    }
  }
  
  // ----------------------------------------------------------------------------------------------------------
  // Name :  GetSyntax
  // Goal :  Get the syntax of the current line
  // History:
  // 08-09-2015  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  @Override
  public String GetSyntax(ByRef<XmlNode> ndxForest) {
    ByRef<StringBuilder> sbBack = new ByRef(null);
    sbBack.argValue = new StringBuilder();
    if (objParse.TravTree(crpThis.intProjType, ndxForest.argValue.getNode(), 0, sbBack))
      return sbBack.argValue.toString();
    else
      return "";
  }

  // ----------------------------------------------------------------------------------------------------------
  // Name :  GetPde
  // Goal :  Get the PDE (present-day English translation) of the current line
  // History:
  // 08-09-2015  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  @Override
  public String GetPde(ByRef<XmlNode> ndxForest) {
    return objParse.GetPde(ndxForest.argValue);
  }
  
  @Override
  public void close() {
    // Close the underlying index reader
    if (bUseRa && loc_xrdRaFile != null) loc_xrdRaFile.close();
  }
    
}
