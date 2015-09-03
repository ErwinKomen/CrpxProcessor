/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.xq;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.search.JobXq;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.xmltools.XmlForest;
import nl.ru.xmltools.XmlForestFoliaIndex;
import nl.ru.xmltools.XmlForestPsdxIndex;
import nl.ru.xmltools.XmlForestPsdxLine;
import nl.ru.xmltools.XmlNode;

/**
 * CrpFile - holds the combination of CRP and the file being processed
 * 
 * @author Erwin R. Komen
 */
public class CrpFile {
  // ================= Static id counter =======================================
  static long id=0;
  // ================= Variables readable and writable by others ===============
  public CorpusResearchProject crpThis; // Link to the corpus research project
                                        // ALTERNATIVE: use an index to an array(list) of CRPs??
  public File flThis;                   // File that is being treated
  public int QCcurrentLine;             // Current QC line of project being executed
  public XmlNode ndxCurrentForest;      // The sentence element we are now working on
  public String currentSentId;          // ID of the currently treated sentence
  public XmlNode ndxHeader;             // The header object of this file
  public DocumentBuilder oSaxDoc;       // The document-builder used for this CRP-File combination
  public DocumentBuilderFactory oDocFac;// The DOM document-builder used for this CRP-File combination
  public XmlForest objProcType;         // My own copy of the XmlForest processor
  public String currentPeriod;          // Downwards compatibility: current period
  // ================= Local variables =========================================
  private Processor objSaxon;           // The processor (shared among threads)
  private ErrHandle errHandle;          // My own access to the error handler
  // =========== Class initializer =============================================
  public CrpFile(CorpusResearchProject oCrp, File fFile, Processor oProc, JobXq jobCaller) {
    try {
      // Access error handler
      errHandle = oCrp.errHandle;
      // Initialise variables
      this.crpThis = oCrp;
      this.flThis = fFile;
      this.QCcurrentLine = -1;
      this.ndxCurrentForest = null;
      this.currentPeriod = "";
      // Set the processor
      this.objSaxon = oProc;
      // Create a new document builder
      oSaxDoc = objSaxon.newDocumentBuilder();
      oDocFac = DocumentBuilderFactory.newInstance();
      // Create a new xml-processor type
      // Set the XmlForest element correctly
      switch (this.crpThis.getForType()) {
        case PsdxWholeFile:
        case PsdxPerForest:
        case PsdxPerForgrp:
          // Create an XmlForest object
          this.objProcType = new XmlForestPsdxLine(this.crpThis, jobCaller, this.errHandle); 
          // Set the project type correctly
          this.objProcType.setProcType(XmlForest.ForType.PsdxPerForest);
          break;
        case PsdxIndex:
          // Create an XmlForest object
          this.objProcType = new XmlForestPsdxIndex(this.crpThis, jobCaller, this.errHandle); 
          // Set the project type correctly
          this.objProcType.setProcType(XmlForest.ForType.PsdxIndex);
          break;
        case FoliaWholeFile:
        case FoliaPerS:
        case FoliaPerPara:
        case FoliaPerDiv:
        case FoliaIndex:
          // Create an XmlForest object
          this.objProcType = new XmlForestFoliaIndex(this.crpThis, jobCaller, this.errHandle); 
          // Set the project type correctly
          this.objProcType.setProcType(XmlForest.ForType.FoliaIndex);
          break;
        default:
          // Create an XmlForest object
          this.objProcType = new XmlForestPsdxIndex(this.crpThis, jobCaller, this.errHandle);
          // Set the project type correctly
          this.objProcType.setProcType(XmlForest.ForType.PsdxPerForest);
          break;
          
      }
      // Set my unique identifier
      id++;
    } catch (Exception ex) {
      errHandle.DoError("Problem creating a [CrpFile] element", ex, CrpFile.class);
    }
  }
  public long getId() { return this.id; }
}
