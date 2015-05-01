/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.project;

/**
 *
 * @author E.R.Komen
 */
/* ---------------------------------------------------------------------------
   Class:   ExecutePsdxFast
   Goal:    CRP execution for .psdx files file-by-file
   History:
   20/apr/2015   ERK Created
   --------------------------------------------------------------------------- */
public class ExecutePsdxFast  extends Execute {
  public ExecutePsdxFast() {
    // Do the initialisations for all Execute() classes
    super();
  }
  @Override
  public boolean ExecuteQueries() {
    try {
      // Perform general setup
      if (!super.ExecuteQueriesSetUp()) return false;
    
      // Return positively
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      DoError("ExecutePsdxFast/ExecuteQueries error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
  

}
