/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

package nl.ru.crpx.search;

import java.io.File;

/**
 *
 * @author Erwin R. Komen
 */
public class JobXq extends Job {
// <editor-fold defaultstate="collapsed" desc="Variables">
  // ========== Variables needed for this Xq search job ========================
  public int intPrecNum;                    // Number of preceding context lines
  public int intFollNum;                    // Number of following context lines
  public int intCurrentQCline = 0;          // The current QC line we are working on
  // NO: this should be part of CrpFile...
  // public XmlNode ndxCurrentHeader = null;   // XML header of the current XML file
  public boolean ru_bFileSaveAsk = false;   // Needed for ru:setattrib()
  public boolean bTraceXq = false;          // Trace on XQ processing
  String sQueryXq = "";                 // Copy of the query
  File fInput;                              // The file to be searched
// </editor-fold>
  // =================== Class initialisation ==================================
  public JobXq(SearchManager searchMan, String userId, SearchParameters par) {
    // Make sure the class I extend is initialized
    super(searchMan, userId, par);
    // Other initializations for this Xq search job
    intFollNum = crpThis.getFollNum();
    intPrecNum = crpThis.getPrecNum();
    this.currentuserId = userId;
  }
  
  // ======================= Perform the search ================================
  @Override
  public void performSearch() throws QueryException {
    String sStatus = "started";  // My copy of the job status
    
    try {
      // Validate
      if (crpThis==null) { errHandle.DoError("There is no CRP"); return;}
      // Note start time
      long startTime = System.currentTimeMillis();
      // Set the query 
      sQueryXq = par.getString("query");
      // Store the user/session id in our local list
      Integer iTaskNumber = addUserJob("jobxq", currentuserId, id, sQueryXq);

      // Set the job status to indicate that we are working
      this.jobStatus = "working";
      
      // Execute queries
      if (crpThis.Execute(this, this.userId)) {
        // Check for interrupt
        if (errHandle.bInterrupt) {
          errHandle.DoError("JobXq: The program has been interrupted");
          sStatus = "error";
        } else {
          // There is no need to say anything here
          errHandle.debug("JobXq: performSearch: ready handling job");
          sStatus = "completed";
        }
      } else {
        errHandle.DoError("JobXq: The queries could not be executed");
        sStatus = "error";
      }
      // Note finish time
      long stopTime = System.currentTimeMillis();
      long elapsedTime = stopTime - startTime; 
      // Log the time
      errHandle.debug("Query time: " + elapsedTime + " (ms)");
      
      // Set the job status
      this.jobStatus = sStatus;
      // Set the task number, since it has been successfully completed
      jobTaskId = iTaskNumber;    
      // Check if an error message needs to be generated
      if (!sStatus.equals("completed")) {
        // Store an error message in the job-resultß
        this.jobResult = errHandle.getErrList().toString();
      }
      
    } catch (Exception ex) {
      // Show the error
      errHandle.DoError("Could not perform search", ex, JobXq.class);
    }
  }
}
