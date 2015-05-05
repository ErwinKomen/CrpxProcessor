/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.project;


/**
 *
 * @author u459154
 */
/* ---------------------------------------------------------------------------
   Class:   ExecuteFoliaStream
   Goal:    CRP execution for .FoLiA.xml files line-by-line
   History:
   20/apr/2015   ERK Created
   --------------------------------------------------------------------------- */
public class ExecuteFoliaStream extends ExecuteXml {

  /* ---------------------------------------------------------------------------
     Name:    ExecutePsdxStream
     Goal:    Perform initialisations needed for this class
     History:
     20/apr/2015   ERK Created
     --------------------------------------------------------------------------- */
  public ExecuteFoliaStream(CorpusResearchProject oProj) {
    // Do the initialisations for all Execute() classes
    super(oProj);
  }

  /* ---------------------------------------------------------------------------
     Name:    ExecuteQueriesPsdxStream
     Goal:    Perform initialisations needed for this class
     History:
     20/apr/2015   ERK Created
     --------------------------------------------------------------------------- */
  @Override
  public boolean ExecuteQueries() {
    try {
      // Perform general setup
      if (!super.ExecuteQueriesSetUp()) return false;
    
      // Return positively
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      DoError("ExecuteFoliaStream/ExecuteQueries error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
  

  
}
