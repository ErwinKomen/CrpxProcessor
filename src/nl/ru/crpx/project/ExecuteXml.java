/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.project;
// <editor-fold desc="import">
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import net.sf.saxon.Configuration;
import net.sf.saxon.FeatureKeys;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import static nl.ru.crpx.tools.FileIO.getFileNameWithoutExtension;
import static nl.ru.crpx.project.CrpGlobal.Status;
import nl.ru.util.ByRef;
import nl.ru.util.FileUtil;
import nl.ru.util.json.JSONObject;
import nl.ru.xmltools.Parse;
import nl.ru.xmltools.XqErr;
// </editor-fold>

/**
 *
 * @author Erwin R. Komen
 */
public class ExecuteXml extends Execute {
// <editor-fold desc="Variables">
  // ============= Variables that are only for the XML-processing "Execute" classes
  protected String strForTag;           // Name of the forest tag
  protected ByRef<String> strDbType;    // Kind of database
  protected Processor objSaxon;         // A saxon Processor object to host Xquery
  protected XQueryCompiler objCompiler; // Saxon query compiler
  protected DocumentBuilder objSaxDoc;  // Saxon document builder
  protected Dbase oDbase;               // Database pointer
  protected boolean bIsDbase;           // Whether this is a database or not
  protected List<String> objBack;       // Element to collect Html output
  protected List<String> objExmp;       // Element to collect examples for Html output (for the current QC step)
  protected List<String> objSubCat;     // Element to collect subcagegorisation examples for HTML output
  protected Parse objParseXq;           // Object to parse Xquery
  protected Configuration xconfig;
  protected StaticQueryContext sqc;
  // protected Extensions objExt;          // Make the extensions available
  // =========== Local variables ===============================================
  private FileUtil fHandle;             // For processing FileUtil functions
  private XqErr oXq;                    // For using XqErr
  // =========== Accessible variables ==========================================
  // public boolean bIsDbase;           // Whether this is a database or not
// </editor-fold>
  // ============= Class initializer ==========================================
  public ExecuteXml(CorpusResearchProject oProj) {
    // Do the initialisations for all Execute() classes
    super(oProj);
    // For work with Saxon Xquery see the 9.1.0.1 documentation:
    //  D:/DownLoad/XML/saxon-resources9-1-0-1/doc/using-xquery/api-query/s9api-query.html
    strDbType = new ByRef("");
    // Set the saxon processor correctly
    objSaxon = oProj.getSaxProc();
    xconfig = new Configuration();
    xconfig.setAllowExternalFunctions(true);
    sqc = new StaticQueryContext(xconfig);
    // Trace binding of external functions (see definition in CrpGlobal)
    if (bTraceXq) {
      // objSaxon.setConfigurationProperty(FeatureKeys.TRACE_EXTERNAL_FUNCTIONS, true);
      objSaxon.setConfigurationProperty(FeatureKeys.COMPILE_WITH_TRACING, true);
      objSaxon.setConfigurationProperty(FeatureKeys.LINE_NUMBERING, true);
      objSaxon.setConfigurationProperty(FeatureKeys.VALIDATION_WARNINGS, true);
      objSaxon.setConfigurationProperty(FeatureKeys.VERSION_WARNING, true);
    }
    // Set the class-internal access to FileUtil
    fHandle = new FileUtil();
    // Create an object for XqErr processing
    oXq = new XqErr();
    // Other initialisations for this class
    bIsDbase = false;
    oDbase = new Dbase();
    // Create a compiler and a document builder
    objCompiler = objSaxon.newXQueryCompiler();
    objCompiler.setCompileWithTracing(true);
    
    objSaxDoc = objSaxon.newDocumentBuilder();
    // objExt = new Extensions();
    objParseXq = new Parse(oProj, errHandle);
  }
  
// <editor-fold desc="Preparation of Xml execution">
  // ----------------------------------------------------------------------------------------------------------
  // Name :  ExecuteXmlSetup
  // Goal :  Set the contents of [arQuery] specifically for Xml projects using Xquery
  // History:
  // 13-09-2010   ERK Created for .NET in [ExecuteQueriesXqueryFast]
  // 28/apr/2015  ERK Transformed for Java
  // ----------------------------------------------------------------------------------------------------------  
  public boolean ExecuteXmlSetup() {
    String strTemp = "";    // Temporary string used to contain intermediate results
    DbLoc dbLoc;            // Database locator
    
    try {
      // Build the executable objects
      CrpGlobal.Status("Building queries...");
      for (int intI = 0; intI < arQuery.length; intI++) {
        // Show where we are
        int intPtc = (intI + 1) * 100 / arQuery.length;
        CrpGlobal.Status("Building query " + (intI + 1), intPtc);
        // Check interrupt
        if (bInterrupt) return false;
        // Build the Xquery executable for this step
        Query qThis = arQuery[intI];
        // Store the executable in the [arQuery] array
        if (! (GetExec(qThis.QueryFile, qThis))) {
          CrpGlobal.LogOutput("Problem with executable in step " + (intI + 1), true);
          oXq.XqErrShow();
          // Add the error to the error object
          errHandle.DoError(oXq.lQerr);
          return false;
        }
        // Determine trace file name
        String strTraceFile = crpThis.getDstDir() + "/" + strName + "-QC" + (intI + 1) + "-trace.txt";
        // Clear the subcategorisation collection for this QC step
  //VB TO JAVA CONVERTER TODO TASK: The following 'ReDim' could not be resolved. A possible reason may be that the object of the ReDim was not declared as an array:
        qThis.CatExamp = null;
        // Set the number of subcategories to zero here
        qThis.CatNum = 0;
        // Load the executable into a query evaluate
        qThis.Qeval = qThis.Exe.load();
        qThis.Qstring = FileUtil.readFile(qThis.QueryFile);
        // make room for examples
        qThis.Examp = new ArrayList<>();
        // Determine HTML filename for this step
        qThis.HtmlFile = FileUtil.nameNormalize(crpThis.getDstDir() + "/" + strName + "-QC" + (intI + 1) + ".html");
        // Initialise this file
        strTemp = "<html><body>QC=" + (intI + 1) + "<br>" + "\r\n";
        // .Examp.Add(strTemp)
        fHandle.writeFile(qThis.HtmlFile, strTemp, "utf-8");
      }
      // Derive the input file or the list of input files from [arQuery(0).InputFile]
      Status("Getting source file information...");
      if (!BuildSource(lSource)) {
        bInterrupt = true;
        return DoError("ExecuteXmlSetup: Could not build sources");
      }
      // Check if we have ONE source file that is a <CrpOview> one
      if (lSource.size()==1) {
        // Check existence of this one file
        File fThis = new File(lSource.get(0));
        if (fThis.exists()) {
          // Initialize database processing
          // TODO: implement
          oDbase.DbaseQueryInit(fThis, strDbType);
          // Check the type of the file
          bIsDbase = (strDbType.argValue == "CrpOview");
          // Read first record (if any)
          // TODO: implement
          // DbaseQueryRead(ndxDbRes)
        }
      }
      // Start up output/overview counting
      // TODO: implement
      // InitOviewOutHeader(strTemp, bIsDbase, pdxDbase)
      
      // Initialize the list of strings we are going to return
      objBack = new ArrayList<>();
      objBack.add(strTemp);
      // Save the first results into [strBack] (see 2674-2676 in [modMain])
      // TODO: find a better method to deal with this...
      
      // Reset the query examples
      for (Query arQuery1 : arQuery) arQuery1.Examp.clear();
      
      // Initialize the database-locator
      dbLoc = new DbLoc();
      
      // Initialize the lexeme-collector
      // lexCol = new LexCol();
      
      // Note: skip period initialisation and the For-Each-Period loop
      
      // If the input is a database, then adapt source files...
      if (bIsDbase) {
        // DO the source file adaptations
        // if (!BuildSourceDbase(arSource, pdxDbase)) {
        //   // Warn the user
        //   DoError("There is a problem building the source files from the database");
        //   return false;
        // }
      }

      // TODO: ga verder bij regel #2728 in ExecuteQueriesXqueryStream
      
      // Return positively
      return true;
    } catch (RuntimeException ex) {
      // Show error
      DoError("ExecuteXml/ExecuteXmlSetup error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Support functions">
  // ----------------------------------------------------------------------------------------------------------
  // Name :  GetExec
  // Goal :  Compile the Xquery text file into an executable Saxon object
  // History:
  // 13-09-2010  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  private boolean GetExec(String strQfile, Query objQuery) {
    // StaticError objErr;               //  To try and figure out static errors
    String strError;                  // String rep of error
    List<Exception> errorList;        // List of errors
    TransformerException exThis;      // Exceptions in the transformation process
    MyErrorListener listener = null;  // Keep track of errors
    String qText;                     // The complete query as string
    ByRef<String> strDQname;          // The name of the part (query/def) where the error occurs
    ByRef<String> strType;            // The part of the query (def/ru/qry) where the error occurs
    ByRef<Integer> intLocalPos;       // The line number (within query or definition) where the error occurs
    ByRef<Integer> intCol;            // Column where error occurs

    try {
      // Transform the Query into an executable
      // (1) Create error listener
      listener = new MyErrorListener();
      // (2) Attach the error listener to the compiler
      objCompiler.setErrorListener(listener);
      
      // Store the query file as string into [objQuery.exe]
      qText = fHandle.readFile(strQfile);
      objQuery.Exe = objCompiler.compile(qText);
      // Alternative method: XQueryExpression
      // objQuery.Exp = sqc.compileQuery(qText);     
      // Return success
      return true;
    } catch (SaxonApiException ex) {
      // For debugging...
      // strError = ex.getMessage();
      // Start the error message string
      String strFile = getFileNameWithoutExtension(strQfile);
      strError = "Error executing query " + strFile + ":\r\n";
      // Do we have an errorlist?
      if (listener == null) return false;
      errorList = listener.getErrList();
      if (errorList.isEmpty())
        strError += "No errorlist: "+ ex.getMessage() + "\r\n";
      else {
        // Initialisations
        strType = new ByRef(""); intLocalPos = new ByRef(0); intCol  = new ByRef(0);
        strDQname = new ByRef("");
        // Walk through all the errors
        for (int i=0; i< errorList.size(); i++) {
          // Get the line number
          int iLineNum = listener.lineNum(i);
          // Properly initialize
          intLocalPos.argValue = 0; 
          intCol.argValue = listener.colNum(i);
          // Determine where this error is occurring
          String strLoc = oXq.getXqErrLoc(arQinfo, strFile, iLineNum, strType, strDQname, intLocalPos, intCol);
          // NOTE: some of the results return ByRef in [strType], [strDQname]
          // Add this error to the list
          oXq.AddXqErr(strType, strDQname, intLocalPos, intCol, listener.getMsg(i), strFile);
        }
      }
      // Make sure interrupt is set
      bInterrupt = true;
      return false;
    } catch (RuntimeException ex) {
      // Show error
      DoError("ExecuteXml/GetExec error: " + ex.getMessage() + "\r\n");
      ex.printStackTrace();
      // Return failure
      return false;
    }
  }
  
  /**
   * getErrorLoc
   *    Given an error in query [strFile], at context [iLine, iCol]
   *    provide a JSON error object containing the type (def/query)
   *    and the position within this item
   * 
   * @param strFile
   * @param iLine
   * @param iCol
   * @return 
   */
  public JSONObject GetErrorLoc(String strFile, String sMsg, int iLine, int iCol) {
    ByRef<String> strType = new ByRef("");
    ByRef<String> strDQname = new ByRef("");
    ByRef<Integer> intLocalPos = new ByRef(0);
    ByRef<Integer> intLocalCol = new ByRef(iCol);
    JSONObject oBack;
    
    try {
      // Determine where this error is occurring
      String strLoc = oXq.getXqErrLoc(arQinfo, strFile, iLine, strType, strDQname, intLocalPos, intLocalCol);
      // NOTE: some of the results return ByRef (e.g [strType], [strDQname])
      // Get the JSON error object
      oBack = oXq.getXqErrObject(strType.argValue, strDQname.argValue, 
              intLocalPos.argValue, intLocalCol.argValue, sMsg, strFile);
    
      // Return the result
      return oBack;
    } catch (Exception ex) {
      errHandle.DoError("ExecuteXml/GetErrorLoc", ex);
      return null;
    }
  }
  
  // ----------------------------------------------------------------------------------------------------------
  // Name :  BuildSource
  // Goal :  Build an array of source files 
  //         This assumes that the "InputFile" consists of only one element
  //           (but one with a wildcard)
  //         It also assumes that the *first* query receives input
  // History:
  // 13-09-2010  ERK Created for .NET
  // 30/apr/2015 ERK Adapted for Java
  // ----------------------------------------------------------------------------------------------------------
  private boolean BuildSource(List<String> lInputFiles) {
    String sInputFile;  // The input file we are working with
    int intI = 0;       // Counter

    try {
      // Derive the input file or the list of input files from [arQuery(0).InputFile]
      sInputFile = arQuery[0].InputFile;
      // (1) Does the input file consist of wildcards?
      if (sInputFile.contains("*")) {
        // Check if this contains [*.] --> that should be replaced with [*?.]
        if (sInputFile.contains("*.")) {
          // Replace it
          sInputFile = sInputFile.replace("*.", "*?.");
        }
        // There are wildcards, so construct the inputfiles now
        // Look for all relevant files starting from [SrcDir]
        Path pStart = Paths.get(crpThis.getSrcDir().getAbsolutePath());
        FileUtil.getFileNames(lInputFiles, pStart, sInputFile);
        // Throw out ALL files that do not strictly adhere to the output extension criteria
        // There should be at least 1 file
        if (lInputFiles.isEmpty()) {
          // Give appropriate error message
          String sMsg = "Neither the input directory that you have specified, nor its sub-directories contain any .psdx files:" + 
                  "\r\n" + "\r\n" + "\t" + crpThis.getSrcDir() + "\r\n" + "\r\n" + 
                  "(Note: input specification used is [" + sInputFile + "])";
          logger.error(sMsg);
          errHandle.DoError(sMsg);
          errHandle.bInterrupt = true;
          bInterrupt = true;
          return false;
        }
      } else {
        // (2) There is no wildcard, so fill [lInputFiles] with individual files
        //     Note that @sInputFiles contains the files separated by semicolon
        String[] arList = sInputFile.split("[;]", -1);
        // Add all the files individually
        for (intI = 0; intI < arList.length; intI++) {
          // Add this file
          lInputFiles.add(FileUtil.nameNormalize(crpThis.getSrcDir() + 
                  "/" + arList[intI]));
        }
      }
      // Return success
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      DoError("ExecuteXml/BuildSource error: " + ex.getMessage() + "\r\n");
      bInterrupt = true;
      // Return failure
      return false;
    }
  }
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="ErrorListener class">
  public class MyErrorListener implements ErrorListener {
    // private ExecuteXml parent;
    List<Exception> errorList;  // List of all errors
    List<Integer> lineList;     // List of line numbers
    List<Integer> colList;      // List of column  numbers
    List<Boolean> fatalList;    // Fatal error lists

    private MyErrorListener() {
      // this.parent = parent;
      errorList = new ArrayList<>();
      lineList = new ArrayList<>();
      colList = new ArrayList<>();
      fatalList = new ArrayList<>();
    }
    @Override
    public void error(TransformerException exception) {
      errorList.add(exception);
      SourceLocator sl = exception.getLocator();
      if (sl == null) {
        lineList.add(-1);
        colList.add(-1);
      } else {
        lineList.add(sl.getLineNumber());
        colList.add(sl.getColumnNumber());
      }
      fatalList.add(false);
    }
    @Override
    public void fatalError(TransformerException exception)  {
      errorList.add(exception);
      SourceLocator sl = exception.getLocator();
      if (sl == null) {
        lineList.add(-1);
        colList.add(-1);
      } else {
        lineList.add(sl.getLineNumber());
        colList.add(sl.getColumnNumber());
      }
      
      // throw exception;
      fatalList.add(true);
    }
    @Override
    public void warning(TransformerException exception) {
      // no action
      lineList.add(-1);
      colList.add(-1);
      fatalList.add(false);
    }
    public int lineNum(int i) {return lineList.get(i);}
    public int colNum(int i) {return colList.get(i);}
    public boolean getFatal(int i) {return fatalList.get(i);}
    public String getMsg(int i) {return errorList.get(i).getMessage();}
    public List<Exception> getErrList() {return errorList;}
  }
// </editor-fold>
}
