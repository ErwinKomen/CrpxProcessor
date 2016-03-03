/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.xq;

import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import net.sf.saxon.s9api.SaxonApiException;
import nl.ru.crpx.project.Qinfo;
import static nl.ru.crpx.tools.FileIO.getFileNameWithoutExtension;
import nl.ru.util.ByRef;
import nl.ru.util.FileUtil;
import nl.ru.xmltools.XqErr;

/**
 *
 * @author Erwin
 */
public class XqErrorListener implements ErrorListener {
  // =========== Local variables ===============================================
  //private XqErr oXq;                    // For using XqErr
  // private ExecuteXml parent;
  List<Exception> errorList;  // List of all errors
  List<Integer> lineList;     // List of line numbers
  List<Integer> colList;      // List of column  numbers
  List<Boolean> fatalList;    // Fatal error lists

  public XqErrorListener() {
    // this.parent = parent;
    errorList = new ArrayList<>();
    lineList = new ArrayList<>();
    colList = new ArrayList<>();
    fatalList = new ArrayList<>();
  }
  @Override
  public void error(TransformerException exception) {
    errorList.add(exception);
    SourceLocator sl = exception.getLocator();
    if (sl == null) {
      lineList.add(-1);
      colList.add(-1);
    } else {
      lineList.add(sl.getLineNumber());
      colList.add(sl.getColumnNumber());
    }
    fatalList.add(false);
  }
  @Override
  public void fatalError(TransformerException exception)  {
    errorList.add(exception);
    SourceLocator sl = exception.getLocator();
    if (sl == null) {
      lineList.add(-1);
      colList.add(-1);
    } else {
      lineList.add(sl.getLineNumber());
      colList.add(sl.getColumnNumber());
    }

    // throw exception;
    fatalList.add(true);
  }
  @Override
  public void warning(TransformerException exception) {
    // no action
    lineList.add(-1);
    colList.add(-1);
    fatalList.add(false);
  }
  public int lineNum(int i) {return lineList.get(i);}
  public int colNum(int i) {return colList.get(i);}
  public boolean getFatal(int i) {return fatalList.get(i);}
  public String getMsg(int i) {return errorList.get(i).getMessage();}
  public List<Exception> getErrList() {return errorList;}
  
  /**
   * processError
   *    Process a runtime Xquery error
   * 
   * @param strQfile
   * @param sMessage
   * @param arQinfo
   * @param oXq       - Where we store and return the error
   * @return 
   */
  public boolean processError(String strQfile, String sMessage, Qinfo[] arQinfo, XqErr oXq) {
    ByRef<String> strDQname;          // The name of the part (query/def) where the error occurs
    ByRef<String> strType;            // The part of the query (def/ru/qry) where the error occurs
    ByRef<Integer> intLocalPos;       // The line number (within query or definition) where the error occurs
    ByRef<Integer> intCol;            // Column where error occurs
    String strError;                  // String rep of error
    
    try {
      // Validate
      if (oXq == null) return false;
      // Start the error message string
      String strFile = getFileNameWithoutExtension(strQfile);
      strError = "Error executing query " + strFile + ":\n";
      errorList = this.getErrList();
      if (errorList.isEmpty())
        strError += "No errorlist: "+ sMessage + "\n";
      else {
        // Initialisations
        strType = new ByRef(""); intLocalPos = new ByRef(0); intCol  = new ByRef(0);
        strDQname = new ByRef("");
        // Walk through all the errors
        for (int i=0; i< errorList.size(); i++) {
          // Get the line number
          int iLineNum = this.lineNum(i);
          // Properly initialize
          intLocalPos.argValue = 0; 
          intCol.argValue = this.colNum(i);
          // Determine where this error is occurring
          String strLoc = oXq.getXqErrLoc(arQinfo, strFile, iLineNum, strType, strDQname, intLocalPos, intCol);
          // NOTE: some of the results return ByRef in [strType], [strDQname]
          // Add this error to the list
          oXq.AddXqErr(strType, strDQname, intLocalPos, intCol, this.getMsg(i), strFile);
        }
      }
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

}
