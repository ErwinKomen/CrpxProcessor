/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.xq;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import nl.ru.crpx.project.CrpGlobal;
import nl.ru.crpx.tools.ErrHandle;

/**
 *
 * @author Erwin R. Komen
 */
public class PatternStore  {
  // =============== Locally needed variables ==================================
  private List<MatchHelp> lMatchHelp;
  private ErrHandle objErr;
  private boolean bBusy;
  // ============== CLASS initialization =======================================
  public PatternStore(ErrHandle oErr) {
    // Initialize a list of string arrays to help ru:matches()
    lMatchHelp = new ArrayList<>();
    // Set the correct error handler
    objErr = oErr;
    // I am not busy
    bBusy = false;
  }
  
  // Find a string-array of regular expressions derived from @sPatterns
  //   and implementing the "like" operator
  public Pattern[] getMatchHelp(String sPatterns) {
    // Make sure no one else is playing around with lMatchHelp
    synchronized(lMatchHelp) {
      try {
        for (int i=0;i<lMatchHelp.size(); i++) {
          // Check if this matches the pattern
          if (lMatchHelp.get(i).key.equals(sPatterns)) {
            // We found the correct pattern: return the array
            return lMatchHelp.get(i).patt;
          }
        }
        // The pattern is not yet known: implement it
        MatchHelp oNew = new MatchHelp(sPatterns);
        /*
        // Make sure we are not interrupted
        while (bBusy) {
          int i = 1;
        }
        bBusy = true;
                */
        lMatchHelp.add(oNew);
        /* bBusy = false; */
        // Return this new pattern
        return oNew.patt;
      } catch (RuntimeException ex) {
        // Warn user
        objErr.DoError("PatternStore/getMatchHelp error", ex, PatternStore.class);
        // Return failure
        return null;
      }
    }
  }
  private class MatchHelp {
    public String key;      // The initial full pattern
    public String[] value;  // The string array implementing the pattern
    public Pattern[] patt;  // The patterns for each string
    // =============== Class initialisation ======================================
    public MatchHelp(String sPatterns) {
      // Get and set the key
      key = sPatterns;
      // Get the patterns into an array
      value = sPatterns.split("\\|");
      // Make room for the patterns
      patt = new Pattern[value.length];
      // Convert each of the patterns
      for (int i=0;i < value.length;i++) {
        // Turn the string into a regular expression
        // String sThis = Pattern.quote(value[i]);
        String sThis = value[i];
        sThis = "^" + sThis.replace("*", ".*").replace("?", ".") + "$";
        patt[i] = Pattern.compile(sThis);
      }
    }
  }  
}


