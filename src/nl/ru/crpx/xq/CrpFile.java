/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.xq;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.search.JobXq;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.tools.FileIO;
import nl.ru.util.ByRef;
import nl.ru.util.json.JSONObject;
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
  public int idxCurrentForest;          // Line number of [ndxCurrentForest] in index file
  public String currentSentId;          // ID of the currently treated sentence
  public String sTextId;                // The name of the text this is
  public XmlNode ndxHeader;             // The header object of this file
  public XmlNode ndxMdi;                // Pointer to MDI node
  public XmlNode ndxRoot;               // The root element of the XmlDocument
  public List<XmlNode> lstAntSent;      // List of accessed antecedents
  public List<Integer> lstAntSentIdx;   // List of the indices of the accessed antecedents
  public DocumentBuilder oSaxDoc;       // The document-builder used for this CRP-File combination
  public DocumentBuilderFactory oDocFac;// The DOM document-builder used for this CRP-File combination
  public XmlForest objProcType;         // My own copy of the XmlForest processor
  public String currentPeriod;          // Downwards compatibility: current period
  public List<LexDict> lstLexDict;      // One ru:lex() dictionary per QC
  public List<String> lstMessage;       // List of messages
  // ================= Local variables =========================================
  private Processor objSaxon;           // The processor (shared among threads)
  private ErrHandle errHandle;          // My own access to the error handler
  private JSONObject metaInfo;          // Metadata of this file
  // =========== Class initializer =============================================
  public CrpFile(CorpusResearchProject oCrp, File fFile, Processor oProc, JobXq jobCaller) {
    try {
      // Access error handler
      errHandle = oCrp.errHandle;
      // Initialise variables
      this.crpThis = oCrp;
      this.flThis = fFile;
      this.sTextId = FileIO.getFileNameWithoutDirectory(fFile.getName().replace(oCrp.getTextExt(), ""));
      this.QCcurrentLine = -1;
      this.ndxCurrentForest = null;
      this.lstAntSent = new ArrayList<>();    // List of antecedent sentences
      this.lstAntSentIdx = new ArrayList<>(); // List of antecedent sentence indices
      this.currentPeriod = "";
      this.ndxHeader = null;
      this.ndxRoot = null;
      this.lstLexDict = new ArrayList<>();  // A list of LexDict items
      this.lstMessage = new ArrayList<>();
      // Initialize the lexdict items
      for (int i=0;i<oCrp.getListQC().size();i++) {
        this.lstLexDict.add(new LexDict(i+1));
      }
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
  
  /**
   * close -- Nicely close XmlForest handle if it is still open
   */
  public void close() {
    if (this.objProcType != null) {
      this.objProcType.close();
    }
  }
  
  public void setMeta(JSONObject oMeta) {this.metaInfo = new JSONObject(oMeta.toString()); }
  public JSONObject getMeta() {return this.metaInfo; }
  
  /**
   * getLexDictQC
   *    Return the LexDict for the indicated QC
   * 
   * @param iQC
   * @return 
   */
  public LexDict getLexDictQC(int iQC) { 
    for (int i=0;i<this.lstLexDict.size();i++ ) {
      if (lstLexDict.get(i).QC == iQC) return lstLexDict.get(i);
    }
    // Return nothing
    return null;
  }
  
  /**
   * getSentence
   *    Find the sentence inside which [sConstId] is
   *    If this sentence differs from the [ndxCurrentForest], then add
   *      an item to the [lstAntSent] stack
   * 
   * @param ndxSent
   * @param sConstId
   * @return 
   */
  public boolean getSentence(ByRef<XmlNode> ndxSent, String sConstId) {
    int idx = -1;     // line number in index file for [sConstId]
    int iFound = -1;  // Result of looking for the index in the current list
    
    try {
      // Find out which line in the index this would have
      idx = this.objProcType.getIndexFromConst(sConstId);
      
      // Check if we have an entry with this index line
      if (idx == this.idxCurrentForest) {
        // The sentence equals the current one
        ndxSent.argValue = this.ndxCurrentForest;
      } else {
        // Check if we already 'have' this one 
        iFound = this.lstAntSentIdx.indexOf(idx);
        if (iFound < 0) {
          // It is not yet in the list: add it
          this.lstAntSentIdx.add(idx);
          if (this.objProcType.FindForest(ndxSent, sConstId)) {
            this.lstAntSent.add(ndxSent.argValue);
          }
        } else {
          // We have it in the list: get it from there
          ndxSent.argValue = this.lstAntSent.get(iFound);
        }
      }
      // Return positively
      return true;
    } catch (Exception ex) {
      errHandle.DoError("CrpFile cannot get sentence", ex, CrpFile.class);
      return false;
    }
  }
  
}
