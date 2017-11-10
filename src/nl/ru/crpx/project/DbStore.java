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
import java.util.Iterator;
import java.util.List;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.util.ByRef;
import nl.ru.util.FileUtil;
import nl.ru.util.StringUtil;
import nl.ru.util.json.JSONArray;
import nl.ru.util.json.JSONObject;
import nl.ru.xmltools.XmlNode;
import nl.ru.xmltools.XmlResultPsdxIndex;
import org.sqlite.util.StringUtils;

/**
 *
 * @author Erwin R. Komen
 */
public class DbStore {
  // =============== Local class stuff =================================
  private ErrHandle errHandle;                // Local copy of error handler
  private Connection conThis = null;          // JDBC connection
  private String loc_sDbName = "";
  private String loc_sDbXmlFile = "";
  private String loc_sDbSqlFile = "";
  private String loc_sOrder = "ASC";          // Sort order
  private String loc_sSortField = "RESID";    // Normally we sort by the Result Id
  private JSONArray loc_arFilter = null;
  private List<String> loc_lFilter = null;    // List of SQL filter statements
  private List<String> loc_lValue = null;
  private boolean loc_bIsFeature = false;     // Search column is a feature
  private int loc_iResId = 0;                 // ResId for 
  private int loc_iFeatIdx = 0;               // Unique ID for each Feature
  private int loc_iSize = 0;                  // Size of the database
  private Statement loc_stmt = null;
  private List<String> loc_lFeatName = null;  // List of feature names
  private List<String> loc_lSortField = null; // List of fields for which indices have been built
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
  /*
  private final String loc_sqlCreateFeature = 
          "CREATE TABLE FEATURE " +
          "(ID INT PRIMARY KEY  NOT NULL,"+
          " RESID          INT  NOT NULL,"+
          " NAME           TEXT NOT NULL,"+
          " VALUE          TEXT NOT NULL)";
  private final String loc_sqlInsertFeature = 
          "INSERT INTO FEATURE (ID, RESID, NAME, VALUE) ";*/
  private final String loc_sqlCreateFeatName = 
          "CREATE TABLE FEATNAME " +
          "(ID INT PRIMARY KEY  NOT NULL,"+
          " NAME           TEXT NOT NULL)";
  private final String loc_sqlInsertFeatName = 
          "INSERT INTO FEATNAME (ID, NAME) ";
  private final String loc_sqlCreateResult = 
          "CREATE TABLE RESULT " +
          "(RESID INT PRIMARY KEY  NOT NULL,"+
          " METAID         INT,"+
          " SEARCH         CHAR(200),"+
          " CAT            CHAR(100),"+
          " LOCS           CHAR(200),"+
          " LOCW           CHAR(200),"+
          " SUBTYPE        CHAR(200)";
  /*
  private final String loc_sqlInsertResult
          = "INSERT INTO RESULT (RESID, METAID, SEARCH, CAT, "
          + "LOCS, LOCW, SUBTYPE) ";*/
  private final String loc_sqlCreateMeta = 
          "CREATE TABLE META " +
          "(METAID INT PRIMARY KEY NOT NULL,"+
          " FILE           TEXT NOT NULL,"+
          " TEXTID         CHAR(100),"+
          " SUBTYPE        CHAR(200),"+
          " TITLE          TEXT NOT NULL,"+
          " GENRE          CHAR(200),"+
          " AUTHOR         CHAR(200),"+
          " DATE           TEXT NOT NULL,"+
          " SIZE           INT )";
  private final String loc_sqlCreateIndices = 
          "CREATE INDEX subtype_index ON RESULT (SUBTYPE); "+
          "CREATE INDEX cat_index ON RESULT (CAT); ";
  private PreparedStatement loc_psInsertResult = null;
  private PreparedStatement loc_psInsertMeta = null;
  private PreparedStatement loc_psInsertFeature = null;
  // private PreparedStatement loc_psFilterResult = null;
// </editor-fold>  
  // =============== Class initializer =================================
  public DbStore(ErrHandle objErr) {
    this.errHandle = objErr;    // Take over the error object
    try {
      // Cause the class to be initialized
      Class.forName("org.sqlite.JDBC");
      this.loc_lFilter = new ArrayList<>();
      this.loc_lValue = new ArrayList<>();
    } catch (Exception ex) {
      // Provide error message
      errHandle.DoError("DbStore/DbStore error: ", ex, DbStore.class);
    }
  }
  
  // =============== Public methods ====================================
  /**
   * getSize -- Get the size of a dbase that has been read
   * 
   * @return 
   */
  public int getSize() {
    try {
      // Validate
      if (conThis == null) return 0;
      // Create a statement
      Statement stmt = conThis.createStatement();
      ResultSet resThis = stmt.executeQuery("SELECT COUNT(*) FROM RESULT");
      if (resThis.next()) {
        // Get the result
        this.loc_iSize = resThis.getInt(1);
      }
      // REturn what we found
      return this.loc_iSize;
    } catch (Exception ex) {
      // Provide error message
      errHandle.DoError("DbStore/getSize error: ", ex, DbStore.class);
      return 0;
    }
  }
  
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
      // Now make sure the array with feature names is created
      if (this.loc_lFeatName == null || this.loc_lFeatName.isEmpty()) {
        this.loc_lFeatName = getFeatureList();
      }
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
      ResultSet resThis = null;
      stmt = conThis.createStatement();
      try {
        resThis = stmt.executeQuery("SELECT * FROM GENERAL;");
      } catch (Exception ex) {
        // Ignore the exception
      }
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
      // Look at the table with the feature names
      ResultSet resThis = stmt.executeQuery("SELECT * FROM FEATNAME;");
      while (resThis.next()) {
        lBack.add(resThis.getString("NAME"));
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
   * @param arListTotal
   * @return 
   */
  public boolean xmlToDbNew(String sFileName) {
    return xmlToDbNew(sFileName, null);
  }
  public boolean xmlToDbNew(String sFileName, JSONArray arListTotal) {
    List<JSONObject> lTextlist;
    
    try {
      // Process the header
      CorpusResearchProject oCrpx = new CorpusResearchProject(true);
      XmlResultPsdxIndex oDbIndex = new XmlResultPsdxIndex(oCrpx, null, errHandle);
      if (!oDbIndex.Prepare(sFileName)) { errHandle.DoError("DbStore/xmlToDb: cannot prepare database"); return false;}
      
      // Get the General part
      JSONObject oHdr = oDbIndex.headerInfo();   
      
      // Create a writer
      List<String> lFeatName = oDbIndex.featureList();
      // Adapt the list
      for (int i=0;i<lFeatName.size();i++) { lFeatName.set(i, "ft_"+lFeatName.get(i));}
      this.loc_lFeatName = lFeatName;
      if (!createWrite(sFileName, lFeatName)) return false;
      
      // Add header to the database
      if (!addGeneral(oHdr)) return false;
      
      // Prepare making a list of feature names
      String sAnalysis = oHdr.getString("Analysis");
      boolean bDoFeatNames = true;
      
      // Initialize a list of MetaId-File-TextId elements 
      //     that need to be put into a separate table
      lTextlist = new ArrayList<>();
      
      // process the results one-by-one
      ByRef<XmlNode> ndxResult = new ByRef(null);
      int iFeatIdx = 1;
      int iCount = 0;
      if (oDbIndex.FirstResult(ndxResult)) {
        // Walk through them
        int iCheck = 1; 
        while (ndxResult.argValue != null) {
          // Keep track of the count
          iCount += 1;
          // Get the values of this result
          XmlNode ndxThis = ndxResult.argValue;
          JSONObject oResult = new JSONObject();
          // Get the index
          int iResId = Integer.parseInt(ndxThis.getAttributeValue("ResId"));
          // Double check
          if (iCheck != iResId) { 
            errHandle.DoError("DbStore/xmlToDb: index is wrong at " + iCheck); return false; 
          }
          // Get all the other relevant values
          String sFile = ndxThis.getAttributeValue("File");
          String sTextId = ndxThis.getAttributeValue("TextId");
          // Add the text/file to the file list if not already in there
          int iMetaId = addToTextList(lTextlist, sTextId, sFile, arListTotal);
          oResult.put("MetaId", iMetaId);
          oResult.put("Search", ndxThis.getAttributeValue("Search"));
          oResult.put("Cat", ndxThis.getAttributeValue("Cat"));
          oResult.put("Locs", ndxThis.getAttributeValue("forestId"));
          oResult.put("Locw", ndxThis.getAttributeValue("eTreeId"));
          oResult.put("Notes", ndxThis.getAttributeValue("Notes"));
          oResult.put("SubType", ndxThis.getAttributeValue("Period"));          
          oResult.put("ResId", Integer.parseInt(ndxThis.getAttributeValue("ResId")));
          // Determine the features for this result
          List<XmlNode> lFeatValue = ndxThis.SelectNodes("./child::Feature");
          for (int j=0;j<lFeatValue.size();j++) {
            // Create name for feature
            String sFeatName = "ft_" + lFeatValue.get(j).getAttributeValue("Name");
            // Add this in the result
            oResult.put(sFeatName, lFeatValue.get(j).getAttributeValue("Value"));
          }
          // If this is the first go, we should create an insert statement
          if (bDoFeatNames) {
            // Prepare a string for the values
            String sSql = "INSERT INTO RESULT (RESID, METAID, SEARCH, CAT, "+
                "LOCS, LOCW, SUBTYPE, "+
                StringUtil.join(this.loc_lFeatName, ", ")+
                ") VALUES (?, ?, ?, ?, ?, ?, ?"+
                new String(new char[this.loc_lFeatName.size()]).replace("\0", ", ?")+
                ")";
            // Prepare an insert statement
            this.loc_psInsertResult = conThis.prepareStatement(sSql);            
          }
          
          // Switch off feature-name extraction after the first go
          bDoFeatNames = false;
          
          // Process this Result
          if (!addResult(oResult)) return false;
          
          
          // Get the next result
          oDbIndex.NextResult(ndxResult);
          iCheck += 1;
        }
        
        // Retrieve the textlist information
        String sSql = "INSERT INTO META (METAID, TEXTID, FILE, SUBTYPE, "+
                "TITLE, GENRE, AUTHOR, DATE, SIZE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        this.loc_psInsertMeta = conThis.prepareStatement(sSql);
        // Add all appropriate lines to the Meta table
        for (JSONObject oMeta : lTextlist) {
          // Add the meta-information for this text
          
          // Process this meta-table line
          if (!addMeta(oMeta)) return false;
        }
        
      }
      
      // CLose the database result index
      oDbIndex.close();
      oDbIndex = null;
      
      // Store the size
      this.loc_iSize = iCount;
                  
      // Add the feature names in a separate table
      if (!addFeatNames(lFeatName, sAnalysis)) return false;
      // Also add the list of feature names locally
      this.loc_lFeatName = lFeatName;
      
      // Add index building statements
      loc_stmt.executeUpdate(this.loc_sqlCreateIndices);

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
   * addToTextList
   *    Add the combination [sTextId/sFile] to the textlist and return the index
   * 
   * @param lTexts
   * @param sTextId
   * @param sFile
   * @return 
   */
  private int addToTextList(List<JSONObject> lTexts, String sTextId, 
          String sFile, JSONArray arListTotal) {
    int idx = -1;
    JSONObject oListOneFile = null;
    JSONObject oInfo;
    
    try {
      for (int i=0;i<lTexts.size(); i++) {
        JSONObject oItem = lTexts.get(i);
        if (sTextId.equals(oItem.getString("TextId")) &&
            sFile.equals(oItem.getString("File")) ) {
          // Found it
          return i+1;
        }
      }
      // It is not yet processed...
      
      if (arListTotal == null) {
        oListOneFile = null;
      } else {
        // Find the index within arListTotal
        for (int i=0;i<arListTotal.length(); i++) {
          oListOneFile = arListTotal.getJSONObject(i);
          if (sTextId.equals(oListOneFile.getString("textid"))) {
            // Found it
            idx = i;
            break;
          }
        }
      }
      
      // Validate
      oInfo = new JSONObject();
      oInfo.put("MetaId", lTexts.size()+1);
      if (idx >=0 && oListOneFile != null) {
        // Add the information here
        JSONObject oMeta = oListOneFile.getJSONObject("meta");
        if (oMeta.has("subtype")) {oInfo.put("SubType", oMeta.getString("subtype"));}
        if (oMeta.has("title")) {oInfo.put("Title", oMeta.getString("title"));}
        if (oMeta.has("genre")) {oInfo.put("Genre", oMeta.getString("genre"));}
        if (oMeta.has("author")) {oInfo.put("Author", oMeta.getString("author"));}
        if (oMeta.has("date")) {oInfo.put("Date", oMeta.getString("date"));}
        if (oMeta.has("size")) {oInfo.put("Size", oMeta.getInt("size"));}
      }
      
      oInfo.put("TextId", sTextId);
      oInfo.put("File", sFile);
      lTexts.add(oInfo);
      idx = lTexts.size();
      
      // Return success
      return idx;
    } catch (Exception ex) {
      errHandle.DoError("DbStore/addToTextList error: ", ex, DbStore.class);
      return -1;
    }
  }
    
  /**
   * createWrite
   *    Open an SQLite database for writing
   * 
   * @param sFileName
   * @param arFeatList
   * @return 
   */
  public boolean createWrite(String sFileName, List<String> arFeatList) {
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
      // Create a table for the Feature Names only (not linked)
      loc_stmt.executeUpdate(loc_sqlCreateFeatName);
      // Create a table for the metainformation
      loc_stmt.executeUpdate(loc_sqlCreateMeta);
      // Prepare the create statement for the RESULTS table
      String sSqlCreateResult = loc_sqlCreateResult;
      for (int i=0;i<arFeatList.size();i++) {
        sSqlCreateResult += ", " + arFeatList.get(i) + " TEXT NOT NULL";
      }
      // Finish the statement
      sSqlCreateResult += ")";
      // Create a table for the Results
      loc_stmt.executeUpdate(sSqlCreateResult);
      
      
      // Commit these steps
      conThis.commit();

/* ---------------------- OLD SYSTEM ------------      
      // Prepare some statements
      this.loc_psInsertResult = conThis.prepareStatement("INSERT INTO RESULT (RESID, FILE, TEXTID, SEARCH, CAT, "+
          "LOCS, LOCW, NOTES, SUBTYPE, TEXT, PSD, PDE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
      this.loc_psInsertFeature = conThis.prepareStatement("INSERT INTO FEATURE VALUES (?, ?, ?, ?)");
      ------------------------------------------- */
      
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
      //String sFile = oResult.getString("File");
      //String sTextId = oResult.getString("TextId");
      int iMetaId = oResult.getInt("MetaId");
      String sSearch =  oResult.getString("Search");
      String sCat =  oResult.getString("Cat");
      String sLocS =  oResult.getString("Locs");
      String sLocW =  oResult.getString("Locw");
      String sSubType =  oResult.getString("SubType");
      // Do NOT calculate values for Text, Psd and Pde -- these are not determined anyway
      
      this.loc_psInsertResult.setInt(1, iResId);
      this.loc_psInsertResult.setInt(2, iMetaId);
      this.loc_psInsertResult.setString(3, sSearch);
      this.loc_psInsertResult.setString(4, sCat);
      this.loc_psInsertResult.setString(5, sLocS);
      this.loc_psInsertResult.setString(6, sLocW);
      this.loc_psInsertResult.setString(7, sSubType);
      // Add the feature values
      for (int i=0;i<this.loc_lFeatName.size();i++ ) {
        int iNum = 7+i+1;  // The number of the element to be set
        // Get the feature 
        String sFeatValue = oResult.getString(this.loc_lFeatName.get(i));
        this.loc_psInsertResult.setString(iNum, sFeatValue);
      }
      this.loc_psInsertResult.executeUpdate();

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
   * addMeta
   *    Add one item to the table META
   * 
   * @param oMeta
   * @return 
   */
  public boolean addMeta(JSONObject oMeta) {
    try {
      // Get the ID for this record
      int iMetaId = oMeta.getInt("MetaId");

      // Get all the other relevant values
      String sTextId = oMeta.getString("TextId");
      String sFile = oMeta.getString("File");
      String sSubType =  oMeta.getString("SubType");
      String sTitle =  oMeta.getString("Title");
      String sGenre =  oMeta.getString("Genre");
      String sAuthor =  oMeta.getString("Author");
      String sDate =  oMeta.getString("Date");
      int iSize = oMeta.getInt("MetaId");

      // Do NOT calculate values for Text, Psd and Pde -- these are not determined anyway
      
      this.loc_psInsertMeta.setInt(1, iMetaId);
      this.loc_psInsertMeta.setString(2, sTextId);
      this.loc_psInsertMeta.setString(3, sFile);
      this.loc_psInsertMeta.setString(4, sSubType);
      this.loc_psInsertMeta.setString(5, sTitle);
      this.loc_psInsertMeta.setString(6, sGenre);
      this.loc_psInsertMeta.setString(7, sAuthor);
      this.loc_psInsertMeta.setString(8, sDate);
      this.loc_psInsertMeta.setInt(9, iSize);
      this.loc_psInsertMeta.executeUpdate();

      // Return positively
      return true;
    } catch (Exception ex) {
      errHandle.DoError("DbStore/addResult error: ", ex, DbStore.class);
      return false;
    }
  }
  
  /**
   * sort -- Sort the database on the indicated criteria
   * 
   * @param sSortField
   * @param bAscending
   * @return 
   */
  public boolean sort(String sSortField, boolean bAscending) {
    try {
      // Validate
      if (conThis == null) return false;
      if (sSortField.isEmpty()) {
        this.loc_sSortField = "RESID";
        return true;
      }
      // Access the general table
      conThis.setAutoCommit(false);
      Statement stmt = null;
      stmt = conThis.createStatement();
      // Determine the sort order string
      this.loc_sOrder = (bAscending) ? "ASC" : "DESC";
      // Keep the sort field
      this.loc_sSortField = sSortField;
      try {
        // Create an index
        stmt.execute("CREATE INDEX IF NOT EXISTS Res_"+sSortField+" ON RESULT("+sSortField+")");
        // Make sure it is created - but do not complain if it goes wrong here
        conThis.commit();
      } catch (Exception exCon) {
        int iOkay = 1;
      }
      // Return positively
      return true;
    } catch (Exception ex) {
      errHandle.DoError("DbStore/sort error: ", ex, DbStore.class);
      return false;
    }
  }
  
  /**
   * filter
   *    Specify the filter that needs to be used when downloading
   * 
   * @param oFilter
   * @return 
   */
  public boolean filter(JSONObject oFilter) {
    boolean bEscape = false;
    try {
      // Validate
      if (conThis == null) return false;
      if (oFilter == null || oFilter.length() == 0) {
        this.loc_arFilter = null;
        return true;
      }
      
      // Access the general table
      conThis.setAutoCommit(false);
      Statement stmt = null;
      stmt = conThis.createStatement();
      
      // Reset the filter
      this.loc_lFilter.clear();
      this.loc_lValue.clear();
      // Walk all elements of the filter
      Iterator keys = oFilter.keys();
      while (keys.hasNext()) {
        StringBuilder sbThis = new StringBuilder();

        String sKey = keys.next().toString();
        String sValue = oFilter.getString(sKey);
        
        try {
          // Create an index
          stmt.execute("CREATE INDEX IF NOT EXISTS Res_"+sKey+" ON RESULT("+sKey+")");
          // Make sure it is created - but do not complain if it goes wrong here
          conThis.commit();
        } catch (Exception exCon) {
          int iOkay = 1;
        }

        sbThis.append(sKey);
        // Add the key/value pair to the list of SQL filter expressions
        if (sValue.contains("*")) {
          sbThis.append(" LIKE ? ");
        } else {
          sbThis.append(" = ?");
        }
        // Add the filter line
        loc_lFilter.add(sbThis.toString());
        // Possible filter out the percent sign
        sValue = sValue
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replace("[", "![");
        loc_lValue.add(sValue.replace("*", "%"));
      }
      
      // Return positively
      return true;
    } catch (Exception ex) {
      errHandle.DoError("DbStore/filter error: ", ex, DbStore.class);
      return false;
    }
  }
  
  /**
   * getResults
   *    Return [iCount] (sorted) results that start at [iStart]
   * 
   * @param iStart
   * @param iCount
   * @return 
   */
  public JSONArray getResults(int iStart, int iCount) {
    JSONArray arBack = new JSONArray();
    ResultSet resThis = null;
    String sSql;
    
    try {
      // Validate
      if (conThis == null) return null;
      // Access the general table
      conThis.setAutoCommit(false);
      Statement stmt = null;
      stmt = conThis.createStatement();
      PreparedStatement psThis = null;
      // Create the SQL query
      // Note: the [iStart] indicates the first
      if (this.loc_lFilter.isEmpty())  {
        // No filtering
        sSql = "SELECT * FROM RESULT "+
                "INNER JOIN META ON RESULT.METAID = META.METAID "+
                "ORDER BY "+this.loc_sSortField+" "+this.loc_sOrder+
                " LIMIT "+iCount+" OFFSET "+iStart+
                ";";
        psThis = conThis.prepareStatement(sSql);
      } else {
        // Filtering
        sSql = "SELECT * FROM RESULT "+
                "INNER JOIN META ON RESULT.METAID = META.METAID "+
                "WHERE " + StringUtils.join(loc_lFilter, " AND ") +
                "ORDER BY "+this.loc_sSortField+" "+this.loc_sOrder+
                " LIMIT "+iCount+" OFFSET "+iStart+
                ";";
        psThis = conThis.prepareStatement(sSql);
        for (int i=0;i<loc_lValue.size(); i++) {
          psThis.setString(i+1, loc_lValue.get(i));
        }
      }
      
      try {
        // resThis = psThis.executeQuery(sSql);
        resThis = psThis.executeQuery();
        // resThis = stmt.executeQuery(sSql);
      } catch (Exception exExe) {
        // Adapt the SQL, taking the ordering out
        sSql = "SELECT * FROM RESULT "+
                "INNER JOIN META ON RESULT.METAID = META.METAID "+
                " LIMIT "+iCount+" OFFSET "+iStart+
                ";";
        resThis = stmt.executeQuery(sSql);
      }
    
      // Walk the result set
      while (resThis.next()) {
        JSONObject oResult = new JSONObject();
        // Get all the result items
        oResult.put("ResId", resThis.getInt("RESID"));
        oResult.put("File", resThis.getString("FILE"));
        oResult.put("TextId", resThis.getString("TEXTID"));
        oResult.put("Search", resThis.getString("SEARCH"));
        oResult.put("Cat", resThis.getString("CAT"));
        oResult.put("Locs", resThis.getString("LOCS"));
        oResult.put("Locw", resThis.getString("LOCW"));
        oResult.put("SubType", resThis.getString("SUBTYPE"));
        oResult.put("Title", resThis.getString("TITLE"));
        oResult.put("Genre", resThis.getString("GENRE"));
        oResult.put("Author", resThis.getString("AUTHOR"));
        oResult.put("Date", resThis.getString("DATE"));
        oResult.put("Size", resThis.getInt("SIZE"));
        // Get all the features from the result object
        JSONArray arFeat = new JSONArray();
        for (int i=0;i<this.loc_lFeatName.size();i++) {
          String sFeatName = loc_lFeatName.get(i);
          JSONObject oFeat = new JSONObject();
          oFeat.put("Name", sFeatName);
          oFeat.put("Value", resThis.getString(sFeatName));
          arFeat.put(oFeat);
        }
        oResult.put("Features", arFeat);
        // Add this result to the array
        arBack.put(oResult);
      }
      
      // Return our findings
      return arBack;
    } catch (Exception ex) {
      errHandle.DoError("DbStore/getResults error: ", ex, DbStore.class);
      return null;
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
      conThis = null;
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

 // ========================= PRIVATE METHODS ======================  
  /**
   * addColumn
   *    Add one column to the indicated table
   * 
   * @param sTable
   * @param sColName
   * @return 
   */
  private boolean addColumn(String sTable, String sColName) {
    try {
      String sSql = "ALTER TABLE "+sTable+" ADD COLUMN "+sColName+" TEXT;";
      // Put it into motion
      Statement stmt = conThis.createStatement();
      if (!stmt.execute(sSql)) return false;      
      // Return success
      return true;      
    } catch (Exception ex) {
      errHandle.DoError("DbStore/addColumn error: ", ex, DbStore.class);
      return false;
    }
  }
}
