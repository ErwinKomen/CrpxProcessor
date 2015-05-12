/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

package nl.ru.crpx.cmd;

import nl.ru.crpx.dataobject.DataObject;
import nl.ru.crpx.project.CorpusResearchProject;

/**
 *
 * @author Erwin R. Komen
 */
public class RequestHandlerShow extends RequestHandler {

  public RequestHandlerShow(CrpxProcessor servlet, String indexName, CorpusResearchProject crpThis) {
    super(servlet, indexName, crpThis);
  }

  @Override
  public DataObject handle() throws InterruptedException {
    return null;
  }

}
