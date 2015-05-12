/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.project;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.ru.crpx.search.Job;
import nl.ru.crpx.search.JobXq;
import nl.ru.crpx.search.JobXqF;
import nl.ru.crpx.search.QueryException;
import nl.ru.crpx.search.SearchParameters;
import nl.ru.crpx.xq.CrpFile;
import nl.ru.crpx.xq.RuBase;
import nl.ru.util.StringUtil;

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
  // ============ Local variables =====================================
  List<JobXqF> arJob = new ArrayList<>(); // A list of all the current XqF jobs running
  List<String> arRes = new ArrayList<>(); // The results of each job

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
        SearchParameters searchXqF = new SearchParameters(this.searchMan);
        searchXqF.put("crpfileid", Integer.toString(iCrpFileId));
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
          search = searchMan.searchXqF(crpThis, userId, searchXqF);
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
  
}
