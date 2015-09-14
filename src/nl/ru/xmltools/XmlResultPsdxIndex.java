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
 * XmlResultPsdxIndex - use the indexed reader to go through result database
 * 
 * @author Erwin R. Komen
 */
public class XmlResultPsdxIndex extends XmlResult {
  // ============ Local variables ==============================================
  private XmlIndexReader loc_xrdFile;   // Indexed reader for xml files
  // ============ Call the standard class initializer ==========================
  public XmlResultPsdxIndex(CorpusResearchProject oCrp, JobXq oJob, ErrHandle oErr) {
    super(oCrp,oJob,oErr);
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
    XmlNode ndxWork;  // Working node
    File fThis;       // File object of [strFile]

    try {
      // Validate: existence of file
      if (strFile == null || strFile.length() == 0) return false;
      fThis = new File(strFile);
      if (!fThis.exists()) return false;
      // Initialisations
      ndxResult.argValue = null;
      ndxHeader.argValue = null;
      // N.B: do *not* use context arrays
      // Start up the streamer
      loc_xrdFile = new XmlIndexReader(fThis, crpThis, loc_pdxThis, CorpusResearchProject.ProjType.Dbase);
      // Do we have a header?
      String sHeader = loc_xrdFile.getHeader();
      if (sHeader.isEmpty()) {
        // Indicate that it is empty
        ndxHeader.argValue = null;
      } else {
        // Load this obligatory <General> header 
        loc_pdxThis.LoadXml(sHeader);

        ndxWork = loc_pdxThis.SelectSingleNode(loc_path_General);
        ndxHeader.argValue =ndxWork;
        // Also put the header in the class variable
        this.loc_ndxHeader = ndxWork;
      }
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
    } catch (SaxonApiException | IOException ex) {
      objErr.DoError("XmlResult/FirstResult IO/SAX error: ", ex);
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
      // Try to read another piece of <Result> xml
      if (! loc_xrdFile.EOF) strNext = loc_xrdFile.getNextLine();
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
  @Override
  public boolean FirstResult(ByRef<XmlNode> ndxResult) {
    XmlNode ndxWork;      // Working node
    String strNext = "";  // Another chunk of <Result>

    try {
      // Validate
      if (loc_pdxThis == null)  return false;
      // More validateion
      if (loc_xrdFile==null) return false;
      // Try to read another piece of <Result> xml
      if (! loc_xrdFile.EOF) strNext = loc_xrdFile.getFirstLine();
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
