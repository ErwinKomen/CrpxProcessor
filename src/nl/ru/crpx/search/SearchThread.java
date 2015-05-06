/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */
package nl.ru.crpx.search;

import nl.ru.crpx.tools.ErrHandle;
import org.apache.log4j.Logger;

/**
 *
 * @author erwin
 */
final class SearchThread extends Thread implements Thread.UncaughtExceptionHandler {
  protected static final Logger logger = Logger.getLogger(SearchThread.class);
  // =============== Local variables for this search thread ====================
  private final Job search;         // The search to be executed by this search thread
  // =============== Exception we implement ===================================
  Throwable thrownException = null;
  /**
   * Construct a new SearchThread
   * @param search the search to execute in the thread
   */
  SearchThread(Job search) {
    // Set internal variable
    this.search = search;
    // Set exception handler
    setUncaughtExceptionHandler(this);
  }
  
  /*
   * Run the thread
  */
  public void run() {
    try {
      // Perform the search
      search.performSearch();
      // Note when the search has finished
      search.finishedAt = System.currentTimeMillis();
    } catch (Throwable ex) {
      thrownException = ex;
    }
  }
  
  // ======== Functions implemented by SearchThread ============================
  public boolean finished() {
    State state = getState();
    return state == State.TERMINATED;
  }
  public boolean threwException() { return thrownException != null; }
  public Throwable getThrownException() { return thrownException;}
  @Override
  public void uncaughtException(Thread t, Throwable e) {
    logger.debug("Search thread threw an exception, saving it:\n" + e.getClass().getName() + ": " + e.getMessage());
    thrownException = e;
  }
}
