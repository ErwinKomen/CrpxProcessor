/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */
package nl.ru.xmltools;

import java.io.File;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;
import net.sf.saxon.s9api.SaxonApiException;
import nl.ru.crpx.dataobject.DataObjectMapElement;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.project.CorpusResearchProject.ProjType;
import nl.ru.crpx.search.JobXq;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.util.ByRef;
import nl.ru.util.json.JSONObject;

/**
 * XmlResultPsdxIndex - use the indexed reader to go through result database
 * 
 * @author Erwin R. Komen
 */
public class XmlResultPsdxIndex extends XmlResult {
  // ============ Local variables ==============================================
  private XmlIndexRaReader loc_xrdFile;   // Indexed reader for xml files -- one random-access file
  private XmlIndexTgReader loc_xrdTgFile; // Indexed reader for xml files -- tag-parts are stored separately
  // ============ Call the standard class initializer ==========================
  public XmlResultPsdxIndex(CorpusResearchProject oCrp, JobXq oJob, ErrHandle oErr) {
    super(oCrp,oJob,oErr);
  }
  
  // ======================== Make the list of @File elements available =========
  @Override
  public List<String> getResultFileList() { return this.lstResFile; }
  
  /**
   * headerInfo -- provide information from the header through a JSON object
   * 
   * @return 
   */
  public JSONObject headerInfo() {
    JSONObject oHeader = new JSONObject();
    
    try {
      // Validate
      if (this.loc_ndxHeader == null) return null;
      // Get information from the <General> section
      List<XmlNode> lChildren = loc_ndxHeader.SelectNodes("./child::node()");
      for (int i=0;i<lChildren.size(); i++) {
        XmlNode ndChild = lChildren.get(i);
        // Determine the key and the value
        String sKey = ndChild.getNodeName().toString();
        String sValue = ndChild.getNodeValue();
        oHeader.put(sKey, sValue);
      }
      // Return the object
      return oHeader;
    } catch (Exception ex) {
      objErr.DoError("XmlResultPsdxIndex/headerInfo error: ", ex);
      return null;
    }
  }
  public DataObjectMapElement headerInfoDO() {
    DataObjectMapElement oGeneral = new DataObjectMapElement();
    try {
      // Validate
      if (this.loc_ndxHeader == null) return null;
      // Get information from the <General> section
      List<XmlNode> lChildren = loc_ndxHeader.SelectNodes("./child::node()");
      for (int i=0;i<lChildren.size(); i++) {
        XmlNode ndChild = lChildren.get(i);
        // Determine the key and the value
        String sKey = ndChild.getNodeName().toString();
        String sValue = ndChild.getNodeValue();
        oGeneral.put(sKey, sValue);
      }
      // Return the object
      return oGeneral;
    } catch (Exception ex) {
      objErr.DoError("XmlResultPsdxIndex/headerInfoDO error: ", ex);
      return null;
    }
  }
  
  /**
   * Prepare -- Prepare the database @strFile for reading
   * 
   * @param strDbaseFile
   * @return 
   */
  @Override
  public boolean Prepare(String strDbaseFile) {
    File fThis;       // File object of [strDbaseFile]

    try {
      // Validate: existence of file
      if (strDbaseFile == null || strDbaseFile.length() == 0) return false;
      fThis = new File(strDbaseFile);
      if (!fThis.exists()) return false;
      // Start up the streamer
      loc_xrdFile = new XmlIndexRaReader(fThis, crpThis, loc_pdxThis, CorpusResearchProject.ProjType.Dbase);
      // Get the list of @File elements inside the database
      this.lstResFile = loc_xrdFile.getPartList();
      // Do we have a header?
      String sHeader = loc_xrdFile.getHeader();
      if (sHeader.isEmpty()) {
        // Indicate that it is empty
        loc_ndxHeader = null;
      } else {
        // Load this obligatory <General> header 
        loc_pdxThis.LoadXml(sHeader);
        loc_ndxHeader =loc_pdxThis.SelectSingleNode(loc_path_General);
      }
      // This does not pertain to one particular part
      this.loc_strPart = "";
      // Return positively
      return true;
    } catch (Exception ex) {
      objErr.DoError("XmlResult/Prepare error: ", ex);
      return false;
    }
  }
  
  
  /**
   * Prepare -- Prepare reading [strPart] from database [strDbaseFile]
   * 
   * @param strDbaseFile
   * @param strPart
   * @return 
   * @history
   *  15/sep/2015 ERK Created
   */
  @Override
  public boolean Prepare(String strDbaseFile, String strPart) {
    boolean bUseRa = true;  // Use Ra method or Tg?

    try {
      // This pertains to one particular part
      this.loc_strPart = strPart;
      // Action depends on method
      if (bUseRa) 
        return PrepareRa(strDbaseFile, strPart);
      else
        return PrepareTg(strDbaseFile, strPart);
    } catch (Exception ex) {
      objErr.DoError("XmlResult/Prepare error: ", ex);
      return false;
    }
  }
  /**
   * Prepare -- PrepareRa reading [strPart] from database [strDbaseFile]
   *            Perform preparations using the Random-Access method
   * 
   * @param strDbaseFile
   * @param strPart
   * @return 
   * @history
   *  17/sep/2015 ERK Created
   */
  private boolean PrepareRa(String strDbaseFile, String strPart) {
    File fThis;       // File object of [strDbaseFile]
    File fIdxThis;    // database index File object

    try {
      // Validate: existence of file
      if (strDbaseFile == null || strDbaseFile.length() == 0) return false;
      fThis = new File(strDbaseFile);
      if (!fThis.exists()) return false;    // Cannot find xml database file
      
      // We should locate the .index file
      if (strDbaseFile.endsWith(".xml")) strDbaseFile = strDbaseFile.substring(0, strDbaseFile.length() - 4);
      strDbaseFile += ".index";
      fIdxThis = new File(strDbaseFile);
      if (!fIdxThis.exists()) return false;    // Cannot find index file...
      
      // Create my own copy of a random-access reader to the database file
      loc_xrdFile = new XmlIndexRaReader(fThis, crpThis, loc_pdxThis, ProjType.Dbase);
      // Get the list of @File elements inside the database
      this.lstResFile = loc_xrdFile.getPartList();
      // Do we have a header?
      String sHeader = loc_xrdFile.getHeader();
      if (sHeader.isEmpty()) {
        // Indicate that it is empty
        loc_ndxHeader = null;
      } else {
        // Load this obligatory <General> header 
        loc_pdxThis.LoadXml(sHeader);
        loc_ndxHeader =loc_pdxThis.SelectSingleNode(loc_path_General);
      }
      // Return positively
      return true;
    } catch (Exception ex) {
      objErr.DoError("XmlResult/PrepareRa error: ", ex);
      return false;
    }
  }
  
  /**
   * PrepareTg -- Prepare reading [strPart] from database [strDbaseFile]
   *            Perform preparations using the tag-based file-split method
   * 
   * @param strDbaseFile
   * @param strPart
   * @return 
   * @history
   *  15/sep/2015 ERK Created
   */
  private boolean PrepareTg(String strDbaseFile, String strPart) {
    File fThis;       // File object of [strDbaseFile]

    try {
      // Validate: existence of file
      if (strDbaseFile == null || strDbaseFile.length() == 0) return false;
      fThis = new File(strDbaseFile);
      if (!fThis.exists()) return false;
      // There should be a directory [strPart] under the database file
      if (strDbaseFile.endsWith(".xml")) strDbaseFile = strDbaseFile.substring(0, strDbaseFile.length() - 4);
      strDbaseFile += "/" + strPart;
      // If this ends on the CRP text extension, then take that off
      if (strDbaseFile.endsWith(crpThis.getTextExt())) {
        strDbaseFile = strDbaseFile.substring(0, strDbaseFile.length() - crpThis.getTextExt().length());
      }
      fThis = new File(strDbaseFile);
      if (!fThis.exists()) return false;
      
      // Start up the streamer
      loc_xrdTgFile = new XmlIndexTgReader(fThis, crpThis, loc_pdxThis, crpThis.intProjType);
      // Get the list of @File elements inside the database
      this.lstResFile = loc_xrdTgFile.getPartList();
      // Do we have a header?
      String sHeader = loc_xrdTgFile.getHeader();
      if (sHeader.isEmpty()) {
        // Indicate that it is empty
        loc_ndxHeader = null;
      } else {
        // Load this obligatory <General> header 
        loc_pdxThis.LoadXml(sHeader);
        loc_ndxHeader =loc_pdxThis.SelectSingleNode(loc_path_General);
      }
      // Return positively
      return true;
    } catch (Exception ex) {
      objErr.DoError("XmlResult/PrepareTg error: ", ex);
      return false;
    }
  }
  
  /**
   * FirstResult
   *    Load the first <Result> element using an XmlReader and also read the <Global> header
   * 
   * @param ndxResult - Byref returns the <Result> node
   * @param ndxHeader - Byref returns the <General> header 
   * @param strFile   - The file that is to be used
   * @return          - True upon success
   * @history
   *   14/sep/2015 ERK Adapated for "PsdxIndex" in Java
   */
  @Override
  public boolean FirstResult(ByRef<XmlNode> ndxResult, ByRef<XmlNode> ndxHeader, String strFile) {
    try {
      // Initialisations
      ndxResult.argValue = null;
      // Perform the necessary preparations
      if (!Prepare(strFile)) return false;
      // Return the header
      ndxHeader.argValue = this.loc_ndxHeader;

      // Read the first <Result> node 
      String sResult = loc_xrdFile.getFirstLine();
      // TODO: what if this is empty??
      loc_pdxThis.LoadXml(sResult);
      // Set the value of the result that is to be returned
      ndxResult.argValue = loc_pdxThis.SelectSingleNode(loc_path_Result);
      // Validate: do we have a result?
      if (ndxResult.argValue == null) {
        // This should not happen. Check what is the matter
        String sWork = loc_pdxThis.getDoc();
        objErr.debug("XmlResult empty ndxResult.argValue: " + sWork);
      } 
      // Return success
      return true;
    } catch (XPathExpressionException ex) {
      // Warn user
      objErr.DoError("XmlResult/FirstResult Xpath error: ", ex);
      // Return failure
      return false;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlResult/FirstResult Runtime error: " , ex);
      // ex.printStackTrace();
      // Return failure
      return false;
    } catch (SaxonApiException ex) {
      objErr.DoError("XmlResult/FirstResult IO/SAX error: ", ex);
      return false;
    }
  }
  @Override
  public boolean FirstResult(ByRef<XmlNode> ndxResult) {
    XmlNode ndxWork;      // Working node
    String strNext = "";  // Another chunk of <Result>

    try {
      // Validate
      if (loc_pdxThis == null)  return false;
      // More validateion
      if (loc_xrdFile==null) return false;
      // Reading part or whole?
      if (this.loc_strPart.isEmpty()) {
        // Try to read another piece of <Result> xml
        if (! loc_xrdFile.EOF) strNext = loc_xrdFile.getFirstLine();
      } else {
        // We are reading the first part of a part...
        strNext = loc_xrdFile.getFirstLine(loc_strPart);
      }
      // Double check what we got
      if (strNext == null || strNext.length() == 0) {
        ndxResult.argValue = null;
        return true;
      }
      // Proceed by loading the new <Result>
      loc_pdxThis.LoadXml(strNext);
      // Get this result as a node
      ndxWork = loc_pdxThis.SelectSingleNode(loc_path_Result);
      // Validate
      if (ndxWork == null) {
        // This should not happen. Check what is the matter
        String sWork = loc_pdxThis.getDoc();
        objErr.debug("XmlResult empty work: " + sWork);
      } 

      // Return the current <Result> node
      ndxResult.argValue = ndxWork;
      // Return success
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlResult/FirstResult runtime error: ", ex);
      // Return failure
      return false;
    } catch (SaxonApiException | XPathExpressionException ex) {
      objErr.DoError("XmlResult/FirstResult IO/Sax/Xpath error: ", ex);
      // Return failure
      return false;
    }
  }
  
  /**
   * OneResult
   *    Load the sentence with the indicated [sResultId] using an XmlReader 
   * 
   * @param ndxResult
   * @param sResultId
   * @return 
   * @history
   *   14/sep/2015 ERK Adapated for "PsdxIndex" in Java
   */
  @Override
  public boolean OneResult(ByRef<XmlNode> ndxResult, String sResultId) {
    String strNext = "";  // Another chunk of <forest>

    try {
      // Validate
      if (loc_pdxThis == null)  return false;
      // Check for end of stream
      if (IsEnd()) {
        ndxResult.argValue = null;
        return true;
      }
      // More validateion
      if (loc_xrdFile==null) return false;
      // Read this <Result>
      strNext = loc_xrdFile.getOneLine(sResultId);
      // Double check what we got
      if (strNext == null || strNext.length() == 0) {
        ndxResult.argValue = null;
        return true;
      }      
      // Load this line...
      loc_pdxThis.LoadXml(strNext);
      // Find and return the indicated sentence
      ndxResult.argValue = loc_pdxThis.SelectSingleNode(loc_path_Result);
      // Return positively
      return true;
    } catch (Exception ex) {
      // Warn user
      objErr.DoError("XmlResult/OneResult error: ", ex);
      // Return failure
      return false;
    }
  }

  /**
   * GetResultId
   *    Get the ID of the result [ndxThis]
   * 
   * @param ndxResult
   * @param intResultId
   * @return 
   * @history
   *   14/sep/2015 ERK Adapated for "PsdxIndex" in Java
   */
  @Override
  public boolean GetResultId(ByRef<XmlNode> ndxResult, ByRef<Integer> intResultId) {
    String sAttr;   // Attribute value
    
    try {
      // Supply default value to indicate failure
      intResultId.argValue = -1;
      // Check if there is a @ResId attribute
      sAttr = ndxResult.argValue.getAttributeValue(loc_xq_ResId);
      if (sAttr.isEmpty()) return objErr.DoError("<Result> does not have @ResId");
      // Get the @ResId value
      intResultId.argValue = Integer.parseInt(sAttr);
      // Return success
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlResult/GetResultId error: ", ex);
      // Return failure
      return false;
    }
  }

  /**
   * IsEnd -- check if the stream is at its end
   * 
   * @return 
   * @history
   *   14/sep/2015 ERK Adapated for "PsdxIndex" in Java
   */
  @Override
  public boolean IsEnd() {
    try {
      // Validate
      if (loc_xrdFile == null) {
        // How could this possibly be null?
        return true;
      }
      return (loc_xrdFile.EOF);
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlResult/IsEnd error: ",  ex);
      // Return failure
      return false;
    }
  }

  /**
   * NextResult -- Load the next result using an XmlReader 
   * 
   * @param ndxResult
   * @return 
   * @history
   *   14/sep/2015 ERK Adapated for "PsdxIndex" in Java
   */
  @Override
  public boolean NextResult(ByRef<XmlNode> ndxResult) {
    XmlNode ndxWork;      // Working node
    String strNext = "";  // Another chunk of <Result>

    try {
      // Validate
      if (loc_pdxThis == null)  return false;
      // Check for end of stream
      if (IsEnd()) {
        ndxResult.argValue = null;
        return true;
      }
      // More validateion
      if (loc_xrdFile==null) return false;
      // Reading part or whole?
      if (this.loc_strPart.isEmpty()) {
        // Try to read another piece of <Result> xml
        if (! loc_xrdFile.EOF) strNext = loc_xrdFile.getNextLine();
      } else {
        // We are reading the first part of a part...
        strNext = loc_xrdFile.getNextLine(loc_strPart);
      }
      // Check for end-of-file and file closing
      if (loc_xrdFile.EOF) loc_xrdFile = null;
      // Double check what we got
      if (strNext == null || strNext.length() == 0) {
        ndxResult.argValue = null;
        return true;
      }
      // Proceed by loading the new <Result>
      loc_pdxThis.LoadXml(strNext);
      // Get this result as a node
      ndxWork = loc_pdxThis.SelectSingleNode(loc_path_Result);
      // Validate
      if (ndxWork == null) {
        // This should not happen. Check what is the matter
        String sWork = loc_pdxThis.getDoc();
        objErr.debug("XmlResult empty work: " + sWork);
      } 

      // Double check for EOF
      if (loc_pdxThis == null) {
        ndxResult.argValue = null;
        return true;
      }
      // Find the current <Result> node
      ndxResult.argValue = loc_pdxThis.SelectSingleNode(loc_path_Result);
      // Return success
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      objErr.DoError("XmlResult/NextResult runtime error: ", ex);
      // Return failure
      return false;
    } catch (SaxonApiException | XPathExpressionException ex) {
      objErr.DoError("XmlResult/NextResult IO/Sax/Xpath error: ", ex);
      // Return failure
      return false;
    }
  }

  /**
   * CurrentResult -- return the current result
   * 
   * @param ndxResult
   * @return 
   */
  @Override
  public boolean CurrentResult(ByRef<XmlNode> ndxResult) {
    try {
      // Validate
      if (loc_pdxThis == null )  return false;
      if (loc_xrdFile == null || loc_xrdFile.EOF) return false;
      // Find the current <Result> node
      ndxResult.argValue = loc_pdxThis.SelectSingleNode(loc_path_Result);
      // Return success
      return true;
    } catch (Exception ex) {
      objErr.DoError("XmlResult/CurrentResult error: ", ex);
      // Return failure
      return false;
    }
  }

  /**
   * Percentage -- Show where we are in reading
   * 
   * @param intPtc
   * @return 
   * @history
   *   14/sep/2015 ERK Adapated for "PsdxIndex" in Java
   */
  @Override
  public boolean Percentage(ByRef<Integer> intPtc) {
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
      objErr.DoError("XmlResult/NextResult error: ", ex);
      // Return failure
      return false;
    }
  } 
  
  /**
   * GetHeader -- return the <General> header of the result database
   * 
   * @param ndxHeader
   * @return 
   */
  @Override
  public boolean GetHeader(ByRef<XmlNode> ndxHeader) {
    try {
      // Validate
      if (loc_pdxThis == null)  return false;
      // More validateion
      if (loc_xrdFile==null) return false;
      // Return the local header
      ndxHeader.argValue = this.loc_ndxHeader;
      // Return success
      return true;
    } catch (Exception ex) {
      // Warn user
      objErr.DoError("XmlResult/GetHeader error: ", ex);
      // Return failure
      return false;
    }
  }
}
