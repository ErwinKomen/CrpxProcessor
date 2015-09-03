/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import nl.ru.crpx.project.Qinfo;
import static nl.ru.crpx.project.CrpGlobal.Status;
import nl.ru.crpx.tools.FileIO;
import nl.ru.util.ByRef;
import nl.ru.util.json.JSONException;
import nl.ru.util.json.JSONObject;
import org.apache.log4j.Logger;

/**
 *
 * @author Erwin
 */
public class XqErr {
  protected static final Logger logger = Logger.getLogger(XqErr.class);
  // ============ Elements that are available globally =========================
  public List<JSONObject> lQerr;    // List of error objects
  // ============ Local copies for internal use ================================
  
  // ============ Class initiator
  public XqErr() {
    lQerr = new ArrayList<>();  // List of objects for each error
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  XqErrShow
  // Goal :  Retrieve the correct location of the error
  // History:
  // 20-08-2014   ERK Created for .NET
  // 28/apr/2015  ERK Transformed into Java
  // ----------------------------------------------------------------------------------------------------------
  public boolean XqErrShow() {
    // If an error has occurred - inform the user!
    if (lQerr.size()>0) {
      // Command-line method: walk all the errors and show them to the user
      for (int i=0; i <lQerr.size(); i++) {
        JSONObject oErr = lQerr.get(i);
        Status("Xquery error #" + (i+1) + ":");
        Status("  Type=" + oErr.getString("Type"));
        Status("  Name=" + oErr.getString("Name"));
        Status("  Location=[" + oErr.getInt("Line") + "," + oErr.getInt("Col") + "]");
        Status("  Description: " + oErr.getString("Descr"));
        // Status("  Code:\r\n" + oErr.getString("Code"));
      }
    }
    // Always return success
    return true;
  }
  
  // ----------------------------------------------------------------------------------------------------------
  // Name :  GetXqErrLoc
  // Goal :  Retrieve the correct location of the error
  // History:
  // 20-08-2014   ERK Created for .NET
  // 28/apr/2015  ERK Transformed into Java
  // ----------------------------------------------------------------------------------------------------------
  public String getXqErrLoc(Qinfo[] arQinfo, String strFile, int intLine,
          ByRef<String> strType, ByRef<String> strName, ByRef<Integer> intLocalPos, 
          ByRef<Integer> intCol) {
    int intI;       // Counter
    int intK;       // Counter
    int intPos;     // Local string position
    int intPrevPos; // Previous string position
    
    try {
      // Validate
      if (arQinfo == null) return "(empty query array)";
      // Make sure we only have the local part of the file
      // strFile = (new File(strFile)).getName();
      strFile = FileIO.getFileNameWithoutExtension(strFile);
      // Remove the temp prefix
      if (strFile.startsWith("temp_")) { strFile = strFile.substring(5);}
      // Visit all the queries
      for (intI=0;intI<arQinfo.length;intI++) {
        // Do we have query information here?
        if (arQinfo[intI] == null) {
          // Something is wrong: we should have had query information here...
          
        } else {
          // Is this the query?
          if (arQinfo[intI].Name.equals(strFile)) {
            // Walk through the locations in the file
            intPos = 0; intPrevPos = 0;
            // Walk through all the Qel elements of this query
            for (intK=0; intK < arQinfo[intI].arQel.length; intK++) {
              // Advance our position
              intPrevPos = intPos;
              intPos += arQinfo[intI].arQel[intK].Lines;
              // Get out of the for-loop when we are inside the area of attention
              if (intLine < intPos) break;
            }
            // Validate: gone past it?
            if (intK >= arQinfo[intI].arQel.length) return "indeterminable";
            // Go one piece back
            intPos = intPrevPos;
            // Get the position within the local file
            intLocalPos.argValue = intLine - intPos;
            // We now have the position of the error
            // Determine name and type
            strName.argValue = arQinfo[intI].arQel[intK].Name;
            strType.argValue = arQinfo[intI].arQel[intK].Type;
            // Action depends on the type
            switch(strType.argValue) {
              case "def":
                return "Definition [" + strName.argValue + "] line "  + intLocalPos.argValue;
              case "ru": case "tb": case "functx":
                return "Namespace declaration [" + strType.argValue + "] line "  + intLocalPos.argValue;
              case "query":
                return "Query [" + strName.argValue + "] line " + intLocalPos.argValue;
              default:
                return "Type=[" + strType.argValue + "] Name=[" + strName.argValue + "]";
            }
          }
        }
      }

      // Return failure
      return "indeterminable";
    } catch (RuntimeException ex) {
      // Warn user
      logger.error("XqErr/getXqErrLoc error", ex);
      // Return failure
      return "";
    }
  }

  // ----------------------------------------------------------------------------------------------------------
  // Name :  AddXqErr
  // Goal :  Add error-details to the list we will be showing
  // History:
  // 20-08-2014   ERK Created for .NET
  // 28/apr/2015  ERK Transformed into Java
  // ----------------------------------------------------------------------------------------------------------
  public void AddXqErr(ByRef<String> strType, ByRef<String> strName, ByRef<Integer> intLocalPos, 
           ByRef<Integer> intCol, String strDescr, String strQuery) {
    try {
      JSONObject oErr = getXqErrObject(strType.argValue, strName.argValue, intLocalPos.argValue,
              intCol.argValue, strDescr, strQuery);
      // Add the error to the global Qerr list
      this.lQerr.add(oErr);
    }  catch (JSONException ex) {
      // Warn user
      logger.error("XqErr/ExecuteQueries error", ex);
    }
  }
  
  /**
   * getXqErrObject
   *    Construct an Xquery error object
   * 
   * @param strType
   * @param strName
   * @param iLocalPos
   * @param iCol
   * @param sDescr
   * @param sQuery
   * @return 
   */
  public JSONObject getXqErrObject(String strType, String strName, int iLocalPos, 
          int iCol, String sDescr, String sQuery) {
    JSONObject oErr = new JSONObject();
    String strCode;   // What we report to the user

    try {
      // Make a combined description
      strCode = "Error executing query " + sQuery + ":" + "\r\n";
      strCode += "The error is in the [" + strType + "] called [" + strName + "]" + "\r\n";
      strCode += "Line number: " + iLocalPos + "\r\n";
      strCode += "Position: " + iCol + "\r\n";
      strCode += "Xquery error message:" + "\r\n" + sDescr;
      // Compile the error into a JSONObject
      oErr.put("Type", strType);
      oErr.put("Name", strName);
      oErr.put("Line", iLocalPos);
      oErr.put("Col", iCol);
      oErr.put("Descr", sDescr);
      oErr.put("Code", strCode);
      // Return this object
      return oErr;
    }  catch (JSONException ex) {
      // Warn user
      logger.error("XqErr/getXqErrObject error", ex);
      return null;
    }
  }

}
