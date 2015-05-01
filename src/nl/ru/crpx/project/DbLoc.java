/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

package nl.ru.crpx.project;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Erwin R. Komen
 */
public class DbLoc {
  // =============== Accessible members of [DbLoc] =============================
  public NodeList ndxDbList; // All nodes for the current text/forest combination
  public Node ndxDbPrev;     // Previous database location
  public Node ndxDbRes;      // Current result
  // =============== Class instantiation =======================================
  public DbLoc() {
    ndxDbPrev = null;
    ndxDbRes = null;
    ndxDbList = null;
  }
  
}
