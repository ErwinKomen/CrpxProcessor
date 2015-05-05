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

/**
 *
 * @author Erwin R. Komen
 */
public class PatternStore extends CrpGlobal {
  private List<MatchHelp> lMatchHelp;
  // ============== CLASS initialization =======================================
  public PatternStore() {
    // Initialize a list of string arrays to help ru:matches()
    lMatchHelp = new ArrayList<>();
  }
  
  // Find a string-array of regular expressions derived from @sPatterns
  //   and implementing the "like" operator
  public Pattern[] getMatchHelp(String sPatterns) {
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
      lMatchHelp.add(oNew);
      // Return this new pattern
      return oNew.patt;
    } catch (RuntimeException ex) {
      // Warn user
      DoError("Extensions/getMatchHelp error", ex, PatternStore.class);
      // Return failure
      return null;
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
      value = sPatterns.split("|");
      // Make room for the patterns
      patt = new Pattern[value.length];
      // Convert each of the patterns
      for (int i=0;i < value.length;i++) {
        // Turn the string into a regular expression
        String sThis = Pattern.quote(value[i]);
        sThis = "^" + sThis.replace("*", ".*").replace("?", ".") + "$";
        patt[i] = Pattern.compile(sThis);
      }
    }
  }  
}


