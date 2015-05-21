/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPathExpressionException;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import nl.ru.crpx.search.Job;
import nl.ru.crpx.search.JobXq;
import nl.ru.crpx.search.JobXqF;
import nl.ru.crpx.search.QueryException;
import nl.ru.crpx.search.SearchParameters;
import nl.ru.crpx.xq.CrpFile;
import nl.ru.crpx.xq.RuBase;
import nl.ru.util.ByRef;
import nl.ru.util.StringUtil;
import nl.ru.util.json.JSONArray;
import nl.ru.util.json.JSONObject;
import nl.ru.xmltools.Parse;
import nl.ru.xmltools.XmlForest;
import nl.ru.xmltools.XmlForest.ForType;
import nl.ru.xmltools.XmlNode;
import org.w3c.dom.Node;

/**
 *
 * @author E.R.Komen
 */
/* ---------------------------------------------------------------------------
   Class:   ExecutePsdxStream
   Goal:    CRP execution for .psdx files forest-by-forest
   History:
   20/apr/2015   ERK Created
   --------------------------------------------------------------------------- */
public class ExecutePsdxStream extends ExecuteXml {
  // ============ Local variables for "Xq" =====================================
  List<JobXqF> arJob = new ArrayList<>(); // A list of all the current XqF jobs running
  List<String> arRes = new ArrayList<>(); // The results of each job
  JSONArray arCount = new JSONArray();    // Array with the counts per file
  // ========== constants ======================================================
  private static final QName loc_xq_EtreeId = new QName("", "", "TreeId");
  private static final QName loc_xq_Section = new QName("", "", "Section");
  private static final QName loc_xq_Location = new QName("", "", "Location");

  // ============ Local variables for "XqF" ====================================
  
  /* ---------------------------------------------------------------------------
  Name:    ExecutePsdxStream
  Goal:    Perform initialisations needed for this class
  Note:    We go file by file, <forest> by <forest>, and execute the queries
              consecutively on this <forest> element.
           This method does *NOT* allow resolving references across the <forest> borders to be pursued
           This procedure also adds the RESULTS in *xml* form ONLY
           This procedure uses a StreamReader for the XML files
  History:
  20/apr/2015   ERK Created
  ------------------------------------------------------------------------------ */
  public ExecutePsdxStream(CorpusResearchProject oProj) {
    // Do the initialisations for all Execute() classes
    super(oProj);
    
    
    // TODO: make sure we provide [intCurrentQCline] with the correct value!!
  }

// <editor-fold desc="Part 1: Xq">
  /* ---------------------------------------------------------------------------
     Name:    ExecuteQueries
     Goal:    Execute the queries in the given order for Xquery type processing
     History:
     18-03-2014  ERK Created with elements from [ExecuteQueriesXqueryFast] for .NET
     23/apr/2015 ERK Transformation into Java started
     --------------------------------------------------------------------------- */
  @Override
  public boolean ExecuteQueries(Job jobCaller)  {
    int iCrpFileId; // Index of CrpFile object
    
    try {
      // Set the job for global access within Execute > ExecuteXml > ExecutePsdxStream
      this.objSearchJob = jobCaller;
      /* ========= Should not be here
      // Set the XmlForest element correctly
      this.objProcType = new XmlForest(this.crpThis,(JobXq) jobCaller, this.errHandle);
      // Make sure 
      this.objProcType.setProcType(ForType.PsdxPerForest);
      ========================== */
      // Perform general setup
      if (!super.ExecuteQueriesSetUp()) return false;
      // Perform setup part that is specifically for Xml/Xquery
      if (!super.ExecuteXmlSetup()) return false;
      
      // Initialise the job array and the results array
      arJob.clear();  arRes.clear();
      
      // Visit all the source files stored in [lSource]
      for (int i=0;i<lSource.size(); i++) {
        // Take this input file
        File fInput = new File(lSource.get(i));
        // Add the combination of File/CRP to the stack
        CrpFile oCrpFile = new CrpFile(this.crpThis, fInput, objSaxon, (JobXq) jobCaller);
        RuBase.setCrpCaller(oCrpFile);
        // Get the @id of this combination
        iCrpFileId = RuBase.getCrpCaller(oCrpFile);
        // Add the id to the search parameters
        SearchParameters searchXqFpar = new SearchParameters(this.searchMan);
        searchXqFpar.put("crpfileid", Integer.toString(iCrpFileId));
        // Also add the "query" parameter
        searchXqFpar.put("query", "{\"crp\": \"" + crpThis.getName() + "\"" +
                ", \"file\": \"" + fInput.getName() + "\"" + "}");
        // Keep track of the old jobs and make sure not too many are running now
        if (!monitorXqF(this.iMaxParJobs)) {
          // Getting here means that we are UNABLE to wait for the number of jobs
          //  of this user to decrease below @iMaxParJobs
          return errHandle.DoError("ExecuteQueries: unable to get below max #jobs " + 
                  this.iMaxParJobs, ExecutePsdxStream.class);
        }
        // Create a job for this Crp/File treatment
        JobXqF search = null;
        try {
          // Initiate the XqF job
          search = searchMan.searchXqF(crpThis, userId, searchXqFpar);
        } catch (QueryException | InterruptedException ex) {
          // Return error and failure
          return errHandle.DoError("Failed to execute file ", ex, ExecutePsdxStream.class);
        }
        
        // Get the @id of the job that has been created
        String sThisJobId = search.getJobId();
        String sNow = Job.getCurrentTimeStamp();
        // Additional debugging to find out where the errors come from
        logger.debug("XqFjob [" + sNow + "] userid=[" + userId + "] jobid=[" + 
                sThisJobId + "], finished=" + 
                search.finished() + " status=" + search.getJobStatus() );
        
        // Add the job to the list of jobs for this project/user
        arJob.add(search);
      }
      // Monitor the end of the jobs
      if (!monitorXqF(1)) return false;
      
      // TODO: combine the results of the queries
      // Combine [arRes] into a result string (JSON)
      String sCombiJson = "[" + StringUtil.join(arRes, ",") + "]";
      jobCaller.setJobResult(sCombiJson);

      // Also combine the job count results
      JSONObject oCount = new JSONObject();
      oCount.put("counts", arCount);
      jobCaller.setJobCount(oCount);
      
      // Return positively
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      DoError("ExecutePsdxStream/ExecuteQueries error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
  
  /**
   * monitorXqF - Process and monitor jobs of XqF type until @iuntil are left.
   * Traverse the stack of jobs and when one is finished:
   * 1) gather its results
   * 2) take it from the [arJob] list
   * 
   * @param iUntil
   * @return 
   */
  private boolean monitorXqF(int iUntil) {
    try {
      // Loop while the number of jobs is larger than the maximum
      while (arJob.size() >= iUntil) {
        // Visit all jobs
        for (int i = 0; i<arJob.size(); i++ ) {
          // Get this XqF job
          JobXqF jThis = arJob.get(i);
          // Is it finished?
          if (jThis.finished()) {
            // It is ready, so gather its results
            String sResultXqF = jThis.getJobResult();
            // Process the job results
            arRes.add(sResultXqF);
            arCount.put(jThis.getJobCount());
            // We have its results, so take it away from our job list
            arJob.remove(jThis);
          }
        }
      }
      // Return success
      return true;
    } catch (Exception ex) {
      // Return failure
      return errHandle.DoError("monitorXqF failure", ex, ExecutePsdxStream.class);
    }
  }
// </editor-fold>

// <editor-fold desc="Part 2: XqF">
  /** 
   * ExecuteQueriesFile - Execute all the queries in [arQuery] on one file
   * 
   * Assumptions: the file does indeed need to be treated.
   *              the file is in a 'local' place (local HD)
   * 
   * Note: communication with extended functions ("Extensions") goes
   *       via the [oCrpFile] objectß
   * 
   * @param jobCaller
   * @param iCrpFileIdx
   * @return 
   */
  public boolean ExecuteQueriesFile(Job jobCaller, int iCrpFileIdx) {
    int intNumForest=0;         // Forest number we are processing
    int intLastId;              // 
    int intHitsTotal = 0;       // Total number of hits so far
    int intOviewLine;           // The target overview line where we have to store results
    int intCatLine;             // Where we are in the subcategorization
    boolean bDoForest = false;  // Should we process the current <forest> node?
    boolean bHasInput = false;  // Does this line have input?
    boolean bParsed = false;    // Line has been parsed
    String strForestFile;       // Name of this file
    String strExpPsd;           // 
    String strExpText;          // 
    String strTreeId;           // The @id of the Node that results from parsing
    String strEtreeMsg;         // Message attached to the found result
    String strEtreeCat;         // Subcategorization attached to found result
    String strEtreeDb;          // Database values attached to found result
    String strForestId;         // String representation of the forest id
    String sSeg;                // DEBUGGING
    boolean[] arOutExists;      // Array signalling that output on step i exists
    boolean[] arCmpExists;      // Array signalling that output on step i exists
    List<XmlNode> ndxDbList;    // All nodes for current text/forest combination
    XmlNode ndxDbPrev;          // Previous database location
    XmlNode ndxForestBack;      // Forest inside which the resulting [eTree] resides
    Parse objParse;             // Functions to parse and get results
    ByRef<XmlNode> ndxForest;   // Forest we are working on
    ByRef<XmlNode> ndxHeader;   // Header of this file
    ByRef<XmlNode> ndxDbRes;    // Current result
    ByRef<Integer> intForestId; // ID (numerical) of the current <forest>
    ByRef<Integer> intPtc;      // Percentage of where we are
    JSONArray colParseJson;     // Array with json results
    XmlForest objProcType;      // Access to the XmlForest object allocated to me
    JSONArray[] arXqf;          // An array of JSONArray results for each QC item
    
    // Note: this uses [objProcType, which is a 'protected' variable from [Execute]
    try {
      // Get the CrpFile object
      CrpFile oCrpFile = RuBase.getCrpFile(iCrpFileIdx);
      // Get the file
      File fThis = oCrpFile.flThis;
      // Initialisations
      objProcType = oCrpFile.objProcType;
      ndxForest = new ByRef(null); ndxDbRes = new ByRef(null);
      ndxHeader = new ByRef(null);
      strForestFile = fThis.getAbsolutePath();
      intForestId = new ByRef(-1);
      intPtc = new ByRef(0);
      ndxDbList = new ArrayList<>();
      objParse = new Parse(crpThis,errHandle);
      colParseJson = new JSONArray();
      // Initialise the Out and Cmp arrays
      arOutExists = new boolean[arQuery.length + 2];
      arCmpExists = new boolean[arQuery.length + 2];
      // Initialise the array of JSONArray results
      arXqf = new JSONArray[arQuery.length];
      for (int i=0; i< arXqf.length; i++) arXqf[i] = new JSONArray();
      // Validate existence of file
      if (!fThis.exists()) { DoError("File not found: " + strForestFile); return false; }
      // Start walking through the file...
      // (a) Read the first <forest>, including the <teiHeader>
      // Line: modMain - 2798
      if (!objProcType.FirstForest(ndxForest, ndxHeader, strForestFile)) 
        return errHandle.DoError("ExecuteQueriesFile could not process firest forest of " + fThis.getName());
      // Store the [ndxHeader] in the CrpFile object
      oCrpFile.ndxHeader = ndxHeader.argValue;
      // Loop through the file in <forest> chunks
      while (ndxForest.argValue != null) {
        // ============ DEBUG ==================
        // sSeg = ndxForest.argValue.SelectSingleNode("./child::div[@lang='org']/child::seg").getNodeValue();
        // logger.debug("PsdxStream seg = [" + sSeg + "]");
        // =====================================
        // Get the @forestId value of this forest
        if (!objProcType.GetForestId(ndxForest, intForestId)) return errHandle.DoError("Could not obtain @forestId");
        // Get a percentage of where we are
        if (!objProcType.Percentage(intPtc)) return errHandle.DoError("Could not find out where we are");
        
        // ======= DEBUG =========== not needed for real work ==========
        // Show where we are in the status
        // errHandle.debug("file [" + fThis.getName() + "] " + intPtc.argValue + "%");
        // =========================
        
        // TODO: convey the status to a global status gathering object for this Execute object??
        
        // Initialize the firest elements of arOut and arCmp
        arOutExists[0] = true; arCmpExists[0] = false;
        // Reset the text and psd values
        strExpPsd = ""; strExpText = ""; intLastId = -1;
        // Make this forest available to the Xquery Extensions connected with *this* thread
        oCrpFile.ndxCurrentForest = ndxForest.argValue;
        // Check for start of section if this is a database?
        if (this.bIsDbase) {
          String strNextFile = "";  // points to the next file
          
          // TODO: implement. See modMain.vb [2890-2920]
          oDbase.DbaseQueryCurrent(ndxDbRes);
        } else {
          // Always process
          bDoForest = true;
          // Check if this <forest> node contains a start-of-section marker
          if (ndxForest.argValue.getNodeName().getLocalName().equals("forest")) {
            // Try get tyhe "Section" attribute
            String attrS = ndxForest.argValue.getAttributeValue(loc_xq_Section);
            if (attrS != null) {
              // Clear the stack for colRuStack
              // TODO: implement
            }
          }
        }
        // Should this forest be processed? (modMain 2937)
        if (bDoForest) {
          // Yes, start processing this <forest> for all queries in [arQuery]
          for (int k=0;k<this.arQuery.length;k++) {
            // Make the QC line number available
            oCrpFile.QCcurrentLine = k+1;
            // Make sure there is no interrupt
            if (errHandle.bInterrupt) return false;
            // Get the input node for the current query
            int iInputLine = arQuery[k].InputLine;
            bHasInput = (arQuery[k].InputCmp) ? arCmpExists[iInputLine] : arOutExists[iInputLine];
            // Okay, is there any input?
            if (!bHasInput) {
              // There is neither output nor complement, since there is no input
              arOutExists[k+1] = false;
              // Signal that there is no complement
              if (arQuery[k].Cmp) arCmpExists[k+1] = false;
            } else {
              // Reset the parse results
              colParseJson = new JSONArray();
              // Parsing depends on Dbase too
              if (bIsDbase && ndxDbList.size() > 0) {
                // Parse all the <Result> elements within this forestId
                bParsed = false;
                for (int m=0;m<ndxDbList.size(); m++) {
                  // Perform a parse that only resets the collection when m==0
                  if (objParseXq.DoParseXq(arQuery[k], objSaxDoc, this.xconfig, oCrpFile,
                        ndxDbList.get(m), colParseJson, (m==0))) bParsed = true;
                }
              } else {
                // Parse this forest
                bParsed = objParseXq.DoParseXq(arQuery[k], objSaxDoc, this.xconfig, oCrpFile, 
                        ndxForest.argValue, colParseJson, true);
              }
              
              // Now is the time to execute stack movement for colRuStack
              // TODO: RuStackExecute()
              
              // Do we have result(s)?
              if (!bParsed) {
                // Make sure the output of this line (=k+1) is signalled as empty
                arOutExists[k+1] = false;
                // Signal that the complement node is *not* empty 
                //   (since it is the complement of the output)
                if (arQuery[k].Cmp) arCmpExists[k+1] = true;
              } else {
                // Check how many results there are for this sentence/forest
                int intInstances = colParseJson.length();
                // ======== DEBUG =========
                if (intInstances > 1) {
                  int iWatch = intInstances;
                }
                // ========================
                // Validate
                if (intInstances == 0) {
                  // There is no output, so there must be a complement
                  arOutExists[k+1] = false;
                  // Signal that there is no complement
                  if (arQuery[k].Cmp) arCmpExists[k+1] = true;
                } else {
                  // Signal that this output DOES exist
                  arOutExists[k+1] = true;
                  // Signal that there is no complement
                  if (arQuery[k].Cmp) arCmpExists[k+1] = false;
                  // Determine the sentence node (the <forest>) within the current file
                  // For the stream-processing, this is the current forest
                  //
                  // NOTE: for other processing this forest may not be the same as [ndxForest]
                  //       since the Query may result in an <eTree> node that is located
                  //       in another <forest> in the document
                  ndxForestBack = ndxForest.argValue;
                  // Check if the result is usable
                  if (ndxForestBack == null) return false;
                  // Walk all the results that come from the parsing
                  // Note: there may be multiple *hits* within one <forest>, each
                  //       returning its own <eTree> node (that's the general idea)
                  for (int L = 0; L < intInstances; L++) {
                    // Consider this result
                    JSONObject oThisRes = (JSONObject) colParseJson.getJSONObject(L);
                    
                    // Determine possible subcategorisation of *this* result
                    strEtreeCat = (oThisRes.has("Cat")) ? oThisRes.getString("Cat") : "";
                    
                    // Get the oview line 
                    intOviewLine = arQuery[k].OviewLine;
                    if (intOviewLine >=0) {
                      // TODO: Convert the oview line to an OviewId (see modMain:3035)
                      // Do we have subcategorization?
                      if (strEtreeCat.isEmpty()) {
                        // Make sure the intCatLine is negative
                        intCatLine = -1;
                      } else {
                        // Is this a new QCline/Subcat combination?
                        intCatLine = 0;
                        // TODO: calculate correct numbers for the sub-categorization...
                      }
                    }
                    // Add information to this result
                    oThisRes.put("oview", intOviewLine);
                    oThisRes.put("file", fThis.getName());

                    // Add this object to the JSONArray with the objects for this 
                    //  combination of Crp/File
                    arXqf[k].put(oThisRes);
                  }
                }
              }
            }
          }
        }
        // Go to the next forest chunk
        if (!objProcType.NextForest(ndxForest)) return errHandle.DoError("Could not read <forest>");
      }
      
      // TODO: combine the results of the queries
      JSONObject oCombi;
      JSONArray arCombi = new JSONArray();
      for (int k=0;k<arQuery.length;k++) {
        oCombi = new JSONObject();
        oCombi.put("file",fThis.getName());
        oCombi.put("qc", k+1);
        oCombi.put("count", arXqf[k].length());
        oCombi.put("results", arXqf[k]);
        arCombi.put(oCombi);
      }
      jobCaller.setJobResult(arCombi.toString());

      // Pass on the number of hits for this XqF job
      JSONArray arCount = new JSONArray();
      JSONObject oCount;
      for (int k=0;k<arQuery.length;k++) {
        oCount = new JSONObject();
        oCount.put("file", fThis.getName());
        oCount.put("qc", k+1);
        oCount.put("count", arXqf[k].length());
        arCount.put(oCount);
      }
      oCount = new JSONObject();
      oCount.put("stats", arCount);
      jobCaller.setJobCount(oCount);


      // Return positively
      return true;
    } catch (RuntimeException | SaxonApiException ex) {
      // Warn user
      DoError("ExecutePsdxStream/ExecuteQueriesFile runtime error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    } /* catch (XPathExpressionException ex) {
      // Warn user
      DoError("ExecutePsdxStream/ExecuteQueriesFile xpath error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    } */
  }
// </editor-fold>  

}
