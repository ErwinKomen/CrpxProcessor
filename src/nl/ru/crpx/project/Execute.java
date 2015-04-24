/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.project;
// Which methods need to be imported
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import static nl.ru.crpx.project.ExecuteFoliaFast.ExecuteQueriesFoliaFast;
import static nl.ru.crpx.project.ExecuteFoliaStream.ExecuteQueriesFoliaStream;
import static nl.ru.crpx.project.ExecutePsdxFast.ExecuteQueriesPsdxFast;
import static nl.ru.crpx.project.ExecutePsdxStream.ExecuteQueriesPsdxStream;
import nl.ru.crpx.tools.FileIO;
import nl.ru.crpx.tools.General;
import static nl.ru.crpx.tools.General.DoError;
import static nl.ru.crpx.tools.General.Status;
import static nl.ru.crpx.tools.General.getCurrentTimeStamp;
import static nl.ru.crpx.tools.General.getJsonString;
import nl.ru.util.json.JSONObject;
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
public class Execute {
  protected static final Logger logger = Logger.getLogger(Execute.class);
  protected CorpusResearchProject crpThis;
  protected General objGen;
  // ===================== The elements of an execution object =================
  protected Query[] arQuery;      // Array of queries to be executed (so this is the constructor) 
  protected Qinfo[] arQinfo;      // Information for each *query* (not query-line)
  protected Result objRes;        // Make room for results
  protected String[] arInput;     // Array of input files
  protected boolean bKeepGarbage; // Retain the in-between files for inspection
  protected String strQext;       // Extension of queries
  protected String strDext;       // Extension of definitions
  // ===================== Local stuff =========================================
  private String TEMP_FILE = "CrpTemp";
  
  // Initialisation of the Execute class
  public Execute(CorpusResearchProject prjThis, General oGen) {
    String strInput = "";           // Source files
    boolean bXmlData = false;       // Whether this is XML data (from Xquery) or Treebank
    boolean bShowPsd = false;       // Whether PSD should be shown or not

    // Perform class initialisations that are valid for all extended classes
    this.crpThis = prjThis;       // Keep a local copy of the project
    this.objGen = oGen;           // Keep a local copy of the GENERAL object
    this.bKeepGarbage = false;    // Do NOT normally retain temporary files
    // Validate: are there any query lines in here?
    int arSize = prjThis.getListQCsize();
    if (arSize == 0) {
      // Check if there are any queries at all
      if (prjThis.getListQuerySize() ==0) {
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
    // Reset interruption
    objGen.setInterrupt(false);
    // Reset output file initialisation
    // TODO: RuInit(false);
    
    // Make sure adaptations are processed
    // TODO: AdaptQnameToQC()
    
    // Clear the output tabpage
    // TODO: Me.tbOutput.Text = ""
    
    // Gather the source files and validate the result
    strInput = prjThis.getSource();   // Usually this is [*.psdx] or something like that
    if (strInput.isEmpty()) {DoError("First choose input files"); objGen.setInterrupt(true); return;}
    // Make an array of input files, as defined in [strInput]
    arInput = strInput.split(objGen.GetDelim(strInput));
    
    // Which tab is selected?
    // TODO: automatically switch to the correct tab
    
    // Clear the monitor-log first
    // TODO: clear monitor log (or do that on the client-side in JS??)
    
    // Make a new results object
    this.objRes = new Result(oGen);
    
    // Get Qext and Dext
    strQext = crpThis.getPrjTypeManager().getQext();
    strDext = crpThis.getPrjTypeManager().getDext();
    
  }
          
  /* ---------------------------------------------------------------------------
     Name:    ExecuteQueries
     Goal:    Execute the queries defined in @prjThis
              Make a query list (in [arQuery]), and execute the queries in the given order
              An array with the source files [arInput] should have been defined
                at the creation of an instance of this class
     History:
     29-07-2009  ERK Created for .NET
     20/apr/2015   ERK Start transfer to Java
     --------------------------------------------------------------------------- */
  public boolean ExecuteQueries() {
    // DataTable tblQc = null;       // Points to the [QueryConstructor]
    // DataRow[] dtrQc = null;       // Sorted query constructor
    String strInput = null;       // Temporary storage of input files
    String strQtemp = null;       // Prefix to query file to indicate it is temporary
    String strSrcName = "";       // Name of the combined Source input file
    List<String> lSources = null; // All source files together in a List
    String[] arInput = null;      // Array of the input information
    int intI = 0;                 // Counter
    int intOviewLine = 0;         // The line number in the overview
    int intWait = 0;              // Amount of milliseconds to wait before continuing
    int iSize = 0;                // Size of a table
    boolean bIsXquery = false;    // Whether the engine is an Xquery type
    boolean bDoStream = false;    // Whether execution should take place stream-wise
    boolean bErrors = false;      // Did we encounter any errors?
    boolean bSrcBuilt = false;    // Whether a Source.xml has been built or not yet
    boolean bResult = true;       // The result of execution
    
    // Validate
    if (this.crpThis == null) return false;
    // Initialisations
    strQtemp = "temp_";                     // Prefix to temporary file
    intWait = 500;                          // TODO: Waiting time for...
    this.objGen.setInterrupt(false);        // Clear interrupt flag
    bDoStream = this.crpThis.getStream();   // Execute streaming mode or not
    objGen.setCurrentPsdx("");              // Indicate that there is no currently loaded file
    objGen.ru_bFileSaveAsk = false;         // Needed for function ru:setattrib()
    
    // Initialise syntax error dataset
    // TODO: InitXqErr()
    
    // Combine all source files into one List
    lSources = new ArrayList<>();
    for (int i=0;i<this.arInput.length;i++) {
      // Check if this is not empty
      if ( ! this.arInput[i].trim().equals("")) {
        // Add this to the list of sources
        lSources.add(this.arInput[i].trim());
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
      oQuery.ErrorFile = this.crpThis.getDstDir().getAbsolutePath() + 
              "/ErrorLogStep" + (i + 1) + ".txt";
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
      oQuery.InputFile = strInput;
      // Set the name of the query file
      oQuery.QueryFile = this.crpThis.getDstDir().getAbsolutePath() + 
              "/" + strQtemp + getJsonString(oThis, "Query") + strQext;
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
    
    // Indicate that we are starting execution of a CRP query series
    Status("Starting execution of queries at: " + getCurrentTimeStamp());
    
    // Check what kind of project this is
    switch (this.crpThis.getProjectType()) {
      case "Xquery-psdx": // Okay, we are able to process this kind of project
        // Do we need to do streaming or not?
        if (bDoStream) {
          bResult = ExecuteQueriesPsdxStream();
        } else {
          bResult = ExecuteQueriesPsdxFast();
        }
        break;
      case "FoLiA-xml":
        // Do we need to do streaming or not?
        if (bDoStream) {
          bResult = ExecuteQueriesFoliaStream();
        } else {
          bResult = ExecuteQueriesFoliaFast();
        }
        break;
      default: 
        bResult = DoError("Sorry, cannot execute projects of type [" + this.crpThis.getProjectType() + "]");
    }
    
    // Return the result of this operation
    return bResult;
  }
  
  /* ---------------------------------------------------------------------------
     Name:    VerifyQC
     Goal:     Check for problems in the [Output] and [Result] fields of the QCeditor
     History:
     10-02-2011  ERK Created for .NET CorpusStudio
     21/apr/2015 ERK transformed to Java CRPP
     --------------------------------------------------------------------------- */
  public boolean VerifyQC() {
    List objTag = new ArrayList<String>();  // Keep track of Result objects
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
      String sFile = crpThis.getDstDir().getAbsolutePath() + "/" + strPrefix +
        elThis.getString("Name") + strExt;
      // Store the contents of this element as an appropriately called file
      try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(sFile, true)))) {
        out.println(elThis.getString("Text"));
      } catch (IOException e) {
        // Transfer the exception
        logger.error("ExtractFiles: cannot write. " + e.getMessage());
      }
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
    String strRuDef = "declare namespace ru = 'clitype:CorpusStudio.RuXqExt.RU?asm=CorpusStudio';\r\n";
    String strTbDef = "declare namespace tb = 'http://erwinkomen.ruhosting.nl/software/tb';\r\n";
    String strFunctxDef = "declare namespace functx = 'http://www.functx.com';\r\n";
    
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
    for (JSONObject oDef: lDef) {
      // Get the correct file name
      strFile = crpThis.getDstDir().getAbsolutePath()+ "/" + strDpfx + oDef.getString("Name") +
              strDext;
      FileIO fThis = new FileIO();
      try {
        // Add the content to the text of the combined definitions
        strDtext += fThis.readFile(strFile);
      } catch (IOException ex) {
        logger.error("Could not read definition file " + strFile,ex);
        return false;
      }
      // Create a Qel element
      Qel qElThis = new Qel();
      qElThis.Type = "def";
      qElThis.Lines = fThis.getLinesRead();
      qElThis.Name = oDef.getString("Name");
    }
    // Check presence of tb and functx definition
    bAddFunctx = !strDtext.contains("declare namespace functx");
    bAddTbDef = !strDtext.contains("declare namespace tb");
    // Calculate the size for each array of Qel's
    intSize = 1 + ((bAddFunctx) ? 1 :  0) + ((bAddTbDef) ? 1 : 0) + lDef.size() + 1;
    // Make room for the query information
    lQuery = crpThis.getTable("Query");
    arQinfo = new Qinfo[lQuery.size()];
    
    // Return positively
    return true;
  }
}

