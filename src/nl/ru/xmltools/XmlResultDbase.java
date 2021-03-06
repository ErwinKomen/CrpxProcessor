/**
 * Copyright (c) 2016 TSG-HL, Radboud University Nijmegen
 * All rights reserved.
 *
 * This software has been developed at the "Technical Support Group" 
 *   of the Humanities Lab, Radboud University Nijmegen
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 * 
 * @author Erwin R. Komen
 */
package nl.ru.xmltools;

import java.io.File;
import java.util.List;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.project.DbStore;
import nl.ru.crpx.search.JobXq;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.util.ByRef;
import nl.ru.util.FileUtil;
import nl.ru.util.json.JSONArray;
import nl.ru.util.json.JSONObject;

/**
 * XmlResultDbase - use an SQLite database created with nl.ru.crpx.project.DbStore
 * 
 * @author Erwin R. Komen
 */
public class XmlResultDbase extends XmlResult {
  // ============ Local variables ==============================================
  private DbStore loc_oStore;
  // ============ Call the standard class initializer ==========================
  public XmlResultDbase(CorpusResearchProject oCrp, JobXq oJob, ErrHandle oErr) {
    super(oCrp,oJob,oErr);
    // Initialize database
    this.loc_oStore = null;
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
      // Check if a database is loaded
      if (this.loc_oStore == null) return null;
      // Get the HEADER table from the database
      oHeader = this.loc_oStore.getGeneral();
      // Return the object
      return oHeader;
    } catch (Exception ex) {
      objErr.DoError("XmlResultPsdxIndex/headerInfo error: ", ex);
      return null;
    }
  }
  
  /**
   * featureList --
   *    Create a list of <Feature> names for this result database file
   * 
   * @return 
   */
  public List<String> featureList() {
    List<String> lBack; // Initialize the list we want to return
    
    try {
      lBack = this.loc_oStore.getFeatureList();
      // Return the list that has been created
      return lBack;
    } catch (Exception ex) {
      objErr.DoError("XmlResultPsdxIndex/featureList error: ", ex);
      return null;
    }
  }
  
  /**
   * Size -- return the number of results
   * @return 
   */
  @Override
  public int Size() {
    return this.loc_size;
  }
  
  public boolean Filter(JSONObject oFilter) {
    try {
      // Pass on the FILTER request to DbStore
      if (!this.loc_oStore.filter(oFilter)) return false;
      this.loc_size = this.loc_oStore.getSize();
      
      // Return positively
      return true;
    } catch (Exception ex) {
      objErr.DoError("XmlResult/Filter error: ", ex);
      return false;
    }
  }
  
  /**
   * Sort -- Sort the current database according to the [sSortOrder] parameter
   *    This parameter is, in principle, the name of one column.
   *    If the name starts with a minus '-' sign, then the order is descending
   *      otherwise the order is assumed to be ascending
   * 
   * @param sSortOrder
   * @return 
   */
  @Override
  public boolean Sort(String sSortOrder) {
    boolean bAscending = true;  // Sort order is ascending
    boolean bIsFeature = false; // Sort column is a feature
    
    try {
      // Review the sort-order string
      if (sSortOrder.startsWith("-")) {
        bAscending = false;
        // DO away with the leading minus sign
        sSortOrder = sSortOrder.substring(1);
      }
      /* ===== Obsolete =========
      // CHeck if this is a feature or not
      if (sSortOrder.startsWith("ft:")) {
        bIsFeature = true;
        sSortOrder = sSortOrder.substring(3);
      }
      ============================ */
      // Pass on the SORT request to DbStore
      if (!this.loc_oStore.sort(sSortOrder, bAscending)) return false;
      
      // Return positively
      return true;
    } catch (Exception ex) {
      objErr.DoError("XmlResult/Sort error: ", ex);
      return false;
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
    DbStore oStore = new DbStore(objErr, false);

    try {
      // Validate
      if (!strDbaseFile.endsWith(".xml")) strDbaseFile = strDbaseFile + ".xml";
      String strDbaseDb = strDbaseFile.replace(".xml", ".db");
      String strDbaseGz = strDbaseDb + ".gz";
      String strDbaseIdx = strDbaseFile.replace(".xml", ".index");
      // Make sure files are okay on this system
      File fDbase = new File(strDbaseFile);
      if (fDbase.getAbsolutePath().startsWith("C:\\")) {
        strDbaseFile = "D:" + strDbaseFile;
        strDbaseGz = "D:" + strDbaseGz;
        strDbaseDb = "D:" + strDbaseDb;
        strDbaseIdx = "D:" + strDbaseIdx;
      }
      // Check which one to take
      fDbase = new File(strDbaseDb);
      File fDbaseGz = new File(strDbaseGz);
      File fDbaseXml = new File(strDbaseFile);
      File fDbaseIdx = new File(strDbaseIdx);
      // Find out which one exists and was modified last
      if (!fDbaseXml.exists()) {
        // Check if we need to unpack it
        String strDbaseXmlGz = strDbaseFile.replace(".xml", ".xml.gz");
        File fDbaseXmlGz = new File(strDbaseXmlGz);
        if (fDbaseXmlGz.exists()) {
          // Unpack it
          FileUtil.decompressGzipFile(strDbaseXmlGz, strDbaseFile);
        } else {
          // XML file does not exist
          objErr.DoError("Prepare: cannot fine .xml version of database, nor .xml.gz", XmlResultDbase.class);
          return false;
        }
      }    
      if (!fDbaseIdx.exists()) {
        objErr.DoError("Prepare: cannot find .index file", XmlResultDbase.class);
        return false;
      }
      if (!fDbase.exists()) {
        // There is no .db file -- but is there a .gz file?
        if (fDbaseGz.exists()) {
          // There is a .db.gz file: check date: it may not be younger than the .index file
          if (fDbaseGz.lastModified() < fDbaseIdx.lastModified()) {
            // The .db.gz file is STALE -- delete it, and create a new .db file
            objErr.debug("Prepare: create new db [3]");
            fDbaseGz.delete();
            oStore.xmlToDbNew(strDbaseFile);
          }
        } else {
          // There is no .db.gz file, so create it
          objErr.debug("Prepare: create new db [4]");
          oStore.xmlToDbNew(strDbaseFile);
        }
        // There now is a .db.gz file: unpack it
        objErr.debug("Prepare: unpacking gz to db [1]");
        FileUtil.decompressGzipFile(strDbaseGz, strDbaseDb);
      } else {
        // Is the .db newer or older than the .xml file?
        if (fDbase.lastModified() < fDbaseXml.lastModified()) {
          // The .db file is stale --> create a new one
          objErr.debug("Prepare: unpacking gz to db [2]");
          oStore.xmlToDbNew(strDbaseFile);
          FileUtil.decompressGzipFile(strDbaseGz, strDbaseDb);
        }
      }
      // There now is a good .db file: open it into my local DbStore copy
      this.loc_oStore = new DbStore(objErr, false);
      this.loc_oStore.openDb(strDbaseDb);
      this.loc_size = this.loc_oStore.getSize();
      
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
      return true;
    } catch (Exception ex) {
      objErr.DoError("XmlResult/Prepare error: ", ex);
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

      return true;
    } catch (Exception ex) {
      objErr.DoError("XmlResult/FirstResult error: ", ex);
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

      
      // Return success
      return true;
    } catch (Exception ex) {
      // Warn user
      objErr.DoError("XmlResult/FirstResult error: ", ex);
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
   * getResults
   *    Get an array of results to the requester
   * 
   * @param arResult
   * @param iStart
   * @param iCount
   * @return 
   */
  public boolean getResults(ByRef<JSONArray> arResult, int iStart, int iCount) {
    try {
      arResult.argValue = this.loc_oStore.getResults(iStart, iCount);
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

      
      return false;
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

      
      // Return success
      return true;
    } catch (Exception ex) {
      // Warn user
      objErr.DoError("XmlResult/NextResult error: ", ex);
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
 
      
      // Return success
      return true;
    } catch (Exception ex) {
      // Warn user
      objErr.DoError("XmlResult/GetHeader error: ", ex);
      // Return failure
      return false;
    }
  }
  
  public void close() {
    // Try close database connection
    if (this.loc_oStore != null) {
      this.loc_oStore.closeWrite();
    }
  }
  
}
