package nl.ru.crpx.search;

import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.xq.Extensions;

/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

/**
 *
 * @author Erwin R. Komen
 */
public class Job {
  // ============== The error handler can be accessed globally =================
  public static ErrHandle errHandle;
  public CorpusResearchProject crpThis;
  // ============== Class initialisation ======================================
  public Job(CorpusResearchProject objPrj) {
    // initialize my own error handler
    errHandle = new ErrHandle(Extensions.class );
    // Set my copy of the corpus research project we are dealing with
    crpThis = objPrj;
  }
}
