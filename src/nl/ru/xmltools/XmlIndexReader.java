/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import net.sf.saxon.s9api.QName;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.util.FileUtil;

/**
 * XmlIndexReader - read 'xml' files by first making an index for them
 * 
 * @author Erwin R. Komen
 */
public class XmlIndexReader {
  // This class uses the other's error handler
  private final ErrHandle errHandle;
  // ======================== variables for internal use
  private int iLines;             // Number of lines in this file
  private int iCurrentLine;       // Most recently read line index into [arIndex]
  private String loc_sIndexDir;   // Name of directory used for index
  private File loc_fIndexDir;     // Directory for index as a file
  private File loc_fIndexFile;    // Index file (within the directory)
  private File loc_fThis;         // Pointer to the associated file
  private File loc_fHeader;       // Pointer to the header file
  private CorpusResearchProject crpThis;  // Reference to the CRP that is calling me
  private XmlDocument loc_pdxThis;        // Possibility to read and interpret XML chunks
  private List<IndexEl> arIndex;  // The datastructure containing the index          
  // ======================== publicly accessible information
  public boolean EOF;             // Hit the end of file or not
  // ----------------------------------------------------------------------------------------------------------
  // Class : XmlIndexReader
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
  public XmlIndexReader(File fThis, CorpusResearchProject prjThis, XmlDocument pdxThis) throws FileNotFoundException  {
    try {
      // Set the error handler
      this.errHandle = prjThis.errHandle;
      this.crpThis = prjThis;
      this.loc_pdxThis = pdxThis;
      this.loc_fThis = fThis;
      // Validate existence of file
      if (!fThis.exists()) throw new FileNotFoundException("XmlIndexReader cannot find file " + fThis.getAbsolutePath());
      // Make sure an up-to-date index exists
      if (!doCheckIndex(fThis))  throw new FileNotFoundException("XmlIndexReader cannot create an index " + fThis.getAbsolutePath());
      // Read the index file into a data structure
      if (!readIndex()) throw new FileNotFoundException("XmlIndexReader could not read the index");
      // Initialise numbers
      iLines = arIndex.size();  // Total number of lines in this file
      iCurrentLine = -1;        // No line has been read
      this.EOF = false;
    } catch (Exception ex) {
      // Make sure the exception gets propagated further
      throw new FileNotFoundException("XmlIndexReader exception" + ex.getMessage());
    }
  }
  
  /**
   * doCheckIndex -- check if the indicated file has an index
   *                 If this is not the case, then make one for it
   * 
   * @param fThis - file that we want to read
   * @return 
   */
  private boolean doCheckIndex(File fThis) {
    int iIndexLine = 0;   // index line used to make a file name
    
    try {
      // Get the date/time of the file
      long tmFile = fThis.lastModified();
      // Set the directory used for the index
      String strFile = fThis.getAbsolutePath();
      // Get the file extension that is expected for this file
      int iExt = strFile.lastIndexOf(crpThis.getTextExt());
      // Validate
      if (iExt==0) throw new FileNotFoundException("XmlIndexReader doesn't find expected extension [" + 
              crpThis.getTextExt() + "] in " + fThis.getName());
      // Set the directory name correctly
      loc_sIndexDir = strFile.substring(0, iExt);
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
        // Create a string builder to hold the index of this file
        StringBuilder sIndexData = new StringBuilder();
        // Create an XmlChunkReader to do this
        XmlChunkReader rdThis = new XmlChunkReader(fThis);
        // First read the header
        String sTagHeader = crpThis.getTagHeader();
        if (!sTagHeader.isEmpty()) {
          // Since the header tag is defined, it is obligatory
          if (! (rdThis.ReadToFollowing(sTagHeader))) {
            errHandle.DoError("FirstForest error: cannot find <" + sTagHeader + 
                    "> in file [" + strFile + "]");
            return false;
          }
          // Write the obligatory teiHeader to the header file
          sHeaderFile = loc_sIndexDir + "/header.xml";
          File loc_fHeader = new File(sHeaderFile);
          FileUtil.writeFile(loc_fHeader, rdThis.ReadOuterXml()); 
        }
        // Add header file name to IndexData (this is a separate starting line)
        sIndexData.append(sHeaderFile).append("\n");
        // Copy and create chunks for each line
        String sTagLine = crpThis.getTagLine();
        QName qAttrLineId = crpThis.getAttrLineId();
        QName qAttrConstId = crpThis.getAttrConstId();
        String sPathLine = crpThis.getNodeLine();
        String sLastNode = crpThis.getNodeLast();
        while (!rdThis.EOF ) {
          // Read to the following start-of-line
          rdThis.ReadToFollowing(sTagLine);
          if (!rdThis.EOF) {
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
            // Save the chunk using our own separate index numbering
            iIndexLine++;
            String sLineFile = loc_sIndexDir + "/" + iIndexLine + ".xml";
            FileUtil.writeFile(new File(sLineFile), strNext);
            // Add information to Index Data
            sIndexData.append(sLineId).append("\t");
            sIndexData.append(sLineFile).append("\t");
            sIndexData.append(sLastId).append("\n");
          }
        }
        // Write the index file to its position
        FileUtil.writeFile(loc_fIndexFile, sIndexData.toString());
      }

      // Return positively
      return true;
    } catch (Exception ex) {
      errHandle.DoError("DoCheckingIndex could not be completed for " + fThis.getAbsolutePath(), ex, XmlIndexReader.class);
      // Return failure
      return false;
    }
  }
  
  /**
   * readIndex -- Load the index file into a datastructure
   * 
   * @return 
   */
  public boolean readIndex() {
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
        arIndex.add(new IndexEl(lineArray[0], lineArray[1], lineArray[2]));
      }
      // Return positively
      return true;
    } catch (Exception ex) {
      errHandle.DoError("readIndex encountered a problem for " + loc_fThis.getAbsolutePath(), ex, XmlIndexReader.class);
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
      return FileUtil.readFile(loc_fHeader);
    } catch (Exception ex) {
      errHandle.DoError("Could not read header " + loc_fThis.getAbsolutePath(), ex, XmlIndexReader.class);
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
      return FileUtil.readFile(new File(arIndex.get(iCurrentLine).sFile));      
    } catch (Exception ex) {
      errHandle.DoError("Could not read first line of " + loc_fThis.getAbsolutePath(), ex, XmlIndexReader.class);
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
      return FileUtil.readFile(new File(arIndex.get(iCurrentLine).sFile));      
    } catch (Exception ex) {
      errHandle.DoError("Could not read next line [" + iCurrentLine + "] of " + loc_fThis.getAbsolutePath(), ex, XmlIndexReader.class);
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
          // Get this line and return it
          return FileUtil.readFile(new File(objThis.sFile));
        }
      }
      return "";      
    } catch (Exception ex) {
      errHandle.DoError("Could not read line [" + sLineId + "] of " 
              + loc_fThis.getAbsolutePath(), ex, XmlIndexReader.class);
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
          return FileUtil.readFile(new File(objThis.sFile));
        }
      }
      return "";      
    } catch (Exception ex) {
      errHandle.DoError("Could not read line with constituent [" + sConstId + 
              "] of " + loc_fThis.getAbsolutePath(), ex, XmlIndexReader.class);
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
  public int getPtc() {return (int) ( (this.iCurrentLine / this.iLines) * 100); }
}
// Define one index element
class IndexEl {
  public String LineId;   // the id of the line this represents
  public String sFile;    // the full name of the file where this element is stored
  public String LastId;   // The id of the last constituent
  public IndexEl(String sLineId, String sFileName, String sLastId) {
    this.LineId = sLineId; this.sFile = sFileName; this.LastId = sLastId;
  }
}
