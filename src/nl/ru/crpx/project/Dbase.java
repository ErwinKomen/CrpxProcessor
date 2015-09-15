/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

package nl.ru.crpx.project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import nl.ru.crpx.search.JobXq;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.tools.FileIO;
import nl.ru.util.ByRef;
import nl.ru.xmltools.XmlDocument;
import nl.ru.xmltools.XmlNode;
import nl.ru.xmltools.XmlResult;
import nl.ru.xmltools.XmlResultPsdxIndex;

/**
 *
 * @author Erwin R. Komen
 */
public class Dbase extends CrpGlobal {
  // ========================================== Constants ======================
  protected static final QName loc_xq_File = new QName("", "", "File");
  protected static final QName loc_xq_ResId = new QName("", "", "ResId");
  protected static final QName loc_xq_SrcDir = new QName("", "", "SrcDir");
  // ================= local variables =========================================
  private String loc_strDbSource;       // Database file name
  private String loc_strDbCurrentFile;  // File name of the current database <entry>
  private String loc_strSrcDir;         // Source directory referred to from database <General> intro
  private List<String> loc_colDbFiles;  // Collection of files that have been read
  private List<DbaseItem> lstDbItems;   // Ordered list of database file names and locations
  private XmlNode loc_ndxDbRes;         //
  private XmlResult oXmlResult;         // Reader for the <CrpResult> database
  private XmlDocument pdxThis;          // Local place for an XmlDocument
  protected Processor objSaxon;         // Local access to the processor
  protected DocumentBuilder objSaxDoc;  // My own document-builder
  private CorpusResearchProject oCrp;   // Link to the CRP being executed
  private ErrHandle oErr;               // Link to error handling object
  
  // ================= Class initialisation ====================================
  public Dbase(CorpusResearchProject oCrp) {
    loc_strDbSource = "";
    loc_strDbCurrentFile = "";
    loc_colDbFiles = new ArrayList<>();
    lstDbItems = new ArrayList<>();
    loc_ndxDbRes = null;
    oXmlResult = null;
    this.oCrp = oCrp;
    this.oErr = oCrp.errHandle;
    // Get the processor
    this.objSaxon = oCrp.getSaxProc();
    // Create a document builder
    this.objSaxDoc = this.objSaxon.newDocumentBuilder();
    pdxThis = new XmlDocument(this.objSaxDoc, this.objSaxon);
  }
  // =========================== GET and SET functions =======================
  // Provide access to the list of files found using DbaseFileScan
  public List<String> getDbFiles() { return this.loc_colDbFiles;}
  // =========================== Other functions =============================
  // <editor-fold desc="DbaseQueryInit">
  // ---------------------------------------------------------------------------------------------------------
  // Name :  DbaseQueryInit
  // Goal :  Initialise database reading for Dbase oriented queries
  //         This function is only intended for projects querying a database
  // History:
  // 23-01-2014  ERK Created for .NET
  // 14-09-2015  ERK Adapted for Java
  // ---------------------------------------------------------------------------------------------------------
  public final boolean DbaseQueryInit(File fDbSource, ByRef<String> strFirstEl, JobXq objJobCaller) {
    // XmlNode ndxThis = null; // Node
    ByRef<XmlNode> ndxHeader = new ByRef(null);   // Copy of the header

    try {
      // Validate
      if (strFirstEl == null) return false;
      // Initialise: tell the caller that this is a database (assuming it is...)
      strFirstEl.argValue = "CrpOview";
      loc_colDbFiles = new ArrayList<>();
      this.lstDbItems = new ArrayList<>();
      // Keep the file name
      loc_strDbSource = fDbSource.getAbsolutePath();
      // Validate
      if (! fDbSource.exists()) return false;
      // Create an XmlResult reader for this file 
      this.oXmlResult = new XmlResultPsdxIndex(oCrp, objJobCaller, oCrp.errHandle);
      // Scan the whole database for availability of files...
      if (! (DbaseFileScan(fDbSource, objJobCaller))) return false;
      
      // Read the header of the file
      if (!oXmlResult.GetHeader(ndxHeader)) return false;
      // Get the source directory from the header
      this.loc_strSrcDir = ndxHeader.argValue.getAttributeValue(loc_xq_SrcDir);
      
      // Initialize the 'current' pointer
      this.loc_ndxDbRes = null;

      return true;
    } catch (RuntimeException ex) {
      // Show there is something wrong
      oErr.DoError("Dbase/DbaseQueryInit error: ", ex);
      // Return failure
      return false;
    } 
  }
// </editor-fold>
// <editor-fold desc="DbaseFileScan">
  // ---------------------------------------------------------------------------------------------------------
  // Name :  DbaseFileScan
  // Goal :  Scan the whole database for available files..
  // NOTE :  Is this still necessary, since the database is already indexed by the XmlIndexReader??
  // History:
  // 24-09-2014  ERK Created for .NET
  // 14/sep/2015 ERK Implemented afresh for Java
  // ---------------------------------------------------------------------------------------------------------
  private boolean DbaseFileScan(File fDbSource, JobXq objJobCaller) {
    ByRef<XmlNode> ndxResult = new ByRef(null);
    ByRef<XmlNode> ndxHeader = new ByRef(null);
    ByRef<Integer> intPtc = new ByRef(0);
    String strFile = "";                          // File name
    int iResId = 0;                               // Current record's id
    int iLastItem = -1;                           // Last index of [lstDbItem]

    try {
      // Clear storage
      loc_colDbFiles.clear(); lstDbItems.clear();
      // Prepare the database file
      if (!oXmlResult.Prepare(fDbSource.getAbsolutePath())) return false;
      // Get the list of @File elements
      loc_colDbFiles = oXmlResult.getResultFileList();
      // Walk the list of files and put information into [lstDbItems
      for (int i=0;i<loc_colDbFiles.size(); i++) {
        // TODO: informatie toevoegen aan lstDbItems
        // lstDbItems.add( new DbaseItem(strFile, iResId));
      }
      
      /*
      while (!oXmlResult.IsEnd() && ndxResult.argValue != null) {
        // Show that we are 'indexing' the database file
        if (!oXmlResult.Percentage(intPtc)) return false;
        // Get the current record's id
        iResId = Integer.parseInt(ndxResult.argValue.getAttributeValue(loc_xq_ResId));
        // Set the status in the calling job
        objJobCaller.setJobPtc(intPtc.argValue);
        // Get the @File attribute
        strFile = ndxResult.argValue.getAttributeValue(loc_xq_File);
        // Add the short representation of the filename to a list
        strFile = FileIO.getFileNameWithoutExtension(strFile);
        if (!loc_colDbFiles.contains(strFile)) {
          // Add it to the list of files
          loc_colDbFiles.add(strFile);
          lstDbItems.add( new DbaseItem(strFile, iResId));
          iLastItem = lstDbItems.size()-1;
        } else {
          // Set the last item
          if (iLastItem>=0) {
            // Adjust the value of the last occurrence of filename [strFile] in the database
            lstDbItems.get(iLastItem).iLastId = iResId;
          } else {
            // This should not occur
            oErr.DoError("Dbase/DbaseFileScan: files do not occur consecutively in database");
          }
        }
        // Read the next element
        if (!oXmlResult.NextResult(ndxResult)) return false;
      }
      */
      
      // Return success
      return true;
    } catch (RuntimeException ex) {
      // Show there is something wrong
      oErr.DoError("Dbase/DbaseFileScan error: ", ex);
      // Return failure
      return false;
    }
  }
// </editor-fold>
// <editor-fold desc="getDbFileFirst">
  /**
   * getDbFileFirst
   *    Get the first 'ResId' number the File @sFile occurs in the database
   * 
   * @param sFile
   * @return 
   */
  /*
  public int getDbFileFirst(String sFile) {
    try {
      // Validate
      if (lstDbItems == null || lstDbItems.size() == 0) return -1;
      // Find the item
      for (int i =0;i< lstDbItems.size()-1;i++) {
        // Get this item
        DbaseItem oThis = lstDbItems.get(i);
        if (oThis.Name.equals(sFile)) {
          // Found it!
          return oThis.iFirstId;
        }
      }
      // Return failure
      return -1;
    } catch (Exception ex) {
      // Show there is something wrong
      oErr.DoError("Dbase/getDbFileFirst error: ", ex);
      // Return failure
      return -1;
    }
  } */
// </editor-fold>
// <editor-fold desc="getDbFileFirst">
  /**
   * getDbFileLast
   *    Get the last 'ResId' number the File @sFile occurs in the database
   * 
   * @param sFile
   * @return 
   */
  /*
  public int getDbFileLast(String sFile) {
    try {
      // Validate
      if (lstDbItems == null || lstDbItems.size() == 0) return -1;
      // Find the item
      for (int i =0;i< lstDbItems.size()-1;i++) {
        // Get this item
        DbaseItem oThis = lstDbItems.get(i);
        if (oThis.Name.equals(sFile)) {
          // Found it!
          return oThis.iLastId;
        }
      }
      // Return failure
      return -1;
    } catch (Exception ex) {
      // Show there is something wrong
      oErr.DoError("Dbase/getDbFileLast error: ", ex);
      // Return failure
      return -1;
    }
  } */
// </editor-fold>
// <editor-fold desc="DbaseQueryCurrent">
  // ---------------------------------------------------------------------------------------------------------
  // Name :  DbaseQueryCurrent
  // Goal :  Return the current <Result> node
  // History:
  // 23-01-2014  ERK Created for .NET
  // 1/may/2015  ERK Adapted for Java
  // ---------------------------------------------------------------------------------------------------------
  public boolean DbaseQueryCurrent(ByRef<XmlNode> ndxThis) {
    try {
      // Validate
      if (this.oXmlResult == null) return false;
      // Get current result
      ndxThis.argValue = this.loc_ndxDbRes;
      // Return success
      return true;
    } catch (RuntimeException ex) {
      // Show there is something wrong
      oErr.DoError("modDbase/DbaseQueryCurrent error: ", ex);
      // Return failure
      return false;
    }
  }
// </editor-fold>
// <editor-fold desc="DbaseQueryRead">
  /**
   * DbaseQueryRead
   *    Given database [pdxDb], read the next node, provided @forestId and @File match
   *         This function is only used by modMain/ExecuteQueriesXqueryFast()
   *         And it is only intended for projects querying a database
   * 
   * @param ndxThis
   * @return 
   * @history
   *  23-01-2014  ERK Created for .NET
   *  14-09-2015  ERK Adapted for Java
   */
  public boolean DbaseQueryRead(ByRef<XmlNode> ndxThis) {
    try {
      // Validate
      if (this.oXmlResult == null) return false;
      // Check if something has already been read
      if (this.loc_ndxDbRes == null) {
        // Nothing has been read, so get the first record
        if (!this.oXmlResult.FirstResult(ndxThis)) return false;
      } else {
        // Read the next one
        if (!this.oXmlResult.NextResult(ndxThis)) return false;
      }
      // Set the current record
      this.loc_ndxDbRes = ndxThis.argValue;
      // Return success
      return true;
    } catch (RuntimeException ex) {
      // Show there is something wrong
      oErr.DoError("modDbase/DbaseQueryRead error: ", ex);
      // Return failure
      return false;
    }
  }
// </editor-fold>
// <editor-fold desc="DbaseQuerySeek">
  // ---------------------------------------------------------------------------------------------------------
  // Name :  DbaseQuerySeek
  // Goal :  Seek the first entry of [strFileName] in the database and return the node
  // History:
  // 23-01-2014  ERK Created for .NET
  // 1/may/2015  ERK Adapted for Java
  // ---------------------------------------------------------------------------------------------------------
  public boolean DbaseQuerySeek(ByRef<XmlNode> ndxThis, String strFileName) {
    try {
      // Validate
      if (this.oXmlResult == null) return false;
      // Check if this file has been read
      
      
      // Get current result
      ndxThis.argValue = this.loc_ndxDbRes;
      // Return success
      return true;
    } catch (RuntimeException ex) {
      // Show there is something wrong
      oErr.DoError("modDbase/DbaseQuerySeek error: ", ex);
      // Return failure
      return false;
    }
  }
// </editor-fold>

}
// ============= Define one database item =================================
class DbaseItem {
  public String Name;   // Name of this file
  public int iFirstId;  // First ResId where this file occurs
  public int iLastId;   // Last ResId where this file occurs
  public DbaseItem(String sName, int iResId) {
    this.Name = sName; this.iFirstId = iResId; this.iLastId = iResId;
  }
  public void setLastId(int iResId) {this.iLastId = iResId;}
  public String getName() { return this.Name; }
  public int getFirstId() { return this.iFirstId; }
  public int getLastId()  { return this.iLastId;}
}