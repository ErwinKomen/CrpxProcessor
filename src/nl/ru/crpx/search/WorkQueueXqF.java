package nl.ru.crpx.search;
/*
 * This software has been developed at the "Radboud University"
 *   in order to support the CLARIAH project "ACAD".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */


import java.util.LinkedList;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.util.MemoryUtil;

/**
 * A "WorkQueueXqF" contains the queue of JobXqF threads
 * 
 * @author Erwin R. Komen
 */
public class WorkQueueXqF {
  private ErrHandle errHandle = null;
  private String userId = "";        // The user that 'owns' this work queue
  private final int nThreads;         // Number of threads that may be used
  private final PoolWorker[] threads; // The pool of threads
  private final LinkedList queue;     // The queue of threads for a user

  // =================== Class initialisation ==================================
  public WorkQueueXqF(ErrHandle errThis, String sUserId, int nThreads) {
    // Take over the error handler
    this.errHandle = errThis;
    errHandle.debug("WorkQueueXqF: new WQ for user: ["+sUserId+"]");
    this.nThreads = nThreads;
    this.userId = sUserId;
    queue = new LinkedList();
    threads = new PoolWorker[nThreads];

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
   * @param runnableXqF
   */
  public void execute(RunXqF runnableXqF) throws InterruptedException, QueryException {
    synchronized(queue) {
      queue.addLast(runnableXqF);
      queue.notify();
    }
  }

  /**
   * The PoolWorker class does the actual running
   */
  private class PoolWorker extends Thread {
    @Override
    public void run() {
      RunXqF runnableXqF;

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

          runnableXqF = (RunXqF) queue.removeFirst();
          // Show what happens
          // errHandle.debug("workQueue: starting XqF for: " + runnableXqF.fInput.getName());
        }

        // If we don't catch RuntimeException, 
        // the pool could leak threads
        try {
          runnableXqF.run();
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
