/**
 * Copyright (c) 2016 CLARIN-NL.
 * All rights reserved.
 *
 * This software has been developed at the Technical Service Group
 *   of the Humanities Lab at the "Radboud University Nijmegen"
 * The original development has been for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 * 
 * @author Erwin R. Komen
 */
package nl.ru.crpx.project;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.ru.crpx.dataobject.DataObject;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.util.ByRef;
import nl.ru.util.FileUtil;
import nl.ru.util.json.JSONArray;
import nl.ru.util.json.JSONObject;
import nl.ru.xmltools.XmlNode;
import nl.ru.xmltools.XmlResultPsdxIndex;

/**
 *
 * @author Erwin R. Komen
 */
public class DbStore {
  // =============== Local class stuff =================================
  private ErrHandle errHandle;            // Local copy of error handler
  private Connection conThis = null;      // JDBC connection
  private String loc_sDbName = "";
  private String loc_sDbXmlFile = "";
  private String loc_sDbSqlFile = "";
  private int loc_iResId = 0;             // ResId for 
  private int loc_iFeatIdx = 0;           // Unique ID for each Feature
  private Statement loc_stmt = null;
  // =============== Local constants ===================================
// <editor-fold desc="Local constants">
  private final String loc_sqlCreateGeneral = 
          "CREATE TABLE GENERAL " +
          "(ID INT PRIMARY KEY  NOT NULL,"+
          " PROJECTNAME    TEXT NOT NULL,"+
          " CREATED        TEXT NOT NULL,"+
          " DSTDIR         TEXT NOT NULL,"+
          " SRCDIR         TEXT NOT NULL,"+
          " LANGUAGE       CHAR(20),"+
          " PART           TEXT NOT NULL,"+
          " QC             INT  NOT NULL,"+
          " NOTES          TEXT NOT NULL,"+
          " ANALYSIS       TEXT NOT NULL)";
  private final String loc_sqlInsertGeneral = 
          "INSERT INTO GENERAL (ID, PROJECTNAME, CREATED, DSTDIR, SRCDIR, "+
          "LANGUAGE, PART, QC, NOTES, ANALYSIS) ";
  private final String loc_sqlCreateFeature = 
          "CREATE TABLE FEATURE " +
          "(ID INT PRIMARY KEY  NOT NULL,"+
          " RESID          INT  NOT NULL,"+
          " NAME           TEXT NOT NULL,"+
          " VALUE          TEXT NOT NULL)";
  private final String loc_sqlInsertFeature = 
          "INSERT INTO FEATURE (ID, RESID, NAME, VALUE) ";
  private final String loc_sqlCreateFeatName = 
          "CREATE TABLE FEATNAME " +
          "(ID INT PRIMARY KEY  NOT NULL,"+
          " NAME           TEXT NOT NULL)";
  private final String loc_sqlInsertFeatName = 
          "INSERT INTO FEATNAME (ID, NAME) ";
  private final String loc_sqlCreateResult = 
          "CREATE TABLE RESULT " +
          "(RESID INT PRIMARY KEY  NOT NULL,"+
          " FILE           TEXT NOT NULL,"+
          " TEXTID         CHAR(100),"+
          " SEARCH         CHAR(100),"+
          " CAT            CHAR(100),"+
          " LOCS           CHAR(200),"+
          " LOCW           CHAR(200),"+
          " NOTES          TEXT NOT NULL,"+
          " SUBTYPE        CHAR(200),"+
          " TEXT           TEXT NOT NULL,"+
          " PSD            TEXT NOT NULL,"+
          " PDE            TEXT NOT NULL)";
  private final String loc_sqlInsertResult = 
          "INSERT INTO RESULT (RESID, FILE, TEXTID, SEARCH, CAT, "+
          "LOCS, LOCW, NOTES, SUBTYPE, TEXT, PSD, PDE) ";
  private PreparedStatement loc_psInsertResult = null;
  private PreparedStatement loc_psInsertFeature = null;
// </editor-fold>  
  // =============== Class initializer =================================
  public DbStore(ErrHandle objErr) {
    this.errHandle = objErr;    // Take over the error object
    try {
      // Cause the class to be initialized
      Class.forName("org.sqlite.JDBC");
    } catch (Exception ex) {
      // Provide error message
      errHandle.DoError("DbStore/DbStore error: ", ex, DbStore.class);
    }
  }
  
  // =============== Public methods ====================================
  
  /**
   * openDb -- Open an SQLite .db file for reading
   * 
   * @param sDbFile
   * @return 
   */
  public boolean openDb(String sDbFile) {
    try {
      // Validate
      loc_sDbSqlFile = sDbFile;
      File fDbFile = new File(sDbFile);
      if (!fDbFile.exists()) return false;
      // Try make connection
      conThis = DriverManager.getConnection("jdbc:sqlite:" + sDbFile);
      // Check if it is read-only...
      if (conThis.isReadOnly()) return false;
      // Return positively 
      return true;
    } catch (Exception ex) {
      // Provide error message
      errHandle.DoError("DbStore/openDb error: ", ex, DbStore.class);
      return false;
    }
  }
  
  /**
   * getGeneral -- Read the GENERAL table into a JSONObject
   * @return 
   */
  public JSONObject getGeneral() {
    JSONObject oGeneral = new JSONObject();
    
    try {
      // Validate
      if (conThis == null) return null;
      // Access the general table
      conThis.setAutoCommit(false);
      Statement stmt = null;
      stmt = conThis.createStatement();
      ResultSet resThis = stmt.executeQuery("SELECT * FROM GENERAL;");
      if (resThis.next()) {
        oGeneral.put("ProjectName", resThis.getString("PROJECTNAME"));
        oGeneral.put("Created", resThis.getString("CREATED"));
        oGeneral.put("DstDir", resThis.getString("DSTDIR"));
        oGeneral.put("SrcDir", resThis.getString("SRCDIR"));
        oGeneral.put("Language", resThis.getString("LANGUAGE"));
        oGeneral.put("Part", resThis.getString("PART"));
        oGeneral.put("QC", resThis.getInt("QC"));
        oGeneral.put("Notes", resThis.getString("NOTES"));
        oGeneral.put("Analysis", resThis.getString("ANALYSIS"));
      }   
      // Return the result
      return oGeneral;
    } catch (Exception ex) {
      // Provide error message
      errHandle.DoError("DbStore/getGeneral error: ", ex, DbStore.class);
      return null;
    }
  }
  
  /**
   * geteatureList -- Create a list of the names of the features
   * 
   * @return 
   */
  public List<String> getFeatureList() {
    List<String> lBack = new ArrayList<>(); // Initialize the list we want to return

    try {
      // Validate
      if (conThis == null) return null;
      // Access the general table
      conThis.setAutoCommit(false);
      Statement stmt = null;
      stmt = conThis.createStatement();
      ResultSet resThis = stmt.executeQuery("SELECT * FROM GENERAL;");
      if (resThis.next()) {
        String sAnalysis = resThis.getString("ANALYSIS");
        String[] arAnalysis = sAnalysis.split(";");
        for (int i=0;i<arAnalysis.length;i++) {
          lBack.add(arAnalysis[i]);
        }
      }   
      // Return the list
      return lBack;
    } catch (Exception ex) {
      // Provide error message
      errHandle.DoError("DbStore/getFeatureList error: ", ex, DbStore.class);
      return null;
    }
  }
  
  
  /**
   * xmlToDb
   *    Complete conversion of a .xml Result Database into an SQLite .db.gz
   *    This is a linear method that is not too fast ...
   * 
   * @param sFileName
   * @return 
   */
  public boolean xmlToDbNew(String sFileName) {
    try {
      // Create a writer
      if (!createWrite(sFileName)) return false;
      
      // Process the header
      CorpusResearchProject oCrpx = new CorpusResearchProject(true);
      XmlResultPsdxIndex oDbIndex = new XmlResultPsdxIndex(oCrpx, null, errHandle);
      if (!oDbIndex.Prepare(sFileName)) { errHandle.DoError("DbStore/xmlToDb: cannot prepare database"); return false;}
      
      // Get the General part
      JSONObject oHdr = oDbIndex.headerInfo();   
      
      // Add header to the database
      if (!addGeneral(oHdr)) return false;
      
      // Prepare making a list of feature names
      List<String> lFeatName = new ArrayList<>();
      String sAnalysis = oHdr.getString("Analysis");
      boolean bDoFeatNames = true;
      
      // process the results one-by-one
      ByRef<XmlNode> ndxResult = new ByRef(null);
      int iFeatIdx = 1;
      if (oDbIndex.FirstResult(ndxResult)) {
        // Walk through them
        int iCheck = 1;
        while (ndxResult.argValue != null) {
          // Get the values of this result
          XmlNode ndxThis = ndxResult.argValue;
          JSONObject oResult = new JSONObject();
          // Get the index
          int iResId = Integer.parseInt(ndxThis.getAttributeValue("ResId"));
          // Double check
          if (iCheck != iResId) { errHandle.DoError("DbStore/xmlToDb: index is wrong at " + iCheck); return false; }
          // Get all the other relevant values
          oResult.put("File", ndxThis.getAttributeValue("File"));
          oResult.put("TextId", ndxThis.getAttributeValue("TextId"));
          oResult.put("Search", ndxThis.getAttributeValue("Search"));
          oResult.put("Cat", ndxThis.getAttributeValue("Cat"));
          oResult.put("Locs", ndxThis.getAttributeValue("forestId"));
          oResult.put("Locw", ndxThis.getAttributeValue("eTreeId"));
          oResult.put("Notes", ndxThis.getAttributeValue("Notes"));
          oResult.put("SubType", ndxThis.getAttributeValue("Period"));
          oResult.put("ResId", Integer.parseInt(ndxThis.getAttributeValue("ResId")));
          // Determine the features for this result
          List<XmlNode> lFeatValue = ndxThis.SelectNodes("./child::Feature");
          JSONArray arFeature = new JSONArray();
          for (int j=0;j<lFeatValue.size();j++) {
            // Add this feature
            JSONObject oFeat = new JSONObject();
            // Add this feature
            String sFeatName = lFeatValue.get(j).getAttributeValue("Name");
            oFeat.put("Name", sFeatName);
            oFeat.put("Value", lFeatValue.get(j).getAttributeValue("Value"));
            arFeature.put(oFeat);
            // Check if feature names need to be adapted
            if (bDoFeatNames) lFeatName.add(sFeatName);
          }
          oResult.put("Features", arFeature);
          
          // Switch off feature-name extraction after the first go
          bDoFeatNames = false;
          
          // Process this Result
          if (!addResult(oResult)) return false;
          
          
          // Get the next result
          oDbIndex.NextResult(ndxResult);
          iCheck += 1;
        }
      }
      
      // CLose the database result index
      oDbIndex.close();
      oDbIndex = null;
                  
      // Add the feature names
      if (!addFeatNames(lFeatName, sAnalysis)) return false;

      // Commit all changes
      conThis.commit();
      
      // Close the database
      if (!closeWrite()) return false;
      
      // Return success
      return true;
    } catch (Exception ex) {
      errHandle.DoError("DbStore/xmlToDbNew error: ", ex, DbStore.class);
      return false;
    }
  }
    
  /**
   * createWrite
   *    Open an SQLite database for writing
   * 
   * @param sFileName
   * @return 
   */
  public boolean createWrite(String sFileName) {
    try {
      // NOTE: do not check existence of the database -- we are creating...
      File fThis = new File(sFileName);
      // Make database name(s) available for this class instance
      String sDbFile = sFileName.replace(".xml", ".db");
      String sDbName = fThis.getName().replace(".xml", "");
      // Make them available:
      this.loc_sDbName = sDbName;
      this.loc_sDbXmlFile = sFileName;
      this.loc_sDbSqlFile = sDbFile;
      // REmove if exists
      File fDb = new File(sDbFile);
      if (fDb.exists()) { 
        boolean bDbIsDel = fDb.delete(); 
        if (!bDbIsDel) { int iIsNotDel = 1; }
      }
      // Also remove a .gz version if it exists
      fDb = new File(sDbFile + ".gz");
      if (fDb.exists()) {
        boolean bDbGzIsDel = fDb.delete();
        if (!bDbGzIsDel) { int iIsNotDel = 1; }
      }
      // Try make connection
      conThis = DriverManager.getConnection("jdbc:sqlite:" + sDbFile);
      
      // Switch off auto-commit
      conThis.setAutoCommit(false);
      
      // Getting here means that a database *has* been created
      loc_stmt = conThis.createStatement();
      // Create a general table
      loc_stmt.executeUpdate(loc_sqlCreateGeneral);
      // Create a table for the Features (linked to Result)
      loc_stmt.executeUpdate(loc_sqlCreateFeature);
      // Create a table for the Feature Names only (not linked)
      loc_stmt.executeUpdate(loc_sqlCreateFeatName);
      // Create a table for the Results
      loc_stmt.executeUpdate(loc_sqlCreateResult);
      
      // Commit these steps
      conThis.commit();
      
      // Prepare some statements
      this.loc_psInsertResult = conThis.prepareStatement("INSERT INTO RESULT (RESID, FILE, TEXTID, SEARCH, CAT, "+
          "LOCS, LOCW, NOTES, SUBTYPE, TEXT, PSD, PDE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
      this.loc_psInsertFeature = conThis.prepareStatement("INSERT INTO FEATURE VALUES (?, ?, ?, ?)");
      
      // Other initialisations
      loc_iFeatIdx = 1; loc_iResId = 1;
      
      // Return success
      return true;
    } catch (Exception ex) {
      errHandle.DoError("DbStore/createWrite error: ", ex, DbStore.class);
      return false;
    }
    
  }
  
  /**
   * addGeneral
   *    Fill the table "GENERAL" from the JSON object
   * 
   * @param oGeneral
   * @return 
   */
  public boolean addGeneral(JSONObject oGeneral) {
    try {
      
      // Populate the content of the general part
      String sProjectName = ""; // Name of CRPX that created the DB
      String sCreated = "";     // Created date in sortable date/time
      String sDstDir = "";
      String sSrcDir = "";
      String sLanguage = "";    // Main language
      String sPart = "";        // Part of corpus
      String sNotes = "";       // Notes to the DB
      String sAnalysis = "";    // Names of all features used
      int iQC = 1;              // The QC number for this database
      if (oGeneral.has("ProjectName")) sProjectName = oGeneral.getString("ProjectName");
      if (oGeneral.has("Created")) sCreated = oGeneral.getString("Created");
      if (oGeneral.has("DstDir")) sDstDir = oGeneral.getString("DstDir");
      if (oGeneral.has("SrcDir")) sSrcDir = oGeneral.getString("SrcDir");
      if (oGeneral.has("Language")) sLanguage = oGeneral.getString("Language");
      if (oGeneral.has("Part")) sPart = oGeneral.getString("Part");
      if (oGeneral.has("Notes")) sNotes = oGeneral.getString("Notes");
      if (oGeneral.has("Analysis")) sAnalysis = oGeneral.getString("Analysis");
      if (oGeneral.has("QC")) iQC = oGeneral.getInt("QC");
      String sSql = loc_sqlInsertGeneral+"VALUES (1, '"+sProjectName+
              "', '"+sCreated+"', '"+sDstDir+
              "', '"+sSrcDir+"', '"+sLanguage+
              "', '"+sPart+"', "+iQC+
              ", '"+sNotes+"', '"+sAnalysis+"');";
      loc_stmt.executeUpdate(sSql);      // Return positively
      return true;
    } catch (Exception ex) {
      errHandle.DoError("DbStore/addGeneral error: ", ex, DbStore.class);
      return false;
    }
  }
  
  /**
   * addFeatNames -- Add feature names to table FEATNAMES
   * 
   * @param lFeatName
   * @param sAnalysis
   * @return 
   */
  public boolean addFeatNames(List<String> lFeatName, String sAnalysis) {
    try {
      // If there are no feature names - get them from the analysis
      if (lFeatName.isEmpty()) lFeatName.addAll(Arrays.asList(sAnalysis.split(";")));
      
      // Add all feature names to the database
      for (int i=0;i<lFeatName.size();i++) {
        String sSql = loc_sqlInsertFeatName +"VALUES ("+(i+1)+ ", '"+lFeatName.get(i)+ "');";
        loc_stmt.executeUpdate(sSql);
      }
      
      return true;
    } catch (Exception ex) {
      errHandle.DoError("DbStore/addFeatNames error: ", ex, DbStore.class);
      return false;
    }
  }
  
  /**
   * addResult
   *    Add one item to the table RESULT
   *    Note: this also contains the features
   * 
   * @param oResult
   * @return 
   */
  public boolean addResult(JSONObject oResult) {
    try {
      int iResId = oResult.getInt("ResId");
      // Validate and correct
      if (iResId != loc_iResId) iResId = loc_iResId;
      // Get all the other relevant values
      String sFile = oResult.getString("File");
      String sTextId = oResult.getString("TextId");
      String sSearch =  oResult.getString("Search");
      String sCat =  oResult.getString("Cat");
      String sLocS =  oResult.getString("Locs");
      String sLocW =  oResult.getString("Locw");
      String sNotes =  oResult.getString("Notes");
      String sSubType =  oResult.getString("SubType");
      // Do NOT calculate values for Text, Psd and Pde -- these are not determined anyway
      
      this.loc_psInsertResult.setInt(1, iResId);
      this.loc_psInsertResult.setString(2, sFile);
      this.loc_psInsertResult.setString(3, sTextId);
      this.loc_psInsertResult.setString(4, sSearch);
      this.loc_psInsertResult.setString(5, sCat);
      this.loc_psInsertResult.setString(6, sLocS);
      this.loc_psInsertResult.setString(7, sLocW);
      this.loc_psInsertResult.setString(8, sNotes);
      this.loc_psInsertResult.setString(9, sSubType);
      this.loc_psInsertResult.setString(10, "");
      this.loc_psInsertResult.setString(11, "");
      this.loc_psInsertResult.setString(12, "");
      this.loc_psInsertResult.executeUpdate();


      // Add features to the FEATURE table
      JSONArray arFeats = oResult.getJSONArray("Features");
      for (int i=0;i<arFeats.length(); i++) {
        JSONObject oFeat = arFeats.getJSONObject(i);
        if (!addFeature(iResId, oFeat)) return false;
      }
      // Book keeping
      loc_iResId += 1;
      
      // Return positively
      return true;
    } catch (Exception ex) {
      errHandle.DoError("DbStore/addResult error: ", ex, DbStore.class);
      return false;
    }
  }
  
  /**
   * addFeature
   *    Add one item to the table FEATURE
   * 
   * @param iResId
   * @param oFeature
   * @return 
   */
  public boolean addFeature(int iResId, JSONObject oFeature) {
    try {
      this.loc_psInsertFeature.setInt(1, loc_iFeatIdx);
      this.loc_psInsertFeature.setInt(2, iResId);
      this.loc_psInsertFeature.setString(3, oFeature.getString("Name"));
      this.loc_psInsertFeature.setString(4, oFeature.getString("Value"));
      this.loc_psInsertFeature.executeUpdate();

      /*
      // Add this feature
      String sSql = loc_sqlInsertFeature+"VALUES ("+loc_iFeatIdx+", "+
              iResId+", '"+oFeature.getString("Name")+
              "', '"+oFeature.getString("Value")+"');";
      loc_stmt.executeUpdate(sSql);  
      */
      
      loc_iFeatIdx += 1;
      // Return positively
      return true;
    } catch (Exception ex) {
      errHandle.DoError("DbStore/addFeature error: ", ex, DbStore.class);
      return false;
    }
  }
  
  /**
   * sort -- Sort the database on the indicated criteria
   * 
   * @param sSortField
   * @param bAscending
   * @param bIsFeature
   * @return 
   */
  public boolean sort(String sSortField, boolean bAscending, boolean bIsFeature) {
    try {
      // Validate
      if (conThis == null) return false;
      // Access the general table
      conThis.setAutoCommit(false);
      Statement stmt = null;
      stmt = conThis.createStatement();
      // Determine the sort order string
      String sOrder = (bAscending) ? "ASC" : "DESC";
      // Action depends on bIsFeature
      if (bIsFeature) {
        
      } else {
        // Just look up result
        ResultSet resThis = stmt.executeQuery("SELECT * FROM RESULT ORDER BY "+sSortField+" "+sOrder+" ;");
      }
      // Return positively
      return true;
    } catch (Exception ex) {
      errHandle.DoError("DbStore/sort error: ", ex, DbStore.class);
      return false;
    }
  }
  
  /**
   * closeWrite
   *    Close a database file to which we have been writing
   * 
   * @return 
   */
  public boolean closeWrite() {
    try {
      // Validate
      if (this.loc_sDbSqlFile.isEmpty()) return false;
      File fDb = new File(this.loc_sDbSqlFile);
      if (!fDb.exists()) return false;
      // Close the database
      conThis.close();  
      // Convert the database to GZ
      FileUtil.compressGzipFile(this.loc_sDbSqlFile, this.loc_sDbSqlFile+".gz");
      // Remove the actual .db file
      fDb.delete();
      // Return success
      return true;      
    } catch (Exception ex) {
      errHandle.DoError("DbStore/closeWrite error: ", ex, DbStore.class);
      return false;
    }
  }
  
}
