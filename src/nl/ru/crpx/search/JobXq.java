/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

package nl.ru.crpx.search;

import java.io.File;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.xq.CrpFile;
import nl.ru.crpx.xq.Extensions;
import nl.ru.xmltools.XmlNode;
import org.w3c.dom.Node;

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
  String sReqArgument = "";                 // Copy of the query
  File fInput;                              // The file to be searched
// </editor-fold>
  // =================== Class initialisation ==================================
  public JobXq(SearchManager searchMan, String userId, SearchParameters par) {
    // Make sure the class I extend is initialized
    super(searchMan, userId, par);
    // Other initializations for this Xq search job
    intFollNum = crpThis.getFollNum();
    intPrecNum = crpThis.getPrecNum();
    // Set the query 
    sReqArgument = par.getString("query");
    this.currentuserId = userId;
  }
  
  // ======================= Perform the search ================================
  @Override
  public void performSearch() throws QueryException {
    try {
      // Validate
      if (crpThis==null) { errHandle.DoError("There is no CRP"); return;}
      // Note start time
      long startTime = System.currentTimeMillis();
      // Store the user/session id in our local list
      Integer iTaskNumber = addUserJob("jobxq", currentuserId, id, sReqArgument);
      // Execute queries
      if (crpThis.Execute(this, this.userId)) {
        // Check for interrupt
        if (errHandle.bInterrupt) {
          errHandle.DoError("JobXq: The program has been interrupted");
        } else {
          // There is no need to say anything here
          errHandle.debug("JobXq: performSearch: ready handling job");
        }
      } else {
        errHandle.DoError("JobXq: The queries could not be executed");
      }
      // Note finish time
      long stopTime = System.currentTimeMillis();
      long elapsedTime = stopTime - startTime; 
      // Log the time
      errHandle.debug("Query time: " + elapsedTime + " (ms)");
      
      // Set the job status
      this.jobStatus = "completed";
      // Set the task number, since it has been successfully completed
      jobTaskId = iTaskNumber;      
      
    } catch (Exception ex) {
      // Show the error
      errHandle.DoError("Could not perform search", ex, JobXq.class);
    }
  }
}
