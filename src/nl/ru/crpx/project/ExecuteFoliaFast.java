/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.project;
// import static nl.ru.crpx.project.CrpGlobal.DoError;

/**
 *
 * @author u459154
 */
/* ---------------------------------------------------------------------------
   Class:   ExecuteFoliaFast
   Goal:    CRP execution for .folia.xml files file-by-file
   History:
   20/apr/2015   ERK Created
   --------------------------------------------------------------------------- */
public class ExecuteFoliaFast extends ExecuteXml {
  //protected CrpGlobal objGen;
  public ExecuteFoliaFast(CorpusResearchProject oProj) {
    // Do the initialisations for all Execute() classes
    super(oProj);
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
      DoError("ExecuteFoliaFast/ExecuteQueries error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
}
