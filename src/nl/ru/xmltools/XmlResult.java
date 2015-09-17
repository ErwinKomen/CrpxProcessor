/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */
package nl.ru.xmltools;

import java.util.ArrayList;
import java.util.List;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.search.JobXq;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.util.ByRef;

/**
 * XmlResult - Reader through xml results
 * 
 * @author Erwin R. Komen
 */
public abstract class XmlResult {
  // ========================================== Constants ======================
  protected static final QName loc_xq_ResId = new QName("", "", "ResId");
  protected static final String loc_path_Result = "./descendant-or-self::Result[1]";
  protected static final String loc_path_General = "./descendant-or-self::General[1]";
  // ========================================== LOCAL VARIABLE ================================================
  protected String loc_strCurrent = "";     // XML code of current forest
  protected String loc_strPart = "";        // Part to which this one pertains (or empty)
  protected int loc_intCurrent;             // Position of current node within [loc_arContext]
  protected XmlDocument loc_pdxThis;        // Current one
  protected XmlNode loc_ndxHeader;          // My copy of the header node
  protected ErrHandle objErr;               // Local access to the general object with global variables
  protected JobXq objJob;                   // Access to the job that is being executed
  protected CorpusResearchProject crpThis;  // The corpus research project for which I am created
  protected Parse objParse;                 // Object to use my own version of the "GetSeg()" function
  protected Processor objSaxon;             // Local access to the processor
  protected DocumentBuilder objSaxDoc;      // My own document-builder
  protected List<String> lstResFile;        // List of @File elements within the <Result> records
  // ==========================================================================================================
  // Class instantiation
  public XmlResult(CorpusResearchProject oCrp, JobXq oJob, ErrHandle oErr) {
    try {
      // Set the correct error handler
      this.objErr = oCrp.errHandle;
      // Get the processor
      this.objSaxon = oCrp.getSaxProc();
      // Create a document builder
      this.objSaxDoc = this.objSaxon.newDocumentBuilder();
      // Set a new XML document
      loc_pdxThis = new XmlDocument(this.objSaxDoc, this.objSaxon);
      // Other initialisations
      objErr = oErr;
      crpThis = oCrp;
      objParse = new Parse(oCrp, oErr);
      this.objJob = oJob;
      lstResFile = new ArrayList<>();
    } catch (Exception ex) {
      objErr.DoError("XmlResult initialisation error", ex);
    }
  }  
  // Methods that are overridden by the classes that extend XmlResult:
  public abstract List<String> getResultFileList();                                           // List of @File attributes inside Result database
  public abstract boolean Prepare(String strDbaseFile);                                       // Prepare the database file for reading
  public abstract boolean Prepare(String strDbaseFile, String strPart);                       // Prepare the database file for reading
  public abstract boolean FirstResult(ByRef<XmlNode> ndxResult, ByRef<XmlNode> ndxHeader, String strFile);  // Get first <Result>
  public abstract boolean GetResultId(ByRef<XmlNode> ndxResult, ByRef<Integer> intResultId);  // Get the ResId of ndxResult
  public abstract boolean CurrentResult(ByRef<XmlNode> ndxResult);                            // Get current <Result>
  public abstract boolean FirstResult(ByRef<XmlNode> ndxResult);                              // Get first <Result>
  public abstract boolean NextResult(ByRef<XmlNode> ndxResult);                               // Get next <Result>
  public abstract boolean OneResult(ByRef<XmlNode> ndxResult, String sResultId);              // Get <Result> with indicated id
  public abstract boolean GetHeader(ByRef<XmlNode> ndxHeader);                                // Get the <General> header of the database
  public abstract boolean IsEnd();                                                            // Are we at the end of the file?
  public abstract boolean Percentage(ByRef<Integer> intPtc);                                  // Return percentage where we are
}
