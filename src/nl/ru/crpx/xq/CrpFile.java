/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.xq;

import java.io.File;
import nl.ru.crpx.project.CorpusResearchProject;

/**
 *
 * @author Erwin R. Komen
 */
public class CrpFile {
  public CorpusResearchProject crpThis; // Link to the corpus research project
                                        // ALTERNATIVE: use an index to an array(list) of CRPs??
  public File flThis;                   // File that is being treated
  public int QCcurrentLine;             // Current QC line of project being executed
  public CrpFile(CorpusResearchProject oCrp, File fFile) {
    this.crpThis = oCrp;
    this.flThis = fFile;
    this.QCcurrentLine = -1;
  }
}
