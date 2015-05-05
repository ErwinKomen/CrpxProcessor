/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.project;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import nl.ru.crpx.xq.Extensions;
import nl.ru.util.json.JSONObject;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;

/**
 *
 * @author Erwin R. Komen
 */
public class CrpGlobal {
  // === Variables that need to be available globally, and which are kept here
  /* 
  public static boolean ru_bFileSaveAsk = false;   // Needed for ru:setattrib()
  public static int intPrecNum = 2;                // Number of preceding context lines
  public static int intFollNum = 1;                // Number of following context lines
  public static Node ndxCurrentHeader = null;      // XML header of the current XML file
  public static int intCurrentQCline = 0;          // The current QC line we are working on
  public static boolean bTraceXq = false;         // Trace on XQ processing
  */
  // ========== Variables that are 'globally' available to all classes that "Extend CrpGlobal" ================
  // protected int intPrecNum = 2;                 // Number of preceding context lines
  // protected int intFollNum = 1;                 // Number of following context lines
  // protected int intCurrentQCline = 0;           // The current QC line we are working on
  // protected Node ndxCurrentHeader = null;       // XML header of the current XML file
  // protected boolean ru_bFileSaveAsk = false;    // Needed for ru:setattrib()
  protected boolean bInterrupt = false;         // Global interrupt flag
  protected boolean bTraceXq = false;           // Trace on XQ processing
  // protected Extensions objExt = null;           // The extensions
  // public String strOutputDir = "";           // Where to put output files
  // =================== Global variables, but only available through interfaces =============
  // private static boolean bInterrupt = false;       // Global interrupt flag
  private static String strCurrentPsdx = "";       // Full path of the currently processed PSDX file
  // ================== Internal usage ===============================
  private static List<String> arDelim = new ArrayList<>();
  // Initiate a logger
  private Logger logger;
  // ================== Interrupt access ==============================
  public boolean getInterrupt() {return bInterrupt;}
  public void setInterrupt(boolean bSet) { bInterrupt = bSet; }
  // ================== Current file ==================================
  public String getCurrentPsdx() { return strCurrentPsdx;}
  public void setCurrentPsdx(String sName) { strCurrentPsdx = sName;}
  // ================== Provide outside access to the crp =============
  // public CorpusResearchProject getCrpx() { return crpThis; }
  
  // ================== Initialisation of the CrpGlobal class
  public CrpGlobal() {
    // Populate the arDelim list
    arDelim.add("\n\r"); arDelim.add("\r\n"); arDelim.add("\r"); arDelim.add("\n");
    // Other initialisations
    // objExt = new Extensions();
  }
  /* ---------------------------------------------------------------------------
   Name:    DoError
   Goal:    Show an error and return false
   History:
   17/10/2014   ERK Created
   --------------------------------------------------------------------------- */
  public boolean DoError(String msg) {
    // Issue a message
    System.err.println("CorpusResearchProject error:\n" + msg);
    // Set the interrupt flag
    bInterrupt = true;
    // Return failure
    return(false);
  }
  public boolean DoError(String msg, Class cls) {
    // Issue a logger message
    logger = Logger.getLogger(cls);
    logger.error(msg);
    // Set interrupt
    bInterrupt = true;
    // Return failure
    return(false);
  }
  public boolean DoError(String msg, Exception ex, Class cls) {
    // Issue a logger message
    logger = Logger.getLogger(cls);
    logger.error(msg, ex);
    // Set interrupt
    bInterrupt = true;
    // Return failure
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

}
