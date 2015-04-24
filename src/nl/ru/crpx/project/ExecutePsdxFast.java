/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.project;
import nl.ru.crpx.tools.General;

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
  protected General objGen;
  public ExecutePsdxFast(CorpusResearchProject crpThis, General oGen) {
    // Do the initialisations for all Execute() classes
    super(crpThis, oGen);
    this.objGen = oGen;
  }
  public static boolean ExecuteQueriesPsdxFast() {
    
    // Return positively
    return true;
  }
  

}
