/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

package nl.ru.xmltools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.tools.FileIO;
import nl.ru.util.FileUtil;
import nl.ru.util.StringUtil;

/**
 *
 * @author Erwin R. Komen
 */
public class MdiReader {
  // ================== local constants ====================
  // List of keys
  private final String[] arKey = {"root_dir", "xmldir", "pri_dir", "skp_dir", "tag_dir", "audio_dir",
    "syn_dir", "tig_dir", "bpt_dir", "lxk_dir", "prx_dir_1", "prx_dir_2",
    "prx_dir_3", "prx_dir_4", "xmlsessiondir", "xmlsessionsdir",
    "audioroot_dir", "wrd_dir", "release_dir", "dvdname"};  
  // List of values
  private final String[] arValue = {"../../../annot/xml", ".", "/pri/", "/../corex/skp/", "/tag/", "$AUDIOROOT/",
    "/syn/", "/tig/", "/bpt-fon/", "/lxk/", "/prx1/", "/prx2/",
    "/prx3/", "/prx4/", "../sessions", "../sessions",
    "", "/wrd/", "", ""};               
  // ================== local variables ====================
  private CorpusResearchProject crpThis;
  private Processor objSaxon;             // Local access to the processor
  private DocumentBuilder objSaxDoc;      // My own document-builder
  private XmlDocument pdxBack;            // Entry to document
  private ErrHandle errHandle;            // Error handler linking to my caller
  //private java.util.HashMap<String, String> dictEntity;
  
  // ================== Class initialisation ===============
  /**
   * MdiReader -- class initializer
   * 
   * @param oErr
   * @param pdxMdi
   */
  public MdiReader(ErrHandle oErr, XmlDocument pdxMdi) {
    // Pick up error handler
    errHandle = oErr;
    // Get the processor
    //this.objSaxon = oCrp.getSaxProc();
    // Create a document builder
    //this.objSaxDoc = this.objSaxon.newDocumentBuilder();
    // Create an XmlDocument object
    //pdxBack = new XmlDocument(objSaxDoc, objSaxon);
    pdxBack = pdxMdi;
  }

  /**
   * getMdi - return the correct filename for a .imdi or .cmdi file
   *    
   * @param strFile
   * @return 
   */
  public String getMdi(String strFile) {
    String strFileMdi = "";
    
    try {
      // Get the *start* of the name for the .imdi or .cmdi file
      if (strFile.endsWith(".folia.xml")) {
        strFileMdi = strFile.replace(".folia.xml", "");
      } else if (strFile.endsWith(".folia.xml.gz")) {
        strFileMdi = strFile.replace(".folia.xml.gz", "");
      } else if (strFile.endsWith(".psdx.gz")) {
        strFileMdi = strFile.replace(".psdx.gz", "");
      } else {
        strFileMdi = FileIO.getDirectory(strFile) + "/" + FileIO.getFileNameWithoutExtension(strFile);
      }
      //  Check if .imdi or .cmdi exist
      File fMdi = new File(strFileMdi + ".imdi");
      if (fMdi.exists()) {
        strFileMdi += ".imdi";
      } else {
        fMdi = new File(strFileMdi + ".cmdi.xml");
        if (fMdi.exists()) 
          strFileMdi += ".cmdi.xml";
        else {
          strFileMdi = strFileMdi.toLowerCase();
          fMdi = new File(strFileMdi + ".cmdi.xml");
          if (fMdi.exists()) 
            strFileMdi += ".cmdi.xml";
          else
            strFileMdi = "";
        }
      }
      // Return the filename we created
      return strFileMdi;
    } catch (Exception ex) {
      errHandle.DoError("getMdi problem", ex);
      return "";
    }
  }

  /**
   * Read - Convert the MDI file into an XmlDocument object
   * 
   * @param strFile
   * @return 
   */
  public XmlDocument Read(String strFile) {
    String strDocT = "<!DOCTYPE ";
    String strXMlNs = "xmlns=";
    FileUtil fuThis = new FileUtil();

    try {
      //  Validate
      File fThis = new File(strFile);
      if (!fThis.exists()) return null;

      // Read the whole file
      List<String> lText = fuThis.readLines(fThis);
      //  Look for line with doctype
      for (int i=0;i<lText.size();i++) {
        if (lText.get(i).contains(strDocT)) {
          // Split line
          String[] arPart = lText.get(i).split(" ");
          // Recombine the first two parts
          if (arPart.length > 1) { 
            String sCombi = arPart[0]+" "+arPart[1]+">";
            lText.set(i, sCombi);
            // Exit the for-loop
            break;
          } 
        }
      }

      //  Look for line with METATRANSCRIPT
      for (int i=0;i<lText.size();i++) {
        if (lText.get(i).contains(strXMlNs)) {
          // Split line
          String[] arPart = lText.get(i).split(" ");
          // Walk through the parts, building a new array that only contains the good stuff
          List<String> lPart = new ArrayList<>();
          for (String sPart : arPart) {
            if (!sPart.contains(strXMlNs)) {
              lPart.add(sPart);
            }
          }
          // Recombine into a string
          String sCombined = StringUtil.join(lPart, " ");
          // Possibly add a closing > sign
          if (!sCombined.endsWith(">")) {
            sCombined += ">";
          }
          // Change line into just the beginning tag without the XMLNS stuff
          // OLD lText.set(i, arPart[0]+">");
          lText.set(i, sCombined);
        }
      }

      //  Read file into memory
      String strText = StringUtil.join(lText, "\n"); // Join(arText, "\n");
      //  Expand keys
      for (int i=0;i<arKey.length;i++) {
        String sFrom = "&" + arKey[i]+";";
        strText = strText.replace(sFrom, arValue[i]);
      }

      //  Load the document into XmlDocument
      pdxBack.LoadXml(strText);
      //  Return the document root
      return pdxBack;
    } catch (Exception ex) {
      //  Warn user
      errHandle.DoError("MdiReader/Read error: ", ex); 
      //  Return failure
      return null;
    }

  }
}