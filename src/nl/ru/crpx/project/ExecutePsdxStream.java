/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

package nl.ru.crpx.project;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XQueryEvaluator;
import nl.ru.crpx.dataobject.DataObject;
import nl.ru.crpx.dataobject.DataObjectList;
import nl.ru.crpx.dataobject.DataObjectMapAttribute;
import nl.ru.crpx.dataobject.DataObjectMapElement;
import nl.ru.crpx.search.Job;
import nl.ru.crpx.search.JobXq;
import nl.ru.crpx.search.JobXqF;
import nl.ru.crpx.search.QueryException;
import nl.ru.crpx.search.SearchParameters;
import nl.ru.crpx.tools.FileIO;
import nl.ru.crpx.xq.CrpFile;
import nl.ru.crpx.xq.LexDict;
import nl.ru.crpx.xq.RuBase;
import nl.ru.util.ByRef;
import nl.ru.util.FileUtil;
import nl.ru.util.json.JSONArray;
import nl.ru.util.json.JSONObject;
import nl.ru.xmltools.XmlAccess;
import nl.ru.xmltools.XmlAccessPsdx;
import nl.ru.xmltools.XmlForest;
import nl.ru.xmltools.XmlNode;

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
  JSONArray arTotal = new JSONArray();    // Array combining all the individual arXqf arrays
  JSONObject oProgress = null;            // Progress of this Xq job
  DataObjectList dlDbase = null;          // List of database information per QC line
  DataObjectMapElement dmProgress = null; // Progress of this Xq job
  // ========== constants ======================================================
  private static final QName loc_xq_EtreeId = new QName("", "", "TreeId");
  private static final QName loc_xq_Section = new QName("", "", "Section");
  private static final QName loc_xq_Location = new QName("", "", "Location");
  private static final QName loc_xq_subtype = new QName("", "", "subtype");

  // ============ Local variables for "XqF" ====================================
  // (none apparently)
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
    /*
    arHits = new DataObjectList("hitlist");
    arMonitor = new DataObjectList("filehits");
    */
    
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
    int iPtc;       // Percentage progress
    
    try {
      // Set the job for global access within Execute > ExecuteXml > ExecutePsdxStream
      this.objSearchJob = jobCaller;
      oProgress = new JSONObject();
      dmProgress = new DataObjectMapElement();
      dlDbase = new DataObjectList("dblist");
      /* ========= Should not be here
      // Set the XmlForest element correctly
      this.objProcType = new XmlForest(this.crpThis,(JobXq) jobCaller, this.errHandle);
      // Make sure 
      this.objProcType.setProcType(ForType.PsdxPerForest);
      ========================== */
      // Perform general setup
      if (!super.ExecuteQueriesSetUp()) {
        jobCaller.setJobErrors(errHandle.getErrList());
        return false;
      }
      // Perform setup part that is specifically for Xml/Xquery
      if (!super.ExecuteXmlSetup()) {
        jobCaller.setJobErrors(errHandle.getErrList());
        return false;
      }
      
      // Initialise the job array and the results array
      arJob.clear();  arRes.clear();
      // arHits.clear(); arMonitor.clear();

      // Indicate we are working
      jobCaller.setJobStatus("working");
      oProgress.put("total", lSource.size());
      
      // Visit all the source files stored in [lSource]
      for (int i=0;i<lSource.size(); i++) {
        // Where are we?
        iPtc = (100 * (i+1)) / lSource.size();
        jobCaller.setJobPtc(iPtc);
        
        // ================= DEBUGGING =============
        if (oProgress==null) {
          logger.debug("ExecuteQueries " + (i+1) + "/" + lSource.size() + ": oProgress is null for [" + this.userId + "]");
        } 
        // =========================================
        
        // Take this input file
        File fInput = new File(lSource.get(i));
        synchronized(oProgress) {
          oProgress.put("start", fInput.getName());
          oProgress.put("count", i+1);
          jobCaller.setJobProgress(oProgress);
        }
        // Add the combination of File/CRP to the stack
        CrpFile oCrpFile = new CrpFile(this.crpThis, fInput, objSaxon, (JobXq) jobCaller);
        RuBase.setCrpCaller(oCrpFile);
        // Get the @id of this combination
        iCrpFileId = RuBase.getCrpCaller(oCrpFile);
        // Add the id to the search parameters
        SearchParameters searchXqFpar = new SearchParameters(this.searchMan);
        searchXqFpar.put("crpfileid", Integer.toString(iCrpFileId));
        // Do *NOT* add the "query" parameter -- that would influence the max number of *total* jobs per user that can be re-used
        // Give it a different name, like "xqf"
        searchXqFpar.put("query", "{\"crp\": \"" + crpThis.getName() + "\"" +
                ", \"file\": \"" + fInput.getName() + "\"" + "}");
        // Keep track of the old jobs and make sure not too many are running now
        if (!monitorXqF(this.iMaxParJobs, jobCaller)) {
          // Getting here means that we are UNABLE to wait for the number of jobs
          //  of this user to decrease below @iMaxParJobs
          return errHandle.DoError("ExecuteQueries: unable to get below max #jobs " + 
                  this.iMaxParJobs, ExecutePsdxStream.class);
        }
        // Create a job for this Crp/File treatment
        JobXqF search = null;
        try {
          // Initiate the XqF job
          search = searchMan.searchXqF(crpThis, userId, searchXqFpar, jobCaller);
        } catch (QueryException ex) {
          // Return error and failure
          return errHandle.DoError("Failed to execute file ", ex, ExecutePsdxStream.class);
        } catch (InterruptedException ex) {
          // Interruption as such should not be such a problem, I think
          return errHandle.DoError("Interrupted during sleep: " + ex.getMessage());
        }
        
        synchronized(search) {
          // Get the @id of the job that has been created
          String sThisJobId = search.getJobId();
          String sNow = Job.getCurrentTimeStamp();
          // Additional debugging to find out where the errors come from
          logger.debug("XqFjob [" + sNow + "] " + (i+1) + "/" + lSource.size() + 
                  " on [" + fInput.getName() + "] userid=[" + 
                  userId + "] jobid=[" + sThisJobId + "], finished=" + 
                  search.finished() + " status=" + search.getJobStatus() );

          // Add the job to the list of jobs for this project/user
          arJob.add(search);
        }
      }
      // Monitor the end of the jobs
      if (!monitorXqF(1, jobCaller)) return false;
      
      // Combine the results of the queries into a table
      // NOTE: this may now be obsolete because of JobTable
      String sCombiJson = combineResults(arCount);
      jobCaller.setJobResult(sCombiJson);
      
      // Provide a dataobject table for better processing
      jobCaller.setJobTable(getResultsTable(arCount));
      
      // If a DATABASE needs to be created, combine the parts that have been made already
      makeResultsDbaseList(dlDbase, arTotal);
      jobCaller.setJobDbList(dlDbase);

      // Also provide the job count results (which are perhaps less interesting)
      // NOTE: unclear whether this should be kept - superfluous?
      JSONObject oCount = new JSONObject();
      oCount.put("counts", arCount);
      jobCaller.setJobCount(oCount);
      
      // Return positively
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      errHandle.DoError("ExecutePsdxStream/ExecuteQueries error: ", ex, ExecutePsdxStream.class);
      // Return failure
      return false;
    }
  }
  
  /**
   * makeResultsDbaseList
   *    Combine all database-parts for each QC line
   * 
   * @param lstBack     - The dataobject list we will be returning with the dbase information
   * @param arListTotal - Array of all individual .setJobList() objects
   */
  private void makeResultsDbaseList(DataObjectList lstBack, JSONArray arListTotal) {
    PrintWriter[] arPwCombi = new PrintWriter[arQuery.length];
    
    try {
      // Create a new list
      lstBack = new DataObjectList("dblist");
      
      // Walk all the QC items
      for (int i=0;i<arQuery.length;i++) {
        // Does this query create a database?
        if (arQuery[i].DbFeatSize>0) {
          int iQCid = i+1;
          // Create a name for the combined database for this QC
          String sCombi = this.crpThis.getDbaseName(iQCid);
          File fCombi = new File(sCombi);
          PrintWriter pCombi = FileUtil.openForAppend(fCombi);
          
          // Create this file and add a first part.
          String sIntro = "<CrpOview>\n <General>\n <ProjectName>" + this.crpThis.getName() + "</ProjectName>\n" +
                  " <Created>" + crpThis.dateToString(new Date()) + "</Created>\n" +
                  " <DstDir></DstDir>\n" +
                  " <SrcDir>" + this.crpThis.getSrcDir() + "</SrcDir>\n" +
                  " <Notes>Created by CorpusStudio from query line " + iQCid + ": [" + arQuery[i].Descr + "]</Notes>\n" +
                  " <Analysis></Analysis>\n</General>\n";
          pCombi.append(sIntro);
          arPwCombi[i] = pCombi;
        } else 
          arPwCombi[i] = null;
      }

      // Get to the list for all files
      for (int i=0;i<arListTotal.length();i++) {
        // Start counting result id
        int iResId = 1;
        // Get the object for this file
        JSONObject oListOneFile = arListTotal.getJSONObject(i);
        // Get information from this object
        String sFileName = oListOneFile.getString("file");
        String sTextId = oListOneFile.getString("textid");
        String sSubType = oListOneFile.getString("subtype");
        JSONArray arHits = oListOneFile.getJSONArray("hits");
        // Walk the "hits" array: one item for each QC
        for (int j=0;j<arHits.length(); j++) {
          // Get this item
          JSONObject oHitTotal = arHits.getJSONObject(j);
          // Get the info from this item
          int iQCid = oHitTotal.getInt("qc");
          if (arQuery[iQCid-1].DbFeatSize>0) {
            String sResult = oHitTotal.getString("result");
            JSONArray arHitsPerQc = oHitTotal.getJSONArray("qchits");
            // Walk the hits for this File/QC combination
            StringBuilder bThis = new StringBuilder();
            for (int k=0;k<arHitsPerQc.length(); k++) {
              // Get this hit
              JSONObject oOneHit = arHitsPerQc.getJSONObject(k);
              // Process the information in this hit
              bThis.append(getResultXml(sFileName, sTextId, sSubType, 
                      arQuery[j].DbFeat, oOneHit, iResId++));
            }
            // Append this information to the PrintWriter for this QC
            arPwCombi[j].append(bThis);
          }
        }
      }
      // Walk all the QC items
      for (int i=0;i<arQuery.length;i++) {
        // Does this query create a database?
        if (arQuery[i].DbFeatSize>0) {
          // Finish this file
          arPwCombi[i].append("</CrpOview>\n");
          arPwCombi[i].close();
        } 
      }
      // COmbine
    } catch (Exception ex) {
      // Warn user
      errHandle.DoError("ExecutePsdxStream/makeResultsDbaseList error: ", ex, ExecutePsdxStream.class);
    }
  }
  
  /* ---------------------------------------------------------------------------
     Name:    getResultsTable
     Goal:    Re-combine the overall Xq counts from [arLines] to make it a better
                input for the creation of a table
     History:
     09/jun/2015 ERK Created for JAVA
     --------------------------------------------------------------------------- */
  private DataObject getResultsTable(JSONArray arLines) {
    List<String> arSub = new ArrayList<>();   // each sub-category gets an entry here
    List<String> arFile = new ArrayList<>();  // List of files
    List<Integer> arSubCount = new ArrayList<>(); // each sub-category gets a count total here
    JSONObject arJsonRes[];                   // Object "hits" of the current QC line
    JSONObject arJsonSub[];                   // Object "sub" of the current QC line
    DataObjectList arCombi = new DataObjectList("QCline");

    try {
      // Walk all the QC lines
      for (int iQC=0; iQC<arQuery.length; iQC++) {
        // Initialise a counter for the hits
        int iHitCount = 0;
        // Clear the arraylist of subcats
        arSub.clear(); arFile.clear();
        arJsonRes = new JSONObject[arLines.length()];
        arJsonSub = new JSONObject[arLines.length()];
        // Walk all the lines
        for (int i = 0; i< arLines.length(); i++ ) {
          // Initialise object here
          arJsonRes[i] = new JSONObject();
          arJsonSub[i] = new JSONObject();
          // Each line has fields "hits" and "file"
          JSONArray oHit = arLines.getJSONObject(i).getJSONArray("hits");
          // Add the file name to the list
          arFile.add(arLines.getJSONObject(i).getString("file"));
          // Get the element corresponding with the current QC line
          JSONObject oEl = oHit.getJSONObject(iQC);
          arJsonRes[i] = oEl;
          // Get the "sub" element from here
          JSONObject oSub = oEl.getJSONObject("sub");
          arJsonSub[i] = oSub;
          // Walk all the fields
          JSONArray arKeys = oSub.names();
          if (arKeys != null) for (int iField =0; iField < arKeys.length(); iField++) {
            // Get this field
            String sField = arKeys.getString(iField);
            // Possibly add the field to [arSub]
            if (!arSub.contains(sField)) arSub.add(sField);
          }
        }
        // Sort the sub-category list
        Collections.sort(arSub);
        // Get access to information on this QC item
        JSONObject oQcThis = this.crpThis.getListQCitem(iQC);
        // Reset the array that contains the counts for each sub-category
        arSubCount.clear();
        for (int i=0;i<arSub.size(); i++ ) {arSubCount.add(0);}
        // We now have three arrays containing one entry for each processed file
        // (1) arFile     file name
        // (2) arJsonRes  object "hits" = { "sub":..., "count": ..., "qc": ...}
        // (3) arJsonSub  object "sub"  = key/value object of sub-category/counts
        // Re-combine the results for this QC line
        DataObjectMapElement dmOneQc = new DataObjectMapElement();
        // The dataobject for one QC line contains: "hits", "subcats", "counts" and "qc"
        dmOneQc.put("qc", iQC+1);
        dmOneQc.put("result", oQcThis.getString("Result"));
        // Create and add the array of sub-categories
        DataObjectList aSubNames = new DataObjectList("subcat");
        // Add the name of each "sub" category
        for (String sSubCatName : arSub) { aSubNames.add(sSubCatName); }
        dmOneQc.put("subcats", aSubNames);
        // Calculate and create an array of "hit" elements
        DataObjectList aHits = new DataObjectList("hit");
        int iTotal = 0;
        // Each "hit" object consists of: {"sub": ..., "count": ..., "file": ...}
        for (int i=0;i<arLines.length(); i++) {
          // Create a hit element for each line
          DataObjectMapElement oResThis = new DataObjectMapElement();
          // Add the "count" and the "file" parameters
          oResThis.put("file", arFile.get(i));
          oResThis.put("count", arJsonRes[i].getInt("count"));
          // Also keep track of overall total
          iTotal += arJsonRes[i].getInt("count");
          // Get fields and values for each "sub" category
          DataObjectList aResSub = new DataObjectList("sub");
          for (int j=0; j< arSub.size(); j++) {
            // Add this field
            int iCount = 0;
            if (arJsonSub[i].has(arSub.get(j))) {
              iCount = arJsonSub[i].getInt(arSub.get(j));
            } 
            aResSub.add(iCount);
            // Add this to the overall counting for sub-categories
            arSubCount.set(j, arSubCount.get(j) + iCount);
          }
          // Add the array of sub-cat counts
          oResThis.put("subs", aResSub);
          // Add the results to the "hit" list
          aHits.add(oResThis);
        }
        // Sort the result based on the file name
        aHits.sort("file");
        // Create the sub-totals
        DataObjectList aSubCount = new DataObjectList("count");
        for (int i=0;i<arSub.size(); i++) {aSubCount.add(arSubCount.get(i)); }
        // Add the array of subcat-totals
        dmOneQc.put("counts", aSubCount);
        dmOneQc.put("total", iTotal);
        // Add the array of "hit" elements
        dmOneQc.put("hits", aHits);
        // Add this QC object to the list
        arCombi.add(dmOneQc);
      }
      // Return the result
      return arCombi;
    } catch (Exception ex) {
      // Warn user
      DoError("ExecutePsdxStream/getResultsTable error", ex, ExecutePsdxStream.class);
      // Return failure
      return null;
    }
  }
  /* ---------------------------------------------------------------------------
     Name:    combineResults
     Goal:    Re-combine the overall Xq counts from [arLines] to make it a better
                input for the creation of a table
     History:
     01/jun/2015 ERK Created for JAVA
     --------------------------------------------------------------------------- */
  private String combineResults(JSONArray arLines) {
    List<String> arSub = new ArrayList<>();   // each sub-category gets an entry here
    List<String> arFile = new ArrayList<>();  // List of files
    JSONObject arJsonRes[];
    JSONObject arJsonSub[];
    
    try {
      // Walk an object for all QC lines
      JSONArray oTotal = new JSONArray();
      // Walk all the QC lines
       for (int iQC=0; iQC<arQuery.length; iQC++) {
        // Clear the arraylist of subcats
        arSub.clear(); 
        arJsonRes = new JSONObject[arLines.length()];
        arJsonSub = new JSONObject[arLines.length()];
        // Walk all the lines
        for (int i = 0; i< arLines.length(); i++ ) {
          // Initialise object here
          arJsonRes[i] = new JSONObject();
          arJsonSub[i] = new JSONObject();
          // Each line has fields "hits" and "file"
          JSONArray oHit = arLines.getJSONObject(i).getJSONArray("hits");
          // Add the file name to the list
          arFile.add(arLines.getJSONObject(i).getString("file"));
          // Get the element corresponding with the current QC line
          JSONObject oEl = oHit.getJSONObject(iQC);
          arJsonRes[i] = oEl;
          // Get the "sub" element from here
          JSONObject oSub = oEl.getJSONObject("sub");
          arJsonSub[i] = oSub;
          // Walk all the fields
          JSONArray arKeys = oSub.names();
          if (arKeys != null) for (int iField =0; iField < arKeys.length(); iField++) {
            // Get this field
            String sField = arKeys.getString(iField);
            // Possibly add the field to [arSub]
            if (!arSub.contains(sField)) arSub.add(sField);
          }
        }
        // Review the results for this QC line
        JSONObject oQCresults = new JSONObject();
        oQCresults.put("qc", iQC+1);
        JSONArray aSubNames = new JSONArray();
        // Add fields and values for each "sub" category
        for (int j=0; j< arSub.size(); j++) {
          // Add this field
          aSubNames.put(arSub.get(j));
        }
        oQCresults.put("subcats", aSubNames);
        // Create an array for each file
        JSONArray aResFile = new JSONArray();
        for (int i=0;i<arLines.length(); i++) {
          // Create the object for this line
          JSONObject oResThis = new JSONObject();
          oResThis.put("file", arFile.get(i));
          oResThis.put("count", arJsonRes[i].getInt("count"));
          // Add fields and values for each "sub" category
          JSONArray aResSub = new JSONArray();
          for (int j=0; j< arSub.size(); j++) {
            // Add this field
            int iCount = 0;
            if (arJsonSub[i].has(arSub.get(j))) {
              iCount = arJsonSub[i].getInt(arSub.get(j));
            } 
            aResSub.put(iCount);
          }
          // Add the array of sub-cat counts
          oResThis.put("sub", aResSub);
          aResFile.put(oResThis);
        }
        
        oQCresults.put("hits", aResFile);
        // Add to the array with totals
        oTotal.put(oQCresults);
      }
      
      // Return the results
      return oTotal.toString(1);
    } catch (Exception ex) {
      // Warn user
      DoError("ExecutePsdxStream/combineResults error: " + ex.getMessage() + "\r\n");
      // Return failure
      return "";
    }
  }
  
  /**
   * monitorXqF - Process and monitor jobs of XqF type until @iuntil are left.
   * Traverse the stack of jobs and when one is finished:
   * 1) gather its results
   * 2) take it from the [arJob] list
   * 
   * @param iUntil
   * @param jobCaller
   * @return 
   */
  private boolean monitorXqF(int iUntil, Job jobCaller) {
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
            // arRes.add(sResultXqF);
            arCount.put(jThis.getJobCount());
            
            // Add the individual Xqf to the total
            arTotal.put(jThis.getJobList());
                        
            // ================= DEBUGGING =============
            if (oProgress==null) {
              logger.debug("monitorXqF job " + jThis.getJobId() + ": oProgress is null for [" + this.userId + "]");
            } else {
              // More double checking
              if (jThis.intCrpFileId <0)
                logger.debug("monitorXqF job " + jThis.getJobId() + " fileid=" + jThis.intCrpFileId);
              CrpFile oCrpFile = RuBase.getCrpFile(jThis.intCrpFileId);
              if (oCrpFile==null)
                logger.debug("monitorXqF job " + jThis.getJobId() + " CrpFile=null");
              if (oCrpFile.flThis == null)
                logger.debug("monitorXqF job " + jThis.getJobId() + " CrpFile.flThis=null");
            }
            // =========================================
            
            // Note that it has finished for others too
            synchronized(oProgress) {
              oProgress.put("finish",RuBase.getCrpFile(jThis.intCrpFileId).flThis.getName());
              oProgress.put("ready", arCount.length());
              jobCaller.setJobProgress(oProgress);
            }
            // We have its results, so take it away from our job list
            arJob.remove(jThis);
            // The job must also be removed to clear room
            jThis.changeClientsWaiting(-1);
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
   *       via the [oCrpFile] object
   * 
   * @param jobCaller
   * @param iCrpFileIdx
   * @return 
   */
  public boolean ExecuteQueriesFile( Job jobCaller, int iCrpFileIdx ) {
    int intNumForest=0;         // Forest number we are processing
    int intLastId;              // 
    int intHitsTotal = 0;       // Total number of hits so far
    int intOviewLine;           // The target overview line where we have to store results
    int intCatLine;             // Where we are in the subcategorization
    boolean bDoForest = false;  // Should we process the current <forest> node?
    boolean bHasInput = false;  // Does this line have input?
    boolean bParsed = false;    // Line has been parsed
    String strForestFile;       // Name of this file
    String strSubType;          // Subtype for this file
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
    ByRef<XmlNode> ndxForest;   // Forest we are working on
    ByRef<XmlNode> ndxHeader;   // Header of this file
    ByRef<XmlNode> ndxDbRes;    // Current result
    ByRef<Integer> intForestId; // ID (numerical) of the current <forest>
    ByRef<Integer> intPtc;      // Percentage of where we are
    JSONArray colParseJson;     // Array with json results
    XmlForest objProcType;      // Access to the XmlForest object allocated to me
    JSONArray[] arXqf;          // An array of JSONArray results for each QC item
    JSONObject[] arXqfSub;      // An array of JSONObject items containing subcat-count pairs
    // DataObjectList arHitList;   // List with hit information per hit: file // qc // number
    JSONArray[] arDbRes;        // Array with RESULT elements
    XQueryEvaluator[] arQeval;  // Our own query evaluators
    XmlAccess objXmlAcc = null; // Access to the XML file
    
    // Note: this uses [objProcType, which is a 'protected' variable from [Execute]
    try {
      // Get the CrpFile object
      CrpFile oCrpFile = RuBase.getCrpFile(iCrpFileIdx);
      // Get the file
      File fThis = oCrpFile.flThis;
      String fName = fThis.getName();
      // Initialisations
      objProcType = oCrpFile.objProcType;
      ndxForest = new ByRef(null); ndxDbRes = new ByRef(null);
      ndxHeader = new ByRef(null);
      strForestFile = fThis.getAbsolutePath();
      strSubType = oCrpFile.currentPeriod;
      intForestId = new ByRef(-1);
      intPtc = new ByRef(0);
      ndxDbList = new ArrayList<>();
      colParseJson = new JSONArray();
      // arHitList = new DataObjectList("hitlist");
      // Initialise the Out and Cmp arrays
      arOutExists = new boolean[arQuery.length + 2];
      arCmpExists = new boolean[arQuery.length + 2];
      // Initialise the array of JSONArray results and some other arrays
      arXqf = new JSONArray[arQuery.length];
      arXqfSub = new JSONObject[arQuery.length];
      arDbRes = new JSONArray[arQuery.length];
      arQeval = new XQueryEvaluator[arQuery.length];
      for (int i=0; i< arXqf.length; i++) { 
        // Initialise the JSON array for this query
        arXqf[i] = new JSONArray(); 
        // Set a new XQueryEvaluator for this combination of Query / File
        arQeval[i] = arQuery[i].Exe.load();
        // Initialise the JSONObject containing sub-cat counts per QC-item
        arXqfSub[i] = new JSONObject();
        // Initialize JSON array for database output
        arDbRes[i] = new JSONArray();
      }
      
      // Validate existence of file
      if (!fThis.exists()) { errHandle.DoError("File not found: " + strForestFile); return false; }
      
      // === Debugging: Get the name of this file
      // String sCurrentFile = this.sFile;
      
      // Start walking through the file...
      // (a) Read the first <forest>, including the <teiHeader>
      if (!objProcType.FirstForest(ndxForest, ndxHeader, strForestFile)) 
        return errHandle.DoError("ExecuteQueriesFile could not process firest forest of " + fName);
      
      // This is when we can also read the textid
      String sTextId = objProcType.getCurrentTxtId();

      // Downwards compatibility: determine (if possible) the CurrentPeriod
      oCrpFile.currentPeriod = "";
      XmlNode ndxCreation = ndxHeader.argValue.SelectSingleNode("./descendant::creation");
      if (ndxCreation != null) {
        String sSubType = ndxCreation.getAttributeValue(loc_xq_subtype);
        oCrpFile.currentPeriod = sSubType;
      }
      
      // Store the [ndxHeader] in the CrpFile object
      oCrpFile.ndxHeader = ndxHeader.argValue;
      // Loop through the file in <forest> chunks
      while (ndxForest.argValue != null) {
        // Get the @forestId value of this forest
        if (!objProcType.GetForestId(ndxForest, intForestId)) return errHandle.DoError("Could not obtain @forestId");
        // Get a percentage of where we are
        if (!objProcType.Percentage(intPtc)) return errHandle.DoError("Could not find out where we are");
        
       
        // TODO: convey the status to a global status gathering object for this Execute object??
        
        // Initialize the firest elements of arOut and arCmp
        arOutExists[0] = true; arCmpExists[0] = false;
        // Reset the text and psd values
        strExpPsd = ""; strExpText = ""; intLastId = -1;
        // Make this forest available to the Xquery Extensions connected with *this* thread
        oCrpFile.ndxCurrentForest = ndxForest.argValue;
        // Make the current sentence id available too
        oCrpFile.currentSentId = String.valueOf(intForestId);
        // Check for start of section if this is a database?
        if (this.bIsDbase) {
          String strNextFile = "";  // points to the next file
          
          // TODO: implement. See modMain.vb [2890-2920]
          this.oDbase.DbaseQueryCurrent(ndxDbRes);
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
          // String sForestLoc = ndxForest.argValue.getAttributeValue(loc_xq_Location);
          String sForestLoc = objProcType.getCurrentLoc();
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
              if (this.bIsDbase && ndxDbList.size() > 0) {
                // Parse all the <Result> elements within this forestId
                bParsed = false;
                for (int m=0;m<ndxDbList.size(); m++) {
                  // Perform a parse that only resets the collection when m==0
                  if (this.objParseXq.DoParseXq(arQuery[k],arQeval[k],this.objSaxDoc, this.xconfig, oCrpFile,
                        ndxDbList.get(m), colParseJson, (m==0))) bParsed = true;
                }
              } else {
                // Parse this forest
                bParsed = this.objParseXq.DoParseXq(arQuery[k], arQeval[k], this.objSaxDoc, this.xconfig, oCrpFile, 
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
                  // If a database should be created, then we need to do some more
                  if (arQuery[k].DbFeatSize>0) {
                    // Get the context
                    
                    // Get the syntax of this line
                    
                    
                  }
                  // Walk all the results that come from the parsing
                  // Note: there may be multiple *hits* within one <forest>, each
                  //       returning its own <eTree> node (that's the general idea)
                  for (int L = 0; L < intInstances; L++) {
                    // Consider this result
                    JSONObject oThisRes = (JSONObject) colParseJson.getJSONObject(L);
                    
                    // Determine possible subcategorisation of *this* result
                    strEtreeCat = (oThisRes.has("cat")) ? oThisRes.getString("cat") : "";
                    // Process the sub-category
                    if (!strEtreeCat.isEmpty()) {
                      // Keep track of counts for this subcat
                      arXqfSub[k].increment(strEtreeCat);
                    }
                    
                    // Get the oview line 
                    intOviewLine = arQuery[k].OviewLine;
                    intCatLine = -1;
                    if (intOviewLine >=0) {
                      // TODO: Convert the oview line to an OviewId (see modMain:3035)
                      // Do we have subcategorization?
                      if (!strEtreeCat.isEmpty()) {

                        // TODO: calculate correct numbers for the sub-categorization...
                      }
                    }
                    
                    // Check if a database output is required
                    if (arQuery[k].DbFeatSize>0) {
                      // A database output is required, so we need to add context, syntax and pde
                      oThisRes.put("con", objProcType.GetContext());
                      oThisRes.put("eng",  objProcType.GetPde(ndxForest));
                      oThisRes.put("syn",  objProcType.GetSyntax(ndxForest));
                      oThisRes.put("locl", sForestLoc);
                    }
                    
                    // Store the computed output for this hit into the array
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
      JSONObject oHitInfo = new JSONObject();
      oHitInfo.put("file", fName);
      JSONObject oCombi;
      JSONArray arCombi = new JSONArray();
      for (int k=0;k<arQuery.length;k++) {
        oCombi = new JSONObject();
        oCombi.put("qc", k+1);
        oCombi.put("count", arXqf[k].length());
        oCombi.put("results", arXqf[k]);
        // gather the results-per-subcategory
        JSONArray arPerCat = new JSONArray();
        if (arXqfSub[k].length()>0) {
          Iterator keys = arXqfSub[k].keys();
          while (keys.hasNext()) {
            String sCatName = keys.next().toString();
            int iCatCount = arXqfSub[k].getInt(sCatName);
            JSONObject oPerCat = new JSONObject();
            oPerCat.put("cat", sCatName);
            oPerCat.put("count", iCatCount);
            // Walk the results for this QC, looking for those with cat=sCatName
            JSONArray arCatRes = new JSONArray();
            for (int j=0;j<arXqf[k].length(); j++) {
              JSONObject oThis = arXqf[k].getJSONObject(j);
              if (oThis.getString("cat").equals(sCatName)) {
                // Add this object
                JSONObject oAdd = new JSONObject();
                oAdd.put("locs", oThis.getString("locs"));
                oAdd.put("locw", oThis.getString("locw"));
                oAdd.put("msg", oThis.getString("msg"));
                /*
                // Possibly copy database features
                if (oThis.has("locl")) oAdd.put("locl", oThis.getString("locl"));
                if (oThis.has("con")) oAdd.put("con", oThis.getString("con"));
                if (oThis.has("syn")) oAdd.put("syn", oThis.getString("syn"));
                if (oThis.has("eng")) oAdd.put("eng", oThis.getString("eng"));
                        */
                arCatRes.put(oAdd);
              }
            }
            oPerCat.put("results", arCatRes);
            // Add this element to percat
            arPerCat.put(oPerCat);
          }
        }
        oCombi.put("percat", arPerCat);
        arCombi.put(oCombi);
      }
      oHitInfo.put("hits", arCombi);

      // Original handling: keep the results available in the XqF job
      //   jobCaller.setJobResult(oHitInfo.toString());
      // New handling: store the results in a separate file
      // N.B: the path to this file must contain the project's name
      String sDir = this.crpThis.getHitsDir();
      File fResultDir = new File(sDir);
      if (!fResultDir.exists()) { fResultDir.mkdir(); }
      String sLoc = sDir + "/" + fThis.getName() + ".hits";
      File fResultXqF = new File(FileUtil.nameNormalize(sLoc));
      // Write the results to a file (give number of spaces to "toString" for 'pretty-print')
      FileUtil.writeFile(fResultXqF, oHitInfo.toString());
      // Store the filename, so that the calling JobXq knows where the results are
      jobCaller.setJobResult(fResultXqF.getAbsolutePath());
      
      // Get the lexicon results for this XqF into a JSON object
      JSONObject oLexInfo = new JSONObject();
      oLexInfo.put("file", fName);
      // oLexInfo.put("subtype", oCrpFile.currentPeriod);   // Dit doen of niet?  
      JSONObject oLexCombi;
      JSONArray arLexCombi = new JSONArray();
      for (int k=0;k<oCrpFile.lstLexDict.size();k++) {
        // Do we have entries for this QC?
        oLexCombi = new JSONObject();
        LexDict ldThis = oCrpFile.lstLexDict.get(k);
        oLexCombi.put("QC", ldThis.QC);
        JSONArray arLexDict = new JSONArray();
        for (int m=0;m<ldThis.lDict.size();m++) {
          JSONObject oLexEl = new JSONObject();
          oLexEl.put("word", ldThis.getWord(m));
          oLexEl.put("pos", ldThis.getPos(m));
          oLexEl.put("freq", ldThis.getFreq(m));
          // Add this element to the current dictionary
          arLexDict.put(oLexEl);
        }
        oLexCombi.put("dict", arLexDict);
        // Add this dictionary to the others
        arLexCombi.put(oLexCombi);
      }
      oLexInfo.put("lexdicts", arLexCombi);      
      
      // Store the lexicon results for this XqF in a separate file
      sLoc = this.crpThis.getLexName(fName);
      File fLexDictXqF = new File(FileUtil.nameNormalize(sLoc));
      FileUtil.writeFile(fLexDictXqF, oLexInfo.toString());
      
      // Pass on the number of hits for this XqF job
      JSONArray arHitsCount = new JSONArray();
      JSONObject oCount;
      for (int k=0;k<arQuery.length;k++) {
        oCount = new JSONObject();
        oCount.put("qc", k+1);                  // Number of this QC line
        oCount.put("count", arXqf[k].length());
        // oCount.put("name", arQuery[k].Descr);   // Name of this QC line
        oCount.put("sub", arXqfSub[k]);
        arHitsCount.put(oCount);
      }
      oCount = new JSONObject();
      oCount.put("file", fThis.getName());
      oCount.put("hits", arHitsCount);
      jobCaller.setJobCount(oCount);
      
      // Pass on the arXqf information for this XqF job in job.getJobList
      JSONObject oTotal = new JSONObject();
      oTotal.put("file", fThis.getName());
      oTotal.put("textid", sTextId);
      oTotal.put("subtype", strSubType);
      JSONArray arTotalHits = new JSONArray();
      for (int k=0;k<arQuery.length;k++) {
        JSONObject oTotalHit = new JSONObject();
        oTotalHit.put("qc", k+1);
        oTotalHit.put("result", arQuery[k].Descr);
        oTotalHit.put("qchits", arXqf[k]);
        arTotalHits.put(oTotalHit);
      }
      oTotal.put("hits", arTotalHits);
      jobCaller.setJobList(oTotal);


      // Return positively
      return true;
    } catch (RuntimeException | XPathExpressionException ex) {
      // Warn user
      errHandle.DoError("ExecutePsdxStream/ExecuteQueriesFile runtime error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    } 
  }
// </editor-fold>   
}
