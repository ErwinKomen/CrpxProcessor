/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */
package nl.ru.xmltools;

/**
 *
 * @author erwin R. Komen
 */
public class ParseResult {
  // ============= Elements of one item ========================================
  public String treeId;    // Identifier of this element
  public String forestId;  // The forest id
  public String cat;       // Category for sub=categorization
  public String msg;       // Message associated with this element
  public String db;        // Database results;
  // ============= Creation of one element =====================================
  public ParseResult(String sTreeId, String sForestId, String sCat, String sMsg, String sDb) {
    this.treeId = sTreeId;
    this.forestId = sForestId;
    this.cat = sCat;
    this.msg = sMsg;
    this.db = sDb;
  }
}
