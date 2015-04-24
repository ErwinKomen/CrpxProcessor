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
   Class:   ExecuteFoliaFast
   Goal:    CRP execution for .folia.xml files file-by-file
   History:
   20/apr/2015   ERK Created
   --------------------------------------------------------------------------- */
public class ExecuteFoliaFast extends Execute {
  protected General objGen;
  public ExecuteFoliaFast(CorpusResearchProject crpThis, General oGen) {
    // Do the initialisations for all Execute() classes
    super(crpThis, oGen);
    this.objGen = oGen;
  }
    public static boolean ExecuteQueriesFoliaFast() {
    
    // Return positively
    return true;
  }

}
