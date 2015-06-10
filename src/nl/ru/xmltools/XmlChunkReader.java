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
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  private int iLinesRead = 0;     // Number of lines read during the *last* readFile
  private String strTag;          // Name of the tag we are using
  private String strLastLine;     // Line that we read last
  private String ls;              // line separator
  // private long lSize;             // Size of the file in number of lines
  private long lFileLen;          // Length of file in bytes
  private long lFilePos;          // Current reading position of file
  // ======================== publicly accessible information
  public boolean EOF;             // Hit the end of file or not
  public XmlChunkReader() {
    // Set default values
    lFileLen = 1;
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
    // Create a buffered file reader, so that we know the position
    FileReader plain = new FileReader (fThis);
    reader = new BufferedReader( plain);
    // lSize = reader.lines().count();
    lFileLen = fThis.length();
    // Now start reading for real
    reader = new BufferedReader( new FileReader (fThis));
    iLinesRead=0;
    strTag = "";
    EOF = false;
    strLastLine = "";
    ls = System.getProperty("line.separator");
  }

  // ----------------------------------------------------------------------------------------------------------
  // Name :  ReadToFollowing
  // Goal :  Skip lines until reaching the line that has the first occurrance of start element [sItem]
  // History:
  // 24/apr/2015  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  public boolean ReadToFollowing(String sTag) throws IOException {
    String line;      // Room for each line
    Pattern patStart; // Regular Expression pattern for starting @sTag
    
    // Validate
    if (reader==null) return false;
    // Keep track of the number of lines that have been read
    strTag = sTag;
    patStart = Pattern.compile("(\\<" + sTag + "\\s|\\<" + sTag + "\\>)");
    // Check if there is a last line that contains this tag
    
    // Loop through all the lines
    while( ( line = reader.readLine() ) != null ) {
      // Keep track of lines
      iLinesRead++; strLastLine = line;
      // Get approximate file position: number of bytes + 1 for \n (we are lower if \r\n is used)
      lFilePos += line.getBytes().length + 1;
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

    // Validate
    if (reader==null || strTag.isEmpty()) return "";
    // Make regular expression for detection of ending tag
    patEnd = Pattern.compile("</" + strTag + ">");
    // Start storing the first line
    sChunk = new StringBuilder();
    if (!strLastLine.isEmpty())  { sChunk.append(strLastLine); sChunk.append(ls);}
    // Loop through all the lines
    while( ( line = reader.readLine() ) != null ) {
      // Keep track of lines
      iLinesRead++; strLastLine = line;
      // Store this line
      sChunk.append(line); sChunk.append(ls);
      // Get approximate file position: number of bytes + 1 for \n (we are lower if \r\n is used)
      lFilePos += line.getBytes().length + 1;
      // Check if this line contains the ending tag
      Matcher m = patEnd.matcher(line);
      // Do we have a matching end tag?
      if (m.find()) {
        /* NO: last line was already read
        // Store the last line
        sChunk.append(line); sChunk.append(ls);
              */
        // Return the chunk we have read
        String sCombi = sChunk.toString();
        return sCombi;
      }
    }
    // Do note the last line, even if it is empty
    strLastLine = "";
    // Coming here means EOF
    EOF = true;

    // Return empty string
    return "";
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
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  getLinesRead
  // Goal :  Retrieve the number of lines read so far
  // History:
  // 24/apr/2015  ERK Created
  // ----------------------------------------------------------------------------------------------------------
  public int getLinesRead() { return iLinesRead;}
  public int getPtc() {return (int) ( (lFilePos / lFileLen) * 100); }
}
