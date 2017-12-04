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
import net.sf.saxon.s9api.QName;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.project.CorpusResearchProject.ProjType;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.tools.FileIO;
import nl.ru.util.ByRef;
import nl.ru.util.FileUtil;

/**
 * XmlIndexRaReader - read 'xml' files by first making an index for them
 *                     use the "random-access-file" method
 * 
 * @author Erwin R. Komen
 */
public class XmlIndexRaReader {
  // This class uses the other's error handler
  private final ErrHandle errHandle;
  // ======================== variables for internal use
  private int iLines;                     // Number of lines in this file
  private int iCurrentLine;               // Most recently read line index into [arIndex]
  private String loc_sIndexBase;          // Name of directory used for index
  private File loc_fIndexDir;             // Directory for index as a file
  private File loc_fIndexFile;            // Index file (within the directory)
  private File loc_fThis;                 // Pointer to the associated file
  private File loc_fHeader;               // Pointer to the header file
  private String loc_sTextId;             // Short file name serves as text identifier
  private RandomAccessFile loc_fRa;       // Random access to the file this index belongs to
  private CorpusResearchProject crpThis;  // Reference to the CRP that is calling me
  private XmlDocument loc_pdxThis;        // Possibility to read and interpret XML chunks
  private List<XmlIndexItem> arIndex;     // The datastructure containing the index   
  private List<String> lstParts;          // List of all Part elements
  private List<Integer> lstPartFirstIdx;  // List of first indices of Part elements
  private List<Integer> lstPartLastIdx;   // List of last indices of Part elements
  // ======================== publicly accessible information
  public boolean EOF;                     // Hit the end of file or not
  // ----------------------------------------------------------------------------------------------------------
  // Class : XmlIndexRaReader
  // Goal :  Indexed reader for xml files. 
  //         Each file is divided into chunks
  //           that contain meaningful elements: (e.g. one <forest>, <s>, <teiHeader> )
  //         Each file gets ONE index file (.index) associated with it
  // History:
  // 17/sep/2015  ERK Derived from XmlIndexReader
  // ----------------------------------------------------------------------------------------------------------
  public XmlIndexRaReader(File fThis, CorpusResearchProject prjThis, XmlDocument pdxThis, ProjType ptThis) throws FileNotFoundException  {
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
      if (!fThis.exists()) throw new FileNotFoundException("XmlIndexRaReader cannot find file " + fThis.getAbsolutePath());
      // Make sure an up-to-date index exists
      if (!doCheckIndex(fThis, ptThis))  throw new FileNotFoundException("XmlIndexRaReader cannot create an index " + fThis.getAbsolutePath());
      // Read the index file into a data structure
      if (!readIndex()) throw new FileNotFoundException("XmlIndexRaReader could not read the index");
      // Get text identifier
      this.loc_sTextId = FileIO.getFileNameWithoutDirectory(fThis.getName().replace(prjThis.getTextExt(ptThis), ""));
      // Initialise numbers
      iLines = arIndex.size()-1;  // Total number of lines in this file minus 1 for the <General> part
      iCurrentLine = -1;        // No line has been read
      this.EOF = false;
    } catch (Exception ex) {
      // Make sure the exception gets propagated further
      throw new FileNotFoundException("XmlIndexRaReader exception" + ex.getMessage());
    }
  }
  // ======================== Make the filename available =======================
  public String getFileName() { return (this.loc_fThis == null) ? "" : this.loc_fThis.getName(); }
  public String getTextId() { return this.loc_sTextId; }
  public int size() { return this.iLines; }
  // ======================== Make the list of parts available ==================
  public List<String> getPartList() { return this.lstParts; }
  
  
  /**
   * doCheckIndex -- check if the indicated file has an index
   *                 If this is not the case, then make one for it
   * 
   * @param fThis - file that we want to read
   * @return 
   * @history
   *  15/sep/2015 ERK A database xml file gets just one .index file for random read access
   */
  private boolean doCheckIndex(File fThis, ProjType ptThis) {
    List<IndexFile> lIndices; // List of <IndexFile> elements
    int iIndexLine = 0;       // index line used to make a file name
    int iPos = 0;             // POsition in file
    
    try {
      // ===== DEBUGGING ======
      // errHandle.debug("doCheckIndex: RA enter");
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
      if (iExt==0) throw new FileNotFoundException("XmlIndexRaReader doesn't find expected extension [" + 
              CorpusResearchProject.getTextExt(ptThis) + "] in " + fThis.getName());
      // Set the base of the index correctly: file without extension
      loc_sIndexBase = (iExt<0) ? strFile : strFile.substring(0, iExt);
      // Look for the .index file
      loc_fIndexFile = new File(loc_sIndexBase + ".index");
      
      // Check existence/ancienity of the index file w.r.t. the text file
      if (!loc_fIndexFile.exists() || tmFile > loc_fIndexFile.lastModified()) {
        String sHeaderFile = "";  // Name of the header file
        String sHeaderXml = "";   // XML context of the header
        // Create a string builder to hold the index of this file
        StringBuilder sIndexData = new StringBuilder();
        // Create an XmlChunkReader to do this
        XmlChunkReader rdThis = new XmlChunkReader(fThis);
        // First read the header
        String sTagHeader = CorpusResearchProject.getTagHeader(ptThis);
        iPos = 0;
        if (!sTagHeader.isEmpty()) {
          
          // Since the header tag is defined, it is obligatory
          if (! (rdThis.ReadToFollowing(sTagHeader))) {
            errHandle.DoError("doCheckIndex error: cannot find <" + sTagHeader + 
                    "> in file [" + strFile + "]");
            return false;
          }
          iPos = rdThis.getPos();
          // Get the header data
          sHeaderXml = rdThis.ReadOuterXml();
          // Create an entry for the database header
          int iByteLength = sHeaderXml.getBytes("utf-8").length;
          XmlIndexItem oItem = new XmlIndexItem(sTagHeader, "", "", iPos, iByteLength);
          // Add index information for the database header
          sIndexData.append(oItem.csv());
        }
        // Add header file name to IndexData (this is a separate starting line)
        sIndexData.append(sHeaderFile).append("\n");
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
            if (!loc_pdxThis.LoadXml(strNext)) return false;
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
              // Create an index item (without part-specification)
              int iByteLength = strNext.getBytes("utf-8").length;
              XmlIndexItem oItem = new XmlIndexItem(sTagLine, sLineId, "", iPos, iByteLength);
              // Add index information on this item
              sIndexData.append(oItem.csv());
            } else {
              // Create an index item *with* part specification
              int iByteLength = strNext.getBytes("utf-8").length;
              XmlIndexItem oItem = new XmlIndexItem(sTagLine, sLineId, sPartId, iPos, iByteLength);
              // Add index information on this item
              sIndexData.append(oItem.csv());
            }

          }
        }
        // Write the index file to its position
        FileUtil.writeFile(loc_fIndexFile, sIndexData.toString());
      }
      // ===== DEBUGGING ======
      // errHandle.debug("doCheckIndex: exit");
      // ======================

      // Return positively
      return true;
    } catch (Exception ex) {
      errHandle.DoError("DoCheckingIndex could not be completed for " + fThis.getAbsolutePath(), ex, XmlIndexRaReader.class);
      // Return failure
      return false;
    }
  }
  
  
  /**
   * readIndex -- Load the index file into a datastructure
   *              And if Part is specified, read lstPart
   * 
   * @return 
   * @history
   *  17/sep/2015 ERK Implemented for Random-access reader
   */
  private boolean readIndex() {
    String line;      // One line of data
    int iStart;       // Start position (bytes)
    int iSize;        // Size of chunk (in bytes)
    String sPart;     // Name of this part
    String sLastPart; // Last name of part

    try {
      // Initialize the index datastructure
      arIndex = new ArrayList<>();
      lstPartFirstIdx = new ArrayList<>();
      lstPartLastIdx = new ArrayList<>();
      // Use a try-with-resources to make sure the .index file is closed after reading
      try (BufferedReader br = new BufferedReader(new FileReader( this.loc_fIndexFile))) {
        sLastPart = "";
        // Read all lines into the list
        while ((line = br.readLine()) != null) {
          String[] lineArray = line.split("\t");
          // Double check
          if (lineArray.length!=5) {
            // Stop and review what is going on
            int iStop = 0;
          } else {
            // Get start and size
            iStart = Integer.parseInt(lineArray[3]);
            iSize = Integer.parseInt(lineArray[4]);
            sPart = lineArray[2];
            // Add the line to the index
            arIndex.add(new XmlIndexItem(lineArray[0], lineArray[1], sPart, iStart, iSize));
            // Check if part needs to be adapted
            if (!sPart.equals(sLastPart)) {
              // Note where the last index of the previous part was
              if (!sLastPart.isEmpty()) this.lstPartLastIdx.add(arIndex.size()-1);
              // Add the *new* part to thelist
              this.lstParts.add(sPart);
              // Add the index of the new part to the list
              this.lstPartFirstIdx.add(arIndex.size()-1);
              sLastPart = sPart;
              // ======== Debugging ====
              // errHandle.debug("readIndex: " + sPart);
              // =======================
            }
          }
        }
      }
      
      // Note the last index of the last part
      this.lstPartLastIdx.add(arIndex.size()-1);
      // Create a random-access reader entry (READ ONLY)
      this.loc_fRa = new RandomAccessFile(loc_fThis.getAbsolutePath(), "r");   
      // Return positively
      return true;
    } catch (Exception ex) {
      errHandle.DoError("readIndex encountered a problem for " + loc_fThis.getAbsolutePath(), ex, XmlIndexRaReader.class);
      // Return failure
      return false;
    }
  }
  
  /**
   * close -- close the Random-Access reader file
   */
  public void close() {
    try {
      if (this.loc_fRa != null) {
        synchronized(this.loc_fRa) {
          errHandle.debug("Ra reader closing: " + loc_fThis.getName());
          this.loc_fRa.close();
          this.loc_fRa = null;
        }
      }
    } catch (Exception ex) {
      errHandle.DoError("close() RA reader problem " + loc_fThis.getAbsolutePath(), ex, XmlIndexRaReader.class);
    }
  }
  
  /**
   * is_closed -- indicate whether this handle is closed by looking at its value
   * 
   * @return boolean
   */
  public boolean is_closed() {
    return (this.loc_fRa == null);
  }
  
  /**
   * getHeader -- return the header of this text file 
   *              If there is no header, then return null.
   * 
   * @return String with the header
   */
  public String getHeader() {
    ByRef<XmlIndexItem> oItem = new ByRef(null);
    
    try {
      // Validate
      if (this.arIndex.isEmpty()) return "";
      String sBack = getLineByIndex(0, oItem);      
      // Validate: do we have an entry for the header?
      if (!oItem.argValue.part.isEmpty() || !oItem.argValue.id.isEmpty()) {
        errHandle.DoError("No header entry in index for file - " + loc_fThis.getAbsolutePath());
        return "";
      }
      // Return the header
      return sBack;
    } catch (Exception ex) {
      errHandle.DoError("Could not read header " + loc_fThis.getAbsolutePath(), ex, XmlIndexRaReader.class);
      // Return failure
      return "";
    }
  }
  
  /**
   * getFirstLine - Read the first line of xml text from the index
   * 
   * @return String with the first line
   */
  public String getFirstLine() {
    ByRef<XmlIndexItem> oItem = new ByRef(null);
    
    try {
      // Set the current line index
      iCurrentLine = 1;
      // Validate
      if (this.arIndex.isEmpty()) return "";
      String sBack = getLineByIndex(iCurrentLine, oItem).trim();      
      // Return the first line
      return sBack;
    } catch (Exception ex) {
      errHandle.DoError("Could not read first line of " + loc_fThis.getAbsolutePath(), ex, XmlIndexRaReader.class);
      // Return failure
      return "";
    }
  }
  public String getFirstLine(String sPart) {
    ByRef<XmlIndexItem> oItem = new ByRef(null);
    
    try {
      // Validate
      if (this.arIndex.isEmpty()) return "";
      // Set the current line index
      int iFirstPartIdx = getFirstPartIndex(sPart);
      if (iFirstPartIdx <0) return "";
      iCurrentLine = iFirstPartIdx;
      String sBack = getLineByIndex(iCurrentLine, oItem);      
      // Return the first line
      return sBack;
    } catch (Exception ex) {
      errHandle.DoError("Could not read first line of " + loc_fThis.getAbsolutePath(), ex, XmlIndexRaReader.class);
      // Return failure
      return "";
    }
  }
  
  /**
   * getFirstPartIndex
   *    Get the index within arIndex of the first occurrence of part [sPart]
   * 
   * @param sPart
   * @return 
   */
  private int getFirstPartIndex(String sPart) {
    try {
      // Set the current line index
      int iPartIdx = lstParts.indexOf(sPart);
      if (iPartIdx<0) return -1;
      return this.lstPartFirstIdx.get(iPartIdx);      
    } catch (Exception ex) {
      errHandle.DoError("getFirstPartIndex problem ", ex, XmlIndexRaReader.class);
      // Return failure
      return -1;
    }
  }
  
  /**
   * getLastPartIndex
   *    Get the index within arIndex of the last occurrence of part [sPart]
   * 
   * @param sPart
   * @return 
   */
  private int getLastPartIndex(String sPart) {
    try {
      // Set the current line index
      int iPartIdx = lstParts.indexOf(sPart);
      if (iPartIdx<0) return -1;
      return this.lstPartLastIdx.get(iPartIdx);      
    } catch (Exception ex) {
      errHandle.DoError("getLastPartIndex problem ", ex, XmlIndexRaReader.class);
      // Return failure
      return -1;
    }
  }
  /**
   * getLineByIndex
   *    Read the line at index position iIndex
   * 
   * @param iIndex
   * @return 
   */
  private String getLineByIndex(int iIndex, ByRef<XmlIndexItem> oItem) {
    byte[] bBuf = null;
    
    try {
      // Validate
      if (this.arIndex.size() < iIndex+1) return "";
      if (iIndex < 0) return "";
      // Validate: do we have an entry for the first line?
      oItem.argValue = this.arIndex.get(iIndex);
      // Make sure we access reading while it is possible
      if (this.loc_fRa == null) {
        // Return nothing
        return "";
      } else {
        synchronized(this.loc_fRa) {
          // Some error occurring here--get to the root of it
          if (this.loc_fRa == null) {
            errHandle.debug("getLineByIndex: loc_fRa is null");
          }
          if (oItem.argValue == null) {
            errHandle.debug("getLineByIndex: oItem.argValue is null");
          }
          // Read the first line into a string
          this.loc_fRa.seek(oItem.argValue.start);
          bBuf = new byte[oItem.argValue.size];
          this.loc_fRa.read(bBuf);
        }
      }
      // Turn what we read into a string
      String sBack = new String(bBuf, "utf-8");
      // Return the indicated line
      return sBack;      
    } catch (Exception ex) {
      errHandle.DoError("getLineByIndex could not read next line [" + iIndex + "] of " + loc_fThis.getAbsolutePath(), ex, XmlIndexRaReader.class);
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
    ByRef<XmlIndexItem> oItem = new ByRef(null);
    
    try {
      // Go to the next line
      iCurrentLine++;
      // Validate
      if (iCurrentLine >= arIndex.size()) {
        this.EOF = true;
        return "";
      }
      // Validate
      if (this.arIndex.isEmpty()) return "";
      String sBack = getLineByIndex(iCurrentLine, oItem);      
      // Return the first line
      return sBack;
    } catch (Exception ex) {
      errHandle.DoError("Could not read next line [" + iCurrentLine + "] of " + loc_fThis.getAbsolutePath(), ex, XmlIndexRaReader.class);
      // Return failure
      return "";
    }
  }
  public String getNextLine(String sPart) {
    ByRef<XmlIndexItem> oItem = new ByRef(null);
    
    try {
      // What is the last line within arIndex for this part?
      int iLastLine = getLastPartIndex(sPart);
      // Check if we are within limits
      if (iCurrentLine >= iLastLine) {
        this.EOF = true;
        return "";
      }
      // Go to the next line
      iCurrentLine++;
      // Validate
      if (this.arIndex.isEmpty()) { 
        this.EOF = true; return "";}
      String sBack = getLineByIndex(iCurrentLine, oItem);    
      // Check that this line still partains to the indicated part
      if (!oItem.argValue.part.equals(sPart)) { 
        sBack = ""; this.EOF = true; }
      // Return the first line
      return sBack;
    } catch (Exception ex) {
      errHandle.DoError("Could not read next line [" + iCurrentLine + "] of " + loc_fThis.getAbsolutePath(), ex, XmlIndexRaReader.class);
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
    ByRef<XmlIndexItem> oItem = new ByRef(null);
    int iNewLine = 0;
    
    try {
      // Validate
      if (this.arIndex.isEmpty()) return "";
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
      String sBack = getLineByIndex(iCurrentLine, oItem);   
      // Retrieve the sentence id
      sSentId.argValue = oItem.argValue.id;
      // Return the first line
      return sBack;
    } catch (Exception ex) {
      errHandle.DoError("Could not read 'relative' line [" + iNewLine + "] of " + loc_fThis.getAbsolutePath(), ex, XmlIndexRaReader.class);
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
    return getOneLine(sLineId, true);
  }
  public String getOneLine(int iLines) {
    ByRef<XmlIndexItem> oItem = new ByRef(null);
    int iIndex = iCurrentLine + iLines;

    try {
      // Validate
      if (iIndex < 0 || iIndex >= this.arIndex.size()) {
        errHandle.DoError("Line number ["+iCurrentLine+"]+["+iLines+"] is out of reach", 
                XmlIndexRaReader.class);
        return "";
      }
      // Read the chunck from there
      String sBack = getLineByIndex(iIndex, oItem);   
      // Return it
      return sBack;
    } catch (Exception ex) {
      errHandle.DoError("Could not read line [" + iIndex + "] of " 
              + loc_fThis.getAbsolutePath(), ex, XmlIndexRaReader.class);
      // Return failure
      return "";
    }
  }
  public String getOneLine(String sLineId, boolean bTouch) {
    ByRef<XmlIndexItem> oItem = new ByRef(null);

    try {
      // Validate
      if (this.arIndex.isEmpty()) return "";
      // Find out which line should be read
      // TODO: use a better (bisection) algorithm for this in the future
      for (int i=0;i<arIndex.size();i++ ) {
        XmlIndexItem objThis = arIndex.get(i);
        if (objThis.id.equals(sLineId)) {
          // NOTE: make sure we do NOT touch the [iCurrentLine], otherwise [getNextLine] gets into trouble!!
          // Default:   touch it
          // Exception: don't touch if [bTouch] is false (as from XmlForestPsdxIndex.java)
          if (bTouch) iCurrentLine = i;
          // Read the chunck from there
          // String sBack = getLineByIndex(iCurrentLine, oItem);   
          String sBack = getLineByIndex(i, oItem);   
          // Return it
          return sBack;
        }
      }
      return "";      
    } catch (Exception ex) {
      errHandle.DoError("Could not read line [" + sLineId + "] of " 
              + loc_fThis.getAbsolutePath(), ex, XmlIndexRaReader.class);
      // Return failure
      return "";
    }
  }
  
  /**
   * getOneConstLine - get the line in which the indicated constituent id is
   *                   A constituent id may be the <eTree> id within a <forest>
   * 
   * @param sConstId
   * @return String containing the line sought for
   */
  public String getOneConstLine(String sConstId) {
    ByRef<XmlIndexItem> oItem = new ByRef(null);

    try {
      // Validate
      if (this.arIndex.isEmpty()) return "";
      // Find out which line should be read
      // TODO: use a better (bisection) algorithm for this in the future
      for (int i=arIndex.size()-1;i>=0;i-- ) {
        XmlIndexItem objThis = arIndex.get(i);
        // Check if the constituent id we are looking for is *smaller* than the 
        //   last constituent id of this line
        if (!objThis.lastId.equals("-") && sConstId.compareTo(objThis.lastId)<=0) {
          // Read the chunck from there
          String sBack = getLineByIndex(i, oItem);   
          // Return it
          return sBack;
        }
      }
      return "";      
    } catch (Exception ex) {
      errHandle.DoError("Could not read line with constituent [" + sConstId + 
              "] of " + loc_fThis.getAbsolutePath(), ex, XmlIndexRaReader.class);
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
