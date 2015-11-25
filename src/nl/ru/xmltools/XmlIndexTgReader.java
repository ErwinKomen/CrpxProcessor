/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

package nl.ru.xmltools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.project.CorpusResearchProject.ProjType;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.tools.FileIO;
import nl.ru.util.ByRef;
import nl.ru.util.FileUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * XmlIndexTgReader - read 'xml' files by first making an index for them
 *                    method: split all files into tag-based file chunks
 * 
 * @author Erwin R. Komen
 */
public class XmlIndexTgReader {
  // This class uses the other's error handler
  private final ErrHandle errHandle;
  // ======================== variables for internal use
  private int iLines;             // Number of lines in this file
  private int iCurrentLine;       // Most recently read line index into [arIndex]
  private String loc_sIndexDir;   // Name of directory used for index
  private String loc_sTextId;     // Short file name serves as text identifier
  private File loc_fIndexDir;     // Directory for index as a file
  private File loc_fIndexFile;    // Index file (within the directory)
  private File loc_fThis;         // Pointer to the associated file
  private File loc_fHeader;       // Pointer to the header file
  private CorpusResearchProject crpThis;  // Reference to the CRP that is calling me
  private XmlDocument loc_pdxThis;        // Possibility to read and interpret XML chunks
  private List<IndexEl> arIndex;  // The datastructure containing the index   
  private List<String> lstParts;  // List of all Part elements
  // ======================== publicly accessible information
  public boolean EOF;             // Hit the end of file or not
  // ----------------------------------------------------------------------------------------------------------
  // Class : XmlIndexTgReader
  // Goal :  Indexed reader for xml files. Each file is divided into chunks
  //           that contain meaningful elements: (e.g. one <forest>, <s>, <teiHeader> )
  //         Each file gets an index file associated with it, and this file holds:
  //         - the order of the different chunks within the original
  //         - the @id values of each chunk (if applicable)
  //         - the maximum word @id value within that chunk (if applicable)
  //         - the name of the file containing the chunk
  //         The index file is realized as a JSONArray for easy/fast access
  // History:
  // 10/jun/2015  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  public XmlIndexTgReader(File fThis, CorpusResearchProject prjThis, XmlDocument pdxThis, ProjType ptThis) throws FileNotFoundException  {
    try {
      // Take over the CRP
      this.crpThis = prjThis;
      // Set the error handler
      if (this.crpThis == null) {
        // If no CRP has been given, then use my own error handler
        this.errHandle = new ErrHandle(XmlIndexRaReader.class);
      } else {
        // A CRP is given, so use its error handler
        this.errHandle = prjThis.errHandle;
      }
      this.loc_pdxThis = pdxThis;
      this.loc_fThis = fThis;
      this.lstParts = new ArrayList<>();
      // Validate existence of file
      if (!fThis.exists()) throw new FileNotFoundException("XmlIndexReader cannot find file " + fThis.getAbsolutePath());
      // Make sure an up-to-date index exists
      if (!doCheckIndex(fThis, ptThis))  throw new FileNotFoundException("XmlIndexReader cannot create an index " + fThis.getAbsolutePath());
      // Read the index file into a data structure
      if (!readIndex()) throw new FileNotFoundException("XmlIndexReader could not read the index");
      // Get text identifier
      this.loc_sTextId = FileIO.getFileNameWithoutDirectory(fThis.getName().replace(prjThis.getTextExt(ptThis), ""));
      // Initialise numbers
      iLines = arIndex.size();  // Total number of lines in this file
      iCurrentLine = -1;        // No line has been read
      this.EOF = false;
    } catch (Exception ex) {
      // Make sure the exception gets propagated further
      throw new FileNotFoundException("XmlIndexReader exception" + ex.getMessage());
    }
  }
  // ======================== Make the filename available =======================
  public String getFileName() { return (this.loc_fThis == null) ? "" : this.loc_fThis.getName(); }
  public String getTextId() { return this.loc_sTextId; }
  // ======================== Make the list of parts available ==================
  public List<String> getPartList() { return this.lstParts; }
  
  
  /**
   * doCheckIndex -- check if the indicated file has an index
   *                 If this is not the case, then make one for it
   * 
   * @param fThis - file that we want to read
   * @return 
   * @history
   *  14/sep/2015 ERK Database xml files get an index that splits over the 'Part' items
   *  15/sep/2015 ERK A database xml file gets just one .index file for random read access
   *  28/sep/2015 ERK Made it possible to create and access this class 
   *                  *without* having an instance of a CorpusResearchProject
   */
  private boolean doCheckIndex(File fThis, ProjType ptThis) {
    // This call is only possible if there is an instance of a CRP
    if (crpThis == null) return false;
    // We are safe: continue
    return doCheckIndex(fThis, ptThis, crpThis.getTextExt());
  }
  private boolean doCheckIndex(File fThis, ProjType ptThis, String sTextExt) {
    List<IndexFile> lIndices; // List of <IndexFile> elements
    int iIndexLine = 0;       // index line used to make a file name
    int iPos = 0;             // POsition in file
    
    try {
      // ===== DEBUGGING ======
      // errHandle.debug("doCheckIndex Tg: enter");
      // ======================
      // Get the date/time of the file
      long tmFile = fThis.lastModified();
      // Set the directory used for the index
      String strFile = fThis.getAbsolutePath();
      // Initialisations
      lIndices = new ArrayList<>();
      this.lstParts = new ArrayList<>();
      // Do we need to check on the file extension??
      loc_fIndexDir = new File(strFile);
      // Get the file extension that is expected for this file
      int iExt = strFile.lastIndexOf(CorpusResearchProject.getTextExt(ptThis));
      // Validate
      if (iExt==0) throw new FileNotFoundException("XmlIndexReader doesn't find expected extension [" + 
              CorpusResearchProject.getTextExt(ptThis) + "] in " + fThis.getName());
      // Set the directory name correctly
      loc_sIndexDir = (iExt<0) ? strFile : strFile.substring(0, iExt);
      loc_fIndexDir = new File(loc_sIndexDir);
      // Set the name of the index file that should be in there
      loc_fIndexFile = new File(loc_sIndexDir + "/index.csv");
      // Check existence of index dir
      if (!loc_fIndexDir.exists() ) {
        // Create index directory
        loc_fIndexDir.mkdir();
      }
      
      // Check existence/ancienity of the index file w.r.t. the text file
      if (!loc_fIndexFile.exists() || tmFile > loc_fIndexFile.lastModified()) {
        String sHeaderFile = "";  // Name of the header file
        String sHeaderXml = "";   // XML context of the header
        // Create a string builder to hold the index of this file
        StringBuilder sIndexData = new StringBuilder();
        // Create an XmlChunkReader to do this
        XmlChunkReader rdThis = new XmlChunkReader(fThis);
        // ======= DEBUG ===============
        // RandomAccessFile fcDebug = new RandomAccessFile(fThis.getAbsolutePath(), "r");
        // FileChannel fcDebug = FileChannel.open(fThis.toPath(), StandardOpenOption.READ );
        // =============================
        // First read the header
        String sTagHeader = CorpusResearchProject.getTagHeader(ptThis);
        iPos = 0;
        if (!sTagHeader.isEmpty()) {
          
          // Since the header tag is defined, it is obligatory
          if (! (rdThis.ReadToFollowing(sTagHeader))) {
            errHandle.DoError("FirstForest error: cannot find <" + sTagHeader + 
                    "> in file [" + strFile + "]");
            return false;
          }
          iPos = rdThis.getPos();
          // Get the header data
          sHeaderXml = rdThis.ReadOuterXml();
          // Write the data to the header file
          sHeaderFile = loc_sIndexDir + "/header.xml";
          File loc_fHeader = new File(sHeaderFile);
          FileUtil.writeFile(loc_fHeader, sHeaderXml); 
        }
        // Copy and create chunks for each line
        String sTagLine = CorpusResearchProject.getTagLine(ptThis);
        QName qAttrLineId = CorpusResearchProject.getAttrLineId(ptThis);
        QName qAttrConstId = CorpusResearchProject.getAttrConstId(ptThis);
        QName qAttrPartId = CorpusResearchProject.getAttrPartId(ptThis);    // Added 14/sep/15
        String sPathLine = CorpusResearchProject.getNodeLine(ptThis);
        String sLastNode = CorpusResearchProject.getNodeLast(ptThis);
        while (!rdThis.EOF ) {
          // Read to the following start-of-line
          rdThis.ReadToFollowing(sTagLine);
          if (!rdThis.EOF) {
            String sPartId = "";
            // Note the position
            iPos = rdThis.getPos();
            // Read the line into a string
            String strNext = rdThis.ReadOuterXml();
            // Load the line as an XmlDocument, so we can look for attributes
            loc_pdxThis.LoadXml(strNext);
            XmlNode  ndxWork = loc_pdxThis.SelectSingleNode(sPathLine);
            // Get the @id (or @forestId) attribute as string
            String sLineId = (ndxWork == null) ? "-" : ndxWork.getAttributeValue(qAttrLineId);
            // Get the last constituent node
            ndxWork = loc_pdxThis.SelectSingleNode(sLastNode);
            String sLastId = (ndxWork == null) ? "-" : ndxWork.getAttributeValue(qAttrConstId);
            // Need to get the part?
            if (!qAttrPartId.toString().isEmpty()) {
              sPartId = (ndxWork == null) ? "" : ndxWork.getAttributeValue(qAttrPartId);
            }
            // Save the chunk using our own separate index numbering
            iIndexLine++;
            // Treatment is different if the PART is specified
            if (sPartId.isEmpty()) {
              if (iIndexLine==1) {
                // Add a line to this file's index as to where the header is
                sIndexData.append(sHeaderFile).append("\n");
              }
              // Traditional treatment: file name becomes directory with index
              String sLineFile = loc_sIndexDir + "/" + iIndexLine + ".xml";
              FileUtil.writeFile(new File(sLineFile), strNext);
              // Add information to Index Data
              sIndexData.append(sLineId).append("\t");
              sIndexData.append(sLineFile).append("\t");            
              sIndexData.append(sLastId).append("\n");
            } else {
              // Alternative: file name = directory
              //              each part = directory within the above directory
              String sPartDir = loc_sIndexDir + "/" + sPartId;
              // Check the extension
              if (sPartDir.endsWith(sTextExt)) {
                // Remove the text extension
                sPartDir = sPartDir.substring(0, sPartDir.length() - sTextExt.length());
              }
              // Create a part directory
              File fPartDir = new File(sPartDir);
              if (!fPartDir.exists()) fPartDir.mkdir();
              String sLineFile = sPartDir + "/" + iIndexLine + ".xml";
              FileUtil.writeFile(new File(sLineFile), strNext);
              boolean bIsNewIndex = (!hasIndexFile(lIndices, sPartId));
              // Get the correct IndexFile element
              int iIndexFile = getIndexFile(lIndices, sPartId, sPartDir);
              if (iIndexFile < 0) return false;
              IndexFile oIndex = lIndices.get(iIndexFile);
              // Get the stringbuilder for this index
              StringBuilder sb = oIndex.sb;
              // Actions when this is a NEW index
              if (bIsNewIndex) {
                // Add information to 'global' Index Data
                sIndexData.append(sLineId).append("\t");
                sIndexData.append(sPartDir).append("\t");            
                sIndexData.append(sLastId).append("\t");
                sIndexData.append(sPartId).append("\n");
                // Write the header to the header file
                sHeaderFile = oIndex.Dir + "/header.xml";
                FileUtil.writeFile(new File(sHeaderFile), sHeaderXml); 
                // Add a line to this file's index as to where the header is
                sb.append(sHeaderFile).append("\n");
                // Add the part element to the list of parts
                this.lstParts.add(sPartId);
              }
              // Add information to Index Data
              sb.append(sLineId).append("\t");
              sb.append(sLineFile).append("\t");            
              sb.append(sLastId).append("\n");
            }
          }
        }
        // Write the index file to its position
        FileUtil.writeFile(loc_fIndexFile, sIndexData.toString());
        // Write any index file in the list to its position
        for (int i=0;i< lIndices.size(); i++) {
          IndexFile oIndex = lIndices.get(i);
          String sIndexFile = oIndex.Dir + "/index.csv";
          FileUtil.writeFile(new File(sIndexFile),oIndex.sb.toString());
          // The header file should also be written 
        }
      }
      // ===== DEBUGGING ======
      // errHandle.debug("doCheckIndex: exit");
      // ======================

      // Return positively
      return true;
    } catch (Exception ex) {
      errHandle.DoError("DoCheckingIndex could not be completed for " + fThis.getAbsolutePath(), ex, XmlIndexTgReader.class);
      // Return failure
      return false;
    }
  }
  

  /**
   * getIndexFile -- get the "IndexFile" element in the specified list
   *                 that refers to the indicated part
   * 
   * @param lContainer
   * @param sPartId
   * @return 
   */
  private int getIndexFile(List<IndexFile> lContainer, String sPartId, String sPartDir) {
    try {
      for (int i=0;i<lContainer.size();i++ ) {
        if (lContainer.get(i).Name.equals(sPartId)) return i;
      }
      // The Part is not yet in the list: add it
      lContainer.add(new IndexFile(sPartId, sPartDir));
      // Return the last index
      return lContainer.size()-1;
    } catch (Exception ex) {
      errHandle.DoError("getIndexFile error", ex);
      // Return failure
      return -1;
    }
  }
  /**
   * hasIndexFile -- check existence of the "IndexFile" element in the specified list
   *                 that refers to the indicated part
   * 
   * @param lContainer
   * @param sPartId
   * @return 
   */
  private boolean hasIndexFile(List<IndexFile> lContainer, String sPartId) {
    try {
      for (int i=0;i<lContainer.size();i++ ) {
        if (lContainer.get(i).Name.equals(sPartId)) return true;
      }
      // The [sPartId] is not in the index
      return false;
    } catch (Exception ex) {
      errHandle.DoError("hasIndexFile error", ex);
      // Return failure
      return false;
    }
  }
  
  /**
   * readIndex -- Load the index file into a datastructure
   *              And if Part is specified, read lstPart
   * 
   * @return 
   */
  private boolean readIndex() {
    try {
      // Initialize the index datastructure
      arIndex = new ArrayList<>();
      BufferedReader br = new BufferedReader(new FileReader( this.loc_fIndexFile));
      // Get the header line
      // Get the first line
      String sHeaderFile = br.readLine();
      if (sHeaderFile.isEmpty()) return false;
      this.loc_fHeader = new File(sHeaderFile);
      // Get the remaining lines
      String line;
      while ((line = br.readLine()) != null) {
        String[] lineArray = line.split("\t");
        // Double check
        if (lineArray.length<3) {
          // Stop and review what is going on
          int iStop = 0;
        }
        // Action depends on lines being 3 or 4 columns
        if (lineArray.length == 3)
          arIndex.add(new IndexEl(lineArray[0], lineArray[1], lineArray[2], ""));
        else {
          arIndex.add(new IndexEl(lineArray[0], lineArray[1], lineArray[2], lineArray[3]));
          // Part is specified, so add it into the list
          this.lstParts.add(lineArray[3]);
        }
      }
      // Return positively
      return true;
    } catch (Exception ex) {
      errHandle.DoError("readIndex encountered a problem for " + loc_fThis.getAbsolutePath(), ex, XmlIndexTgReader.class);
      // Return failure
      return false;
    }
  }
  
  /**
   * getHeader -- return the header of this text file 
   *              If there is no header, then return null.
   * 
   * @return String with the header
   */
  public String getHeader() {
    try {
      // Validate: does the file exist?
      if (loc_fIndexFile == null || !loc_fIndexFile.exists() || arIndex==null || loc_fHeader == null || !loc_fHeader.exists()) {
        errHandle.DoError("Cannot read header for file - " + loc_fThis.getAbsolutePath());
        return "";
      }
      // Load the header and return it
      return (new FileUtil()).readFile(loc_fHeader, "utf-8");
    } catch (Exception ex) {
      errHandle.DoError("Could not read header " + loc_fThis.getAbsolutePath(), ex, XmlIndexTgReader.class);
      // Return failure
      return "";
    }
  }
  
  /**
   * getFirstLine - Read the first line from the index
   * 
   * @return String with the first line
   */
  public String getFirstLine() {
    try {
      // Validate: does the file exist?
      if (loc_fIndexFile == null || !loc_fIndexFile.exists() || arIndex==null || loc_fHeader == null || !loc_fHeader.exists()) {
        errHandle.DoError("Cannot read header for file - " + loc_fThis.getAbsolutePath());
        return "";
      }
      iCurrentLine = 0;
      return (new FileUtil()).readFile(new File(arIndex.get(iCurrentLine).sFile), "utf-8");      
    } catch (Exception ex) {
      errHandle.DoError("Could not read first line of " + loc_fThis.getAbsolutePath(), ex, XmlIndexTgReader.class);
      // Return failure
      return "";
    }
  }

  /**
   * getNextLine - read the next line in the file
   * 
   * @return String containing the next line
   */
  public String getNextLine() {
    try {
      // Validate: does the file exist?
      if (loc_fIndexFile == null || !loc_fIndexFile.exists() || arIndex==null || loc_fHeader == null || !loc_fHeader.exists()) {
        errHandle.DoError("Cannot read next line for file - " + loc_fThis.getAbsolutePath());
        return "";
      }
      iCurrentLine++;
      // Validate
      if (iCurrentLine >= arIndex.size()) {
        this.EOF = true;
        return "";
      }
      // Yes, we should be able to read it...
      return (new FileUtil()).readFile(new File(arIndex.get(iCurrentLine).sFile), "utf-8");      
    } catch (Exception ex) {
      errHandle.DoError("Could not read next line [" + iCurrentLine + "] of " + loc_fThis.getAbsolutePath(), ex, XmlIndexTgReader.class);
      // Return failure
      return "";
    }
  }
 
  /**
   * getRelativeLine - read another line, relative to the currently loaded one
   * 
   * @param iOffset positive or negative offset 
   * @param sSentId returns the @id of the sentence returned
   * @return String containing the line
   */
  public String getRelativeLine(int iOffset, ByRef<String> sSentId) {
    int iNewLine = 0;
    try {
      // Validate: does the file exist?
      if (loc_fIndexFile == null || !loc_fIndexFile.exists() || arIndex==null || loc_fHeader == null || !loc_fHeader.exists()) {
        errHandle.DoError("Cannot read 'relative' line for file - " + loc_fThis.getAbsolutePath());
        return "";
      }
      // Implement the offset
      iNewLine = iCurrentLine + iOffset;
      // Validate
      if (iNewLine >= arIndex.size()) {
        this.EOF = true;
        return "";
      } else if (iNewLine < 0) return "";
      // Okay this becomes the new line
      iCurrentLine = iNewLine;
      // Yes, we should be able to read it...
      IndexEl elThis = arIndex.get(iCurrentLine);
      sSentId.argValue = elThis.LineId;
      return (new FileUtil()).readFile(new File(elThis.sFile), "utf-8");    
    } catch (Exception ex) {
      errHandle.DoError("Could not read 'relative' line [" + iNewLine + "] of " + loc_fThis.getAbsolutePath(), ex, XmlIndexTgReader.class);
      // Return failure
      return "";
    }
  }
  
  /**
   * getOneLine - get the line with the indicated line @id value
   * 
   * @param sLineId
   * @return String containing the line sought for
   */
  public String getOneLine(String sLineId) {
    try {
      // Validate: does the file exist?
      if (loc_fIndexFile == null || !loc_fIndexFile.exists() || arIndex==null || loc_fHeader == null || !loc_fHeader.exists()) {
        errHandle.DoError("Cannot read line for file - " + loc_fThis.getAbsolutePath());
        return "";
      }
      // Find out which line should be read
      // TODO: use a better (bisection) algorithm for this in the future
      for (int i=0;i<arIndex.size();i++ ) {
        IndexEl objThis = arIndex.get(i);
        if (objThis.LineId.equals(sLineId)) {
          // Make sure we note what the current line is
          iCurrentLine = i;
          // Get this line and return it
          return (new FileUtil()).readFile(new File(objThis.sFile), "utf-8");
        }
      }
      return "";      
    } catch (Exception ex) {
      errHandle.DoError("Could not read line [" + sLineId + "] of " 
              + loc_fThis.getAbsolutePath(), ex, XmlIndexTgReader.class);
      // Return failure
      return "";
    }
  }
  
  /**
   * getOneConstLine - get the line in which the indicated constituent id is
   * 
   * @param sConstId
   * @return String containing the line sought for
   */
  public String getOneConstLine(String sConstId) {
    try {
      // Validate: does the file exist?
      if (loc_fIndexFile == null || !loc_fIndexFile.exists() || arIndex==null || loc_fHeader == null || !loc_fHeader.exists()) {
        errHandle.DoError("Cannot read line for file - " + loc_fThis.getAbsolutePath());
        return "";
      }
      // Find out which line should be read
      // TODO: use a better (bisection) algorithm for this in the future
      for (int i=arIndex.size()-1;i>=0;i-- ) {
        IndexEl objThis = arIndex.get(i);
        // Check if the constituent id we are looking for is *smaller* than the 
        //   last constituent id of this line
        if (!objThis.LastId.equals("-") && sConstId.compareTo(objThis.LastId)<=0) {
          // Get this line and return it
          return (new FileUtil()).readFile(new File(objThis.sFile));
        }
      }
      return "";      
    } catch (Exception ex) {
      errHandle.DoError("Could not read line with constituent [" + sConstId + 
              "] of " + loc_fThis.getAbsolutePath(), ex, XmlIndexTgReader.class);
      // Return failure
      return "";
    }
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  getLinesRead
  // Goal :  Retrieve the number of lines read so far
  // History:
  // 24/apr/2015  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  public int getLinesRead() { return this.iCurrentLine;}
  public int getPtc() {return (int) ( this.iCurrentLine * 100 / this.iLines); }
}
// Define one index element
class IndexEl {
  public String LineId;   // the id of the line this represents
  public String sFile;    // the full name of the file where this element is stored
  public String LastId;   // The id of the last constituent
  public String PartId;   // Part identifier (optional; for dbase: @File)
  public IndexEl(String sLineId, String sFileName, String sLastId, String sPartId) {
    this.LineId = sLineId; this.sFile = sFileName; this.LastId = sLastId; this.PartId = sPartId;
  }
}