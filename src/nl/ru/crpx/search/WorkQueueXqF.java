package nl.ru.crpx.search;
/*
 * This software has been developed at the "Radboud University"
 *   in order to support the CLARIAH project "ACAD".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */


import java.util.ArrayList;
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
  private String userId = "";        // The user that 'owns' this work queue
  private final int nThreads;         // Number of threads that may be used
  private final PoolWorker[] threads; // The pool of threads
  private final LinkedList queue;     // The queue of threads for a user
  private List<RunAny> cache;         // Cache of TxtList jobs (and possibly others)
  private int iMaxCacheSize = 10;      // Max number of jobs to remember

  // =================== Class initialisation ==================================
  public WorkQueueXqF(ErrHandle errThis, String sUserId, int nThreads) {
    // Take over the error handler
    this.errHandle = errThis;
    errHandle.debug("WorkQueueXqF: new WQ for user: ["+sUserId+"]");
    this.nThreads = nThreads;
    this.userId = sUserId;
    queue = new LinkedList();
    threads = new PoolWorker[nThreads];
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
      queue.addLast(runnableAny);
      queue.notify();
    }
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
          while (queue.isEmpty()) {
            try {
              queue.wait();
            }
            catch (InterruptedException ignored) {
              // What to do when we are 'interrupted'?
              // For the moment: ignore this
              
              // TODO: empty the queue completely? 
              //       But will that be enough??
            }
          }

          runnableAny = (RunAny) queue.removeFirst();
          // Show what happens
          // errHandle.debug("workQueue: starting XqF for: " + runnableXqF.fInput.getName());
          
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
          
        }
        catch (RuntimeException e) {
          // Notify the user
          errHandle.DoError("WorkQueueXqF.PoolWorker.run error", e);
        }
      }
    }
    
  }
}
