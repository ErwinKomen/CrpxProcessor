/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

/**
 *
 * @author E.R.Komen
 */
public class FileIO {
  
  // ================== Internal usage ===============================
  private int iLinesRead = 0;    // Number of lines read with last readFile

  public String readFile(String file) throws IOException {
    BufferedReader reader = new BufferedReader( new FileReader (file));
    String         line = null;
    StringBuilder  stringBuilder = new StringBuilder();
    String         ls = System.getProperty("line.separator");

    // Keep track of the number of lines that have been read
    iLinesRead = 0;
    // Loop through all the lines
    while( ( line = reader.readLine() ) != null ) {
      // Increment the lines that have been read
      iLinesRead++;
      // Append the line and a line separator to the string
      stringBuilder.append( line );
      stringBuilder.append( ls );
    }
    // Close the reader
    reader.close();
    // Return what we have been building up
    return stringBuilder.toString();
  }
  // ================== Access to the number of lines that have been read
  public int getLinesRead() { return iLinesRead;}
  // Write the text in @sText into the file name @file
  public boolean writeFileUtf8(String file, String sText) throws IOException {
    BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(file), "UTF-8"));
    String         ls = System.getProperty("line.separator");
    String[]       lines = sText.split(ls);
    
    // Keep track of the number of lines that have been written
    iLinesRead = 0;
    // Loop through all the lines
    for (String line : lines) {
      // Add the lines to the file
      writer.write(line);
      writer.newLine();
      // Increment the lines that have been written
      iLinesRead++;
    }
    // close the writer
    writer.close();
    // Return success
    return true;
  }
  // ================== Transform string filename to bare one w/o extension
  public static String getFileNameWithoutExtension(String sFile) {

    String separator = System.getProperty("file.separator");

    int indexOfLastSeparator = sFile.lastIndexOf(separator);
    String filename = sFile.substring(indexOfLastSeparator + 1);

    int extensionIndex = filename.lastIndexOf(".");
    String fileExtension = filename.substring(0, extensionIndex);

    return fileExtension;
  }
  public static InputStream getProjectDirectory(Class clsThis, String sFile) {
    InputStream is = clsThis.getClassLoader().getResourceAsStream(sFile);
    return is;
  }
}
