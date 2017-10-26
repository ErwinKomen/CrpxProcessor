/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

package nl.ru.crpx.cmd;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import nl.ru.crpx.dataobject.DataObject;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.search.SearchManager;
import nl.ru.crpx.search.SearchParameters;
import nl.ru.crpx.search.WorkQueueXqF;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.util.json.JSONObject;

/**
 * Base class for the command-line request-handlers that handle the different
 * requests that can be made from the command-line interface.
 * A similar class exists for processing requests by a web-service interface.
 * 
 * @author Erwin R. Komen
 */
public abstract class RequestHandler {
  // ============== My own error handler =======================================
  static final ErrHandle errHandle = new ErrHandle(RequestHandler.class);
  // ============== Variables belonging to the request handler ================
  static String userId = "";  // Default user id
  String sReqArgument = "";   // 
  String sLastReqArg = "";    // 
  String sCrpFile = "";       // Full path of the CRP file we are handling
  
  /** Search parameters from request */
  SearchParameters searchParam;

  /** The search manager, which executes and caches our searches */
  SearchManager searchMan;
  
  /** The work queue to handle XqF jobs */
  WorkQueueXqF workQueue;
  ExecutorService workExecutor;
  final int loc_iThreadPoolSize = 5;     // Adapt this number if it is too much or too little
  
  /** The servlet */
  CrpxProcessor servlet;
  
  /** The corpus research project we are processing */
  CorpusResearchProject prjThis;
  String strProject;

  // ============== Class initiator ============================================
  RequestHandler(CrpxProcessor servlet, String indexName, CorpusResearchProject crpThis) {
    try {
      // Get the search manager from the calling CrpxProcessor
      this.searchMan = servlet.getSearchManager();
      this.searchParam = servlet.getSearchParameters(indexName);
      // Add the project name as parameter
      this.searchParam.put("query", crpThis.getName());
      // Get my copy of the project
      this.prjThis = crpThis;
      // Set the project type manager for the CRP
      crpThis.setPrjTypeManager(servlet.getPrjTypeManager());
      crpThis.setSearchManager(this.searchMan);
      // Get the current user/session ID from the system
      this.userId = System.getProperty("user.name");
      // Set the work queue for the CRP
      this.workQueue = new WorkQueueXqF(errHandle, this.userId, loc_iThreadPoolSize);
      crpThis.setWorkQueue(this.workQueue);
      this.workExecutor = Executors.newFixedThreadPool(loc_iThreadPoolSize);
      crpThis.setWorkExecutor(this.workExecutor);
      // Get the name of the project
      strProject = crpThis.getName();
      // Take over the calling servlet
      this.servlet = servlet;

      // Set up a Request Argument JSON string, mimicking server processing
      sReqArgument = "{ \"userid\": \"" + userId + "\", " + 
              "\"query\": \"" + strProject + "\"}";
    } catch (Exception ex) {
      errHandle.DoError("Could not create [RequestHandler]", ex, RequestHandler.class);
    }
  }
  
  /**
   * Handle a request by dispatching it to the corresponding subclass.
   *
   * @param servlet the servlet object
   * @param indexName
   * @param crpThis
   * @return the response data
   * @throws java.io.UnsupportedEncodingException
   */
  public static DataObject handle(CrpxProcessor servlet, String indexName, CorpusResearchProject crpThis) throws UnsupportedEncodingException {
    try {
      // Initialize the userId from the system
      // (Note: subsequent code may get a better userId from the request object)
      userId = System.getProperty("user.name");

      // Choose the RequestHandler subclass
      RequestHandler requestHandler = null;
      switch (indexName) {
        case "execute":
          requestHandler = new RequestHandlerExecute(servlet, indexName, crpThis);
          break;
        case "show":
          requestHandler = new RequestHandlerShow(servlet, indexName, crpThis);
          break;
        default:
          // There is no default handling; requesthandler stays NULL
          break;
      }

      // Make sure we catch empty requesthandlers
      if (requestHandler == null)
        return DataObject.errorObject("INTERNAL_ERROR", "RequestHandler is empty. Use: /execute, /show");

      // Handle the request
      try {
        return requestHandler.handle();
      } catch (InterruptedException e) {
        return DataObject.errorObject("INTERNAL_ERROR", internalErrorMessage(e, false, 8));
      }
      
    } catch (RuntimeException ex) {
      errHandle.DoError("Handle error", ex, RequestHandler.class);
      return null;
    }
  }
  
  /**
   * Get the user id.
   *
   * Used for logging and making sure 1 user doesn't run too many queries.
   *
   * Right now, we simply use the session id, or the value sent in a parameter.
   *
   * @return the unique user id
   */
  public String getUserId() {
    return userId;
  }

  public void setUserId(String sNewId) {
    userId = sNewId;
  }

  /**
   * Get the correct user id.
   *
   * This returns the userid set by the caller or else our own userid. (Our own
   * user id is the session id.)
   *
   * @return the unique user id
   */
  public String getAdaptedUserId(String sReq) {
    // Convert the request string into a json object
    JSONObject jReq = new JSONObject(sReq);
    // Check if there is a "userid" string
    if (jReq.has("userid")) {
      // Return the caller's userid
      return jReq.getString("userid");
    } else {
      // Return the user id we have over here
      return userId;
    }
  }

  /**
   * Return the start of the user id.
   *
   * Useful for logging; should be enough for unique identification.
   *
   * @return the unique session id
   */
  public String shortUserId() {
    return getUserId().substring(0, 6);
  }
  /**
   * Child classes should override this to handle the request.
   * @return the response object
   * @throws java.lang.InterruptedException
   */
  public abstract DataObject handle() throws InterruptedException;
  
  public static String getCurrentTimeStamp() {
    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
    Date now = new Date();
    String strDate = sdfDate.format(now);
    return strDate;
  }

  public static String internalErrorMessage(Exception e, boolean debugMode, int code) {
    if (debugMode) {
      return e.getClass().getName() + ": " + e.getMessage() + " (Internal error code " + code + ")";
    }
    return "An internal error occurred. Please contact the administrator.  Error code: " + code + ".";
  }

}
