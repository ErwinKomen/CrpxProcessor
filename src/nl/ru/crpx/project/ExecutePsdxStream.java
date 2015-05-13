/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import nl.ru.crpx.search.Job;
import nl.ru.crpx.search.JobXqF;
import nl.ru.crpx.search.QueryException;
import nl.ru.crpx.search.SearchParameters;
import nl.ru.xmltools.Parse;
import nl.ru.crpx.xq.CrpFile;
import nl.ru.crpx.xq.RuBase;
import nl.ru.util.ByRef;
import nl.ru.util.StringUtil;
import nl.ru.util.json.JSONArray;
import nl.ru.util.json.JSONObject;
import nl.ru.xmltools.ParseResult;
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

// <editor-fold desc="XqF">
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
        CrpFile oCrpFile = new CrpFile(this.crpThis, fInput);
        RuBase.setCrpCaller(oCrpFile);
        // Get the @id of this combination
        iCrpFileId = RuBase.getCrpCaller(oCrpFile);
        // Add the id to the search parameters
        SearchParameters searchXqFpar = new SearchParameters(this.searchMan);
        searchXqFpar.put("crpfileid", Integer.toString(iCrpFileId));
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
      if (!monitorXqF(0)) return false;
      
      // TODO: combine the results of the queries
      // Combine [arRes] into a result string (JSON)
      String sCombiJson = "[" + StringUtil.join(arRes, ",") + "]";
      jobCaller.setJobResult(sCombiJson);

      
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
      while (arJob.size() > iUntil) {
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

// <editor-fold desc="XqF">
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
    List<XmlNode> ndxDbList;    // All nodes for current text/forest combination
    XmlNode ndxDbPrev;          // Previous database location
    XmlNode ndxForestBack;      // Forest inside which the resulting [eTree] resides
    Parse objParse;             // Functions to parse and get results
    ParseResult oOneParseRes;   // One parse result
    ByRef<XmlNode> ndxForest;   // Forest we are working on
    ByRef<XmlNode> ndxDbRes;    // Current result
    ByRef<Integer> intForestId; // ID (numerical) of the current <forest>
    ByRef<Integer> intPtc;      // Percentage of where we are
    List<ParseResult> colParseRes;  // Results of one parse (values of @eTree etc)
    JSONArray arXqf;
    
    // Note: this uses [objProcType, which is a 'protected' variable from [Execute]
    try {
      // Get the CrpFile object
      CrpFile oCrpFile = RuBase.getCrpFile(iCrpFileIdx);
      // Get the file
      File fThis = oCrpFile.flThis;
      // Initialisations
      ndxForest = new ByRef(null); ndxDbRes = new ByRef(null);
      strForestFile = fThis.getAbsolutePath();
      intForestId = new ByRef(-1);
      intPtc = new ByRef(0);
      ndxDbList = new ArrayList<>();
      objParse = new Parse(crpThis,errHandle);
      colParseRes = new ArrayList<>();
      arXqf = new JSONArray();
      // Validate existence of file
      if (!fThis.exists()) { DoError("File not found: " + strForestFile); return false; }
      // Start walking through the file...
      // (a) Read the first <forest>, including the <teiHeader>
      // Line: modMain - 2798
      if (!objProcType.FirstForest(ndxForest, strForestFile)) 
        return errHandle.DoError("ExecuteQueriesFile could not process firest forest of " + fThis.getName());
      // Loop through the file in <forest> chunks
      while (ndxForest.argValue != null) {
        // Get the @forestId value of this forest
        if (!objProcType.GetForestId(ndxForest, intForestId)) return errHandle.DoError("Could not obtain @forestId");
        // Get a percentage of where we are
        if (!objProcType.Percentage(intPtc)) return errHandle.DoError("Could not find out where we are");
        // Show where we are in the status
        errHandle.debug("file [" + fThis.getName() + "] " + intPtc.argValue + "%");
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
          if (ndxForest.argValue.getLocalName().equals("forest")) {
            // Try get tyhe "Section" attribute
            Node attrS = ndxForest.argValue.Attributes("Section");
            if (attrS != null) {
              // Clear the stack for colRuStack
              // TODO: implement
            }
          }
        }
        // Should this forest be processed? (modMain 2937)
        if (bDoForest) {
          // Yest, start processing this <forest> for all queries in [arQuery]
          for (int k=0;k<this.arQuery.length;k++) {
            // Make the QC line number available
            oCrpFile.QCcurrentLine = k+1;
            // Make sure there is no interrupt
            if (errHandle.bInterrupt) return false;
            // Get the input node for the current query
            int iInputLine = arQuery[k].InputLine;
            bHasInput = (arQuery[k].InputCmp) ? arCmpExists[iInputLine] : arOutExists[iInputLine];
            // Okay, is there any input?
            if (bHasInput) {
              // Parsing depends on Dbase too
              if (bIsDbase && ndxDbList.size() > 0) {
                // Parse all the <Result> elements within this forestId
                bParsed = false;
                for (int m=0;m<ndxDbList.size(); m++) {
                  // Perform a parse that only resets the collection when m==0
                  if (objParseXq.DoParseXq(arQuery[k].Name, arQuery[k].Qeval, objSaxDoc, 
                          ndxDbList.get(m), colParseRes, (m==0))) bParsed = true;
                }
              } else {
                // Parse this forest
                bParsed = objParseXq.DoParseXq(arQuery[k].Name, arQuery[k].Qeval, objSaxDoc, 
                        ndxForest.argValue, colParseRes, true);
              }
              // Now is the time to execute stack movement for colRuStack
              // TODO: RuStackExecute()
              
              // Do we have result(s)?
              if (bParsed) {
                // Check how manuy results there are for this sentence/forest
                int intInstances = colParseRes.size();
                // Validate
                if (intInstances > 0) {
                  // Get the @id of the first result node
                  strTreeId = colParseRes.get(0).treeId;
                  // Determine the sentence node (the <forest>) within the current file
                  // For the stream-processing, this is the current forest
                  ndxForestBack = ndxForest.argValue;
                } else ndxForestBack = null;
                // Walk all the results
                for (int L = 0; L < intInstances; L++) {
                  // Validate - double check
                  if (ndxForestBack == null) return false;
                  // Keep track of the total number of hits
                  intHitsTotal++; // TODO: show this number in the status line
                  // Treat the result
                  oOneParseRes = colParseRes.get(L);
                  strTreeId = oOneParseRes.treeId;
                  strEtreeMsg = oOneParseRes.msg;
                  strEtreeCat = oOneParseRes.cat;
                  strEtreeDb = oOneParseRes.db;
                  // Signal that we have output here
                  arCmpExists[k+1] = true;
                  // Signal that there is no complement
                  if (arQuery[k].Cmp) arCmpExists[k+1] = false;
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
                    }
                    // Create output for this line
                    JSONObject oXqfRes = new JSONObject();
                    oXqfRes.put("oview", intOviewLine);
                    oXqfRes.put("file", fThis.getName());
                    oXqfRes.put("search", ndxForestBack.Attributes("Location").getNodeValue());
                    oXqfRes.put("forestId", String.valueOf(intForestId));
                    oXqfRes.put("eTreeId", strTreeId);
                    oXqfRes.put("Cat", strEtreeCat);
                    oXqfRes.put("Msg", strEtreeMsg);
                    // TODO: add "db" list/collection
                    
                    // Add this object to the JSONArray with the objects for this Crp/File combination
                    arXqf.put(oXqfRes);
                  }
                  // TODO: check in modMain:3110 and further wat hier precies gedaan moet worden; code is onduidelijk.
                  // Should we put output in the complement?
                  if (arQuery[k].Cmp) arCmpExists[k+1] = false;
                }
              }
            }
          }
        }
        
        
        // Go to the next forest chunk
        if (!objProcType.NextForest(ndxForest)) return errHandle.DoError("Could not read <forest>");
      }
      
      // TODO: combine the results of the queries
      // Combine [arRes] into a result string (JSON)
      String sCombiJson = "{" + StringUtil.join(arRes, ",") + "}";
      jobCaller.setJobResult(sCombiJson);

      // Return positively
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      DoError("ExecutePsdxStream/ExecuteQueriesFile error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
// </editor-fold>  

}
