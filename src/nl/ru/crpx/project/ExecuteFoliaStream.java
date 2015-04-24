/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.project;

import nl.ru.crpx.tools.General;

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
public class ExecuteFoliaStream extends Execute {
  protected General objGen;

  /* ---------------------------------------------------------------------------
     Name:    ExecutePsdxStream
     Goal:    Perform initialisations needed for this class
     History:
     20/apr/2015   ERK Created
     --------------------------------------------------------------------------- */
  public ExecuteFoliaStream(CorpusResearchProject crpThis, General oGen) {
    // Do the initialisations for all Execute() classes
    super(crpThis, oGen);
    this.objGen = oGen;
  }

  /* ---------------------------------------------------------------------------
     Name:    ExecuteQueriesPsdxStream
     Goal:    Perform initialisations needed for this class
     History:
     20/apr/2015   ERK Created
     --------------------------------------------------------------------------- */
  public static boolean ExecuteQueriesFoliaStream() {
    
    // Return positively
    return true;
  }
  

  
}
