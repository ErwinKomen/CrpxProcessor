package nl.ru.crpx.search;
/*
 * This software has been developed at the "Radboud University"
 *   in order to support the CLARIAH project "ACAD".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.project.ExecutePsdxStream;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.xq.CrpFile;
import nl.ru.crpx.xq.RuBase;
import org.w3c.dom.Node;

/**
 * A "RunXqF" handles processing of one file for one corpus research project 
 * 
 * @author Erwin R. Komen
 */
public class RunXqF extends RunAny {
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
  ExecutePsdxStream objEx;                  // Execution method
  CorpusResearchProject crpThis;            // The CRP we are running
  Job parentXqJob;                          // The XQ job we are 'under'
// </editor-fold>
  
// <editor-fold defaultstate="collapsed" desc="Class initializer">
  public RunXqF(ErrHandle oErr, Job jParent, CorpusResearchProject prjThis, 
          String userId, SearchParameters par) {
    // Make sure the class I extend is initialized
    super(oErr, par);
    // Then make my own initialisations
    try {
      this.jobStatus = "creating";
      // Set the CRP + File provided for us
      this.crpThis = prjThis;
      // Get a handle to the job above us
      this.parentXqJob = jParent;
      // Other initializations for this Xq search job
      intFollNum = crpThis.getFollNum();
      intPrecNum = crpThis.getPrecNum();
      // Get the execution object
      this.objEx = (ExecutePsdxStream) crpThis.getExe();
      // Get the parameter
      intCrpFileId = par.getInteger("crpfileid");
      // My own copy of the CrpFile object
      CrpFile oCrpFile = RuBase.getCrpFile(intCrpFileId);
      // Validation
      if (oCrpFile == null) {
        // Don't know how this is possible: the CrpFile has been closed perhaps??
        errHandle.DoError("RunXqF initialisation: cannot get copy of CrpFile id="+intCrpFileId);
        return;
      }
      // Get a handle to the input file
      // fInput = RuBase.getCrpFile(par.getInteger("crpfileid")).flThis;
      fInput = oCrpFile.flThis;

      this.jobStatus = "created";
    } catch (Exception ex) {
      errHandle.DoError("RunXqF - Initialisation of class fails: ", ex);
    }
  }
// </editor-fold>
  
  // Getters and setters
  public Job getXqJob() {return parentXqJob;}

  @Override
  public void run() {
    try {
      performSearch();
    } catch (QueryException ex) {
      // Set this in the job caller
      parentXqJob.queryException = true;
      // Show the error
      errHandle.DoError("RunXqF: query exception", ex, RunXqF.class);
    } catch (InterruptedException ex) {
      errHandle.bInterrupt = true;
      // Show the error
      errHandle.DoError("RunXqF: interrupted", ex, RunXqF.class);
    }
  }
  public void performSearch() throws QueryException, InterruptedException {
    try {
      this.jobStatus = "running";
      // Validate
      if (crpThis==null) { errHandle.DoError("There is no CRP"); return;}
      // Get the file name
      String sName = fInput.getName();
      // Perform the queries on the selected CrpFile object
      if (objEx.ExecuteQueriesFile(this, intCrpFileId)) {
        // Check for interrupt
        if (errHandle.bInterrupt) {
          this.jobStatus = "interrupt";
          errHandle.DoError("JobXqF: The program has been interrupted [" + sName + "]");
          parentXqJob.setJobStatus("error");
        } else {
          this.jobStatus = "finished";
          errHandle.debug("JobXqF: performSearch: ready handling job [" + sName + "]");
        }
      } else {
        this.jobStatus = "error";
        errHandle.DoError(parentXqJob.getJobErrors());
        errHandle.DoError("JobXqF: The queries could not be executed [" + sName + "]");
        errHandle.debug("JobXqF errors=" + parentXqJob.getJobErrors());
        parentXqJob.setJobStatus("error");
      }
      
      
    } catch (Exception ex) {
      // Show the error
      errHandle.DoError("RunXqF: Could not perform search", ex, RunXqF.class);
    }
  }

  @Override
  /**
   * close
   *    
   * Close all the resources related to this job
   */
  public void close() {
    try {
      
      // Make sure to release the file handle
      CrpFile oCrpFile = RuBase.getCrpFile(intCrpFileId);
      // Validation
      if (oCrpFile != null) {
        oCrpFile.close();
        // Remove this eCRP from the RuBase arraylist of CRP=FILE objects
        RuBase.removeCrpCaller(oCrpFile);
      }
      this.jobStatus = "closed";
      // Finalize me
      this.finalize();
    } catch (Exception ex) {
      // Show the error
      errHandle.DoError("RunXqF: close problem", ex, RunXqF.class);
    } catch (Throwable ex) {
      // Show the error
      errHandle.DoError("RunXqF: finalization problem: "+ex.getMessage());
    }
  }

  
}
