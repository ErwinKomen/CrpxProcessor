/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.project;
// <editor-fold desc="import">
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
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
import nl.ru.crpx.cmd.CrpxProcessor;
import static nl.ru.crpx.tools.FileIO.getFileNameWithoutExtension;
import static nl.ru.crpx.project.CrpGlobal.Status;
import nl.ru.crpx.search.JobXq;
import nl.ru.crpx.tools.FileIO;
import nl.ru.util.ByRef;
import nl.ru.util.FileUtil;
import nl.ru.util.Json;
import nl.ru.util.StringUtil;
import nl.ru.util.json.JSONArray;
import nl.ru.util.json.JSONObject;
import nl.ru.xmltools.Parse;
import nl.ru.xmltools.XmlNode;
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
  protected ByRef<XmlNode> ndxDbRes;    // Pointer to current database node
  protected Parse objParseXq;           // Object to parse Xquery
  protected Configuration xconfig;
  protected StaticQueryContext sqc;
  protected JSONArray arMetaInfo = null; // THe information in loc_sMetaInfo
  // protected Extensions objExt;          // Make the extensions available
  // =========== Local variables ===============================================
  private FileUtil fHandle;             // For processing FileUtil functions
  private XqErr oXq;                    // For using XqErr
  private Random r;
  // =========== Accessible variables ==========================================
  // public boolean bIsDbase;           // Whether this is a database or not
// </editor-fold>
  // ============= Class initializer ==========================================
  public ExecuteXml(CorpusResearchProject oProj) {
    // Do the initialisations for all Execute() classes
    super(oProj);
    // Validate: check for errors
    if (errHandle.hasErr()) return;

    try {
      // Read the JSON from a local file
      InputStream is = FileIO.getProjectDirectory(CrpxProcessor.class, "nl/ru/xmltools/metaelement.json.txt");
      if (is==null) {
        // Provide an error message
        errHandle.DoError("nl.ru.xmltools.Parse: cannot find file [metaelement.json.txt]");
      } else {
        this.arMetaInfo = Json.readArray(is);
      }
    } catch (Exception ex) {
      errHandle.DoError("Cannot initialize the [ExecuteXml] class: ", ex, ExecuteXml.class);
    }
    
    
    // Perform saxon-specific initialisations
    initSaxon(oProj);
  }
  
// <editor-fold desc="Initialisation of working with Saxon Xquery:>
  /**
   * initSaxon
   *  Initialise a Saxon machine
   * 
   * @param oProj 
   */
  private void initSaxon(CorpusResearchProject oProj) {
    try {
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
      oDbase = new Dbase(oProj);
      // Create a compiler and a document builder
      objCompiler = objSaxon.newXQueryCompiler();
      objCompiler.setCompileWithTracing(true);

      objSaxDoc = objSaxon.newDocumentBuilder();
      // objExt = new Extensions();
      objParseXq = new Parse(oProj, errHandle);    
    } catch (Exception ex) {      
      // Show error
      errHandle.DoError("ExecuteXml/initSaxon error: ", ex);
    }
  }
// </editor-fold>
  
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
        FileUtil oFU = new FileUtil();
        qThis.Qstring = oFU.readFile(qThis.QueryFile);
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
        return errHandle.DoError("ExecuteXmlSetup: Could not build sources");
      }
      // Check if we have ONE source file that is a <CrpOview> one
      if (lSource.size()==1 && lSource.get(0).endsWith(".xml") && !lSource.get(0).endsWith(".folia.xml")) {
        // Check existence of this one file
        File fThis = new File(lSource.get(0));
        if (fThis.exists()) {
          // Initialize database processing
          // TODO: implement
          oDbase.DbaseQueryInit(fThis, strDbType, (JobXq) this.objSearchJob);
          // Check the type of the file
          bIsDbase = (strDbType.argValue.equals("CrpOview"));
          // Read first record (if any)
          // SHOULD I read the first element here? 
          // -- probably not if I want to spread out calculation across files...
          // oDbase.DbaseQueryRead(ndxDbRes);
          // Set the source file list to the list of files referred to from the database
          lSource = oDbase.getDbFiles();
          // Make sure the CRP is recognizable as having a database as input
          this.crpThis.HasDbaseInput = true;
        } else {
          // The requested input file does *not* exist: return error to the caller
          bInterrupt = true;
          return errHandle.DoError("ExecuteXmlSetup: the requested input file does not exist:\n" + 
                  fThis.getAbsolutePath() + "\n" + 
                  "Perhaps you have not run the project to create a database yet?");
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
      for (Query objOneQuery : arQuery) objOneQuery.Examp.clear();
      
      // Initialize the database-locator
      dbLoc = new DbLoc();
      
      // Initialize the lexeme-collector
      // lexCol = new LexCol();
      
      // Note: skip period initialisation and the For-Each-Period loop
      

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
  
  /**
   * getResultXml
   *    Create one dtabase <Result> line on the basis of the filename, the textId
   *      and the JSON object that has been produced during execution
   *    
   * @param sFileName
   * @param sTextId
   * @param sSubType
   * @param lstFtInfo
   * @param oXqf      - Container object of one hit that holds several parts
   * @param iResId
   * @param oResult   - Return the results for this line
   * @return 
   */
  public String getResultXml(String sFileName, String sTextId, String sSubType, 
          List<JSONObject> lstFtInfo, JSONObject oXqf, int iResId,
          ByRef<JSONObject> oResult) {
    StringBuilder bThis = new StringBuilder();
    
    try {
      String sSearch = (oXqf.has("locl")) ? oXqf.getString("locl") : "";
      String sCat = (oXqf.has("cat")) ? StringUtil.escapeXmlChars(oXqf.getString("cat")) : "";
      String sContext = (oXqf.has("con")) ? StringUtil.escapeXmlChars(oXqf.getString("con")) : "";
      String sSyntax = (oXqf.has("syn")) ? StringUtil.escapeXmlChars(oXqf.getString("syn")) : "";
      String sEnglish = (oXqf.has("eng")) ? StringUtil.escapeXmlChars(oXqf.getString("eng")) : "";
      String sMsg = (oXqf.has("msg")) ? oXqf.getString("msg") : "";
      
      // Store results so far
      oResult.argValue = new JSONObject();
      oResult.argValue.put("ResId", iResId);
      oResult.argValue.put("File", sFileName);
      oResult.argValue.put("TextId", sTextId);
      oResult.argValue.put("Search", sSearch);
      oResult.argValue.put("Cat", sCat);
      oResult.argValue.put("Locs", oXqf.getString("locs"));
      oResult.argValue.put("Locw", oXqf.getString("locw"));
      oResult.argValue.put("Notes", "-");
      oResult.argValue.put("SubType", sSubType);
      
      // Add the opening <Result> one
      bThis.append("<Result ResId=\"" + iResId + "\" File=\"").append(sFileName + "\" " + 
        "TextId=\"" + sTextId + "\" " +
        "Search=\"" + sSearch + "\" " +
        "Cat=\"" + sCat + "\" " +
        "forestId=\"" + oXqf.getString("locs") + "\" " +
        "eTreeId=\"" + oXqf.getString("locw") + "\" " +
        "Notes=\"-\" " +
        "Period=\"" + StringUtil.escapeXmlChars(sSubType) + "\">\n");
      // Add underlying nodes: Text, Psd, Pde
      bThis.append("  <Text>").append(sContext).append("</Text>\n" );
      bThis.append("  <Psd>").append(sSyntax).append("</Psd>\n" );
      bThis.append("  <Pde>").append(sEnglish).append("</Pde>\n" );
      // Start adding underlying <Feature> nodes
      String[] arFs = sMsg.split(";",-1);
      JSONArray arFeats = new JSONArray();
      // Walk through the list of Feature Info JSON objects
      for (int q=0;q<lstFtInfo.size(); q++) {
        // Get the number of this feature
        int iFtNum = lstFtInfo.get(q).getInt("FtNum");
        // Get the feature value belonging to this feature
        String sValue = "";
        if (iFtNum > 0) {
          // Check for the correct size of [arFs]
          if (arFs.length < iFtNum) {
            sValue = "(not provided - ftNum="+iFtNum+" arFs="+arFs.length+ ")";
          } else {
            sValue = StringUtil.escapeXmlChars(arFs[iFtNum-1]);
          }
        }
        // Keep for oResult
        JSONObject oFeat = new JSONObject();
        oFeat.put("Name", lstFtInfo.get(q).getString("Name"));
        oFeat.put("Value", sValue);
        arFeats.put(oFeat);
        // Store the results
        bThis.append("  <Feature Name=\"" + 
          lstFtInfo.get(q).getString("Name") + "\" Value=\"" +
          sValue + "\" />\n");
      }
      // Add ending tag for this result
      bThis.append("</Result>\n");
      oResult.argValue.put("Features", arFeats);
      // Return the result
      return bThis.toString();
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("ExecuteXml/getResultXml #1 error: ", ex);
      // Return failure
      return "";
    }
  }  
  public String getResultXml(JSONObject oRes) {
    StringBuilder bThis = new StringBuilder();
    
    try {
      // Add the opening <Result> one
      bThis.append("<Result ResId=\"1\" File\"").append(oRes.getString("File") + "\" " + 
        "TextId\"" + StringUtil.escapeXmlChars(oRes.getString("TextId")) + "\" " +
        "Search\"" + StringUtil.escapeXmlChars(oRes.getString("Search")) + "\" " +
        "Cat\"" + StringUtil.escapeXmlChars(oRes.getString("cat")) + "\" " +
        "forestId\"" + oRes.getString("locs") + "\" " +
        "eTreeId\"" + oRes.getString("locw") + "\" " +
        "Notes\"-\" " +
        "Period\"" + oRes.getString("Period") + "\">\n");
      // Add underlying nodes: Text, Psd, Pde
      bThis.append("  <Text>").append(StringUtil.escapeXmlChars(oRes.getString("Text"))).append("</Text>\n" );
      bThis.append("  <Psd>").append(StringUtil.escapeXmlChars(oRes.getString("Psd"))).append("</Psd>\n" );
      bThis.append("  <Pde>").append(StringUtil.escapeXmlChars(oRes.getString("Pde"))).append("</Pde>\n" );
      // Start adding underlying <Feature> nodes
      JSONArray arFs = oRes.getJSONArray("fs");
      for (int i=0;i< arFs.length(); i++) {
        JSONObject oOneF = arFs.getJSONObject(i);
        bThis.append("  <Feature Name=\"" + 
          StringUtil.escapeXmlChars(oOneF.getString("nme")) + "\" Value=\"" +
          StringUtil.escapeXmlChars(oOneF.getString("val")) + "\" />\n");
      }
      // Add ending tag for this result
      bThis.append("</Result>\n");
      // Return the result
      return bThis.toString();
    } catch (Exception ex) {
      // Show error
      errHandle.DoError("ExecuteXml/getResultXml #2 error: ", ex);
      // Return failure
      return "";
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
      // (2) Check if the compiler still 'exists'
      if (objCompiler == null) {
        // Double check
        if (objSaxon == null) {
          // Check project
          if (this.crpThis == null) {
            // Now we have a problem!
            errHandle.DoError("ExecuteXml/GetExec: there is no CRP anymore...");
            return false;
          } else {
            // Something really weird is going on...
            errHandle.debug("ExecuteXml/GetExec: re-initialising Saxon");
            // Re-initialise all completely
            initSaxon(this.crpThis);
          }
        } else {
          // Show what is happening
          errHandle.debug("ExecuteXml/GetExec: get fresh copy of XQueryCompiler");
          // Get a fresh copy of the compiler
          objCompiler = objSaxon.newXQueryCompiler();
        }
      }
      // (3) Attach the error listener to the compiler
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
    List<String> lAllFiles;
    String sInputFile;  // The input file we are working with
    int iSamples = -1;  // The number of samples to be taken randomly
    int iFirst = -1;    // The number of 'first' items to be taken
    int intI = 0;       // Counter

    try {
      // Derive the input file or the list of input files from [arQuery(0).InputFile]
      sInputFile = arQuery[0].InputFile;
      // Initialize a list where all files will get into
      lAllFiles = new ArrayList <>();
      // Make sure we properly initialize random
      r = new Random();
      // (1) Does the input file consist of wildcards?
      if (sInputFile.contains("*")) {
        // Check if this contains [*.] --> that should be replaced with [*?.]
        if (sInputFile.contains("*.")) {
          // Replace it
          sInputFile = sInputFile.replace("*.", "*?.");
        }
        // Look for optional search parameters
        String sJobQuery = this.objSearchJob.getJobQuery();
        if (!sJobQuery.isEmpty() && !sJobQuery.startsWith("{")) {
          sJobQuery = "{ \"name\": \"" + sJobQuery + "\"}";
        }
        JSONObject oQuery = new JSONObject(sJobQuery);
        if (oQuery.has("options")) {
          // Get the options
          JSONObject oOptions = oQuery.getJSONObject("options");
          if (oOptions.has("search_type")) {
            String sSearchType = oOptions.getString("search_type");
            if (oOptions.has("search_count")) {
              int iSubset = oOptions.getInt("search_count");
              switch(sSearchType) {
                case "random":  // Take random 'search_count' options
                  iSamples = iSubset;
                  break;
                case "first":   // Take the first 'search_count' items
                  iFirst = iSubset;
                  break;
                default:
                  // no need to do anything
                  break;
              }
            }
          }
        }
        // There are wildcards, so construct the inputfiles now
        // Look for all relevant files starting from [SrcDir]
        Path pStart = Paths.get(crpThis.getSrcDir().getAbsolutePath());
        FileUtil.getFileNames(lAllFiles, pStart, sInputFile);
        // If there are no files, then add .gz to the possibilities
        if (lAllFiles.isEmpty()) {
          sInputFile = sInputFile + ".gz";
          FileUtil.getFileNames(lAllFiles, pStart, sInputFile);
        }
        
        int numFiles = lAllFiles.size();
        
        // See how we can derive lInputFiles from lAllFiles
        if (iSamples > 0 && iSamples < numFiles) {
          // We need to take a number of samples: Get an array of indices
          // See: https://www.javamex.com/tutorials/random_numbers/random_sample.shtml
          int i = 0,
              nLeft = numFiles;
          while (iSamples > 0) {
            // Gat a random index
            int rand = r.nextInt(nLeft);
            if (rand < iSamples) {
              lInputFiles.add(lAllFiles.get(i));
              iSamples--;
            }
            nLeft--;
            i++;
          }
        } else if (iFirst>0 &&  iFirst < numFiles) {
          // Take the first [iFirst] items
          for (int i=0;i<iFirst;i++) {
            lInputFiles.add(lAllFiles.get(i));
          }
        } else {
          // Just copy lAllFiles
          lInputFiles.addAll(lAllFiles);
        }
        
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
          String sFullName = arList[intI];
          // Check if the file points to a good one already
          if ((new File(sFullName)).exists()) {
            lInputFiles.add(sFullName);
          } else {
            String sBare = FileIO.getFileNameWithoutDirectory(sFullName);
            // Check this one
            String sStandard = crpThis.getSrcDir() + "/" + sBare;
            if ( (new File(sStandard)).exists()) {
              // Add this file
              lInputFiles.add(FileUtil.nameNormalize(sStandard));
            } else {
              // Perhaps it is in the default database location?
              String sDbaseLoc = getDbaseDir() + "/" + sBare;
              if ( (new File(sDbaseLoc)).exists()) {
                // Add this file
                lInputFiles.add(FileUtil.nameNormalize(sDbaseLoc));
              } else {
                // Return a meaningful error
                return errHandle.DoError("ExecuteXml/BuildSource: could not locate file " + sBare);
              }
            }
          }          
        }
        // Adapt 
        arQuery[0].InputFile = StringUtil.join(lInputFiles, ";");
      }
      // Return success
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      StackTraceElement st[] = ex.getStackTrace();
      DoError("ExecuteXml/BuildSource error: " + ex.getMessage() + "\r\n");
      bInterrupt = true;
      // Return failure
      return false;
    }
  }
  
  
  /*
  private List<String> getNElementsBitSet(List<String> list, int n) {
    List<String> rtn = new ArrayList<>(n);
    int[] ids = generateUniformBitmap(n, 0, list.size());
    for (int i = 0; i < ids.length; i++) {
      rtn.add(list.get(ids[i]));
    }
    return rtn;
  }
  private int[] generateUniformBitmap(int N, int Max) {
        if (N > Max) throw new RuntimeException("not possible");
        int[] ans = new int[N];
        if (N == Max) {
            for (int k = 0; k < N; ++k)
                ans[k] = k;
            return ans;
        }
        BitSet bs = new BitSet(Max);
        int cardinality = 0;
        while (cardinality < N) {
            int v = rand.nextInt(Max);
            if (!bs.get(v)) {
                bs.set(v);
                cardinality++;
            }
        }
        int pos = 0;
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            ans[pos++] = i;
        }
        return ans;
  }  */
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
