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
    
    // Fill the object
    oThis.put("msg", msg);
    if (ex==null) oThis.put("ex", ""); else oThis.put("ex", ex.getMessage());
    if (cls==null) 
      oThis.put("cls", "unknown");
    else
      oThis.put("cls", cls.getName());
    // Add the object to the static stack
    lErrStack.add(oThis);
    // DEBUGGING: also show it in the error logger
    // logger.error(msg, ex);
    Logger.getLogger(cls).error(msg, ex);
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
  }
}
