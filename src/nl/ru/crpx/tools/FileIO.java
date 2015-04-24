/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

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
    // Return what we have been building up
    return stringBuilder.toString();
  }
  // ================== Access to the number of lines that have been read
  public int getLinesRead() { return iLinesRead;}
}
