package nl.ru.crpx.search;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nl.ru.crpx.dataobject.DataFormat;
import nl.ru.crpx.dataobject.DataObject;
import nl.ru.crpx.dataobject.DataObjectMapElement;
import nl.ru.crpx.dataobject.DataObjectString;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.project.CrpInfo;
import nl.ru.util.FileUtil;
import nl.ru.util.Json;
import nl.ru.util.JsonUtil;
import nl.ru.util.MemoryUtil;
import nl.ru.util.json.JSONArray;
import nl.ru.util.json.JSONObject;
import org.apache.log4j.Logger;

public class SearchManager {
  private static final Logger logger = Logger.getLogger(SearchManager.class);

  /**
   * When the SearchManager was created. Used in logging to show ms since
   * server start instead of all-time.
   */
  long createdAt = System.currentTimeMillis();

  /**
   * How long the server should wait for a quick answer when starting a
   * nonblocking request. If the answer is found within this time, the client
   * needs only one request even in nonblocking mode.
   */
  private int waitTimeInNonblockingModeMs;

  /**
   * The minimum time to advise a client to wait before checking the status of
   * a search again.
   */
  private int checkAgainAdviceMinimumMs;

  /**
   * What number to divide the search time so far by to get the check again
   * advice. E.g. if this is set to 5 (the default), if a search has been
   * running for 10 seconds, clients are advised to wait 2 seconds before
   * checking the status again.
   */
  private int checkAgainAdviceDivider;

  /** Maximum context size allowed */
  private int maxContextSize;

  /** Maximum snippet size allowed */
  private int maxSnippetSize;

  /** Parameters involved in search */
  private List<String> searchParameterNames;

  /** Default values for request parameters */
  private Map<String, String> defaultParameterValues;
  
  /** The indexParam holds all the languages we can search */
  private Map<String, IndexParam> indexParam;

  // /** Run in debug mode or not? [no] */
  // private boolean debugMode;

  /** Default number of hits/results per page [20] */
  private int defaultPageSize;

  // NO NEED: private Map<String, IndexParam> indexParam;

  // NO NEED: /** The Searcher objects, one for each of the indices we can search. */
  // NO NEED: private Map<String, Searcher> searchers = new HashMap<String, Searcher>();

  /** All running searches as well as recently run searches */
  private SearchCache cache;

  /** Keeps track of running jobs per user, so we can limit this. */
  private Map<String, Set<Job>> runningJobsPerUser = new HashMap<String, Set<Job>>();

  /** Default pattern language to use. [corpusql] */
  private String defaultPatternLanguage;

  /** Default filter language to use. [luceneql] */
  private String defaultFilterLanguage;

  /** Should requests be blocking by default? [yes] */
  private boolean defaultBlockingMode;

  /** Default number of words around hit. [5] */
  private int defaultContextSize;

  /** Minimum amount of free memory (MB) to start a new search. [50] */
  private int minFreeMemForSearchMegs;

  /**
   * Maximum number of simultaneously running jobs started by the same user.
   * [20] Please note that a search may start 2-4 jobs, so don't set this too
   * low. This is just meant to prevent over-eager scripts and other abuse.
   * Regular users should never hit this limit.
   */
  private long maxRunningJobsPerUser;

  /** IP addresses for which debug mode will be turned on. */
  private Set<String> debugModeIps;

  /** The default output type, JSON or XML. */
  private DataFormat defaultOutputType;

  /**
   * Which IPs are allowed to override the userId using a parameter (for other
   * IPs, the session id is the userId)
   */
  private Set<String> overrideUserIdIps;

  /**
   * How long the client may used a cached version of the results we give
   * them. This is used to write HTTP cache headers. A value of an hour or so
   * seems reasonable.
   */
  private int clientCacheTimeSec;
  
  /**
   * My own copy of the corpus research project we are working on
   */
  private CorpusResearchProject crpThis;
  
  // Properties passed on in the command-line
  private JSONObject oCmdLine;
  
  // Cache of combinations Language-Part
  private JSONObject oLngPart;
  private String sLngPart = CrpInfo.sEtcCorpora + "/lng-part.json";  // "/etc/corpora/lng-part.json";

  /**
   * 
   * @param properties 
   */
  public SearchManager(JSONObject properties) {
    logger.debug("SearchManager created");

    // this.properties = properties;
    JSONArray jsonDebugModeIps = properties.getJSONArray("debugModeIps");
    debugModeIps = new HashSet<String>();
    for (int i = 0; i < jsonDebugModeIps.length(); i++) {
      debugModeIps.add(jsonDebugModeIps.getString(i));
    }
    
    // Initialise the command-line properties to nothing
    oCmdLine = new JSONObject();
    
    // Initialize the Language-Part storage
    oLngPart = initLngPart();

    // Request properties
    JSONObject reqProp = properties.getJSONObject("requests");
    defaultOutputType = DataFormat.JSON; // We normally work with JSON
    if (reqProp.has("defaultOutputType"))
      defaultOutputType = getOutputTypeFromString(
					reqProp.getString("defaultOutputType"), DataFormat.JSON);
    defaultBlockingMode = JsonUtil.getBooleanProp(reqProp,
                    "defaultBlockingMode", true);
    JSONArray jsonOverrideUserIdIps = reqProp
                    .getJSONArray("overrideUserIdIps");
    overrideUserIdIps = new HashSet<String>();
    for (int i = 0; i < jsonOverrideUserIdIps.length(); i++) {
            overrideUserIdIps.add(jsonOverrideUserIdIps.getString(i));
    }

    // Performance properties
    JSONObject perfProp = properties.getJSONObject("performance");
    minFreeMemForSearchMegs = JsonUtil.getIntProp(perfProp,
                    "minFreeMemForSearchMegs", 50);
    maxRunningJobsPerUser = JsonUtil.getIntProp(perfProp,
                    "maxRunningJobsPerUser", 20);
    checkAgainAdviceMinimumMs = JsonUtil.getIntProp(perfProp,
                    "checkAgainAdviceMinimumMs", 200);
    checkAgainAdviceDivider = JsonUtil.getIntProp(perfProp,
                    "checkAgainAdviceDivider", 5);
    waitTimeInNonblockingModeMs = JsonUtil.getIntProp(perfProp,
                    "waitTimeInNonblockingModeMs", 100);
    clientCacheTimeSec = JsonUtil.getIntProp(perfProp,
                    "clientCacheTimeSec", 3600);

    // Cache properties
    JSONObject cacheProp = perfProp.getJSONObject("cache");

    // Find the indices
    indexParam = new HashMap<String, IndexParam>();
    JSONObject indicesMap = properties.getJSONObject("indices");
    Iterator<?> it = indicesMap.keys();
    while (it.hasNext()) {
      String indexName = (String) it.next();
      JSONObject indexConfig = indicesMap.getJSONObject(indexName);

      File dir = JsonUtil.getFileProp(indexConfig, "dir", null);
      if (dir == null || !dir.exists()) {
        logger.error("Language index directory for '" + indexName
                        + "' does not exist: " + dir);
        continue;
      }

      // Get a "pid" (unique per language index) if provided; otherwise ""
      String pid = JsonUtil.getProperty(indexConfig, "pid", "");
      if (pid.length() == 0) {
        // NOTE: pid may be specified in index metadata or not
      }

      boolean mayViewContent = JsonUtil.getBooleanProp(indexConfig,
        "mayViewContent", false);

      indexParam.put(indexName, new IndexParam(dir, pid, mayViewContent));
    }

    // Keep a list of searchparameters.
    searchParameterNames = Arrays.asList("resultsType", "query", "queryid", "tmpdir", "waitfortotal");

    // Set up the parameter default values
    defaultParameterValues = new HashMap<String, String>();
    defaultParameterValues.put("waitfortotal", "no");
    defaultParameterValues.put("tmpdir", "");

    // Start with empty cache
    cache = new SearchCache(cacheProp);
  }
  
  public void setCmdLineProperties(JSONObject oNew) { this.oCmdLine = oNew; }
  public JSONObject getCmdLineProperties() { return this.oCmdLine; }

  public List<String> getSearchParameterNames() {
    return searchParameterNames;
  }
  // ============ Find a job with a particular id -- but it needs to be a jobxq
  public Job searchGetJobXq(String sJobId) {
    Job search = cache.getJob(Integer.parseInt(sJobId));
    // Validate what we get back
    if (search==null) { 
      logger.debug("searchGetJobXq: job "+sJobId+" does not exist at all");
      return null;
    }
    // Check if it has parameters
    if (search.par.isEmpty()) {
      logger.debug("searchGetJobXq: job "+sJobId+" does not have [par] defined");
      return null;
    }
    String sJobCl = search.par.getString("jobclass");
    if (sJobCl.isEmpty()) {
      logger.debug("searchGetJobXq: job "+sJobId+" does not have 'jobclass' defined in [par]");
      return null;
    }
    if (sJobCl.toLowerCase().equals("jobxq")  || sJobCl.toLowerCase().equals("JobXqReUse"))
      return search;
    else {
      logger.debug("searchGetJobXq: job "+sJobId+" is of class "+sJobCl);
      return null;
    }
  }

  public DataObject jobList() {
    return cache.getJobList();
  }
  /**
   * Return the list of indices available for searching.
   * 
   * @return the list of index names
   */
  public Collection<String> getAvailableIndices() {
    return indexParam.keySet();
  }
  /**
   * Get the index directory based on index name
   *
   * @param indexName
   *            short name of the index
   * @return the index directory, or null if not found
   */
  public File getIndexDir(String indexName) {
    // tear apart the index name from the sub-directory specification
    String arPart[] = indexName.split(";");
    
    IndexParam p = indexParam.get(arPart[0]);
    if (p == null)
            return null;
    // Action depends on the size of [arPart]
    if (arPart.length == 1) {
      // Return the File path that is already stored
      return p.getDir();
    } else {
      String sPath = p.getDir().getAbsolutePath();
      for (int i=1;i<arPart.length;i++) {
        sPath = sPath + "/" + arPart[i];
      }
      // Return the extended path as a File object
      return new File(sPath);
    }
  }
  
  /**
   * getCorpusPartDir
   *    Given a "lng" and "dir" specification, return 
   *    the directory where these are found
   * 
   * @param sLng
   * @param sPart
   * @return 
   */
  public String getCorpusPartDir(String sLng, String sPart) {
    JSONObject oPart = null;
    String sFull = "";      // The full string of the path we return
    
    // Get the directory associated with the Language Index
    File fDir = getIndexDir(sLng);
    if (fDir==null) return "";
    // We need the directory as a string
    String sTarget = fDir.getAbsolutePath();
    // Preliminary: take the language directory as basis
    sFull = sTarget;
    
    logger.debug("getCorpusPartDir sTarget="+sTarget);
    
    // Get a sub directory or focus file
    if (!sPart.isEmpty()) {
      // See if this file is in the cache we have 
      if (this.oLngPart.has(sLng)) {
        // There is an entry for this language
        oPart = this.oLngPart.getJSONObject(sLng);
        // Do we have an entry for [sPart]?
        if (oPart.has(sPart)) {
          // There is an entry for this part
          return oPart.getString(sPart);
        } else {
          logger.debug("getCorpusPartDir: language ["+sLng+"] doesn't yeat have part ["+sPart+"]");
        }
      } else {
        logger.debug("getCorpusPartDir: language not in lookup ["+sLng+"]");
        // Create an entry for the language/part
        oPart = new JSONObject();
      }
      // Locate this part 'under' the language index directory      
      sFull = FileUtil.findFileInDirectory(sTarget, sPart);
      // Add this path to the appropriate lng/part position
      oPart.put(sPart, sFull);
      // Add the path to the language
      this.oLngPart.put(sLng, oPart);
      // Make sure changes are saved
      saveLngPart();
    }
    // Return a handle to the target
    return sFull;
  }

  /**
   * searchXq - perform search operation for a whole CRP
   * @param objPrj
   * @param userId
   * @param par
   * @param bCache  - true: allow caching; false: prevent caching
   * @return result of the search job
   * @throws QueryException
   * @throws InterruptedException 
   */
  public JobXq searchXq(CorpusResearchProject objPrj, String userId, SearchParameters par, boolean bCache)
            throws QueryException, InterruptedException {
    // Only copy the query parameter
    SearchParameters parBasic = par.copyWithOnly("query");
    // Set the correct jobclass
    parBasic.put("jobclass", "JobXq");
    return (JobXq) search(objPrj, userId, parBasic, null, bCache);
  }
  
  /**
   * searchXqReUse
   *    Locate and fetch tables stored after a previous search
   * 
   * @param objPrj
   * @param userId
   * @param par
   * @return
   * @throws QueryException
   * @throws InterruptedException 
   */
  public JobXqReUse searchXqReUse(CorpusResearchProject objPrj, String userId, SearchParameters par)
            throws QueryException, InterruptedException {
    // Only copy the query parameter
    SearchParameters parBasic = par.copyWithOnly("query");
    // Set the correct jobclass: this must be "JobXq" to allow the re-used jobs to be retrieved
    parBasic.put("jobclass", "JobXqReUse");
    return (JobXqReUse) search(objPrj, userId, parBasic, null, false);
  }
  
  /**
   * Check if the searchParameters contain a "query" string
   *   that has an object "force" set to "true"
   * 
   * @param par
   * @return 
   */
  private boolean searchHasForce(SearchParameters par) {
    try {
      String sQ = par.getString("query");
      if (!sQ.isEmpty()) {
        JSONObject oQ = new JSONObject(sQ);
        if (oQ.has("force"))
          return oQ.getBoolean("force");
      }
      // Getting here means there is no 'force' parameter
      return false;
    } catch (Exception ex) {
      logger.error("Could not find [force] in: " + par.toString(), ex);
      return false;
    }
  }
  /**
   * Start a new search or return an existing Search object corresponding to
   * these search parameters.
   *
   * @param userId
   *            user creating the job
   * @param searchParameters
   *            the search parameters
   * @param blockUntilFinished
   *            if true, wait until the search finishes; otherwise, return
   *            immediately
   * @param jobParent
   *            the parent of this job
   * @return a Search object corresponding to these parameters
   * @throws QueryException
   *             if the query couldn't be executed
   * @throws InterruptedException
   *             if the search thread was interrupted
   */
  private Job search(CorpusResearchProject objPrj, String userId, SearchParameters searchParameters, 
          Job jobParent, boolean bCache)
            throws QueryException, InterruptedException {
    // Search the cache / running jobs for this search, create new if not
    // found.
    boolean performSearch = false;
    // Retrieve the corpus research project we are working on
    this.crpThis = objPrj;
    // Create room for a job
    Job search = null;
    synchronized (this) {
      // Check if there is a previous job
      search = cache.get(searchParameters);
      // Check if we may re-use a previous query
      if (search != null) {
        // Check the status of this job
        if (search.finished() && (search.jobStatus.equals("error") || !bCache)) {
          // Show what we are doing
          String sReason = (bCache) ? " due to error" : " due to [no-cache]";
          logger.debug("searchManager removing job #" + search.getJobId() + sReason);
          // Now we must remove this job from the cache
          cache.removeOneSearch(search);
          search = null;
        }
      }
      // Now continue
      if (search == null) {
        // Not found in cache
        
        // Remove any old searches that can be removed
        cache.removeOldSearches(); // try to free up space for next

        // Do we have enough memory to start a new search?
        long freeMegs = MemoryUtil.getFree() / 1000000;
        if (freeMegs < minFreeMemForSearchMegs) {
          cache.removeOldSearches(); // try to free up space for next
                                                                  // search
          logger.warn("Can't start new search, not enough memory ("
                          + freeMegs + "M < " + minFreeMemForSearchMegs
                          + "M)");
          throw new QueryException("SERVER_BUSY",
                          "The server is under heavy load right now. Please try again later.");
        }
        //logger.debug("Enough free memory: " + freeMegs + "M");

        // Is this user allowed to start another search?
        int numRunningJobs = 0;
        Set<Job> runningJobs = runningJobsPerUser.get(userId);
        Set<Job> newRunningJobs = new HashSet<Job>();
        if (runningJobs != null) {
          for (Job job : runningJobs) {
            if (!job.finished()) {
              numRunningJobs++;
              newRunningJobs.add(job);
            }
          }
        }
        // logger.debug("Running jobs = " + numRunningJobs);
        if (numRunningJobs >= maxRunningJobsPerUser) {
          // User has too many running jobs. Can't start another one.
          runningJobsPerUser.put(userId, newRunningJobs); // refresh
                                                          // the list
          logger.warn("Can't start new search, user already has "
              + numRunningJobs + " jobs running.");
          throw new QueryException(
              "TOO_MANY_JOBS",
              "You already have too many running searches. Please wait for some previous searches to complete before starting new ones.");
        }

        // Create a new search object with these parameters and place it
        // in the cache
        search = Job.create(this, userId, searchParameters);
        
        // Add the parent
        search.setParent(jobParent);
        
        // Only *cache* the search if it is an Xq one
        if (search.par.getString("jobclass").toLowerCase().equals("jobxq"))
          cache.put(search);

        // Update running jobs
        newRunningJobs.add(search);
        runningJobsPerUser.put(userId, newRunningJobs);

        performSearch = true;
      }
    }

    if (performSearch) {
      // Start the search, waiting a short time in case it's a fast search
      search.perform(waitTimeInNonblockingModeMs);
    }

    // If the search thread threw an exception, rethrow it now.
    if (search.threwException()) {
      search.rethrowException();
    }

    logger.debug("Search triggered: jobId=" + search.getJobId() + " type=" + 
            searchParameters.getString("jobclass") + " finished=" + search.finished());
    return search;
  }

  public long getMinFreeMemForSearchMegs() {
    return minFreeMemForSearchMegs;
  }

  /**
   * getCrp: make the corpus research project we are working on available
   * 
   * @return - corpus research project we are working on
   */
  public CorpusResearchProject getCrp() { return this.crpThis; }
  
  public String getParameterDefaultValue(String paramName) {
    String defVal = defaultParameterValues.get(paramName);
    /*
     * if (defVal == null) { defVal = "";
     * defaultParameterValues.put(paramName, defVal); }
     */
    return defVal;
  }

  public static boolean strToBool(String value)
            throws IllegalArgumentException {
    if (value.equals("true") || value.equals("1") || value.equals("yes")
                    || value.equals("on"))
      return true;
    if (value.equals("false") || value.equals("0") || value.equals("no")
                    || value.equals("off"))
      return false;
    throw new IllegalArgumentException("Cannot convert to boolean: "
                    + value);
  }

  public static int strToInt(String value) throws IllegalArgumentException {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Cannot convert to int: "
                            + value);
    }
  }

  /**
   * Construct a simple error response object.
   *
   * @param code
   *            (string) error code
   * @param msg
   *            the error message
   * @return the data object representing the error message
   */
  public static DataObject errorObject(String code, String msg) {
    DataObjectMapElement error = new DataObjectMapElement();
    error.put("code", new DataObjectString(code));
    error.put("message", new DataObjectString(msg));
    DataObjectMapElement rv = new DataObjectMapElement();
    rv.put("error", error);
    return rv;
  }

  public int getCheckAgainAdviceMinimumMs() {
    return checkAgainAdviceMinimumMs;
  }

  public boolean isDebugMode(String ip) {
    return debugModeIps.contains(ip);
  }

  static void debugWait() {
    // Fake extra search time
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public DataFormat getContentsFormat(String indexName) {
    return DataFormat.XML; // could be made configurable
  }

  public SearchCache getCache() { return cache; }
  public int getMaxContextSize() { return maxContextSize; }
  public DataObject getCacheStatusDataObject() { return cache.getCacheStatusDataObject(); }
  public DataObject getCacheContentsDataObject() { return cache.getContentsDataObject(); }
  public int getMaxSnippetSize() { return maxSnippetSize; }
  public boolean mayOverrideUserId(String ip) { return overrideUserIdIps.contains(ip); }
  public DataFormat getDefaultOutputType() { return defaultOutputType; }
  public int getClientCacheTimeSec() { return clientCacheTimeSec; }
  public Job getXqJob(String sQuery) {    return cache.getJob(sQuery);  }
  public Job getXqJob(Integer iJobId) { return cache.getJob(iJobId); }

  /**
   * Give advice for how long to wait to check the status of a search.
   * 
   * @param search
   *            the search you want to check the status of
   * @return how long you should wait before asking again
   */
  public int getCheckAgainAdviceMs(Job search) {

    // Simple advice algorithm: the longer the search
    // has been running, the less frequently the client
    // should check its progress. Just divide the search time by
    // 5 with a configured minimum.
    int runningFor = search.ageInSeconds();
    int checkAgainAdvice = Math.min(checkAgainAdviceMinimumMs, runningFor
                    * 1000 / checkAgainAdviceDivider);

    return checkAgainAdvice;
  }

  /**
   * Finish all the XqF jobs that have this Xq as parent
   * 
   * @param parentXq 
   */
  public void finishChildXqFjobs(Job parentXq) {
    cache.removeChildren(parentXq);    
  }
  	/**
	 * Translate the string value for outputType to the enum OutputType value.
	 *
	 * @param typeString
	 *            the outputType string
	 * @param defaultValue what to use if neither matches
	 * @return the OutputType enum value
	 */
	public static DataFormat getOutputTypeFromString(String typeString, DataFormat defaultValue) {
		if (typeString.equalsIgnoreCase("xml"))
			return DataFormat.XML;
		if (typeString.equalsIgnoreCase("json"))
			return DataFormat.JSON;
		logger.warn("Onbekend outputtype gevraagd: " + typeString);
		return defaultValue;
	}
        
       
  /**
   * initLngPart
   *    If available, read the JSON file
   * 
   * @param sLngPartFile
   * @return 
   */
  private synchronized JSONObject initLngPart() {
    try {
      // Windows check: should be D
      if (new File(this.sLngPart).getCanonicalPath().startsWith("C:")) {
        this.sLngPart = "D:" + this.sLngPart;
      }
      
      File fLngPartFile = new File(this.sLngPart);
      // Do we have a file?
      if (fLngPartFile.exists()) {
        // Read the file
        this.oLngPart = Json.read(fLngPartFile);
        // Make sure the permissions are okay
        fLngPartFile.setWritable(true, false);
        // 
      } else {
        // There is no file yet
        this.oLngPart = new JSONObject();
      }
      
      return this.oLngPart;
    } catch (Exception ex) {
      logger.error("searchManager/initLngPart: ", ex);
      return null;
    }
  }
  
  /**
   * saveLngPart
   *    Save changes to the JSON file location cache
   *    Make sure this is done synchronized!
   * 
   */
  private synchronized void saveLngPart() {
    try {
      File fLngPartFile = new File(this.sLngPart);
      Json.write(this.oLngPart, fLngPartFile);      
    } catch (Exception ex) {
      logger.error("searchManager/saveLngPart: ", ex);
    }
  }
  
}
