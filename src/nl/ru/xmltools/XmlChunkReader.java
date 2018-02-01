/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.ru.util.FileUtil;
import org.apache.log4j.Logger;

/**
 * XmlChunkReader - read xml files in chunks determined by (unique) starting and
 *                  closing chunks
 * E.g: <forest .. > ... </forest>
 *      <teiHeader>  ... </teiHeader>
 * 
 * @author Erwin R. Komen
 */
public class XmlChunkReader {
  // This class uses a logger
  private static final Logger logger = Logger.getLogger(XmlChunkReader.class);
  // ======================== variables for internal use
  private BufferedReader reader;  // Buffered file reader
  private InputStreamReader rdIS; // Underlying input stream reader
  private FileInputStream fIS;    // Underlying file input stream
  private int iLinesRead = 0;     // Number of lines read during the *last* readFile
  private String strTag;          // Name of the tag we are using
  private String strLastLine;     // Line that we read last
  private String ls;              // line separator
  private String loc_sContents;   // Contents read into memory (if GZ)
  private String sReaderType;     // Kind of reader
  private int chNext;             // next character
  private long lFileLen;          // Length of file in bytes
  private long lFilePos;          // Current reading position of file
  private long lLastPos;          // Last reading position before consuming more
  private char chNextMem;
  private int lMemPos;
  private int lLastMemPos;
  // ======================== publicly accessible information
  public boolean EOF;             // Hit the end of file or not
  public XmlChunkReader() {
    // Set default values
    lFileLen = 1;
    loc_sContents = "";
    sReaderType = "";
  }
  // ----------------------------------------------------------------------------------------------------------
  // Class : XmlChunkReader
  // Goal :  Streaming (buffered) reader of an XML file that retrieves 'chunks':
  //           pieces that start and end with the same tag, such as
  //           <forest> .... </forest>
  // Note:   The class does not (yet) allow for nested tags with the same name
  // History:
  // 24/apr/2015  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  public XmlChunkReader(File fThis) throws FileNotFoundException {
    try {
      // Check if this is a compressed file
      if (fThis.getAbsolutePath().endsWith(".gz")) {
        // Read the zipped file into a string
        loc_sContents = FileUtil.decompressGzipString(fThis.getAbsolutePath());
        // Get the size
        lFileLen = loc_sContents.length();
        // Signal that we are a memory reader
        reader = null;
        sReaderType = "memory";
        lMemPos = 0; lLastMemPos = 0;
      } else {
        // Create a buffered file reader, so that we know the position
        FileReader plain = new FileReader (fThis);
        reader = new BufferedReader( plain);
        // lSize = reader.lines().count();
        lFileLen = fThis.length();
        // Set the input for real
        fIS = new FileInputStream(fThis);
        rdIS = new InputStreamReader(fIS, "utf-8");
        // Now start reading for real
        reader = new BufferedReader(rdIS);
        sReaderType = "file";
      }
      // reader = new BufferedReader(new InputStreamReader(new FileInputStream (fThis), "utf-8"));
      iLinesRead=0;
      strTag = "";
      EOF = false;
      strLastLine = "";
      lFilePos = 0; lLastPos = 0;
      chNext = 0;
      ls = System.getProperty("line.separator");
    } catch (Exception ex) {
      logger.error("XmlChunkReader problem",ex);
    }
  }

  // ----------------------------------------------------------------------------------------------------------
  // Name :  ReadToFollowing
  // Goal :  Skip lines until reaching the line that has the first occurrance of start element [sItem]
  // History:
  // 24/apr/2015  ERK Created
  // 01/feb/2018  ERK Adapted to also work for sReaderType='memory'
  // ----------------------------------------------------------------------------------------------------------
  public boolean ReadToFollowing(String sTag) throws IOException {
    String line;      // Room for each line
    Pattern patStart; // Regular Expression pattern for starting @sTag
    
    // Validate
    switch(sReaderType) {
      case "memory": if (loc_sContents.isEmpty()) return false; break;
      case "file": if (reader==null) return false; break;
      default: return false;
    }
    // Keep track of the number of lines that have been read
    strTag = sTag;
    patStart = Pattern.compile("(\\<" + sTag + "\\s|\\<" + sTag + "\\>)");
    // Check if there is a last line that contains this tag
    
    // Loop through all the lines
    // while( ( line = reader.readLine() ) != null ) {
    while( ( line = readLine() ) != null ) {
      // Keep track of lines
      strLastLine = line;
      // Check if this line contains the starting tag
      Matcher m = patStart.matcher(line);
      // Do we have a matching start tag?
      if (m.find()) {
        // Return positively
        return true;
      }
    }
    // Do note the last line, even if it is empty
    strLastLine = "";
    // Coming here means EOF
    EOF = true;
    // Return negatively: we have not found it
    return false;
  }
  
  /**
   * readMemoryChar
   *    Read one character from the loc_sContents string
   * 
   * @return 
   */
  private char readMemoryChar() {
    char cBack = '\0';
    
    try {
      // Check if we are not past the length
      if (lMemPos >= lFileLen) {
        // In that case we return the empty string
        return '\0';
      }
      // Read the character at [lFilePos]
      cBack = loc_sContents.charAt(lMemPos);
      // Adapt the position
      lMemPos += 1;
      return cBack;
    } catch (Exception ex) {
      return '\0';
    }
  }
  
  /**
   * readLineFromMemory
   *    My own implementation of readline from MEMORY
   *    Read characters one by one and continue until:
   *    a) there are no more \n\r consecutively
   *    b) there is an empty string
   * 
   * @return 
   */
  private String readLineFromMemory() {
    StringBuilder sb = new StringBuilder();
    int m;
    char n;
    char prevN;
    int iNumLT = 0;   // Number of < signs encountered so far
    
    try {
      // Do note our position before reading
      lLastMemPos = lMemPos;
      // Check for EOF
      // Need to have a 1-character read-ahead buffer
      if (iLinesRead == 0) {
        chNextMem = readMemoryChar();
      }
      // check for EOF
      if (chNextMem == '\0') return null;
      // Loop until reaching any \r\n
      // Also look out for end-tag beginner </ and for one-tag finisher />
      boolean bOneTagFinish = false;
      boolean bEndTagStart = false;
      boolean bEndTagFinish = false;
      
      // Retain the previous character
      n = chNextMem; prevN = n;
      while (chNextMem>0 && !(n == '\n' || n == '\r' || bOneTagFinish || bEndTagFinish)) {
        // End-tag and One-tag bookkeeping
        if (prevN == '<' && n == '/') 
          bEndTagStart = true;
        else if (prevN == '/' && n == '>') 
          bOneTagFinish = true;
        else if (bEndTagStart && n == '>')
          bEndTagFinish = true;
        // Add to string
        sb.append(n);
        // Note the number of < signs
        if (n == '<') iNumLT += 1;
        // Read next and keep track of previous
        chNextMem = readMemoryChar();
        prevN = n;
        n = chNextMem;
      }
      // Read and eat all \r\n
      while (chNextMem>0 && (n == '\n' || n == '\r')) {
        // Add to string
        sb.append(n);
        // Read next
        chNextMem = readMemoryChar();
        n = chNextMem;
      }
      // Get the string
      String sBack = sb.toString();
      /*
          THIS IS NO LONGER NEEDED
      // Do note our position before reading
      lLastMemPos = lMemPos;
      // Adapt the file position correctly
      lMemPos += sBack.length;
      */
      
      // Keep track of lines read
      iLinesRead++;
      // Return what we have in the sb
      return sBack;
    } catch (Exception ex) {
      return null;
    }
  }

  /**
   * readLineFromFile
   *    My own implementation of readline from internal reader
   *    Read characters one by one and continue until:
   *    a) there are no more \n\r consecutively
   *    b) there is an EOF sign
   * 
   * @return 
   */
  private String readLineFromFile() {
    StringBuilder sb = new StringBuilder();
    int m;
    char n;
    char prevN;
    int iNumLT = 0;   // Number of < signs encountered so far
    
    try {
      // Check for EOF
      // Need to have a 1-character read-ahead buffer
      if (iLinesRead == 0) {
        chNext = reader.read();
      }
      // check for EOF
      if (chNext<0) return null;
      // Loop until reaching any \r\n
      // Also look out for end-tag beginner </ and for one-tag finisher />
      boolean bOneTagFinish = false;
      boolean bEndTagStart = false;
      boolean bEndTagFinish = false;
      n = (char) chNext; prevN = n;
      while (chNext>0 && !(n == '\n' || n == '\r' || bOneTagFinish || bEndTagFinish)) {
        // End-tag and One-tag bookkeeping
        if (prevN == '<' && n == '/') 
          bEndTagStart = true;
        else if (prevN == '/' && n == '>') 
          bOneTagFinish = true;
        else if (bEndTagStart && n == '>')
          bEndTagFinish = true;
        // Add to string
        sb.append(n);
        // Note the number of < signs
        if (n == '<') iNumLT += 1;
        // Read next and keep track of previous
        chNext = reader.read();
        prevN = n;
        n = (char) chNext;
      }
      // Read and eat all \r\n
      while (chNext>0 && (n == '\n' || n == '\r')) {
        // Add to string
        sb.append(n);
        // Read next
        chNext = reader.read();
        n = (char) chNext;
      }
      // Get the string
      String sBack = sb.toString();
      // Do note our position before reading
      lLastPos = lFilePos;
      // Adapt the file position correctly
      lFilePos += sBack.getBytes("utf-8").length;
      // Keep track of lines read
      iLinesRead++;
      // Return what we have in the sb
      return sBack;
    } catch (Exception ex) {
      return null;
    }
  }
  
  private String readLine() {
    switch (sReaderType ) {
      case "memory": return readLineFromMemory(); 
      case "file": return readLineFromFile();
      default: return "";
    }    
  }

  /**
   * isEof
   *    Own implementation of end-of-file
   * 
   * @return 
   */
  private boolean isEof() {
    switch (sReaderType ) {
      case "memory": return (chNextMem < 0 || chNextMem == '\0'); 
      case "file": return (chNext<0);
      default: return true;
    }    
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  ReadOuterXml
  // Goal :  Read the complete content from start until end element as a string
  // History:
  // 24/apr/2015  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  public String ReadOuterXml() throws IOException {
    String line;          // Room for each line
    Pattern patEnd;       // Regular Expression pattern for starting @sTag
    StringBuilder sChunk; // Chunk

    try {
      // Validate
      switch (sReaderType) {
        case "file": if (reader==null || strTag.isEmpty()) return ""; break;
        case "memory": if (loc_sContents.isEmpty() || strTag.isEmpty()) return ""; break;
      }
      // Make regular expression for detection of ending tag
      patEnd = Pattern.compile("</" + strTag + ">");
      // Start storing the first line
      sChunk = new StringBuilder();
      if (!strLastLine.isEmpty())  { sChunk.append(strLastLine); /* sChunk.append(ls); */}
      // Loop through all the lines
      while( ( line = readLine() ) != null ) {
        // Keep track of lines
        strLastLine = line;
        // Store this line
        sChunk.append(line); /* sChunk.append(ls); */
        // Check if this line contains the ending tag
        Matcher m = patEnd.matcher(line);
        // Do we have a matching end tag?
        if (m.find()) {
          // Return the chunk we have read
          String sCombi = sChunk.toString();
          /*
          lFilePos = lLastPos + sCombi.getBytes("utf-8").length;
          // We have in fact read the last line, so consume it in lastpos
          lLastPos = lFilePos;
          */
          // Return the result
          return sCombi;
        }
      }
      // Do note the last line, even if it is empty
      strLastLine = "";
      // Coming here means EOF
      EOF = true;

      // Return empty string
      return "";      
    } catch (Exception ex) {
      return "";
    }
  }
  
  // ----------------------------------------------------------------------------------------------------------
  // Name :  Close
  // Goal :  Close the reader
  // History:
  // 24/apr/2015  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  public void Close() throws IOException {
    if (reader!=null) {
      reader.close(); reader = null;
    }
    EOF = true;
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  getLinesRead
  // Goal :  Retrieve the number of lines read so far
  // History:
  // 24/apr/2015  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  public int getLinesRead() { return iLinesRead;}
  public int getPtc() {
    int iPtc = 0;
    switch(sReaderType) {
      case "memory": iPtc = (int) ( (lMemPos / lFileLen) * 100); break;
      case "file": iPtc = (int) ( (lFilePos / lFileLen) * 100); break;
    }
    return iPtc; 
  }
  public int getPos() {
    try {
      switch(sReaderType) {
        case "memory": 
          // The position needed is a byte position
          // .getBytes("utf-8").length
          String sPart = loc_sContents.substring(0,lLastMemPos);
          return sPart.getBytes("utf-8").length;
        case "file": return (int) lLastPos; 
      }
      return 0; 
    } catch (Exception ex) {
      return 0;
    }
  }
}
