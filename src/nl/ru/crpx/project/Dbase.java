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
// import static nl.ru.crpx.project.CrpGlobal.DoError;
// import static nl.ru.crpx.project.CrpGlobal.Status;
import nl.ru.util.ByRef;

/**
 *
 * @author Erwin R. Komen
 */
public class Dbase extends CrpGlobal {
  // ================= Class initialisation ====================================
  public Dbase() {
    
  }
  // ================= local variables =========================================
  private String loc_strDbSource;       // Database file name
  private String loc_strDbCurrentFile;  // File name of the current database <entry>
  private List<String> loc_colDbFiles;  // Collection of files that have been read
  // ---------------------------------------------------------------------------------------------------------
  // Name :  DbaseQueryInit
  // Goal :  Initialise database reading for Dbase oriented queries
  //         This function is only used by modMain/ExecuteQueriesXqueryFast()
  //         And it is only intended for projects querying a database
  // History:
  // 23-01-2014  ERK Created
  // ---------------------------------------------------------------------------------------------------------
  public final boolean DbaseQueryInit(File fDbSource, ByRef<String> strFirstEl) {
    // XmlNode ndxThis = null; // Node

    try {
      // Initialise
      strFirstEl.argValue = "";
      loc_colDbFiles = new ArrayList<>();
      // Keep the file name
      loc_strDbSource = fDbSource.getAbsolutePath();
      // Validate
      if (! fDbSource.exists()) return false;
      // Scan the whole database for availability of files...
      if (! (DbaseFileScan(fDbSource))) {
        return false;
      }

      /* ============= TODO: implement and process ==============
      // Start reading the file
      loc_rdDb = new IO.StreamReader(strFile);
      rdDbQ = XmlReader.Create(loc_rdDb);
      loc_ndxDbRes = null;
      loc_strDbCurrentFile = "";
      // Move to content
      rdDbQ.MoveToContent();
      //' Read first element
      //rdDbQ.Read()
      //' Check name ...
      //If (rdDbQ.LocalName = "xml") Then
      //  ' Skip content and read on...
      //  rdDbQ.Skip() : rdDbQ.Read()
      //End If
      // Check the name/local name
      strFirstEl.argValue = rdDbQ.LocalName;
      // Read on until SrcDir
      rdDbQ.ReadToFollowing("SrcDir");
      if (! rdDbQ.EOF) {
        loc_pdxDb = null;
        loc_pdxDb = new XmlDocument();
        // Read the whole <SrcDir> into memory
        loc_pdxDb.LoadXml(rdDbQ.ReadOuterXml);
        // Get hold of the value
        ndxThis = loc_pdxDb.SelectSingleNode("./descendant::SrcDir");
        // Got something?
        if (ndxThis != null) {
          // Check if the new directory exists or not
          if ((IO.File.Exists(ndxThis.InnerText)) || (! (IO.File.Exists(loc_strSrcDir)))) {
            loc_strSrcDir = ndxThis.InnerText;
          } else {
            loc_strSrcDir = "";
            // Log this with a warning
            frmMain.LogOutput("WARNING: the [SrcDir] referred to in the database does not exist: [" + ndxThis.InnerText + "]");
          }
        }
      } */
      return true;
    } catch (RuntimeException ex) {
      // Show there is something wrong
      DoError("Dbase/DbaseQueryInit error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
  // ---------------------------------------------------------------------------------------------------------
  // Name :  DbaseFileScan
  // Goal :  Scan the whole database for available files..
  // History:
  // 24-09-2014  ERK Created for .NET
  // 1/may/2015  ERK Adapted for Java
  // ---------------------------------------------------------------------------------------------------------
  private final boolean DbaseFileScan(File fDbSource) {
    String strRes = ""; // Temporary storage of result
    String strFile = ""; // File name
    boolean bFound = false; // Are we there?
    // IO.StreamReader rdThis = null; // Streamreader basis
    long lngLoc = 0; // Where we are
    long lngSize = 0; // Length of file
    int intPtc = 0; // Percentage

    try {
      // Start reading the file
       /* ================ START IMPLEMENTING ===========================
      rdThis = new IO.StreamReader(strDbSource);
      lngSize = rdThis.BaseStream.getLength();
      rdDbQ = XmlReader.Create(rdThis);
      loc_ndxDbRes = null;
      // Start looping
      do {
        // Show we are rewinding
        Status("Indexing database " + intPtc + "%", intPtc);
        // WHere are we?
        lngLoc = rdThis.BaseStream.Position;
        intPtc = (int)(100 * (lngLoc / (double)lngSize));
        // Read until the next result node
        rdDbQ.ReadToFollowing("Result");
        if (! rdDbQ.EOF) {
          strFile = GetFileNameShort(rdDbQ.GetAttribute("File"));
          loc_colDbFiles.AddUnique(strFile, lngLoc);
        }
      } while (! rdDbQ.EOF);
      rdDbQ.Close();
      rdThis.Close();
      ============= */
      // Return failure
      return true;
    } catch (RuntimeException ex) {
      // Show there is something wrong
      DoError("modDbase/DbaseFileScan error: " + ex.getMessage() + "\r\n" );
      // Return failure
      return false;
    }
  }
}
