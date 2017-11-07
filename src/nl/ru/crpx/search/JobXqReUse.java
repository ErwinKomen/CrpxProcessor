/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

package nl.ru.crpx.search;

import java.io.File;
import nl.ru.crpx.dataobject.DataObject;
import nl.ru.crpx.dataobject.DataObjectList;
import nl.ru.crpx.dataobject.DataObjectMapElement;
import nl.ru.util.json.JSONArray;
import nl.ru.util.json.JSONObject;

/**
 *
 * @author Erwin R. Komen
 * @history 27/jul/2015
 */
public class JobXqReUse extends Job {
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
  public JobXqReUse(SearchManager searchMan, String userId, SearchParameters par) {
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
      // Show where we are
      errHandle.debug("Performsearch: JobXqReUse");
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
      errHandle.debug("xqReUse: set status 'working'");
      
      // Locate and fetch old jobXq results
      this.setJobResult(crpThis.getResultsOneTable("results"));
      errHandle.debug("xqReUse: [results]");
      /*
      this.setJobCount(new JSONObject(crpThis.getResultsOneTable("count")));
      errHandle.debug("xqReUse: [count]");*/
      this.setJobTable(getXqTable("table"));
      errHandle.debug("xqReUse: [table]");
      // Set the status correctly
      sStatus = "completed";
      errHandle.debug("xqReUse: set status 'completed'");
      
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
      errHandle.DoError("Could not perform search", ex, JobXqReUse.class);
    }
  }
  
  /**
   * getXqTable
   *    Read the JSONArray table and turn it into a DataObjectList
   * 
   * @param sType
   * @return 
   */
  DataObject getXqTable(String sType) {
    DataObjectList arCombi = new DataObjectList("QCline");
    try {
      // Read the results table as a JSON array
      JSONArray arInput = new JSONArray(crpThis.getResultsOneTable(sType));
      // Process all lines
      for (int i=0;i<arInput.length();i++) {
        // Get this object
        JSONObject oOneLine = arInput.getJSONObject(i);
        // Elements expected: qc, result, total, subcats[], counts[], hits[];
        DataObjectMapElement oOneQc = new DataObjectMapElement();
        oOneQc.put("qc", oOneLine.getInt("qc"));
        oOneQc.put("result", oOneLine.getString("result"));
        oOneQc.put("total", oOneLine.getInt("total"));
        // Treate subcats[]
        JSONArray arSubNames = oOneLine.getJSONArray("subcats");
        DataObjectList aSubNames = new DataObjectList("subcat");
        for (int j=0;j<arSubNames.length();j++) {aSubNames.add(arSubNames.getString(j)); }
        oOneQc.put("subcats", aSubNames);
        // Treat counts[]
        JSONArray arSubCount = oOneLine.getJSONArray("counts");
        DataObjectList aSubCount = new DataObjectList("count");
        for (int j=0;j<arSubCount.length();j++) {aSubCount.add(arSubCount.getInt(j)); }
        oOneQc.put("counts", aSubCount);
        // Treat hits[]
        JSONArray arHits = oOneLine.getJSONArray("hits");
        DataObjectList aHits = new DataObjectList("hit");
        for (int j=0;j<arHits.length();j++) {
          // Get the hit object. Elements: file, count, subs[]
          JSONObject oOneHit = arHits.getJSONObject(j);
          DataObjectMapElement oOneHitOut = new DataObjectMapElement();
          oOneHitOut.put("file", oOneHit.getString("file"));
          oOneHitOut.put("count", oOneHit.getInt("count"));
          DataObjectList aSubs = new DataObjectList("sub");
          JSONArray arSubs = oOneHit.getJSONArray("subs");
          for (int k=0;k<arSubs.length();k++) { aSubs.add(arSubs.getInt(k));}
          oOneHitOut.put("subs", aSubs);
          // Add the dataobject to the list
          aHits.add(oOneHitOut);
        }
        // Add the list to the overall
        oOneQc.put("hits", aHits);
        // Add the object to the list
        arCombi.add(oOneQc);
      }
      // Return the results
      return arCombi;
    } catch (Exception ex) {
      // Show the error
      errHandle.DoError("getXqTable problem", ex, JobXqReUse.class);
      return null;
    }
  }
}
