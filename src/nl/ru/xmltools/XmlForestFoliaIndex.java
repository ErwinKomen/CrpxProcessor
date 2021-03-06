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
import java.util.List;
import javax.xml.xpath.XPathExpressionException;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.search.JobXq;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.tools.FileIO;
import nl.ru.util.ByRef;
import nl.ru.util.json.JSONObject;

/**
 * Implement the "XmlForest" interface for .folia.xml files, providing 'random-access'
 *   by means of indexing the .folia.xml file
 * 
 * @author Erwin R. Komen
 */
public class XmlForestFoliaIndex extends XmlForest {
  // ============ Local variables ==============================================
  private XmlIndexTgReader loc_xrdFile;   // Indexed reader for xml files
  private XmlIndexRaReader loc_xrdRaFile; // Indexed reader -- RA variant
  protected static final QName loc_xq_Text = new QName("", "", "t");            // Text attribute of <wref> node
  // ============ Call the standard class initializer ==========================
  public XmlForestFoliaIndex(CorpusResearchProject oCrp, JobXq oJob, ErrHandle oErr) {
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
        ndxWork = loc_pdxThis.SelectSingleNode(loc_path_FoliaHeader);
        ndxHeader.argValue =ndxWork;
      }
      // Read the first node + following context
      for (intI = 0; intI <= iFollNum; intI++) {
        // Store it
        String sForest = "";
        if (intI == 0) {
          // Get the FIRST forest
          sForest = (bUseRa) ? loc_xrdRaFile.getFirstLine() : loc_xrdFile.getFirstLine();
          // It is possible that NULL is returned, which indicates error
          if (sForest == null) { return false;}
          // Make sure it is not empty
          while (!loc_xrdRaFile.is_closed() && ( sForest.isEmpty() || !sForest.startsWith(loc_Folia_SentStart))) {
            sForest = (bUseRa) ?  loc_xrdRaFile.getNextLine() : loc_xrdFile.getNextLine();
          }
          // Double check for closure
          if (loc_xrdRaFile.is_closed()) {
            return false;
          }
          // TODO: what if this is empty??
          loc_pdxThis.LoadXml(sForest);
          // Get the current context
          ndxForest.argValue = loc_pdxThis.SelectSingleNode(loc_path_FoliaSent);
          // Validate: do we have a result?
          if (ndxForest.argValue == null) {
            // This should not happen. Check what is the matter
            String sWork = loc_pdxThis.getDoc();
            logger.debug("XmlForest empty ndxForest.argValue: " + sWork);
          } else {
            loc_cntThis.Seg = objParse.GetSeg(ndxForest.argValue);
            // We don't really have the equivalent of Location and TxtId
            loc_cntThis.Loc = ndxForest.argValue.getAttributeValue(loc_xq_Folia_Id);
            loc_cntThis.TxtId = sTextId;
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
          ndxWork = loc_arFoll[intI - 1].SelectSingleNode(loc_path_FoliaSent);
          // Validate: do we have a result?
          if (ndxWork == null) {
            // This should not happen. Check what is the matter
            String sWork = loc_arFoll[intI - 1].getDoc();
            logger.debug("XmlForest empty work: " + sWork);
          } else {
            loc_arFollCnt[intI - 1].Seg = objParse.GetSeg(ndxWork);
            loc_arFollCnt[intI - 1].Loc = ndxWork.getAttributeValue(loc_xq_Folia_Id);
            loc_arFollCnt[intI - 1].TxtId = sTextId;
          }
        }
      }
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
      // More validation
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
      ndxForest.argValue = loc_pdxThis.SelectSingleNode(loc_path_FoliaSent);
      // Return positively
      return true;
    } catch (Exception ex) {
      // Warn user
      objErr.DoError("XmlForest/OneForest error: ", ex);
      // Return failure
      return false;
    }
  }
  @Override
  // Version that looks [intLines] from the forest specified by [sSentId]
  public boolean OneForest(ByRef<XmlNode> ndxForest, String sSentId, int intLines) {
    XmlNode ndxWork;      // Working node
    int iSentIdx = -1;    // Index of the [sSentId] line
    String strNext = "";  // Another chunk of <forest>

    try {
      // Validate
      if (loc_pdxThis == null)  return false;
      // Only suitable for the RA reader
      if (!bUseRa) return false;
      // Check for end of stream
      if (IsEnd()) {
        ndxForest.argValue = null;
        return true;
      }
      // More validation
      if (loc_xrdRaFile==null) return false;
      
      // Get the index of [sSentId]
      iSentIdx = loc_xrdRaFile.getIndexOfLine(sSentId);
      if (iSentIdx<0) { return false; }
      // Read this <forest>
      strNext = loc_xrdRaFile.getOneLine(iSentIdx + intLines);
      // Double check what we got
      if (strNext == null || strNext.length() == 0) {
        ndxForest.argValue = null;
        return true;
      }      
      // Load this line...
      loc_pdxThis.LoadXml(strNext);
      // Find and return the indicated sentence
      ndxForest.argValue = loc_pdxThis.SelectSingleNode(loc_path_FoliaSent);
      // Return positively
      return true;
    } catch (Exception ex) {
      // Warn user
      objErr.DoError("XmlForest/OneForest error: ", ex);
      // Return failure
      return false;
    }
  }
  
  @Override
  public boolean OneForest(ByRef<XmlNode> ndxForest, int iSentIdx) {
    String strNext;
    
    try {
      
      if (iSentIdx<0) { return false; }
      // Read this <forest>
      strNext = loc_xrdRaFile.getOneLine(iSentIdx);
      // Double check what we got
      if (strNext == null || strNext.length() == 0) {
        ndxForest.argValue = null;
        return true;
      }
      // Prepare a new document
      loc_pdxAny = new XmlDocument(this.objSaxDoc, this.objSaxon);
      
      // Load the line that we found
      loc_pdxAny.LoadXml(strNext);
            
      // Find and return the indicated sentence
      ndxForest.argValue = loc_pdxAny.SelectSingleNode(loc_path_FoliaSent);
      
      // Return positively
      return true;
    } catch (Exception ex) {
      // Warn user
      objErr.DoError("XmlForestFoliaIndex/OneForest error: ", ex);
      // Return failure
      return false;
    }
  }
 
  @Override
  public boolean FindForest(ByRef<XmlNode> ndxForest, String sNodeId) {
    // Not (yet) implemented for FoLiA
    return false;
  }
  
  @Override
  public int NodeDist(String sConstFromId, String sConstToId, String sType) {
    int iSrcIdx = -1;
    int iDstIdx = -1;
    int iDist = 0;
    
    try {
      switch (sType) {
        case "lines":
          // Get source node and destination node indexes
          iSrcIdx = loc_xrdRaFile.getIndexOfNode("s", sConstFromId);
          iDstIdx = loc_xrdRaFile.getIndexOfNode("s", sConstToId);
          if (iSrcIdx >=0 && iDstIdx >=0) {
            // Return the difference
            iDist = iSrcIdx - iDstIdx;
          }
          break;
      }
      // Return the distance
      return iDist;
    } catch (Exception ex) {
      // Warn user
      objErr.DoError("XmlForestFoliaIndex/NodeDist error: ", ex);
      // Return failure
      return 0;
    }
  }

  @Override
  public String getSentenceId(ByRef<XmlNode> ndxForest) {
    String sBack = "";  // Returned value
    
    try {
      // Check if there is a @forestId attribute
      sBack = ndxForest.argValue.getAttributeValue(loc_xq_Folia_Id);
      // Return what we found
      return sBack;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/getSentenceId", ex);
      // Return failure
      return sBack;
    }
  }
  
  @Override
  public int getIndexFromSent(XmlNode ndxForest) {
    String sSentId = "";    // Identifier of the sentence
    int idx = -1;
    
    try {
      // Get the sentence index
      sSentId = ndxForest.getAttributeValue(loc_xq_Folia_Id);
      // Find the correct index line number for this sentence
      idx = this.loc_xrdRaFile.getIndexOfLine(sSentId);
      // Return what we found
      return idx;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/getIndexFromSent", ex);
      // Return failure
      return -1;
    }
  }

  @Override
  public int getIndexFromConst(XmlNode ndxConst) {
    String sConstId = "";    // Identifier of the sentence
    
    try {
      // Get the sentence index
      sConstId = ndxConst.getAttributeValue(loc_xq_Folia_Id);
      return getIndexFromConst(sConstId);
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/getIndexFromConst", ex);
      // Return failure
      return -1;
    }
  }
  @Override
  public int getIndexFromConst(String sNodeId) {
    int idx = -1;
    
    try {
      // Find the correct index line number for this sentence
      idx = this.loc_xrdRaFile.getIndexOfNode("s", sNodeId);
      // Return what we found
      return idx;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlForest/getIndexFromConst", ex);
      // Return failure
      return -1;
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
      sAttr = ndxForest.argValue.getAttributeValue(loc_xq_Folia_Id);
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
      String sTextId = "";
      if (bUseRa) {
        if (loc_xrdRaFile==null) return false;
        // Try to read another piece of <forest> xml
        if (! loc_xrdRaFile.EOF) strNext = loc_xrdRaFile.getNextLine();
        // Check for end-of-file and file closing
        if (loc_xrdRaFile.EOF) {
          // Closing should be done by the caller -- We just return empty
          ndxForest.argValue = null;
        }
        // Double check what we got
        if (strNext == null || strNext.length() == 0) {
          // CHeck if there is still stuff in the NEXT
          if (iFollNum == 0 || loc_arFoll[0] == null ) {
            ndxForest.argValue = null;
            return true;
          }
        }
        sTextId = loc_xrdRaFile.getTextId();
      } else {
        if (loc_xrdFile==null) return false;
        // Try to read another piece of <forest> xml
        if (! loc_xrdFile.EOF) strNext = loc_xrdFile.getNextLine();
        // Check for end-of-file and file closing
        if (loc_xrdFile.EOF) loc_xrdFile = null;
        // Double check what we got
        if (strNext == null || strNext.length() == 0) {
          // CHeck if there is still stuff in the NEXT
          if (iFollNum == 0 || loc_arFoll[0] == null) {
            ndxForest.argValue = null;
            return true;
          }
        }
        sTextId = loc_xrdFile.getTextId();
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
        //If (InStr(loc_cntThis.Loc, "." & loc_pdxThis.SelectSingleNode(loc_path_FoliaSent).Attributes("forestId").Value) = 0) Then
        //  Stop
        //End If
        //If (loc_pdxThis.SelectSingleNode(loc_path_FoliaSent).Attributes("forestId").Value = 2) Then Stop
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
        if (strNext == null || strNext.isEmpty()) {
          loc_arFoll[iFollNum - 1] = null;
        } else {
          // The last element becomes what we have physically read
          loc_arFoll[iFollNum - 1] = new XmlDocument(this.objSaxDoc, this.objSaxon);
          loc_arFoll[iFollNum - 1].LoadXml(strNext);
          // Get working node <forest>
          ndxWork = loc_arFoll[iFollNum - 1].SelectSingleNode(loc_path_FoliaSent);
          // Validate
          if (ndxWork == null) {
            // This should not happen. Check what is the matter
            String sWork = loc_arFoll[iFollNum - 1].getDoc();
            logger.debug("XmlForest empty work: " + sWork);
          } else {
            // Calculate the correct context
            loc_arFollCnt[iFollNum - 1].Seg = objParse.GetSeg(ndxWork);
            loc_arFollCnt[iFollNum - 1].Loc = ndxWork.getAttributeValue(loc_xq_Folia_Id);
            loc_arFollCnt[iFollNum - 1].TxtId = sTextId;
          }
        }
      } else {
        // No following context...
        loc_pdxThis.LoadXml(strNext);
        // Get this forest
        ndxWork = loc_pdxThis.SelectSingleNode(loc_path_FoliaSent);
        // Validate
        if (ndxWork == null) {
          // This should not happen. Check what is the matter
          String sWork = loc_pdxThis.getDoc();
          logger.debug("XmlForest empty work: " + sWork);
        } else {
          // Calculate the correct context
          loc_cntThis.Seg = objParse.GetSeg(ndxWork);
          loc_cntThis.Loc = ndxWork.getAttributeValue(loc_xq_Folia_Id);
          loc_cntThis.TxtId = sTextId;
        }
      }
      // Double check for EOF
      if (loc_pdxThis == null) {
        ndxForest.argValue = null;
        return true;
      }
      // Find the current forest
      ndxForest.argValue = loc_pdxThis.SelectSingleNode(loc_path_FoliaSent);
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
  /**
   * getHitLine
   *    Given the sentence in [ndxSentence] get a JSON representation of 
   *    this sentence that includes:
   *    { 'pre': 'text preceding the hit',
   *      'hit': 'the hit text',
   *      'fol': 'text following the hit'}
   * 
   * @param sLocs
   * @param sLocw
   * @return 
   */
  @Override
  public JSONObject getHitLine(String sLocs, String sLocw) {
    JSONObject oBack = new JSONObject();
    ByRef <XmlNode> ndxSent = new ByRef(null);
    String sPre = "";
    String sHit = "";
    String sFol = "";
    
    try {
      // Make sure the indicated sentence is read
      if (!this.OneForest(ndxSent, sLocs)) { logger.error("getHitLine: could not read Sentence ["+sLocs+"]"); return null; }
      // Validate
      if (ndxSent.argValue == null)  { logger.error("getHitSyntax: ndxSent is empty"); return null; }
      // Get word nodes of the whole sentence
      // Attempt to leave out the non-words -- not fool-proof, because we now exclude "*" marks in the text...
      List<XmlNode> arWords = ndxSent.argValue.SelectNodes("./descendant::wref[not(starts-with(@t, '*'))]");
      // Walk the results
      int i=0;
      // Get the preceding context
      ndxSent.argValue.SelectSingleNode("./descendant::wref[@id='"+arWords.get(i).getAttributeValue("id")+"']");
      while(i < arWords.size() && !hasAncestor(arWords.get(i), "xml:id", sLocw))  {
        // Double check for CODE ancestor
        if (!hasAncestor(arWords.get(i), "pos", "CODE"))
          sPre += arWords.get(i).getAttributeValue(loc_xq_Text) + " ";
        i++;
      }
      // Get the hit context
      while(i < arWords.size() && hasAncestor(arWords.get(i), "xml:id", sLocw))  {
        // Double check for CODE ancestor
        if (!hasAncestor(arWords.get(i), "pos", "CODE"))
          sHit += arWords.get(i).getAttributeValue(loc_xq_Text) + " ";
        i++;
      }
      // Get the following context
      while(i < arWords.size() && !hasAncestor(arWords.get(i), "xml:id", sLocw))  {
        // Double check for CODE ancestor
        if (!hasAncestor(arWords.get(i), "pos", "CODE"))
          sFol += arWords.get(i).getAttributeValue(loc_xq_Text) + " ";
        i++;
      }
      
      // Construct object
      oBack.put("pre", sPre.trim());
      oBack.put("hit", sHit.trim());
      oBack.put("fol", sFol.trim());
      return oBack;
    } catch (Exception ex) {
      logger.error("getHitLine failed", ex);
      return null;
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
          // Only load existing context
          if (loc_arPrecCnt[intI].Seg != null && loc_arPrecCnt[intI].Seg.length() > 0) {
            strPrec += "[" + loc_arPrecCnt[intI].Loc + "]" + loc_arPrecCnt[intI].Seg;
          }
        }
      }
      // Add the following context
      if (iFollNum > 0) {
        // Attempt to get the preceding context
        for (int intI = 0; intI < iFollNum; intI++) {
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
      objErr.DoError("XmlForest/GetContext error: " + ex.getMessage() + "\r\n");
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
  
  /**
   * hasAncestor
   *    Check if the indicated node has an ancestor of type @sType with value @sValue
   * 
   * @param ndThis
   * @param sType
   * @param sValue
   * @return 
   */
  @Override
  public boolean hasAncestor(XmlNode ndThis, String sType, String sValue) {
    String sPath;
    try {
      // Validate
      if (ndThis == null) return false;
      // Action depends on the type
      switch (sType) {
        case "id":
          sPath = "./ancestor::su[@id = '" + sValue + "'] ";
          break;
        case "pos":
          sPath = "./ancestor::su[@class = '" + sValue + "'] ";
          break;
        case "xml:id":
          sPath = "./ancestor::su[@xml:id = '" + sValue + "'] ";
          break;
        default:
          return false;
      }
      // Check if the ancestor exists
      return (ndThis.SelectSingleNode(sPath) != null);
    } catch (Exception ex) {
      // Warn user
      logger.error("XmlForestFoliaIndex/hasAncestor error: ", ex);
      // Return failure
      return false;
    }
  }

  @Override
  public int GetSize() {
    return loc_xrdRaFile.size();
  }
  
  @Override
  public void close() {
    // Close the underlying index reader
    if (bUseRa && loc_xrdRaFile != null) {
      // Close the RA reader
      loc_xrdRaFile.close();
    }
  }
    
  
}

