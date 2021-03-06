package nl.ru.crpx.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.ru.crpx.dataobject.DataObject;
import nl.ru.crpx.dataobject.DataObjectList;
import nl.ru.crpx.dataobject.DataObjectMapElement;
import nl.ru.util.JsonUtil;
import nl.ru.util.MemoryUtil;
import nl.ru.util.json.JSONObject;

import org.apache.log4j.Logger;

public class SearchCache {
  private static final Logger logger = Logger.getLogger(SearchCache.class);

  /** Max time searches are allowed to run (24 hours of 60 minutes) 
   * [ERK: changed from 5 to 60 minutes] 
   * ERK: changed to 24 hours because of long CESAR searches */
  public final static int MAX_SEARCH_TIME_SEC = 24 * 3600;

  /** The cached search objects. */
  private Map<SearchParameters, Job> cachedSearches;

  /** Maximum size in MB to target, or -1 for no limit. NOT IMPLEMENTED YET. */
  private long maxSizeMegs = -1;

  /** Maximum number of searches to cache, or -1 for no limit. Defaults to (a fairly low) 20.*/
  private int maxNumberOfJobs = 20;

  /** Maximum age of a cached search in seconds. May be exceeded because it is only cleaned up when
   *  adding new searches. Defaults to MAX_SEARCH_TIME_SEC, which currently is 24 hours. */
  private int maxJobAgeSec = MAX_SEARCH_TIME_SEC;

  /** (Estimated) size of the cache. Only updated in removeOldSearches, so may not
   * always be accurate. */
  private long cacheSizeBytes;

  /** How much free memory we should try to target when cleaning the cache. */
  private long minFreeMemTargetMegs;

  /** If we're below target mem, how many jobs should we get rid of each time we add something to the cache? */
  private int numberOfJobsToPurgeWhenBelowTargetMem;

  /**
   * Initialize the cache.
   *
   * @param settings cache settings
   */
  public SearchCache(JSONObject settings) {
    cachedSearches = new HashMap<SearchParameters, Job>();
    maxJobAgeSec = JsonUtil.getIntProp(settings, "maxJobAgeSec", MAX_SEARCH_TIME_SEC);
    maxNumberOfJobs = JsonUtil.getIntProp(settings, "maxNumberOfJobs", 20);
    maxSizeMegs = JsonUtil.getIntProp(settings, "maxSizeMegs", -1);
    minFreeMemTargetMegs = JsonUtil.getIntProp(settings, "targetFreeMemMegs", 100);
    numberOfJobsToPurgeWhenBelowTargetMem = JsonUtil.getIntProp(settings, "numberOfJobsToPurgeWhenBelowTargetMem", 100);
  }

  /**
   * Get a search from the cache if present.
   *
   * @param searchParameters the search parameters
   * @return the Search if found, or null if not
   */
  public Job get(SearchParameters searchParameters) {
    Job search = cachedSearches.get(searchParameters);
    if (search == null) {
      //logger.debug("Cache miss: " + searchParameters);
    } else {
      //logger.debug("Cache hit: " + searchParameters);
      search.resetLastAccessed();
    }
    return search;
  }
  /**
   * Get the search with the indicated jobid from the cache if present.
   *
   * @param iJobId the jobid we are looking for
   * @return the Search if found, or null if not
   */
  public Job getJob(long iJobId) {
    // Sort cache by last access time
    List<Job> allCachedJobs = new ArrayList<>(cachedSearches.values());
    // Walk all jobs
    for (Job search: allCachedJobs) {
      // Check out the id of this job
      if (search.id == iJobId) {
        // Found the job!
        return search;
      }
    }
    // We did not succeed in finding the correct job...
    return null;
  }
  public Job getJob(String sQuery) {
    try {
      // Sort cache by last access time
      List<Job> allCachedJobs = new ArrayList<>(cachedSearches.values());
      // Walk all jobs
      for (Job search: allCachedJobs) {
        // Check out the id of this job
        if (search.jobQuery.equals(sQuery)) {
          // Found the job!
          return search;
        }
      }
      // We did not succeed in finding the correct job...
      return null;
    } catch (Exception ex) {
      logger.error("Could not getJob", ex);
      return null;
    }
  }
  
  /**
   * getJobList
   *    Get a list of jobs in a dataobject array
   * 
   * @return 
   */
  public DataObject getJobList() {
    DataObjectList lJob = new DataObjectList("jobs");
    
    try {
      // Sort cache by last access time
      List<Job> allCachedJobs = new ArrayList<>(cachedSearches.values());
      // Walk all jobs
      for (Job search: allCachedJobs) {
        // Get the information from this job: type, user, number
        String sUser = search.userId;
        String sId =  String.valueOf(search.id);
        String sQuery = search.jobQuery;
        String sStatus = search.jobStatus;
        // Write this into a dataobject
        DataObjectMapElement oJob = new DataObjectMapElement();
        oJob.put("id", sId);
        oJob.put("user", sUser);
        oJob.put("query", sQuery);
        oJob.put("status", sStatus);
        // Add the job to the list
        lJob.add(oJob);
      }
      // We did not succeed in finding the correct job...
      return lJob;
    } catch (Exception ex) {
      logger.error("Could not getJob", ex);
      return null;
    }
  }


  /** Put a search in the cache.
   *
   * Also cleans older searches from the cache if necessary.
   *
   * @param search the search object
   */
  public void put(Job search) {
    removeOldSearches();

    // Search already in cache?
    SearchParameters searchParameters = search.getParameters();
    if (cachedSearches.containsKey(searchParameters)) {
      if (cachedSearches.get(searchParameters) != search) {
        throw new RuntimeException("Cache already contains different search object!");
      }
      // Same object already in cache, do nothing
      logger.debug("Same object put in cache twice: " + searchParameters);
      return;
    }

    // Put search in cache
    logger.debug("Put in cache: " + searchParameters);
    cachedSearches.put(searchParameters, search);
  }

  /**
   * Get rid of all the cached Searches.
   */
  public void clearCache() {
    cachedSearches.clear();
    logger.debug("Cache cleared.");
  }

  /**
   * Remove @searchRem from the cache (if it is in there)
   * @param searchRem 
   */
  void removeOneSearch(Job searchRem) {
    // Sort cache by last access time
    List<Job> lastAccessOrder = new ArrayList<Job>(cachedSearches.values());
    Collections.sort(lastAccessOrder); // put stalest first
    // Walk all jobs
    for (Job search : lastAccessOrder) {
      // Is this the job te be removed?
      if (search.equals(searchRem)) {
        // Search is taking too long. Cancel it.
        logger.debug("Search is being removed: " + search);
        search.cancelJob();
        // Physically remove from cache
        cachedSearches.remove(search.getParameters());
        cacheSizeBytes -= search.estimateSizeBytes();
        return;
      }
    }
  }
  
  /**
   * Remove all the child jobs under parent 'p'
   * 
   * @param p 
   */
  void removeChildren(Job p) {
    int iRemoved = 0; // Number of children actually removed
    
    try {
      // Sort cache by last access time
      List<Job> lastAccessOrder = new ArrayList<Job>(cachedSearches.values());
      Collections.sort(lastAccessOrder); // put stalest first
      for (Job search: lastAccessOrder) {
        if (search.parent != null && search.parent.equals(p)) {
          // Show what we are doing
          logger.debug("remove child of [" + p.getJobId() + "] job " + search.getJobId());
          // Remove this job
          search.cancelJob();
          // Physically remove from cache
          cachedSearches.remove(search.getParameters());
          cacheSizeBytes -= search.estimateSizeBytes();
          iRemoved++;
        }
      }
      // Show what we've done
      logger.debug("Removed children: " + iRemoved);
    } catch (Exception ex) {
      logger.error("Could not remove children", ex);
    }
  }
  
  /**
   * If the cache exceeds the given parameters, clean it up by
   * removing less recently used searches.
   */
  void removeOldSearches() {

    // OPT: could be optimized a little bit

    // Sort cache by last access time
    List<Job> lastAccessOrder = new ArrayList<Job>(cachedSearches.values());
    Collections.sort(lastAccessOrder); // put stalest first

    calculateSizeBytes(lastAccessOrder);

    // If we're low on memory, always remove a few searches from cache.
    int minSearchesToRemove = 0;
    long freeMegs = MemoryUtil.getFree() / 1000000;
    
    // ===== Debugging ========
    logger.debug("Free mem = " + freeMegs + " Mb (" + freeMegs / 1000 + " Gb)");
    // ========================
    
    // ERK: low on memory -- wait until memory becomes available
    
    if (freeMegs < minFreeMemTargetMegs) {
      minSearchesToRemove = numberOfJobsToPurgeWhenBelowTargetMem; // arbitrary, number but will keep on being removed every call until enough free mem has been reclaimed
      logger.debug("Not enough free mem, will remove some searches.");
    }

    // Get rid of old searches
    boolean lookAtCacheSizeAndSearchAccessTime = true;
    boolean removed = false;
    for (Job search: lastAccessOrder) {
      if (!search.finished() && search.executionTimeMillis() / 1000 > MAX_SEARCH_TIME_SEC) {
        // Search is taking too long. Cancel it.
        logger.debug("Search is taking too long, cancelling: " + search);
        search.cancelJob();

        // For now, remove from cache, but we should really blacklist these
        // kinds of searches so repeating them doesn't matter.
        // TODO blacklist
        cachedSearches.remove(search.getParameters());
        cacheSizeBytes -= search.estimateSizeBytes();
        removed = true;

      } else {
        boolean removeBecauseOfCacheSizeOrAge = false;
        if (lookAtCacheSizeAndSearchAccessTime) {
          boolean isCacheTooBig = cacheTooBig();
          boolean isSearchTooOld = false;
          if (!isCacheTooBig)
            isSearchTooOld = searchTooOld(search);
          removeBecauseOfCacheSizeOrAge = isCacheTooBig || isSearchTooOld;
        }
        if (minSearchesToRemove > 0 || removeBecauseOfCacheSizeOrAge) {
          // Search is too old or cache is too big. Keep removing searches until that's no longer the case
          logger.debug("Remove from cache: " + search);
          cachedSearches.remove(search.getParameters());
          cacheSizeBytes -= search.estimateSizeBytes();
          removed = true;
          minSearchesToRemove--;
        } else {
          // Cache is no longer too big and these searches are not too old. Stop checking that,
          // just check for long-running searches
          lookAtCacheSizeAndSearchAccessTime = false;
        }
      }
    }
    if (removed) {
      // Hint that we want GC to run
      // NOTE: this is not a good idea. Explicitly calling the GC like this can result
      //  in a multi-second pause each time. Just let Java figure it out itself and probably
      //  run sufficient incremental GC in the background.
      //System.gc();
    }
  }

  private long calculateSizeBytes(Collection<Job> collection) {
    // Estimate the total cache size
    cacheSizeBytes = 0;
    for (Job search: collection) {
      cacheSizeBytes += search.estimateSizeBytes();
    }
    return cacheSizeBytes;
  }

  /**
   * Checks if the cache size in bytes or number of searches is too big.
   *
   * Only applies if maxSizeBytes >= 0 or maxSizeSearcher >= 0.
   *
   * @return true iff the cache is too big.
   */
  private boolean cacheTooBig() {
    boolean tooManySearches = maxNumberOfJobs >= 0
                    && cachedSearches.size() > maxNumberOfJobs;
    /*if (tooManySearches)
            logger.debug("Cache has too many searches (" + cachedSearches.size() + " > "
                            + maxNumberOfJobs + ").");*/
    long cacheSizeMegs = cacheSizeBytes / 1000000;
    boolean tooMuchMemory = maxSizeMegs >= 0 && cacheSizeMegs > maxSizeMegs;
    /*if (tooMuchMemory)
            logger.debug("Cache takes too much memory (" + cacheSizeBytes + " > " + maxSizeBytes + ").");*/
    boolean tooBig = tooManySearches || tooMuchMemory;
    return tooBig;
  }

  /**
   * Checks if the search is too old to remain in cache.
   *
   * Only applies if maxJobAgeSec >= 0.
   *
   * @param search the search to check
   * @return true iff the search is too old
   */
  private boolean searchTooOld(Job search) {
    boolean tooOld = maxJobAgeSec >= 0 && search.ageInSeconds() > maxJobAgeSec;
    //if (tooOld) logger.debug("Search is too old: " + search);
    return tooOld;
  }

  /**
   * Return the maximum size of the cache to target, in bytes.
   *
   * @return targeted max. size of the cache in bytes, or -1 for no limit
   */
  public long getMaxSizeBytes() {
    return maxSizeMegs;
  }

  /**
   * Set the maximum size of the cache to target, in bytes.
   *
   * NOTE: the maximum size is checked based on a rough estimate of the
   * memory consumed by each search. Also, the specified value may be exceeded
   * because Search objects are added to the cache before the search is executed,
   * so they grow in size. Choose a conservative size and monitor memory usage in
   * practice.
   *
   * @param maxSizeBytes targeted max. size of the cache in bytes, or -1 for no limit
   */
  public void setMaxSizeBytes(long maxSizeBytes) {
    this.maxSizeMegs = maxSizeBytes;
    removeOldSearches();
  }

  /**
   * Return the maximum size of the cache in number of searches.
   * @return the maximum size, or -1 for no limit
   */
  public int getMaxJobsToCache() {
          return maxNumberOfJobs;
  }

  /**
   * Set the maximum size of the cache in number of searches.
   * @param maxJobs the maximum size, or -1 for no limit
   */
  public void setMaxJobsToCache(int maxJobs) {
          this.maxNumberOfJobs = maxJobs;
          removeOldSearches();
  }

  /**
   * Return the maximum age of a search in the cache.
   *
   * The age is defined as the period of time since the last access.
   *
   * @return the maximum age, or -1 for no limit
   */
  public int getMaxJobAgeSec() {
          return maxJobAgeSec;
  }

  /**
   * Set the maximum age of a search in the cache.
   *
   * The age is defined as the period of time since the last access.
   *
   * @param maxJobAgeSec the maximum age, or -1 for no limit
   */
  public void setMaxJobAgeSec(int maxJobAgeSec) {
          this.maxJobAgeSec = maxJobAgeSec;
  }

  public long getSizeBytes() {
          return calculateSizeBytes(cachedSearches.values());
  }

  public int getNumberOfSearches() {
          return cachedSearches.size();
  }

  public void setMinFreeMemTargetBytes(long minFreeMemTargetBytes) {
          this.minFreeMemTargetMegs = minFreeMemTargetBytes;
  }

  public DataObject getCacheStatusDataObject() {
    DataObjectMapElement doCache = new DataObjectMapElement();
    doCache.put("maxSizeBytes", getMaxSizeBytes());
    doCache.put("maxNumberOfSearches", getMaxJobsToCache());
    doCache.put("maxSearchAgeSec", getMaxJobAgeSec());
    doCache.put("sizeBytes", getSizeBytes());
    doCache.put("numberOfSearches", getNumberOfSearches());
    return doCache;
  }

  public DataObject getContentsDataObject() {
    DataObjectList doCacheContents = new DataObjectList("job");
    for (Job job: cachedSearches.values()) {
      doCacheContents.add(job.toDataObject());
    }
    return doCacheContents;
  }

}
