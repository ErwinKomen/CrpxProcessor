/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

package nl.ru.crpx.cmd;

import nl.ru.crpx.dataobject.DataObject;
import nl.ru.crpx.dataobject.DataObjectMapElement;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.search.Job;

/**
 *
 * @author Erwin R. Komen
 */
public class RequestHandlerExecute extends RequestHandler {

  // ============= Class initializer ===========================================
  public RequestHandlerExecute(CrpxProcessor servlet, String indexName, CorpusResearchProject crpThis) {
    super(servlet, indexName, crpThis);
  }
  
  @Override
  public DataObject handle() throws InterruptedException {
    try {
      // Initial status
      errHandle.debug("REQ execute");
      // Validate
      if (prjThis == null) return null;
      // Retrieve and *keep* our own version of the current user/session id
      String sCurrentUserId = getAdaptedUserId(sReqArgument);
      // Create a job for this query
      Job search;
      // Initiate the search
      search = searchMan.searchXq(prjThis, userId, searchParam);
      // Get the @id of the job that has been created
      String sThisJobId = search.getJobId();
      String sNow = Job.getCurrentTimeStamp();
      // Additional debugging to find out where the errors come from
      errHandle.debug("Xqjob [" + sNow + "] userid=[" + sCurrentUserId + "] jobid=[" + 
              sThisJobId + "], finished=" + 
              search.finished() + " status=" + search.getJobStatus() );
      
      // If search is not done yet, indicate this to the user
      if (!search.finished()) {
        return DataObject.statusObject("started", "Searching, please wait...", sCurrentUserId, sThisJobId, 
                servlet.getSearchManager().getCheckAgainAdviceMinimumMs());
      }

      // Search is done; Create a JSONObject with the correct status and content parts

      // Assemble all the parts
      DataObjectMapElement response = new DataObjectMapElement();
      
      return response;
    } catch (Exception ex) {
      return null;
    }
  }
}
