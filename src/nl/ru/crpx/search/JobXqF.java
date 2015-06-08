/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

package nl.ru.crpx.search;

import java.io.File;
import nl.ru.crpx.project.ExecutePsdxStream;
import nl.ru.crpx.xq.RuBase;
import org.w3c.dom.Node;

/**
 * A "JobXqF" handles processing of one file for one corpus research project
 * 
 * @author Erwin R. Komen
 */
public class JobXqF extends Job {
// <editor-fold defaultstate="collapsed" desc="Variables">
  // ========== Variables needed for this Xq search job ========================
  public int intPrecNum;                    // Number of preceding context lines
  public int intFollNum;                    // Number of following context lines
  public int intCurrentQCline = 0;          // The current QC line we are working on
  public int intCrpFileId;                  // The @id associated with this job
  public Node ndxCurrentHeader = null;      // XML header of the current XML file
  public boolean ru_bFileSaveAsk = false;   // Needed for ru:setattrib()
  public boolean bTraceXq = false;          // Trace on XQ processing
  File fInput;                              // The file to be searched
  ExecutePsdxStream objEx;                            // Execution method
  // ========== Variables local to this search job =============================
  // private CrpFile oCrpFile;                 // The CrpFile object of what we are doing now
// </editor-fold>
  // =================== Class initialisation ==================================
  public JobXqF(SearchManager searchMan, String userId, SearchParameters par) {
    // Make sure the class I extend is initialized
    super(searchMan, userId, par);
    // Other initializations for this Xq search job
    intFollNum = crpThis.getFollNum();
    intPrecNum = crpThis.getPrecNum();
    // Get the execution object
    this.objEx = (ExecutePsdxStream) crpThis.getExe();
    // Get the parameter
    intCrpFileId = par.getInteger("crpfileid");
    // My own copy of the CrpFile object
    // oCrpFile = RuBase.getCrpFile(par.getInteger("crpfileid"));
    // Get a handle to the input file
    fInput = RuBase.getCrpFile(par.getInteger("crpfileid")).flThis;
    //
  }
  
  // ======================= Perform the search ================================
  @Override
  public void performSearch() throws QueryException {
    try {
      // Validate
      if (crpThis==null) { errHandle.DoError("There is no CRP"); return;}
      // Get the file name
      String sName = fInput.getName();
      // Perform the queries on the selected CrpFile object
      if (objEx.ExecuteQueriesFile(this, intCrpFileId)) {
        // Check for interrupt
        if (errHandle.bInterrupt) {
          errHandle.DoError("JobXqF: The program has been interrupted [" + sName + "]");
        } else {
          errHandle.debug("JobXqF: performSearch: ready handling job [" + sName + "]");
        }
      } else {
        errHandle.DoError("JobXqF: The queries could not be executed [" + sName + "]");
      }

      // Get the current file we are searching
      // fInput = oCrpFile.flThis;
      
      
    } catch (Exception ex) {
      // Show the error
      errHandle.DoError("JobXqF: Could not perform search", ex, JobXq.class);
    }
  }
}
