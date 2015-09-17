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
 * @author Erwin R. Komen
 */
public class IndexFile {
  public String Name;       // Name of the index file
  public String Dir;        // Name of directory for this index
  public StringBuilder sb;  // Content for this index file
  public IndexFile(String sName, String sDir) {
    this.Name = sName; this.sb = new StringBuilder();
    this.Dir = sDir;
  }
}
