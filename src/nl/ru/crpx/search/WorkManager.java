/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.search;

import java.util.ArrayList;
import java.util.List;
import nl.ru.crpx.cmd.RequestHandler;
import nl.ru.crpx.tools.ErrHandle;

/**
 *
 * @author Erwin R. Komen
 */
public class WorkManager {
  private final int maxThreadsPerUser = 20;
  private ErrHandle errHandle = null;
  private String created = "";
  private List<WorkQueueXqF> lWorkQueue = null;
  public WorkManager(ErrHandle oErr) {
    // Initialise a list of work queues
    lWorkQueue = new ArrayList<>();
    errHandle = oErr;
    created = RequestHandler.getCurrentTimeStamp();
  }
  
  // Getters/setters
  public String dateCreated() {return created;}
  
  
  /**
   * getWorkQueue
   *    Get the work queue for the indicated user
   * 
   * @param sUserId
   * @return 
   */
  public WorkQueueXqF getWorkQueue(String sUserId) {
    int i;                // Counter
    WorkQueueXqF wqThis;  // Temporary
    
    synchronized(lWorkQueue) {
      // Walk the list of queues
      if (lWorkQueue != null) {
        for(i=0;i<lWorkQueue.size();i++) {
          wqThis = lWorkQueue.get(i);
          if (sUserId.equals(wqThis.user())) {
            return wqThis;
          }
          // Debug info
          errHandle.debug("getWorkQueue: skipping ["+wqThis.user()+"]");
        }
      }
      // Getting here means: no work queue for this user yet
      wqThis = new WorkQueueXqF(errHandle, sUserId, maxThreadsPerUser);
      // Add it
      lWorkQueue.add(wqThis);
    }
    // Debug info
    errHandle.debug("getWorkQueue: created for ["+wqThis.user()+"]");
    return wqThis;
  }
}
