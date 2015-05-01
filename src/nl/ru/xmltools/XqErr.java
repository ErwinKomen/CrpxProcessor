/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;

import java.util.ArrayList;
import java.util.List;
import nl.ru.crpx.project.Qinfo;
import static nl.ru.crpx.project.CrpGlobal.DoError;
import static nl.ru.crpx.project.CrpGlobal.Status;
import nl.ru.util.ByRef;
import nl.ru.util.json.JSONException;
import nl.ru.util.json.JSONObject;

/**
 *
 * @author Erwin
 */
public class XqErr {
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
      DoError("XqErr/getXqErrLoc error: " + ex.getMessage() + "\r\n");
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
    String strCode;   // What we report to the user
    
    try {
      // Make a combined description
      strCode = "Error executing query " + strQuery + ":" + "\r\n";
      strCode += "The error is in the [" + strType.argValue + "] called [" + strName.argValue + "]" + "\r\n";
      strCode += "Line number: " + intLocalPos.argValue + "\r\n";
      strCode += "Position: " + intCol.argValue + "\r\n";
      strCode += "Xquery error message:" + "\r\n" + strDescr;
      // Compile the error into a JSONObject
      JSONObject oErr = new JSONObject();
      oErr.put("Type", strType.argValue);
      oErr.put("Name", strName.argValue);
      oErr.put("Line", intLocalPos.argValue);
      oErr.put("Col", intCol.argValue);
      oErr.put("Descr", strDescr);
      oErr.put("Code", strCode);
      // Add the error to the global Qerr list
      this.lQerr.add(oErr);
    }  catch (JSONException ex) {
      // Warn user
      DoError("ExecutePsdxStream/ExecuteQueries error: " + ex.getMessage() + "\r\n");
    }
  }

}
