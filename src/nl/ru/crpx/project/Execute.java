/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.project;
// Which methods need to be imported
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import nl.ru.crpx.cmd.CrpxProcessor;
import static nl.ru.crpx.project.CrpGlobal.Status;
import static nl.ru.crpx.project.CrpGlobal.getCurrentTimeStamp;
import static nl.ru.crpx.project.CrpGlobal.getJsonString;
import nl.ru.crpx.search.Job;
import nl.ru.crpx.search.SearchManager;
import nl.ru.crpx.search.SearchParameters;
import nl.ru.crpx.search.WorkQueueXqF;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.xq.Extensions;
import nl.ru.util.ByRef;
import nl.ru.util.FileUtil;
import nl.ru.util.StringUtil;
import nl.ru.util.json.JSONObject;
import nl.ru.xmltools.Parse;
import org.apache.log4j.Logger;

/**
 *
 * @author E.R.Komen
 */
/* ---------------------------------------------------------------------------
   Class:   Execute
   Goal:    Methods to execute queries defined in a Corpus Research Project (crp)
   History:
   20/apr/2015   ERK Created
   --------------------------------------------------------------------------- */
public class Execute extends CrpGlobal {
  protected static final Logger logger = Logger.getLogger(Execute.class);
  // protected static final ErrHandle errHandle = new ErrHandle(Execute.class);
  protected ErrHandle errHandle = new ErrHandle(Execute.class);
  // ========================= Constants =======================================
  protected String sProjectBase = CrpxProcessor.sEtcProject + "/";  // "/etc/project/"; // Base directory where user-spaces are stored
  protected String sCorpusBase = CrpxProcessor.sEtcCorpora + "/";   // "/etc/corpora/";  // Base directory where corpora are stored
  // ===================== parameters for this user/execution of a CRP =========
  protected CorpusResearchProject crpThis;// The corpus research project for this execution
  protected String userId;                // ID of the user for this execution
  protected SearchManager searchMan;      // The manager for this search
  protected WorkQueueXqF workQueue;       // The work queue for XqF jobs
  protected ExecutorService workExecutor; // Service to hold executors
  protected SearchParameters searchPar;   // The parameters for this search
  protected Extensions ruExtensions;      // The extensions need to be initialized here
  protected int iMaxParJobs;              // Maximum number of parallel jobs per user
  // ===================== The elements of an execution object =================
  protected Query[] arQuery;        // Array of queries to be executed (so this is the constructor) 
  protected Qinfo[] arQinfo;        // Information for each *query* (not query-line)
  protected Result objRes;          // Make room for results
  protected String[] arInput;       // Array of input files
  protected String strQext;         // Extension of queries
  protected String strDext;         // Extension of definitions
  protected String strName;         // Name of the corpus research project
  protected String strHtmlFile;     // Name of the HTML file containing the results of this project
  protected String strJsonFile;     // Name of the JSON file containing the results of this project
  protected int intWait;            // Amount of milliseconds to wait before continuing
  protected boolean bKeepGarbage;   // Retain the in-between files for inspection
  protected boolean bIsXquery;      // Whether the engine is an Xquery type
  protected boolean bDoStream;      // Whether execution should take place stream-wise
  protected boolean bErrors;        // Did we encounter any errors?
  protected boolean bSrcBuilt;      // Whether a Source.xml has been built or not yet
  protected boolean bXmlData;       // Whether this is XML data (from Xquery) or Treebank
  protected boolean bShowPsd;       // Whether PSD should be shown or not
  // ============== elements that are usable for all classes extending "Execute"
  protected String[] arSource;      // Array of source files for a query using "source" as input with *.psdx
  protected List<String> lSource;   // List of source files for a query using "source" as input with e.g. *.psdx
  protected String strQtemp;        // Prefix to query file to indicate it is temporary
  protected String strOtemp;        // Name of temporary output file
  protected String strCtemp;        // Name of temporary complement file
  // protected XmlForest objProcType;  // Provides functions, depending on processing type in [XmlForest]
  protected Job objSearchJob;
  // =============== local constants ===========================================
  // in .NET: String strRuDef = "declare namespace ru = 'clitype:CorpusStudio.RuXqExt.RU?asm=CorpusStudio';\r\n";
  // private final String strRuDef = "declare namespace ru = 'java:nl.ru.crpx.xq.Extensions';\r\n";
  // private final String strTbDef = "declare namespace tb = 'http://erwinkomen.ruhosting.nl/software/tb';\r\n";
  // private final String strFunctxDef = "declare namespace functx = 'http://www.functx.com';\r\n";
  // ===================== Local stuff =========================================
  private String TEMP_FILE = "CrpTemp";
  
  // Initialisation of the Execute class
  public Execute(CorpusResearchProject oProj) {
    try {
      // Set our copy of the corpus research project
      crpThis = oProj;
      // Set the rubase
      ruExtensions = new Extensions(crpThis);
      // Set the maximum number of parallel XqF jobs
      iMaxParJobs = crpThis.getPrjTypeManager().getMaxParJobs();
      
      // ================= DEBUGGING ==============
      errHandle.debug("Execute maxparjobs: " + iMaxParJobs);
      // ==========================================
      
      // Set the search manager associated with the CRP
      this.searchMan = crpThis.getSearchManager();
      // Set the work queue to process the XqF jobs
      this.workQueue = crpThis.getWorkQueue();
      // Method: concurrent
      this.workExecutor = crpThis.getWorkExecutor();
      // Create a search parameters object
      this.searchPar = new SearchParameters(this.searchMan);
      /* ===========
      // Fill the object with default values
      for (String name: this.searchMan.getSearchParameterNames()) {
        // Get the value for this parameter or the Default value...
      }
       ============ */
      // Perform other initialisations
      String strInput = "";           // Source files

      // Perform class initialisations that are valid for all extended classes
      this.bKeepGarbage = false;    // Do NOT normally retain temporary files
      bIsXquery = false; bDoStream = false;
      bErrors = false; bSrcBuilt = false;
      bXmlData = false; bShowPsd = false;
      intWait = 0;
      // Validate: are there any query lines in here?
      int arSize = crpThis.getListQCsize();
      if (arSize == 0) {
        // Check if there are any queries at all
        if (crpThis.getListQuerySize() ==0) {
          // TODO: Go to the [Query] tab page
          // Me.TabControl1.SelectedTab = Me.tpQuery
          // Show warning
          DoError("First create one or more queries and put them in the constructor editor");
        } else {
          // TODO: Go to the [QC] tab page
          //  Me.TabControl1.SelectedTab = Me.tpQCedit
          // Show warning
          DoError("First put one or more queries in the constructor editor");
        }
        // We cannot continue - leave
        // TODO: include PERIOD + PERIODFILE tests or not??
        return;
      } 
      // Verify the QC consistency
      if (!VerifyQC()) return;
      // Sort the dbfeatlist on the FtNum
      if (!crpThis.doSort("DbFeatList", "FtNum")) return;
      // Reset interruption
      bInterrupt = false;
      // Reset output file initialisation
      // TODO: RuInit(false);

      // Make sure adaptations are processed
      // TODO: AdaptQnameToQC()

      // Clear the output tabpage
      // TODO: Me.tbOutput.Text = ""

      // Gather the source files and validate the result
      strInput = crpThis.getSource();   // Usually this is [*.psdx] or something like that
      if (strInput.isEmpty()) {errHandle.DoError("First choose input files"); errHandle.bInterrupt = true; bInterrupt = true; return;}
      // Make an array of input files, as defined in [strInput]
      arInput = strInput.split(CrpGlobal.GetDelim(strInput));

      // Which tab is selected?
      // TODO: automatically switch to the correct tab

      // Clear the monitor-log first
      // TODO: clear monitor log (or do that on the client-side in JS??)

      // Make a new results object
      this.objRes = new Result();

      // Get Qext and Dext
      strQext = crpThis.getPrjTypeManager().getQext();
      strDext = crpThis.getPrjTypeManager().getDext();

      // Get the name of this project
      strName = crpThis.getName();
      
      // Get the name of the user
      this.userId = crpThis.getUserId();
    } catch (Exception ex) {
      // Give an error and set interrupt
      errHandle.DoError("Problem in Execute.Execute()", ex, Execute.class);
      errHandle.bInterrupt = true;
    }
  }
  
  // ========= GETTERS ====================
  public String getDbaseDir() { 
    try {
      String sDir = this.sProjectBase;
      if (!sDir.endsWith("/")) sDir = sDir + "/";
      sDir = sDir + crpThis.getUserId() + "/dbase"; 
      File fDir = new File(sDir);
      sDir = fDir.getCanonicalPath();
      // Windows check: should be D
      if (sDir.startsWith("C:")) {
        sDir = "D:" + sDir.substring(2);
        fDir = new File(sDir);
      }
      if (!fDir.exists()) fDir.mkdir();
      // Make sure we get a good path
      sDir = fDir.getCanonicalPath();
      return sDir;
    } catch (Exception ex) {
      // Give an error and set interrupt
      errHandle.DoError("Execute/getDebaseDir", ex, Execute.class);
      errHandle.bInterrupt = true;
      return "";
    }
  }
  public void setInterrupt() {
    // this.objSearchJob.s
  }
//  public String getRuDef() { return strRuDef; }
  // ======================================
  /**
   * ExecuteQueries -- Overridable...
   * 
   * @param jobCaller
   * @return 
   */
  @SuppressWarnings("unused")
  public boolean ExecuteQueries(Job jobCaller) {
    // This is an overridable method
    return true;
  }
          
  /* ---------------------------------------------------------------------------
     Name:    ExecuteQueriesSetUp
     Goal:    Execute the queries defined in @prjThis
              Make a query list (in [arQuery]), and execute the queries in the given order
              An array with the source files [arInput] should have been defined
                at the creation of an instance of this class
     History:
     29-07-2009   ERK Created for .NET
     20/apr/2015  ERK Start transfer to Java
     --------------------------------------------------------------------------- */
  public boolean ExecuteQueriesSetUp() {
    String strInput = null;       // Temporary storage of input files
    String strSrcName = "";       // Name of the combined Source input file
    List<String> lSources = null; // All source files together in a List
    int intOviewLine = 0;         // The line number in the overview
    int iSize = 0;                // Size of a table
    boolean bResult = true;       // The result of execution
    
    try {
      // Validate
      if (this.crpThis == null) return false;
      // Initialisations
      strQtemp = "temp_";                     // Prefix to temporary file
      intWait = 500;                          // TODO: Waiting time for...
      bInterrupt = false;                     // Clear interrupt flag
      bDoStream = this.crpThis.getStream();   // Execute streaming mode or not
      this.setCurrentPsdx("");                // Indicate that there is no currently loaded file

      // Initialise syntax error dataset
      // TODO: InitXqErr()

      // Combine all source files into one List
      lSources = new ArrayList<>();
      for (String strInp : this.arInput) {
        // Check if this is not empty
        if (!strInp.trim().equals("")) {
          // Add this to the list of sources
          lSources.add(strInp.trim());
        }
      }

      // Start with overviewline 0
      intOviewLine = 0;
      Status("Checking query lines...");
      // Position the QC items into the [arQuery] element
      // Note: the array has already been sorted through "VerifyQC" which is called
      //       from the creator of class Execute
      iSize = this.crpThis.getListQCsize();
      // Make room for the arQuery structure
      arQuery = new Query[iSize];
      // Walk all rows
      for (int i=0;i<iSize;i++) {
        // Get this element from the QC
        JSONObject oThis = this.crpThis.getListQCitem(i);
        // Get this element from [arQuery]
        Query oQuery = new Query(); // arQuery[i];
        // Note the name of the query
        oQuery.Name = getJsonString(oThis, "Query");
        // Set the error log file
        oQuery.ErrorFile = FileUtil.nameNormalize(this.crpThis.getDstDir().getAbsolutePath() + 
                "/ErrorLogStep" + (i + 1) + ".txt");
        // Note the line of this query
        oQuery.Line = Integer.parseInt(getJsonString(oThis, "QCid"));
        // Derive the output file
        oQuery.OutputFile = getJsonString(oThis, "Output"); 
        if (oQuery.OutputFile.equals("")) {
          // Make an output file (without proper extension still)
          oQuery.OutputFile = TEMP_FILE + String.valueOf(i + 1);
          // Do NOT increment the overview line
          // Instead, indicate that this row does not have overview output
          oQuery.OviewLine = -1;
        } else {
          // Indicate what the overview line is
          oQuery.OviewLine = intOviewLine;
          // DO increment the overview line
          intOviewLine++;
        }
        // Add the path to the output file
        oQuery.OutputFile = this.crpThis.getDstDir().getAbsolutePath() + 
                "/" + oQuery.OutputFile;
        // Does a complement need to be produced?
        oQuery.Cmp = (getJsonString(oThis, "Cmp").equals("True"));
        // Do we need to keep the output and/or complement?
        oQuery.Mother = (getJsonString(oThis, "Mother").equals("True"));
        // What about includeing examples or not?
        oQuery.NoExmp = (getJsonString(oThis, "NoExmp").equals("True"));
        // Get the description
        oQuery.Descr = getJsonString(oThis, "Result");
        // Get an ordered list of database output features for this QC line
        oQuery.DbFeat = this.crpThis.getListDbFeat(oQuery.Line);
        oQuery.DbFeatSize = this.crpThis.getListDbFeatSize(oQuery.Line, true);
        // Calculate the input file
        strInput = getJsonString(oThis, "Input");
        // Divide the name into two parts
        arInput = strInput.split("[/]", -1);
        // Find out what to do
        switch (arInput[0]) {
          case "s":
          case "S":
          case "Src":
          case "src":
          case "Source":
          case "source":
            // Set the InputLine
            oQuery.InputLine = 0;
            oQuery.InputCmp = false;
            // Check the list of source files
            if (lSources.isEmpty()) {
              // Warn user
              Status("No input file is defined - finished");
              // Exit 
              return false;
            }
            // Take the original source names into a semi-list
            strInput = StringUtil.join(lSources, ";");
            break;
          default:
            // Set the input line
            oQuery.InputLine = Integer.parseInt(arInput[0]);
            oQuery.InputCmp = (arInput[1].equalsIgnoreCase("cmp"));
            // Get the input file name from the output we refer to
            strInput = GetOutput(Integer.parseInt(arInput[0]));
            // Add the extension
            strInput += "." + arInput[1];
            break;
        }
        // Set the input file(s)
        oQuery.InputFile = strInput;  // These are bare names; do not normalize them
        // Set the name of the query file
        oQuery.QueryFile = FileUtil.nameNormalize(this.crpThis.getDstDir().getAbsolutePath() + 
                "/" + strQtemp + getJsonString(oThis, "Query") + strQext);
        // Store the query object into arQuery
        arQuery[i] = oQuery;
      }

      // Place all the query files in the output directory (temporarily?)
      if (!ExtractFiles("Query", strQtemp, strQext)) return false;
      // Place all the definition files in the output directory (temporarily?)
      if (!ExtractFiles("Definition", "", strDext)) return false;
      // If this is Xquery, then the definition files need to be "included" in the query files
      if (crpThis.getPrjTypeManager().getEngine().toLowerCase().contains("xquery")) {
        if (!IncludeDefInQueries("Query", strQtemp, strQext, "Definition", "", strDext))
          return false;
      }

      // Set the working directory to the OUTPUT directory

      // Initialisations on variables for all classes that extend "Execute"
      strOtemp = FileUtil.nameNormalize(crpThis.getDstDir() + "/XqOut.xml");
      strCtemp = FileUtil.nameNormalize(crpThis.getDstDir() + "/XqCmp.xml");
      arSource = new String[1]; // TODO: can be taken away when [lSource] is fully operational
      lSource = new ArrayList<>();

      // Determine the correct names of the corpus result files
      strHtmlFile = FileUtil.nameNormalize(crpThis.getDstDir() + "/" + strName + "-results.html");
      strJsonFile = FileUtil.nameNormalize(crpThis.getDstDir() + "/" + strName + "-results.json");

      // Indicate that we are starting execution of a CRP query series
      Status("Starting execution of queries at: " + getCurrentTimeStamp());

      // Return the result of this operation
      return bResult;
    } catch (Exception ex) {
      return errHandle.DoError("ExecuteQueriesSetUp failed: ", ex, Execute.class);
    }
  }
  
  /* ---------------------------------------------------------------------------
     Name:    VerifyQC
     Goal:     Check for problems in the [Output] and [Result] fields of the QCeditor
     History:
     10-02-2011  ERK Created for .NET CorpusStudio
     21/apr/2015 ERK transformed to Java CRPP
     --------------------------------------------------------------------------- */
  public boolean VerifyQC() {
    List objTag = new ArrayList<>();  // Keep track of Result objects
    
    try {
      // Validation
      if (crpThis == null || crpThis.getListQCsize() == 0) return false;
      // Sort the QC list on QCid
      if (!crpThis.doSort("QClist", "QCid")) return false;
      // Get the sorted QC list here
      List<JSONObject> dtrQc = crpThis.getListQC();
      // Walk all lines
      for (int i=0;i<dtrQc.size();i++) {
        JSONObject dtrQcItem = dtrQc.get(i);
        // Get the result tag and output file names
        String strOut = dtrQcItem.getString("Output");
        String strRes = dtrQcItem.getString("Result");
        // Check if there is [Output] but no [Result]
        if (!strOut.isEmpty() &&  strRes.isEmpty()) {
          // If the output has been encountered, we cannot make a suggestion
          if (objTag.contains(strOut)) {
            // Warn the user
            String strErrorMsg = "QC line [" + i + 1 + "] has an output [" + strOut + "], but no Result Tag is defined";
            DoError(strErrorMsg);
            return false;
          } else {
            // We can make a suggestion for Result
            strRes = strOut + "_Res" + (i+1);
            dtrQcItem.put("Result", strRes);
            // Make sure changes ripple through to QC
            dtrQc.set(i, dtrQcItem);
          }
        } else if (strOut.isEmpty() &&  !strRes.isEmpty()) {
          // Warn the user
          String strErrorMsg = "QC line [" + i + 1 + "] has a result tag [" + strRes + "], but no Output was defined";
          DoError(strErrorMsg);
          return false;
        } else if (!strOut.isEmpty() &&  !strRes.isEmpty()) {
          // Check if this result tag already occurs
          if (objTag.contains(strOut)) {
            // Warn the user
            String strErrorMsg = "QC line [" + i + 1 + "] has a result tag [" + strRes + "], but this is already used in an earlier QC line";
            DoError(strErrorMsg);
            return false;
          } else {
            // Add it to the collection
            objTag.add(strRes);
          }
        }
      }
      // Return positively
      return true;
    } catch (Exception ex) {
      return errHandle.DoError("VerifyQC failed: ", ex, Execute.class);
    }
  }

  /* ---------------------------------------------------------------------------
     Name:    GetOutput
     Goal:     Check for problems in the [Output] and [Result] fields of the QCeditor
     History:
     31-07-2009  ERK Created for .NET CorpusStudio
     22/apr/2015 ERK transformed to Java CRPP
     --------------------------------------------------------------------------- */
  private String GetOutput(int iLine) {
    String sOutput = "";
    
    for (Query qElement : arQuery) {
      // Check this one
      if (qElement.Line == iLine) {
        sOutput = qElement.OutputFile;
        break;
      }
    }
    // Return what we found
    return sOutput;
  }
  /* ---------------------------------------------------------------------------
     Name:     ExtractFiles
     Goal:     Copy all text items from [strTable] to the output directory
               A file gets a prefix [strPrefix] attached to it
               The extension of the file becomes [strExt]
     History:
     14-10-2009  ERK Created for .NET CorpusStudio
     22/apr/2015 ERK transformed to Java CRPP
     --------------------------------------------------------------------------- */
  private boolean ExtractFiles(String strTable, String strPrefix, String strExt) {
    List<JSONObject> lTable;
    // Validate: do we have an output directory?
    if (! crpThis.getDstDir().exists()) {
      // Create it
      if (!crpThis.getDstDir().mkdir()) return (DoError("Could not create output directory: " + crpThis.getDstDir().getAbsolutePath()));
      // Tell user we have created a directory
      logger.debug("Output directory created: " + crpThis.getDstDir().getAbsolutePath());
    }
    // Place all the definition files in the output directory (temporarily?)
    lTable = crpThis.getTable(strTable);
    // Visit all elements of the table (query or definition)
    for (JSONObject elThis : lTable) {
      // Make the correct file name
      String sFile = FileUtil.nameNormalize(crpThis.getDstDir().getAbsolutePath() + "/" + strPrefix +
        elThis.getString("Name") + strExt);
      // Store the contents of this element as an appropriately called file
      FileUtil.writeFile(sFile, elThis.getString("Text"), "utf-8");
      /* 
      try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(sFile, true)))) {
        out.println(elThis.getString("Text"));
      } catch (IOException e) {
        // Transfer the exception
        logger.error("ExtractFiles: cannot write. " + e.getMessage());
      } */
    }
    
    // Return positively
    return true;
  }
  /* ---------------------------------------------------------------------------
     Name:     IncludeDefInQueries
     Goal:     Combine all the definition files into one, and include this in the query files
     History:
     14-10-2009  ERK Created for .NET CorpusStudio
     22/apr/2015 ERK transformed to Java CRPP
     --------------------------------------------------------------------------- */
  public boolean IncludeDefInQueries(String strQtable, String strQpfx, String strQext,
          String strDtable, String strDpfx, String strDext) {
    boolean bAddTbDef;        // Need to add the tb: definition
    boolean bAddFunctx;       // Need to add the functx: definition
    Qel[] arDef;              // Array of query elements
    List<JSONObject> lDef;    // The list of definitions
    List<JSONObject> lQuery;  // The list of queries
    int intSize;              // Size of each Qel array
    String strDtext = "";     // text of combined definitions
    String strQtext = "";     // Text of a query
    String strQone = "";      // One query
    String strFile = "";      // A file name
    
    try {
      // Validate
      if (strDtable.isEmpty() ||strQtable.isEmpty()) return false;
      // There are no definitions
      bAddFunctx = false; bAddTbDef = false;
      // Order the definitions in increasing [DefId]
      if (!crpThis.doSort("DefList", "DefId")) return false;
      lDef = crpThis.getListDef();
      // Make room for definition query elements
      arDef = new Qel[lDef.size()];
      // Visit all definitions
      for (int i=0;i< arDef.length;i++) {
        FileUtil fThis = new FileUtil();
        JSONObject oDef = lDef.get(i);
        // Get the correct file name
        strFile = FileUtil.nameNormalize(crpThis.getDstDir().getAbsolutePath()+ "/" + 
                strDpfx + oDef.getString("Name") + strDext);
        // Add the content to the text of the combined definitions
        strDtext += fThis.readFile(strFile) + "\n";
          
        // Create a Qel element
        Qel qElThis = new Qel();
        qElThis.Type = "def";
        qElThis.Lines = fThis.getLinesRead();
        qElThis.Name = oDef.getString("Name");
        // Add this element to the array of definitions
        arDef[i] = qElThis;
        // fThis.writeFile("d:\\temp.txt", strDtext, "utf-8");
      }
      // Check presence of tb and functx definition
      bAddFunctx = !strDtext.contains("declare namespace functx");
      bAddTbDef = !strDtext.contains("declare namespace tb");
      // Calculate the size for each array of Qel's
      intSize = 1 + ((bAddFunctx) ? 1 :  0) + ((bAddTbDef) ? 1 : 0) + lDef.size() + 1;
      // Make room for the query information
      lQuery = crpThis.getTable(strQtable);
      arQinfo = new Qinfo[lQuery.size()];
      // Process the query files: visit all queries
      for (int i=0; i < lQuery.size(); i++) {
        FileUtil fThis = new FileUtil();
        // Create a new Qinfo element
        Qinfo oThis = new Qinfo();
        // Access the JSON object in the Query table
        JSONObject oQuery = lQuery.get(i);
        // Make room for all the query-elements in this query
        oThis.arQel = new Qel[intSize]; 
        ByRef<Integer> intJ = new ByRef(0); 
        strQtext = "";
        oThis.Name = oQuery.getString("Name");
        // Make the pre-amble for the definition/query
        strQtext = Parse.getDeclNmsp("ru"); AddQel(oThis.arQel, intJ, 1, "ru", "-");
        if (bAddFunctx) {strQtext += Parse.getDeclNmsp("functx"); AddQel(oThis.arQel, intJ, 1, "functx", "-");}
        if (bAddTbDef) {strQtext += Parse.getDeclNmsp("tb"); AddQel(oThis.arQel, intJ, 1, "tb", "-");}
        // Add the definitions
        strQtext += strDtext;
        for (Qel arDefThis : arDef) {
          AddQel(oThis.arQel, intJ, arDefThis.Lines, arDefThis.Type, arDefThis.Name);
        }
        // Get the correct Query file name
        // strFile = crpThis.getDstDir() + "/" + strQpfx + oThis.Name + strQext;
        strFile = fThis.nameNormalize(crpThis.getDstDir() + "/" + strQpfx + oThis.Name + strQext);
        // Read and combine the query from file
        strQtext += fThis.readFile(strFile) + "\n";
        // Add the information for this query
        int iLineCount = fThis.getLinesRead();
        AddQel(oThis.arQel, intJ, iLineCount, "query", oThis.Name);
        // Write the query back to file
        FileUtil.writeFile(strFile, strQtext, "utf-8");

        // Add the Qinfo to the array
        arQinfo[i] = oThis;
      }

      // Return positively
      return true;

    } catch (RuntimeException ex) {
      // Warn user
      DoError("Execute/IncludeDefInQueries error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }

  // ----------------------------------------------------------------------------------------------------------
  // Name :  AddQel
  // Goal :  Process one query element [objQel]
  // History:
  // 19-08-2014  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  private boolean AddQel(Qel objQel[], ByRef<Integer> intIdx , int intLines , String strType , 
                            String strName ) {
    try {
      // Validate
      if (objQel == null || intIdx.argValue < 0 || intIdx.argValue >= objQel.length) return false;
      // Process
      Qel oNew = new Qel();
      oNew.Lines = intLines;
      oNew.Name = strName;
      oNew.Type = strType;
      objQel[intIdx.argValue] = oNew;
      // Keep track of index
      intIdx.argValue++;
      // Return positively
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      DoError("Execute/AddQel error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
    
}

