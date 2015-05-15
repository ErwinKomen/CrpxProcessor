/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.xq;

import java.io.File;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.xmltools.XmlNode;

/**
 * CrpFile - holds the combination of CRP and the file being processed
 * 
 * @author Erwin R. Komen
 */
public class CrpFile {
  public CorpusResearchProject crpThis; // Link to the corpus research project
                                        // ALTERNATIVE: use an index to an array(list) of CRPs??
  public File flThis;                   // File that is being treated
  public int QCcurrentLine;             // Current QC line of project being executed
  public XmlNode ndxCurrentForest;      // The sentence element we are now working on
  public XmlNode ndxHeader;             // The header object of this file
  
  // =========== Class initializer =============================================
  public CrpFile(CorpusResearchProject oCrp, File fFile) {
    this.crpThis = oCrp;
    this.flThis = fFile;
    this.QCcurrentLine = -1;
    this.ndxCurrentForest = null;
  }
}
