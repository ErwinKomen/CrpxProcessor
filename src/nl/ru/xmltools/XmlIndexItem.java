/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */
package nl.ru.xmltools;

/**
 * XmlIndexItem -- one element from an index file used to gain random-access
 *              to an xml file
 * 
 * @author Erwin R. Komen
 * @history
 *  16/sep/2015 Created
 */
public class XmlIndexItem {
  public String tag;      // The tag this covers
  public String id;       // Numeric or character identifier of this element
  public String lastId;   // Constituent id (optional)
  public String part;     // Optional part identifier
  public int start;       // Starting position for this tag
  public int size;        // Size of this tag in *bytes*
  public XmlIndexItem(String sTag, String sId, String sPart, int iStart, int iSize) {
    this.tag = sTag; this.start = iStart; this.size = iSize; this.id = sId; this.part = sPart;
  }
  // ============= Specify how one item should be made available in CSV ========
  public String csv() {
    return this.tag + "\t" + this.id + "\t" + this.part + "\t" + this.start + "\t" + this.size + "\n";
  }
}
