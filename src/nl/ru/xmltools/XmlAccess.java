/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

package nl.ru.xmltools;

import java.io.File;
import net.sf.saxon.s9api.Processor;
import nl.ru.crpx.dataobject.DataObject;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.xq.RuBase;
import nl.ru.util.json.JSONObject;

/**
 * XmlAccess
 *    Provide access to XML coded files in a way that hides the actual XML
 *    functions.
 *    This main entry defines the interfaces.
 *    Each inheriting class implements the interfaces for XML codings like:
 *    - psdx
 *    - FoLiA
 *    - Tegra/nig
 *    - alpino ds
 * 
 * @author Erwin R. Komen
 * @history 21/jul/2015 Created
 */
public abstract class XmlAccess { 
  // This class uses a logger
  protected final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XmlForest.class);
  // ========================================== LOCAL VARIABLE ================================================
  protected String sCurrentSentId;    // ID of the currently loaded sentence
  protected XmlDocument pdxDoc;       // Provide facilities to convert a string into an xml document
  protected Processor objSaxon;       // Local access to the processor
  protected XmlNode ndxSent;          // The root node
  protected XmlIndexTgReader objXmlRdr; // Index reader for current file
  protected XmlIndexRaReader objXmlRaRdr;
  protected RuBase objBase;           // Access to RuBase functions
  protected CorpusResearchProject crpThis;
  protected boolean bUseRa = true;  // Use Ra method or Tg?

  // ==========================================================================================================
  // Class instantiation
  public XmlAccess(CorpusResearchProject crpThis, XmlDocument pdxDoc, String sFileName) {
    try {
      // Get access to the XML document handler
      this.pdxDoc = pdxDoc;
      // Get the processor
      this.objSaxon = pdxDoc.getProcessor();
      // Get the CRP
      this.crpThis = crpThis;
      // Get access to RuBase
      this.objBase = new RuBase(crpThis);

      // Get access to the correct xml index reader
      if (objXmlRdr == null || !objXmlRdr.getFileName().equals(sFileName)) {
        File objCurrentFile = new File(sFileName);
        if (bUseRa) {
          this.objXmlRaRdr = new XmlIndexRaReader(objCurrentFile, crpThis, pdxDoc, crpThis.intProjType);
        } else {
          this.objXmlRdr = new XmlIndexTgReader(objCurrentFile, crpThis, pdxDoc, crpThis.intProjType);
        }
      }
      // Initialise some stuff
      ndxSent = null;
      sCurrentSentId = "";
    } catch (Exception ex) {
      logger.error("XmlAccess initialisation error", ex);
    }
  }
  
  // ==========================================================================================================
  // Methods that are overridden by the classes that extend XmlForest:
  public abstract JSONObject getHitLine(String sLngName, String sLocs, String sLocw);
  public abstract DataObject getHitSyntax(String sLngName, String sLocs, String sLocw);
  public abstract JSONObject getHitContext(String sLngName, String sLocs, String sLocw, int iPrecNum, int iFollNum);
  public abstract boolean hasAncestor(XmlNode ndThis, String sType, String sValue);
  public abstract XmlNode getOffsetNode(String sLocs, int iOffset);
  public abstract void close();
  
}
