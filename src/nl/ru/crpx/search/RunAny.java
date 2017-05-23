/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.search;

import java.util.List;
import nl.ru.crpx.dataobject.DataObject;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.util.json.JSONObject;

/**
 *
 * @author erwin
 */
public abstract class RunAny implements Runnable {
// <editor-fold defaultstate="collapsed" desc="Variables">
  static long nextJobId = 0;                // The @id for the next job started
  long id = nextJobId++;                    // This job gets its own unique @id
  // ============== Variables for all sub-classes extending me =================
  protected ErrHandle errHandle;            // The error handler (we use the one handed over to us)
  
  protected long startedAt;                 // Start time of job (or -1 if not yet started)
  protected long finishedAt;                // Finish time or -1 if not yet finished
  protected long lastAccessed;              // Last time this search was accessed
  protected String userId;                  // ID of user attached to this job
  protected String jobResult;               // String representation of the result of this job
  protected String jobStatus;               // Status returned by this job
  protected List<JSONObject> jobErrList;    // List of errors
  protected JSONObject jobCount;            // Counts for this job
  protected JSONObject jobList;             // Results for this Xqf job
  protected JSONObject jobProgress;         // Details of where we are in this job
  protected List<String> jobMsgList;        // List of messages supplied by ru:message()
  protected String jobQuery;                // The query string associated with this job (passed on via SearchParameters)
  protected DataObject jobBack;             // What is returned
  protected String jobName;                 // The kind of job this is
// </editor-fold>
  public RunAny(ErrHandle oErr, SearchParameters par) {
    this.errHandle = oErr;
    jobStatus = "";
    jobResult = "";
    userId = "";
    jobBack = null;
    // Initialise the query string 
    if (par.containsKey("query")) {
      jobQuery = par.getString("query");
    } else {
      jobQuery = "";
    }
    if (par.containsKey("jobtype")) {
      jobName = par.getString("jobtype");
    } else {
      jobName = "";
    }
  }
  
// <editor-fold desc="Abstract methods that must be implemented">
  public abstract void close();
// </editor-fold>

// <editor-fold desc="AboutThisJob">
  public boolean finished(){ return (this.jobStatus.equals("finished") || 
          this.jobStatus.equals("error") || 
          this.jobStatus.equals("interrupt")); };  
  public String getJobId() {return String.valueOf(id);}
  
  public String getJobStatus() {return jobStatus;}
  public void setJobStatus(String sThis) { jobStatus = sThis;}
  public List<JSONObject> getJobErrors() { return jobErrList;}
  public synchronized void setJobErrors(List<JSONObject> arErr) {if (arErr==null) return; jobErrList = arErr;}
  public JSONObject getJobCount() { return jobCount;}
  public void setJobCount(JSONObject oCount) { jobCount = oCount;}
  public JSONObject getJobList() { return jobList;}
  public void setJobList(JSONObject oCount) { jobList = oCount;}
  public List<String> getJobMessage() {return jobMsgList;}
  public void setJobMessage(List<String> lstMsg) {jobMsgList = lstMsg;}
  public String getJobResult() {return jobResult;}
  public void setJobResult(String sData) { jobResult = sData;}
  public String getJobQuery() {return jobQuery;}
  public DataObject getJobBack() { return jobBack;}
// </editor-fold>
  
}
