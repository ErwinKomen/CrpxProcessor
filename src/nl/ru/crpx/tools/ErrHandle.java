/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

package nl.ru.crpx.tools;

import java.util.ArrayList;
import java.util.List;
import net.sf.saxon.expr.XPathContext;
import nl.ru.crpx.xq.CrpFile;
import nl.ru.util.json.JSONObject;
import org.apache.log4j.Logger;

/**
 *
 * @author Erwin R. Komen
 */
public class ErrHandle {
  // protected static final Logger logger = Logger.getLogger(ErrHandle.class);
  protected final Logger logger ;
  // ============== Variables associated with the *class* "ErrHandle" ==========
  private List<JSONObject> lErrStack;
  private Class clsDefault;
  // ============== Interrupt is publicly available ============================
  public boolean bInterrupt;
  // ================== Initializer of this class ==============================
  public ErrHandle(Class clsMine) {
    clsDefault = clsMine;
    bInterrupt = false;
    lErrStack = new ArrayList<>();
    logger = Logger.getLogger(clsMine);
  }
  // ================== Public methods for this class ==========================
  public boolean DoError(String msg, Exception ex, Class cls) {
    JSONObject oThis = new JSONObject();  // Where we store all the info
    boolean bFound = false;               // Prevent additional entries
    
    // Fill the object
    oThis.put("msg", msg);
    if (ex==null) oThis.put("ex", ""); else oThis.put("ex", ex.getMessage());
    if (cls==null) 
      oThis.put("cls", "unknown");
    else
      oThis.put("cls", cls.getName());
    // Add the error to the list (provided it is not there already)
    addErr(oThis);
    // DEBUGGING: also show it in the error logger
    // logger.error(msg, ex);
    Logger.getLogger(cls).error(msg, ex);
    // Return failure
    return false;
  }
  
  public boolean DoError(List<JSONObject> arErr, Exception ex, Class cls) {
    // Validate
    if (arErr != null) {
      // Process all errors
      for (int i=0;i<arErr.size();i++) {
        JSONObject oThis = new JSONObject();
        JSONObject arOneErr = arErr.get(i);
        if (arOneErr.has("msg") && arOneErr.has("ex") && arOneErr.has("cls")) {
          oThis.put("msg", arOneErr.getString("msg"));
          oThis.put("cls", arOneErr.getString("cls"));
          oThis.put("ex", arOneErr.getString("ex"));
        } else {
          oThis.put("msg", arErr.get(i).toString());
          if (ex==null) oThis.put("ex", ""); else oThis.put("ex", ex.getMessage());
          if (cls==null) 
            oThis.put("cls", "unknown");
          else
            oThis.put("cls", cls.getName());
        }
        // Add the object to the static stack
        addErr(oThis);
      }
      Logger.getLogger(cls).error(arErr.toString(), ex);
    }
    // Return failure
    return false;
  }
  public boolean DoError(String msg, Class cls) {
    return DoError(msg, null, cls);
  }
  public boolean DoError(String msg, Exception ex) {
    return DoError(msg, ex, clsDefault);
  }
  public boolean DoError(String msg) {
    return DoError(msg, null, clsDefault);
  }
  public boolean DoError(List<JSONObject> arErr) {
    return DoError(arErr, null, clsDefault);
  }
  // ============ Run-time errors in Xquery processes
  public boolean DoError(String strDescr, String strName, XPathContext objXp) {
    int iLineNum=0;
    int iColNum=0;
    String sQfile= "";    // Full name + location of the query file used
    String sSentId = "";  // ID of the sentence being processed
    JSONObject oErr;
    // String sLoc = "unknown";
    
    if (objXp != null & objXp.getOrigin() != null) {
      iLineNum = objXp.getOrigin().getLineNumber();
      iColNum = objXp.getOrigin().getColumnNumber();
      // sLoc = ((CrpFile) objXp.getController().getParameter("crpfile")).crpThis.getListQuery()
      sQfile = (String) objXp.getController().getParameter("qfile");
      sSentId = (String) objXp.getController().getParameter("sentid");
      oErr = ((CrpFile) objXp.getController().getParameter("crpfile")).crpThis.getExe().GetErrorLoc(sQfile, strDescr, iLineNum, iColNum);
      oErr.put("text", ((CrpFile) objXp.getController().getParameter("crpfile")).flThis.getName());
    } else {
      // Compile the error into a JSONObject
      oErr = new JSONObject();
      oErr.put("Type", "Run-time error");
      oErr.put("Name", sQfile);
      oErr.put("Line", iLineNum);
      oErr.put("Col", iColNum);
      oErr.put("Descr", strDescr);
      oErr.put("Code", strName);
      oErr.put("text", "undetermined");
    }
    // General error object info 
    oErr.put("sentid", sSentId);
    oErr.put("msg", strDescr);
    oErr.put("cls", "Extensions");
    oErr.put("ex", "");
    // Add the error to the list
    lErrStack.add(oErr);
    // Return failure
    return false;
  }
  public void debug(String msg) {
    logger.debug(msg);
    // logger.debug(msg);
  }
  public void debug(String msg, Class cls) {
    Logger.getLogger(cls).debug(msg);
    // logger.debug(msg);
  }
  // ========== Allow others to see that there are errors ===========
  public boolean hasErr() {
    return (lErrStack.size() > 0);
  }
  // ========== Allow retrieval of the complete stack ===============
  public List<JSONObject> getErrList() {
    return lErrStack;
  }
  // ========== Allow others to reset the error stack ===============
  public void clearErr() {
    lErrStack.clear();
    bInterrupt = false;
  }
  // ========== Provide one solid way to add an error element =======
  private void addErr(JSONObject oThis) {
    boolean bFound = false;
    
    // Check if this object is not already  on the stack (prevent equal objects)
    for (int i=0;i<lErrStack.size();i++) {
      if (oThis.getString("msg").equals(lErrStack.get(i).getString("msg")) &&
              oThis.getString("cls").equals(lErrStack.get(i).getString("cls")) &&
              oThis.getString("ex").equals(lErrStack.get(i).getString("ex"))) {
        bFound = true; break;
      }
    }
    // Add the object to the static stack
    if (!bFound) lErrStack.add(oThis);
  }
}
