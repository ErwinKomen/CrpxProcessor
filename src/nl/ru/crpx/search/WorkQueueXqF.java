package nl.ru.crpx.search;
/*
 * This software has been developed at the "Radboud University"
 *   in order to support the CLARIAH project "ACAD".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.util.MemoryUtil;

/**
 * A "WorkQueueXqF" contains the queue of RunXqF threads
 * 
 * @author Erwin R. Komen
 */
public class WorkQueueXqF {
  private ErrHandle errHandle = null;
  private String userId = "";             // The user that 'owns' this work queue
  private final int nThreads;             // Number of threads that may be used
  private final PoolWorker[] threads;     // The pool of threads
  // private final LinkedList<RunAny> queue; // The queue of threads for a user
  private List<RunAny> queue;             // The queue of threads for a user
  private List<RunAny> cache;             // Cache of TxtList jobs (and possibly others)
  private int iMaxCacheSize = 10;         // Max number of jobs to remember

  // =================== Class initialisation ==================================
  public WorkQueueXqF(ErrHandle errThis, String sUserId, int nThreads) {
    // Take over the error handler
    this.errHandle = errThis;
    errHandle.debug("WorkQueueXqF: new WQ for user: ["+sUserId+"]");
    this.nThreads = nThreads;
    this.userId = sUserId;
    // queue = new LinkedList<>();
    queue = new ArrayList<>();
    threads = new PoolWorker[this.nThreads];
    cache = new ArrayList<>();

    for (int i=0; i<nThreads; i++) {
      threads[i] = new PoolWorker();
      threads[i].start();
    }
  }
  
  // Getters and setters
  public String user() {return this.userId;}

  /**
   * execute
   *    Execute one runnable and then issue a 'notify'
   * 
   * @param runnableAny
   * @throws java.lang.InterruptedException
   * @throws nl.ru.crpx.search.QueryException
   */
  public void execute(RunAny runnableAny) throws InterruptedException, QueryException {
    synchronized(queue) {
      // queue.addLast(runnableAny);
      queue.add(runnableAny);
      // Show what happens
      errHandle.debug("workQueue/execute: adding to [queue] jobid=[" + 
              runnableAny.getJobId()+"] (contents ["+queue.size()+"]: "+list()+")");
      // Wake up the one thread that is waiting for me?
      // queue.notify();
      // Wake up *ALL* threads that are waiting for queue
      queue.notifyAll();
    }
  }
  
  /**
   * list
   *    Return a list of all the jobs in the queue
   * 
   * @return 
   */
  public String list() {
    String sBack = "";
    
    synchronized(queue) {
      for (RunAny runXqF : queue) {
        sBack += "[" + runXqF.getJobId() + "] ";
      }
    }
    // Return the contents
    return sBack;
  }
  
  /**
   * numjobs
   *    Return the number of jobs in the cache + in the queue
   * 
   * @return integer
   */
  public int numjobs() {
    int iNumJobs = cache.size() + queue.size();
    return iNumJobs;
  }
  
  public RunAny getRun(int iJobId) {
    String sJobId = Integer.toString(iJobId);
    synchronized(cache) {
      for (int i=0;i<cache.size();i++) {
        RunAny runnableThis = (RunAny) cache.get(i);
        if (sJobId.equals(runnableThis.getJobId())) {
          // Found it
          return runnableThis;
        }
      }
    }
    // Didn't find it
    return null;
  }
  
  public void clear() {
    try {
      // First remove any jobs still present in the queue
      synchronized(queue) {
        queue.clear();
      }
      
      // Next visit all jobs in the cache and stop+remove them
      synchronized(cache) {
        for (int i=cache.size()-1;i>=0;i--) {
          cache.remove(i);
        }
      }
      // All should be clear now
    } catch (RuntimeException e) {
      // Notify the user
      errHandle.DoError("WorkQueueXqF.clear error", e);
    }
  }
  
  public boolean removeRun(int iJobId) {
    // Remove the job with the indicated id
    RunAny runnableThis = getRun(iJobId);
    return removeRun(runnableThis);
  }
  public boolean removeRun(RunAny runnableThis) {
    // Remove this from the cache, but only if the cache becomes too large
    synchronized(cache) {
      if (cache.size() > iMaxCacheSize) {
        for (int i=0;i<cache.size();i++) {
          if (runnableThis.equals(cache.get(i))) {
            // =============== DEBUGGING ===============
            // errHandle.debug("workqueue/removeRun remove from [cache] jobid=["+runnableThis.getJobId()+"]");
            // =========================================
            // Remove it
            cache.remove(i);
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * The PoolWorker class does the actual running
   */
  private class PoolWorker extends Thread {
    @Override
    public void run() {
      RunAny runnableAny;

      while (true) {
        synchronized(queue) {
          // First wait until something enters the queue
          while (queue.isEmpty()) {
            try {
              queue.wait();
            }
            catch (InterruptedException ignored) {
              // What to do when we are 'interrupted'?
              // For the moment: ignore this
              
              // TODO: empty the queue completely? 
              //       But will that be enough??
              errHandle.debug("Workqueue/run: some interrupt?");
            }
          }
          errHandle.debug("workQueue: before remove size=" + queue.size());
          
          // runnableAny = (RunAny) queue.removeFirst();
          
          // Remove element [0], the first in line to be processed
          runnableAny = queue.remove(0);
          // Show what happens
          errHandle.debug("workQueue: starting XqF for jobid=[" + 
                  runnableAny.getJobId()+"] (contents ["+queue.size()+"]: "+list()+")");
          
          // Some jobs need to be put in the cache
          if (runnableAny.jobName.equals("txtlist")) {
            synchronized(cache) {
              // Put it into the cache
              cache.add(runnableAny);
            }
          }
        }

        // If we don't catch RuntimeException, 
        // the pool could leak threads
        try {
          runnableAny.run();
          // Show what happens
          // errHandle.debug("workQueue: finished running XqF for: " + runnableXqF.fInput.getName());
          // ===== Debugging ========
          long freeMegs = MemoryUtil.getFree() / 1000000;
          int nbThreads =  Thread.getAllStackTraces().keySet().size();
          errHandle.debug("WorkQueueXqF: Free mem = " + freeMegs + " Mb (" + freeMegs / 1000 + " Gb) Threads="+nbThreads);
          // ========================
          
        } catch (RuntimeException e) {
          // Notify the user
          errHandle.DoError("WorkQueueXqF.PoolWorker.run error", e);
        }
      }
    }
    
  }
}
