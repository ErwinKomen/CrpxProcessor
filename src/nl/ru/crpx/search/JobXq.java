/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

package nl.ru.crpx.search;

import nl.ru.crpx.project.CorpusResearchProject;
import org.w3c.dom.Node;

/**
 *
 * @author Erwin R. Komen
 */
public class JobXq extends Job {
  // ========== Variables needed for this Xq search job ========================
  public int intPrecNum;                     // Number of preceding context lines
  public int intFollNum;                     // Number of following context lines
  public int intCurrentQCline = 0;           // The current QC line we are working on
  public Node ndxCurrentHeader = null;       // XML header of the current XML file
  public boolean ru_bFileSaveAsk = false;    // Needed for ru:setattrib()
  public boolean bTraceXq = false;           // Trace on XQ processing
  // =================== Class initialisation ==================================
  public JobXq(CorpusResearchProject objPrj) {
    // Make sure the class I extend is initialized
    super(objPrj);
    // Other initializations for this Xq search job
    intFollNum = crpThis.getFollNum();
    intPrecNum = crpThis.getPrecNum();
  }
}
