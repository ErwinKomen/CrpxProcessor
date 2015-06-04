/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.tools;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import nl.ru.util.json.JSONObject;
import org.w3c.dom.Node;

/**
 *
 * @author Erwin
 */
public class General {
  // === Variables that need to be available globally, and which are kept here
  public boolean ru_bFileSaveAsk = false;   // Needed for ru:setattrib()
  public int intPrecNum = 2;                // Number of preceding context lines
  public int intFollNum = 1;                // Number of following context lines
  public Node ndxCurrentHeader;             // XML header of the current XML file
  // public String strOutputDir = "";       // Where to put output files
  // === Local variables that can be accessed through get/set functions
  private boolean bInterrupt = false;       // Global interrupt flag
  private String strCurrentPsdx = "";       // Full path of the currently processed PSDX file
  // ================== Internal usage ===============================
  private static List<String> arDelim = new ArrayList<>();
  // ================== Interrupt access ==============================
  public boolean getInterrupt() {return bInterrupt;}
  public void setInterrupt(boolean bSet) { bInterrupt = bSet; }
  // ================== Current file ==================================
  public String getCurrentPsdx() { return strCurrentPsdx;}
  public void setCurrentPsdx(String sName) { strCurrentPsdx = sName;}
  
  // ================== Initialisation of the General class
  public General() {
    // Populate the arDelim list
    arDelim.add("\n\r"); arDelim.add("\r\n"); arDelim.add("\r"); arDelim.add("\n");
  }
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
  public static void Status(String msg) {
    System.err.println(msg);
  }
  public static void Status(String msg, int intPtc) {
    System.err.println(intPtc + "%: " + msg);
  }
  public static void LogOutput(String msg) {
    System.err.println(msg);
  }
  public static void LogOutput(String msg, boolean bShowStatus) {
    if (bShowStatus) Status(msg);
    // TODO: log the output somewhere
  }
  /* ---------------------------------------------------------------------------
    Name :  GetDelim
    Goal :  Get an appropriate delimiter
    History:
    13-07-2009  ERK Created for .Net
    21/apr/2015 ERK Adapted for Java 
   --------------------------------------------------------------------------- */
  public static String GetDelim(String strText) {
    // Validate
    if (arDelim.isEmpty()) return "";
    // Try all elements
    for (int i=0;i<arDelim.size();i++) {
      // Does the text contain this delimiter?
      if (strText.contains(arDelim.get(i))) return arDelim.get(i);
    }
    // Nothing found, so default: return the first one
    return arDelim.get(0);
  }

  public static String getJsonString(JSONObject o, String key) {
    if (o.has(key)) return o.getString(key);
    else return "";
  }
  public static String getCurrentTimeStamp() {
    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
    Date now = new Date();
    String strDate = sdfDate.format(now);
    return strDate;
  }
  public static String getSaveDate(File fThis) {
    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
    Date dtSave = new Date(fThis.lastModified());
    String strDate = sdfDate.format(dtSave);
    return strDate;
  }

}
