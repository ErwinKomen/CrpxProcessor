/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.project;
import nl.ru.util.json.JSONObject;
import org.apache.log4j.Logger;

/**
 *
 * @author Erwin R. Komen, 22/apr/2015
 */
public class PrjTypeManager {
  private static final Logger logger = Logger.getLogger(PrjTypeManager.class);
  // Local copies of the settings for this engine
  private String sDescr;  // Description of this projec type
  private String sQext;   // Query extension
  private String sDext;   // Definition extension
  private String sSrcExt; // Extension of source types (.psdx, .psd, .folia.xml etc)
  private String sComBeg; // Beginning of comment
  private String sComEnd; // End of comment
  private String sEngine; // The engine used: xquery // csearch2
  private String sDefaultPerFile; // Default period file
  private String sDefaultDefFile; // Default definition file
  private String sDefaultQuery;   // Default query
  /** Which project type is this? That determines locations of files */
  private String sProjType;
  
  public PrjTypeManager(JSONObject properties) {
    logger.debug("EngineManager created");
    // Find out which project type has been chosen
    JSONObject pInfo = properties.getJSONObject("pinfo");   // Defined within the [pinfo] block
    sProjType = pInfo.getString("ProjectType");             // Specific definitions depend on the project type
    logger.debug("Project-manager: project type = " + sProjType);
    JSONObject pInfoCurrent = pInfo.getJSONObject(sProjType); // Access the information for this project type
    sDescr = pInfoCurrent.getString("Descr");
    sQext = pInfoCurrent.getString("Qext");
    sDext = pInfoCurrent.getString("Dext");
    sSrcExt = pInfoCurrent.getString("SrcExt");
    sComBeg = pInfoCurrent.getString("ComBeg");
    sComEnd = pInfoCurrent.getString("ComEnd");
    sEngine = pInfoCurrent.getString("Engine");
    sDefaultPerFile = pInfoCurrent.getString("DefaultPerFile");
    sDefaultDefFile = pInfoCurrent.getString("DefaultDefFile");
    // The default query is an array of strings, and should be joined into one string
    sDefaultQuery = pInfoCurrent.getJSONArray("DefaultQuery").join("\n");
  }
  
  // ============= Get and Set functions for our local copies
  public String getProjType() { return sProjType; }
  public String getDescr() { return sDescr; }
  public String getQext() { return sQext; }
  public String getDext() { return sDext; }
  public String getSrcExt() { return sSrcExt; }
  public String getComBeg() { return sComBeg; }
  public String getComEnd() { return sComEnd; }
  public String getEngine() { return sEngine; }
  public String getDefaultPerFile() { return sDefaultPerFile; }
  public String getDefaultDefFile() { return sDefaultDefFile; }
  public String getDefaultQuery() { return sDefaultQuery; }
  
}
