package nl.ru.crpx.search;

// <editor-fold defaultstate="collapsed" desc="Imports">
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nl.ru.crpx.dataobject.DataObjectMapElement;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.xq.Extensions;
import nl.ru.util.ExUtil;
import nl.ru.util.json.JSONArray;
import nl.ru.util.json.JSONObject;
// </editor-fold>
/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

/**
 *
 * @author Erwin R. Komen
 */
public abstract class Job implements Comparable<Job> {
// <editor-fold defaultstate="collapsed" desc="Variables">
  // ============== The error handler can be accessed globally =================
  public static ErrHandle errHandle;
  public CorpusResearchProject crpThis;
  static long nextJobId = 0;                // The @id for the next job started
  long id = nextJobId++;                    // This job gets its own unique @id
  int clientsWaiting = 0;                   // Number of clients waiting for Job result
  /* This is used to allow clients to cancel long searches: if this number reaches
   * 0 before the search is done, it may be cancelled. Jobs that use other Jobs will
   * also count as a client of that Job, and will tell that Job they're no longer interested
   * if they are cancelled themselves.
   */
  Set<Job> waitingFor = new HashSet<Job>(); // List of jobs we are waiting for
  // ============== Local variables ============================================
  private SearchThread searchThread = null; // The thread used by this particular job
  private boolean performCalled = false;    // Make sure perform() is only called once
  // ============== Variables for all sub-classes extending me =================
  protected long startedAt;                 // Start time of job (or -1 if not yet started)
  protected long finishedAt;                // Finish time or -1 if not yet finished
  protected long lastAccessed;              // Last time this search was accessed
  protected SearchManager searchMan;        // The overal search manager containing the 'config' paramter values
  protected SearchParameters par;           // Parameters specified for this particular search
  protected String userId;                  // ID of user attached to this job
  protected String jobResult;               // String representation of the result of this job
  protected String jobStatus;               // Status returned by this job
  protected String currentuserId;           // Who created this job?
  protected String jobQuery;                // The query string associated with this job (passed on via SearchParameters)
  protected JSONObject jobCount;            // Counts for this job
  protected JSONObject jobProgress;         // Details of where we are in this job
  protected int jobPtc;                     // Percentage progress for this job
  protected boolean reusable = true;        // The job can be re-used if it was not interrupted
  protected Integer jobTaskId = -1;         // The task-id of this job
  protected Job parent = null;              // The 'parent' job I am under

// </editor-fold>
  // ============== Class initialisation ======================================
  public Job(SearchManager searchMan, String userId, SearchParameters par) {
    // Set my copy of the corpus research project we are dealing with
    crpThis = searchMan.getCrp();
    // initialize my own error handler
    errHandle = crpThis.errHandle;
    // Set my copy of the search manager
    this.searchMan = searchMan;
    // Set my copy of the user
    this.userId = userId;
    this.currentuserId = userId;
    // Set my copy of the search parameters
    this.par = par;
    // Initialize results
    jobResult = ""; jobCount = new JSONObject(); jobStatus = "initialized";
    jobProgress = new JSONObject();
    // Initialise the query string 
    jobQuery = par.getString("query");
    // Make sure the finishing time is initialized correctly
    finishedAt = -1;
  }
  // ============= Performing a search 'template' ==============================
  @SuppressWarnings("unused")
  protected void performSearch() throws InterruptedException, QueryException {
    // (this is overridden in all specific JobXX implementations)
  }

// <editor-fold defaultstate="collapsed" desc="Waiting">
  // ========= Handle waiting for list =========================================
  protected void addToWaitingFor(Job j) { waitingFor.add(j);  }
  protected void removeFromWaitingFor(Job j) { waitingFor.remove(j); }
  /**
   * Wait for the specified job to finish
   * @param job the job to wait for
   * @throws InterruptedException
   * @throws QueryException
   */
  protected void waitForJobToFinish(Job job) throws InterruptedException, QueryException {
          waitingFor.add(job);
          job.waitUntilFinished();
          waitingFor.remove(job);
  }
  /**
   * Wait until this job's finished, an Exception is thrown or the specified
   * time runs out.
   *
   * @param maxWaitMs maximum time to wait, or a negative number for no limit
   * @throws InterruptedException if the thread was interrupted
   * @throws QueryException
   * @throws IndexOpenException
   */
  public void waitUntilFinished(int maxWaitMs) throws InterruptedException, QueryException {
    int defaultWaitStep = 100;
    while (searchThread == null || (maxWaitMs != 0 && !searchThread.finished())) {
      int w = maxWaitMs < 0 ? defaultWaitStep : Math.min(maxWaitMs, defaultWaitStep);
      Thread.sleep(w);
      if (maxWaitMs >= 0)
        maxWaitMs -= w;
    }
    // If an Exception occurred, re-throw it now.
    rethrowException();
  }
  /**
   * Wait until this job is finished (or an Exception is thrown)
   *
   * @throws InterruptedException
   * @throws QueryException
   */
  public void waitUntilFinished() throws InterruptedException, QueryException {
    waitUntilFinished(-1);
  }
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Comparable">
  /**
   * Compare based on last access time.
   *
   * @param o the other search, to compare to
   * @return -1 if this search is staler than o;
   *   1 if this search is fresher o;
   *   or 0 if they are equally fresh
   */
  @Override
  public int compareTo(Job o) {
    long diff = lastAccessed - o.lastAccessed;
    if (diff == 0)
      return 0;
    return diff > 0 ? 1 : -1;
  }

  public long getLastAccessed() {
    return lastAccessed;
  }

  public void resetLastAccessed() {
    lastAccessed = System.currentTimeMillis();
  }
// </editor-fold>
  /**
   * Create a new Search (subclass) object to carry out the specified search,
   * and call the perform() method to start the search.
   *
   * @param objPrj the Corpus Research Project being executed
   * @param searchMan the servlet
   * @param userId user creating the job
   * @param par search parameters
   * @return the new Search object
   * @throws QueryException
   */
  public static Job create(SearchManager searchMan, String userId, SearchParameters par) throws QueryException {
    Job search = null;
    String jobClass = par.getString("jobclass");
    // Check what kind of job this is and call the specific job execution
    switch (jobClass) {
      case "JobXq":
        search = new JobXq(searchMan, userId, par);
        break;
      case "JobXqF":
        search = new JobXqF(searchMan, userId, par);
        break;
      default:
        throw new QueryException("INTERNAL_ERROR",
                "An internal error occurred. Please contact the administrator.\n" +
                        "Unknown jobClass [" + jobClass + "]\n" +
                        "Error code: 1.");
    }

    return search;
  }

// <editor-fold desc="Job performance">
  /** Perform the search.
   *
   * @param waitTimeMs if < 0, method blocks until the search is finished. For any
   *   value >= 0, waits for the specified amount of time or until the search is finished,
   *   then returns.
   *
   * @throws QueryException on parse error or other query-related error (e.g. too broad)
   * @throws InterruptedException if the thread was interrupted
   */
  final public void perform(int waitTimeMs) throws QueryException, InterruptedException {
    if (performCalled)
      throw new RuntimeException("Already performing search!");

    // Create and start thread
    // TODO: use thread pooling..?
    startedAt = System.currentTimeMillis();
    searchThread = new SearchThread(this);
    searchThread.start();
    performCalled = true;
    clientsWaiting++; // someone wants to know the answer

    waitUntilFinished(waitTimeMs);
  }
  
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Finishing">
  // =============== Job finish handling =======================================
  /**
   * Is this search operation finished?
   * (i.e. can we start working with the results?)
   *
   * @return true iff the search operation is finished and the results are available
   */
  public boolean finished() {
    if (searchThread == null)
      return false;
    return performCalled && searchThread.finished();
  }
  /**
   * Should this job be cancelled?
   *
   * True if the job hasn't finished and there are no more clients
   * waiting for its results.
   *
   * @return true iff the job should be cancelled
   */
  public boolean shouldBeCancelled() {
    return !finished() && clientsWaiting == 0;
  }

  /**
   * Change how many clients are waiting for the results of this job.
   * @param delta how many clients to add or subtract
   */
  public void changeClientsWaiting(int delta) {
    clientsWaiting += delta;
    // if (clientsWaiting < 0)
    //   error(logger, "clientsWaiting < 0 for job: " + this);
    if (shouldBeCancelled()) {
      cancelJob();
    }
  }
  /**
   * Inform how many clients are waiting for this job to finish.
   */
  public int getClientsWaiting() { return clientsWaiting; }  
  
  /**
   * Try to cancel this job.
   */
  public void cancelJob() {
    if (searchThread == null)
      return; // can't cancel, hasn't been started yet (shouldn't happen)
    searchThread.interrupt();

    // Tell the jobs we were waiting for we're no longer interested
    for (Job j: waitingFor) {
      j.changeClientsWaiting(-1);
    }
    waitingFor.clear();
  }

  /**
   * How long this job took to execute (so far).
   * @return execution time in ms
   */
  public int executionTimeMillis() {
    if (startedAt < 0)
      return -1;
    if (finishedAt < 0)
      return (int)(System.currentTimeMillis() - startedAt);
    return (int)(finishedAt - startedAt);
  }

// </editor-fold>
// <editor-fold desc="AboutThisJob">
  public SearchParameters getParameters() {return par;}
  public String getJobId() {return String.valueOf(id);}
  public String getJobResult() {return jobResult;}
  public void setJobResult(String sData) { jobResult = sData;}
  public JSONObject getJobCount() { return jobCount;}
  public void setJobCount(JSONObject oCount) { jobCount = oCount;}
  public JSONObject getJobProgress() { return jobProgress;}
  public void setJobProgress(JSONObject oCount) { jobProgress = oCount;}
  public String getJobStatus() {return jobStatus;}
  public void setJobStatus(String sThis) { jobStatus = sThis;}
  public String getJobQuery() {return jobQuery;}
  public int getJobPtc() { return jobPtc;}
  public void setJobPtc(int iThis) {jobPtc = iThis;}
  public Integer getJobTaskId() { return jobTaskId; }
  private String shortUserId() {return userId.substring(0, 6);}
  public void setUnusable() { reusable = false; }  
  public boolean getUsable() { return reusable;}
  public void setParent(Job p) { parent = p;}
  @Override
  public String toString() {
    return id + ": " + par.toString();
  }
  public DataObjectMapElement toDataObject() {
    DataObjectMapElement stats = new DataObjectMapElement();
    stats.put("clientsWaiting", clientsWaiting);
    stats.put("waitingForJobs", waitingFor.size());
    stats.put("startedAt", (startedAt - searchMan.createdAt)/1000.0);
    stats.put("finishedAt", (finishedAt - searchMan.createdAt)/1000.0);
    stats.put("lastAccessed", (lastAccessed - searchMan.createdAt)/1000.0);
    stats.put("createdBy", shortUserId());
    stats.put("threadFinished", searchThread == null ? false : searchThread.finished());

    DataObjectMapElement d = new DataObjectMapElement();
    d.put("id", id);
    d.put("class", getClass().getSimpleName());
    d.put("searchParam", par.toDataObject());
    d.put("stats", stats);
    return d;
  }
  // This is a default to 1M, which is arbitrarily
  public long estimateSizeBytes() {    return 1000000; }

  
// </editor-fold>
// <editor-fold desc="User-Job listing">
  /** A list of user-session ids that have requested this job */
  static List<JSONObject> lUserJob = new ArrayList<>();
  /** 
   * Add to the userjob list: a combination of jobname, userid, jobid 
   * 
   * @param sJob
   * @param sUser
   * @param iJob
   * @param sQuery
   * 
   * @return the task number for @sUser
   */
  public static Integer addUserJob(String sJob, String sUser, long iJob, String sQuery) {
    Integer iTasks = 0;   // Number of tasks for user @sUser
    // Create a new object for the stack
    JSONObject oThis = new JSONObject();
    // Set the values for this object
    oThis.put("job", sJob);
    oThis.put("userid", sUser);
    oThis.put("timestamp", getCurrentTimeStamp());
    oThis.put("jobid", iJob);
    oThis.put("query", sQuery);
    // Add object to stack
    lUserJob.add(oThis);
    // Check how many objects there are for user [sUser]
    for (Integer i = 0; i< lUserJob.size(); i++) {
      // Get this object
      oThis = lUserJob.get(i);
      // See if the user id coincides
      if (sUser.equals(oThis.get("userid"))) {
        // Increment number of tasks for this user
        iTasks++;
      }
    }
    // Debugging
    errHandle.debug("Job addUserJob " + sJob + " user=[" + sUser + "] job=[" + 
            String.valueOf(iJob) + "] task=" + String.valueOf(iTasks) +
            " query=[" + sQuery + "]");
    // ========== DEBUGGING =======
    // showUserJob();
    // ============================
    // Return the number of tasks for this user
    return iTasks;
  }
  
  /** 
   * getUserJobList
   * 
   *  List all "jobrx" jobs belonging to user @sUser 
   * 
   * @param sUser     The unique string for a user/window/location-on-window
   * @param sJobType  The type of job (jobrx) for example
   * 
   * @return the number of unwaited jobs
   */
  public static List<Long> getUserJobList(String sUser, String sJobType) {
    JSONObject oThis ;          // One user-job object
    List<Long> lJobList = new ArrayList<>(); // List of jbos
    
    // Visit all the jobs kept in the [lUserJob] list
    for (Integer i = 0; i< lUserJob.size(); i++) {
      // Get this object
      oThis = lUserJob.get(i);
      // See if the user id coincides and this is a "jobrx"
      if (sJobType.equals(oThis.get("job")) && sUser.equals(oThis.get("userid"))) {
        // Add this job id
        lJobList.add((Long) oThis.get("jobid"));
      }
    }
    // Return our result
    return lJobList;
  }
  /**
   * showUserJob
   * 
   *  return a list with the jobs per user
   * 
   * @return 
   */
  public static String showUserJob() {
    // Create an array to be returned
    JSONArray aBack = new JSONArray();
    // Create a new object for the stack
    JSONObject oThis;
    JSONObject oQuery;
    JSONObject oBack;
    String sUser;
    Integer iJob;
    String sQuery;
    String sTimestamp;
    String sBack;
    
    // Go through the cache and show its contents
    for (Integer i = 0; i< lUserJob.size(); i++) {
      // Get this object
      oThis = lUserJob.get(i);
      sUser = oThis.getString("userid");
      iJob = oThis.getInt("jobid");
      oQuery = new JSONObject(oThis.getString("query"));
      sTimestamp = oThis.getString("timestamp");
      sQuery = oQuery.getJSONArray("srchTerms").getString(0);
      // Show contents
      errHandle.debug("Job date=[" + sTimestamp + "] list[" + i + "]: user=[" + sUser + "] job=[" + 
              String.valueOf(iJob) + "] query=" + sQuery);
      oBack = new JSONObject();
      oBack.put("userid", sUser);
      oBack.put("jobid", iJob);
      oBack.put("query", sQuery);
      // Add content to array
      aBack.put(oBack);
    }
    // Return the resulting array
    sBack = aBack.toString();
    return sBack;
  }
  
  /** 
   * Retrieve the last @jobid done by user @sUser
   * Give that job id and then remove it from the stack
   * 
   * @param sUser
   * 
   * @return the jobid or else -1
   */
  public static long popUserJob(String sUser) {
    JSONObject oThis;   // Object from the stack
    JSONObject oRemove; // The object to be removed
    long iJob = -1;     // Job id we are returning
    Integer i = lUserJob.size();
    Integer j;
    
    // Walk all jobs from last to first
    while (--i >= 0) {
      // Get this job
      oThis = lUserJob.get(i);
      // Check if this job belongs to the presented user
      if (sUser.equals(oThis.getString("userid"))) {
        // We found the last job done by this user
        // Remove the object from the list
        lUserJob.remove(oThis);
        // Now look for the next job's id
        j = lUserJob.size();
        while (--j >= 0) {
          // Get this job
          oThis = lUserJob.get(j);
          // Check if this job belongs to the presented user
          if (sUser.equals(oThis.getString("userid"))) {
            // We found the penultimate job done by this user
            iJob = oThis.getInt("jobid");
            // ========== DEBUGGING =======
            // showUserJob();
            // ============================
            // Return the correct result
            return iJob;
          }
        }
        // Leave this loop
        break;
      }
    }
    // ========== DEBUGGING =======
    // showUserJob();
    // ============================
    // Return -1, because normally I would have left at the "return" above
    return iJob;
  }
  
  /** 
   * Retrieve the @jobid done by user @sUser as task number @iTask
   * Note: task numbers start with 1 (NOT with zero)
   * 
   * @param sUser
   * @param iTask
   * 
   * @return the jobid or else -1
   */
  public static long getJobFromTask(String sUser, Integer iTask) {
    JSONObject oThis;   // Object from the stack
    long iJob = -1;     // Job id we are returning
    Integer i;          // Counter within the UserJob list
    Integer j = 0;      // Task number for this user
    // Walk all jobs from last to first
    for (i=0;i<lUserJob.size(); i++) {
      // Get the job at this place
      oThis = lUserJob.get(i);
      // Check if this job belongs to the presented user
      if (sUser.equals(oThis.getString("userid"))) {
        // We found a job done by this user
        // Increment our task number
        j++;
        // Check if this is the desired task
        if (iTask == j) {
          // Get tje jobid of this one
          iJob = oThis.getInt("jobid");
          // Leave the for-loop
          break;
        }
      }
    }
    // ========== DEBUGGING =======
    showUserJob();
    // ============================
    // Return the job id we found
    return iJob;
  }
  
  /**
   * Get the query belonging to the jobid @iJobId
   * 
   * @param iJobId
   * @return the query belonging to @iJobId as a string
   */
  public static String getQueryFromJob(Integer iJobId) {
    JSONObject oThis;     // Object from the stack
    String sQuery = "{}"; // The query string we are returning
    Integer i;            // Counter within the UserJob list

    // Walk all jobs from last to first
    for (i=0;i<lUserJob.size(); i++) {
      // Get the job at this place
      oThis = lUserJob.get(i);
      // Check if this has the right job id
      if (iJobId == oThis.getInt("jobid")) {
        // We found the correct job
        sQuery = oThis.getString("query");
        // Leave the for-loop
        break;
      }
    }
    // Return the job id we found
    return sQuery;
  }
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Exceptions">
  // =============== Exception handling ========================================
  /**
   * Did the search throw an exception?
   *
   * @return true iff the search operation threw an exception
   */
  public boolean threwException() {
    if (searchThread == null)
      return false;
    return finished() && searchThread.threwException();
  }

  /**
   * Get the exception thrown by the search thread, if any
   * @return the exception, or null if none was thrown
   */
  public Throwable getThrownException() {
    return threwException() ? searchThread.getThrownException() : null;
  }
  /**
   * Re-throw the exception thrown by the search thread, if any.

   * @throws IndexOpenException
   * @throws QueryException
   * @throws InterruptedException
   */
  public void rethrowException() throws QueryException, InterruptedException {
    Throwable exception = getThrownException();
    if (exception == null)
      return;
    errHandle.debug("Re-throwing exception from search thread:\n" + exception.getClass().getName() + ": " + exception.getMessage());
    if (exception instanceof QueryException)
      throw (QueryException)exception;
    else if (exception instanceof InterruptedException)
      throw (InterruptedException)exception;
    throw ExUtil.wrapRuntimeException(exception);
  }
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Age">
  /**
   * Return this search's age in seconds.
   *
   * Age is defined as the time between now and the last time
   * it was accessed, but only for finished searches. Running
   * searches always have a zero age. Check executionTimeMillis() for
   * search time.
   *
   * @return the age in seconds
   */
  public int ageInSeconds() {
    if (finished())
      return (int) (System.currentTimeMillis() - lastAccessed) / 1000;
    return 0;
  }
  
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Extra's">
  public static String getCurrentTimeStamp() {
    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
    Date now = new Date();
    String strDate = sdfDate.format(now);
    return strDate;
  }
// </editor-fold>
}
