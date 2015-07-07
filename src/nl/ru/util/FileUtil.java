/*******************************************************************************
 * Copyright (c) 2010, 2012 Institute for Dutch Lexicology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package nl.ru.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for working with files
 */
public class FileUtil {
  /**
   * The default encoding for opening files.
   */
  private static String defaultEncoding = "utf-8";
  private static int iLineCount = 0;

  /**
   * Get the default encoding for opening files.
   *
   * @return the default encoding
   */
  public static String getDefaultEncoding() {
          return defaultEncoding;
  }

  /**
   * Set the default encoding for opening files.
   *
   * @param defaultEncoding
   *            the default encoding
   */
  public static void setDefaultEncoding(String defaultEncoding) {
          FileUtil.defaultEncoding = defaultEncoding;
  }

  /**
   * Opens a file for writing in the default encoding.
   *
   * Wraps the Writer in a BufferedWriter and PrintWriter for efficient and convenient access.
   *
   * @param file
   *            the file to open
   * @return write interface into the file
   */
  public static PrintWriter openForWriting(File file) {
          return openForWriting(file, defaultEncoding);
  }

  /**
   * Opens a file for writing.
   *
   * Wraps the Writer in a BufferedWriter and PrintWriter for efficient and convenient access.
   *
   * @param file
   *            the file to open
   * @param encoding
   *            the encoding to use, e.g. "utf-8"
   * @return write interface into the file
   */
  public static PrintWriter openForWriting(File file, String encoding) {
          try {
                  return new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                                  file), encoding)));
          } catch (Exception e) {
                  throw new RuntimeException(e);
          }
  }

  /**
   * Opens a file for appending.
   *
   * Wraps the Writer in a BufferedWriter and PrintWriter for efficient and convenient access.
   *
   * @param file
   *            the file to open
   * @return write interface into the file
   */
  public static PrintWriter openForAppend(File file) {
          return openForAppend(file, defaultEncoding);
  }

  /**
   * Opens a file for appending.
   *
   * Wraps the Writer in a BufferedWriter and PrintWriter for efficient and convenient access.
   *
   * @param file
   *            the file to open
   * @param encoding
   *            the encoding to use, e.g. "utf-8"
   * @return write interface into the file
   */
  public static PrintWriter openForAppend(File file, String encoding) {
          try {
                  return new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                                  file, true), encoding)));
          } catch (Exception e) {
                  throw new RuntimeException(e);
          }
  }

  /**
   * Opens a file for reading, with the default encoding.
   *
   * Wraps the Reader in a BufferedReader for efficient and convenient access.
   *
   * @param file
   *            the file to open
   * @return read interface into the file
   */
  public static BufferedReader openForReading(File file) {
          return openForReading(file, defaultEncoding);
  }

  /**
   * Opens a file for reading, with the default encoding.
   *
   * Wraps the Reader in a BufferedReader for efficient and convenient access.
   *
   * @param file
   *            the file to open
   * @param encoding
   *            the encoding to use, e.g. "utf-8"
   * @return read interface into the file
   */
  public static BufferedReader openForReading(File file, String encoding) {
          try {
                  return new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
          } catch (Exception e) {
                  throw new RuntimeException(e);
          }
  }
  
  /**
   * Creates an input stream from the path @sPath.
   * If the file does not exist, it returns null.
   * 
   * @param sPath
   * @return 
   */
  public  static InputStream getInputStream(String sPath) {
    try {
      InputStream is = new FileInputStream(new File(sPath));
      return is;
    } catch (FileNotFoundException ex) {
      // Cannot find it, so return null
      return null;
    }
  }

  // TODO: add writeLines()

  /**
   * Read a file into a list of lines
   *
   * @param filePath
   *            the file to read
   * @return list of lines
   * @deprecated use File version instead
   */
  @Deprecated
  public static List<String> readLines(String filePath) {
          return readLines(new File(filePath));
  }

  /**
   * Read a file into a list of lines
   *
   * @param filePath
   *            the file to read
   * @param encoding
   *            the encoding to use, e.g. "utf-8"
   * @return list of lines
   * @deprecated use File version instead
   */
  @Deprecated
  public static List<String> readLines(String filePath, String encoding) {
          return readLines(new File(filePath), encoding);
  }

  /**
   * Read a file into a list of lines
   *
   * @param inputFile
   *            the file to read
   * @return list of lines
   */
  public static List<String> readLines(File inputFile) {
          return readLines(inputFile, defaultEncoding);
  }

  /**
   * Read a file into a list of lines
   *
   * @param inputFile
   *            the file to read
   * @param encoding
   *            the encoding to use, e.g. "utf-8"
   * @return list of lines
   */
  public static List<String> readLines(File inputFile, String encoding) {
    try {
      List<String> result = new ArrayList<String>();
      BufferedReader in = openForReading(inputFile, encoding);
      try {
        String line;
        while ((line = in.readLine()) != null) {
          result.add(line.trim());
        }
      } finally {
        in.close();
      }
      iLineCount = result.size();
      return result;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Replaces illegal characters (/, \, :, *, ?, ", <, > and |) in a filename with an underscore.
   *
   * @param filename
   *            the filename to sanitize
   * @return the sanitized filename
   */
  public static String sanitizeFilename(String filename) {
          return FileUtil.sanitizeFilename(filename, "_");
  }

  /**
   * Replaces illegal characters (/, \, :, *, ?, ", <, > and |) in a filename with the specified
   * character.
   *
   * @param filename
   *            the filename to sanitize
   * @param invalidChar
   *            the replacement character
   * @return the sanitized filename
   */
  public static String sanitizeFilename(String filename, String invalidChar) {
          return filename.replaceAll("[\t\r\n/\\\\:\\*\\?\"<>\\|]", invalidChar);
  }

  /**
   * Get the size of a file or a directory tree
   *
   * @param root
   * @return size of the file or directory tree
   */
  public static long getTreeSize(File root) {
          long size = 0;
          if (root.isFile())
                  size = root.length();
          else {
                  for (File f : root.listFiles()) {
                          size += getTreeSize(f);
                  }
          }
          return size;
  }

  /**
   * A task to execute on a file. Used by processTree().
   */
  public static abstract class FileTask {
          /**
           * Execute the task on this file.
           *
           * @param f
           *            the file to process
           */
          public abstract void process(File f);
  }

  /**
   * Perform an operation on all files in a tree
   *
   * @param root
   *            the directory to start in (all subdirs are processed)
   * @param task
   *            the task to execute for every file
   */
  public static void processTree(File root, FileTask task) {
          if (!root.isDirectory())
                  throw new RuntimeException("FileUtil.processTree: must be called with a directory! "
                                  + root);
          for (File f : root.listFiles()) {
                  if (f.isFile())
                          task.process(f);
                  else if (f.isDirectory()) {
                          processTree(f, task);
                  }
          }
  }

  /**
   * Perform an operation on some files in a tree
   *
   * @param dir
   *            the directory to start in
   * @param glob
   *            which files to process (e.g. "*.xml")
   * @param recurseSubdirs
   *            whether or not to process subdirectories
   * @param task
   *            the task to execute for every file
   */
  public void processTree(File dir, String glob, boolean recurseSubdirs, FileTask task) {
          Pattern pattGlob = Pattern.compile(FileUtil.globToRegex(glob));
          for (File file : dir.listFiles()) {
                  if (file.isDirectory()) {
                          // Process subdir?
                          if (recurseSubdirs)
                                  processTree(file, glob, recurseSubdirs, task);
                  } else if (file.isFile()) {
                          // Regular file; does it match our glob expression?
                          Matcher m = pattGlob.matcher(file.getName());
                          if (m.matches()) {
                                  task.process(file);
                          }
                  }
          }
  }

  /**
   * Convert a simple file glob expression (containing * and/or ?) to a regular expression.
   *
   * Example: "log*.txt" becomes "^log.*\\.txt$"
   *
   * @param glob
   *            the file glob expression
   * @return the regular expression
   */
  public static String globToRegex(String glob) {
          glob = glob.replaceAll("\\^", "\\\\\\^");
          glob = glob.replaceAll("\\$", "\\\\\\$");
          glob = glob.replaceAll("\\.", "\\\\.");
          glob = glob.replaceAll("\\\\", "\\\\");
          glob = glob.replaceAll("\\*", ".*");
          glob = glob.replaceAll("\\?", ".");
          return "^" + glob + "$";
  }

  /**
   * Find a file on the classpath.
   *
   * @param fn name of the file we're looking for
   * @return the file if found, null otherwise
   */
  public static File findOnClasspath(String fn) {
          String sep = System.getProperty("path.separator");
          for (String part: System.getProperty("java.class.path").split("\\" + sep)) {
                  File f = new File(part);
                  File dir = f.isFile() ? f.getParentFile() : f;
                  File ourFile = new File(dir, fn);
                  if (ourFile.exists())
                          return ourFile;
          }
          return null;
  }

  /**
   * Detect the Unicode encoding of an input stream by looking for a BOM at the current position.
   *
   * If no BOM is found, the specified default encoding is returned and
   * the position of the stream is unchanged.
   *
   * If a BOM is found, it is interpreted and the corresponding encoding
   * is returned. The stream will remain positioned after the BOM.
   *
   * This method uses InputStream.mark(), which must be supported by the given stream
   * (BufferedInputStream supports this).
   *
   * Only works for UTF-8 and UTF16 (LE/BE) for now.
   *
   * @param inputStream the input stream
   * @param defaultEncoding encoding to return if no BOM found
   * @return the encoding
   * @throws IOException
   */
  public static String detectBomEncoding(BufferedInputStream inputStream, String defaultEncoding) throws IOException {
          String encoding = "";

          if (!inputStream.markSupported()) {
                  throw new RuntimeException("Need support for inputStream.mark()!");
          }

          inputStream.mark(4); // mark this position so we can reset() later
          int firstByte  = inputStream.read();
          int secondByte = inputStream.read();
          if(firstByte == 0xFF && secondByte == 0xFE) {
                  // BOM voor UTF-16LE
                  encoding = "utf-16le";
                  // We staan nu na de BOM, dus ok
          } else if(firstByte == 0xFE && secondByte == 0xFF) {
                  // BOM voor UTF-16 LE
                  encoding = "utf-16be";
                  // We staan nu na de BOM, dus ok
          } else if(firstByte == 0xEF && secondByte == 0xBB) {
                  int thirdByte = inputStream.read();
                  if(thirdByte == 0xBF) {
                          // BOM voor UTF-8
                          encoding = "utf-8";
                  }
                  // We staan nu na de BOM, dus ok
          } else {
                  // Geen BOM maar wel 2 bytes gelezen; "rewind"
                  inputStream.reset();
                  encoding = defaultEncoding; // (we assume, as we haven't found a BOM)
          }
          return encoding;
  }

  /**
   * Read an entire file into a String
   * @param file the file to read
   * @return the file's contents
   */
  public static String readFile(File file) {
    return StringUtil.join(readLines(file), "\n");
  }
  // ERK: added readFile with a filename string as input
  public static String readFile(String sName) {
    File file = new File(sName);
    List<String> lCombi = readLines(file);
    return StringUtil.join(lCombi, "\n");
  }
  public static int getLinesRead() { return iLineCount;}

  /**
   * Write a String to a file.
   * @param file the file to write
   * @param data what to write to the file
   */
  public static void writeFile(File file, String data) {
    PrintWriter out = openForWriting(file);
    try {
      out.print(data);
    } finally {
      out.close();
    }
  }
  // ERK: added writeFile with the encoding as option and a string filename as first argument
  public static void writeFile(String sName, String data, String encoding) {
    File file = new File(sName);
    PrintWriter out = openForWriting(file, encoding);
    try {
      out.print(data);
    } finally {
      out.close();
    }
  }
  // ERK: added string-to-string file name normalization using .nio
  public static String nameNormalize(String sName) {
    Path pThis = null;
    LinkOption lThis;
    File sMac = new File("/Users/erwin/");
    File sLinux = new File("/etc/project/");
    String sReplace = "/";
    
    String fs = System.getProperty("file.separator");
    // Check if we need Windows >> Mac/nix conversion
    if (fs.equals("/") && sName.contains("\\")) {
      // Check the path
      if (sMac.exists())  sReplace = sMac.getAbsolutePath();
      else { if (sLinux.exists()) sReplace = sLinux.getAbsolutePath(); }
      // Perform conversion first
      sName = sName.replace("\\", "/").toLowerCase().replace("d:/", sReplace + "/");
    }
    try {
      // Perform normalization
      pThis = Paths.get(sName);
      pThis = pThis.normalize();
      // Determine file-separator
      if (pThis.toString().contains("/"))
        fs = "/";
      else
        fs = "\\";
      
      /* ======== OBSOLETE ====
      lThis = LinkOption.NOFOLLOW_LINKS; 
         ======================  */
      // Get the string equivalent and turn it into a File
      File fFile = pThis.toFile();
      // Check if the path exists
      if (!fFile.exists()) {
        String[] arDir;
        // This particular file/directory is not found: attempt dir-by-dir
        if (fs.equals("\\"))
          arDir = fFile.toString().replace("\\", "/").split("/");
        else
          arDir = fFile.toString().split(fs);
        String sTmpPath = "";
        // Action depends on what is inside arDir[0]
        if (arDir[0].endsWith(":")) sTmpPath += arDir[0];
        // Walk the whole directory structure part-for-part
        for (int i=1; i< arDir.length; i++) {
          // Check if this exists
          File fNew = new File(sTmpPath + fs + arDir[i]);
          // Adapt
          fNew = Paths.get(fNew.toString()).normalize().toFile();
          if (!fNew.exists()) {
            // Look for a variant 
            File fOld = new File(sTmpPath);
            if (!fOld.isDirectory()) {
              // This is a problem: we can only look inside directories
            }
            String[] arHere = fOld.list();
            if (arHere == null) {
              Logger.getLogger(FileUtil.class.getName()).log(Level.SEVERE, "arHere is null");
              return "";
            }
            // Find the variant
            boolean bFound = false;
            for (int j=0;j<arHere.length; j++) {
              if (arHere[j].equalsIgnoreCase(arDir[i])) {
                // We found the culprit
                arDir[i] = arHere[j];
                bFound = true;
                break;
              }
            }
            // Check if we found it
            if (!bFound && !arDir[i].contains(".")) {
              // It has not been found, so create the directory
              File fMake = new File(sTmpPath + "/" + arDir[i]);
              fMake.mkdir();
            }
          }
          // Build path
          sTmpPath += "/" + arDir[i];
        }

      }
      
      sName = fFile.toString();
      return sName;
    } catch (Exception ex) {
      Logger.getLogger(FileUtil.class.getName()).log(Level.SEVERE, null, ex);
      return "";
    }
    /* ================= OBSOLETE =========
    try {
      pThis = pThis.toRealPath(lThis);
    } catch (NoSuchFileException | RuntimeException ex) {
      pThis = null;
    } catch (IOException ex) {
      pThis = null;
    }
    try {
      if (pThis==null) {
        // This particular file/directory is not found: attempt dir-by-dir
        String[] arDir = pThis.toString().split("/");
        String sTmpPath = "";
        // Walk the whole directory structure part-for-part
        for (int i=1; i< arDir.length; i++) {
          // Check if this exists
          File fNew = new File(sTmpPath + "/" + arDir[i]);
          if (!fNew.exists()) {
            // Look for a variant 
            File fOld = new File(sTmpPath);
            if (!fOld.isDirectory()) {
              // This is a problem: we can only look inside directories
            }
            String[] arHere = fOld.list();
            if (arHere == null) {
              Logger.getLogger(FileUtil.class.getName()).log(Level.SEVERE, "arHere is null");
              return "";
            }
            // Find the variant
            for (int j=0;j<arHere.length; j++) {
              if (arHere[j].equalsIgnoreCase(arDir[i])) {
                // We found the culprit
                arDir[i] = arHere[j];
              }
            }
          }
          // Build path
          sTmpPath += "/" + arDir[i];
        }
      }
      sName = pThis.toString();
    } catch (Exception ex) {
      Logger.getLogger(FileUtil.class.getName()).log(Level.SEVERE, null, ex);
    }
   return sName;
       ================================ */
  }
  // ERK: added: get all filtered files recursively
  public static void getFileNames(List<String> fileNames, Path dir, String sFilter) {
    // Validate: we can only look inside directories
    if (!dir.toFile().isDirectory()) {
      // add the file to the list
      fileNames.add(dir.toString());
      return;
    }
    // Get all the items inside "dir" that fit "sFilter"
    try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir, sFilter)) {
      // Walk all these items
      for (Path path : stream) {
        // Add the directory to the list to be returned
        fileNames.add(path.toAbsolutePath().toString());
      }
    } catch(IOException e) {
      e.printStackTrace();
    }
    // Get a list of *all* items inside [dir]
    try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      // Walk all the items
      for (Path path : stream) {
        // Is this item a directory?
        if (path.toFile().isDirectory()) {
          // Look for items inside this directory
          getFileNames(fileNames, path, sFilter);
        }
      }
    } catch(IOException e) {
      e.printStackTrace();
    }
  } 
}
