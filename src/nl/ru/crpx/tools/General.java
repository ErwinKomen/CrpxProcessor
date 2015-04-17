/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.tools;

/**
 *
 * @author Erwin
 */
public class General {
  /* ---------------------------------------------------------------------------
   Name:    DoError
   Goal:    Show an error and return false
   History:
   17/10/2014   ERK Created
   --------------------------------------------------------------------------- */
  public static boolean DoError(String msg) {
    System.err.println("CorpusResearchProject error:\n" + msg);
    return(false);
  }

}
