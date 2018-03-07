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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utilities for working with files
 */
public class FileUtil {
  /**
   * The default encoding for opening files.
   */
  private static String defaultEncoding = "utf-8";
  private int iLineCount = 0;

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
      // If it exists: delete it
      if (file.exists()) file.delete();
      // Write content to it
      return new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                            file), encoding)));
    } catch (FileNotFoundException | UnsupportedEncodingException e) {
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
    } catch (FileNotFoundException | UnsupportedEncodingException e) {
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
    } catch (FileNotFoundException | UnsupportedEncodingException e) {
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
  public List<String> readLines(String filePath) {
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
  public List<String> readLines(String filePath, String encoding) {
          return readLines(new File(filePath), encoding);
  }

  /**
   * Read a file into a list of lines
   *
   * @param inputFile
   *            the file to read
   * @return list of lines
   */
  public List<String> readLines(File inputFile) {
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
   * @history
   *    15/oct/2015 ERK removed "line.trim()", since this is undesired
   */
  public List<String> readLines(File inputFile, String encoding) {
    try {
      List<String> result = new ArrayList<String>();
      BufferedReader in = openForReading(inputFile, encoding);
      try {
        String line;
        while ((line = in.readLine()) != null) {
          result.add(line);
          // UNDESIRED: result.add(line.trim());
        }
      } finally {
        in.close();
      }
      iLineCount = result.size();
      // Check the first line for the BOM and remove it
      if ( iLineCount > 0 && encoding.equals("utf-8")) {
        result.set(0, result.get(0).replace("\uFEFF", ""));
      }
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
  public String readFile(File file) {
    return StringUtil.join(readLines(file), "\n");
  }
  public String readFile(File file, String encoding) {
    return StringUtil.join(readLines(file, encoding), "\n");
  }
  // ERK: added readFile with a filename string as input
  public String readFile(String sName) {
    File file = new File(sName);
    List<String> lCombi = readLines(file);
    return StringUtil.join(lCombi, "\n");
  }
  // ERK: added readFile with a filename string as input
  public String readFile(String sName, String encoding) {
    File file = new File(sName);
    List<String> lCombi = readLines(file, encoding);
    return StringUtil.join(lCombi, "\n");
  }
  public int getLinesRead() { return iLineCount;}

  /**
   * Write a String to a file.
   * @param file the file to write
   * @param data what to write to the file
   */
  public static void writeFile(File file, String data) {
    try (PrintWriter out = openForWriting(file, "utf-8")) {
      out.print(data);
    }
  }
  // ERK: added writeFile with the encoding as option and a string filename as first argument
  public static void writeFile(String sName, String data, String encoding) {
    File file = new File(sName);
    try (PrintWriter out = openForWriting(file, encoding)) {
      out.print(data);
    }
  }
  // ERK: added string-to-string file name normalization using .nio
  /**
   * nameNormalize
   *    Normalize the file @sName according to Windows, Mac or Linux practice
   * 
   * @param sName
   * @return 
   */
  public static String nameNormalize(String sName) {
    String[] arUrlParts = {"/ru/corpusstudio", "/fdl-homedirs"};
    File sMac = new File("/Users/erwin/");
    File sLinux = new File("/etc/project/");
    Path pThis = null;
    LinkOption lThis;
    String sReplace = "/";
    
    String fs = System.getProperty("file.separator");
    // Check if we need Windows >> Mac/nix conversion
    if (fs.equals("/") && sName.contains("\\")) {
      // Check the path
      if (sMac.exists())  sReplace = sMac.getAbsolutePath();
      else { if (sLinux.exists()) sReplace = sLinux.getAbsolutePath(); }
      // First normalize backslash to slash
      sName = sName.replace("\\", "/");
      // Next take off any drive specification
      if (sName.matches("\\w[:].*")) {
        // Remove drive letter
        sName = sName.substring(2);
      }
    }
    // Check for URL-style name, which we cannot accept
    if (sName.startsWith("//") || sName.startsWith("\\\\")) {
      int iRuCrpStudio; boolean bFound = false;
      // Try to replace the URL-style header
      for (int i=0;i<arUrlParts.length;i++) {
        // Check if we have a match -- either with forward or backward slashes
        iRuCrpStudio = sName.toLowerCase().indexOf(arUrlParts[i]);
        if (iRuCrpStudio <0) iRuCrpStudio = sName.toLowerCase().indexOf(arUrlParts[i].replace('/', '\\'));
        // If there is a match...
        if (iRuCrpStudio >=0) {
          // Re-combine, starting from /etc/project
          sName = "/etc/project" + sName.substring(iRuCrpStudio + arUrlParts[i].length());
          // Indicate we found it and then break out of the for loop
          bFound = true;
          break;
        }
      }
      // What if we didn't find it?
      if (!bFound) {
        // Remove the first URL-style part
        sName = sName.substring(2);
        int iFirst = sName.replace('\\', '/').indexOf("/");
        if (iFirst >0) sName = sName.substring(iFirst);
      }
    } else if (!sName.startsWith("//") && sName.startsWith("/") && 
            !sReplace.equals("/")) {
      sName = sReplace + sName;
    }

    try {
      // check if it starts with one backslash
      if (fs.equals("\\") && (sName.startsWith("\\") || sName.startsWith("/"))) {
        // Insert the D-drive before it
        sName = "D:" + sName;
      }
      // Perform normalization using the NIO interface
      pThis = Paths.get(sName).normalize();
      // Determine file-separator
      fs =  (pThis.toString().contains("/")) ? "/" : "\\";
      
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
        // Initialize dir-by-dir walking...
        String sTmpPath = "";
        // Action depends on what is inside arDir[0]
        if (arDir[0].endsWith(":")) sTmpPath += arDir[0];
        // Walk the whole directory structure part-for-part
        for (int i=1; i< arDir.length; i++) {
          // Skip empty subdirectories
          if (!arDir[i].isEmpty()) {
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
                Logger.getLogger(FileUtil.class.getName()).log(Level.SEVERE, 
                        "arHere is null. Source=[{0}]. Attempt breaks at dir=[{1}]", 
                        new Object[]{sName, fNew.toString()});
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
  
  /**
   * findFileInDirectory_OLD
   *    Given directory sDir, find ONE (!!!) file named [sFile] in it
   *
   * 
   * @param sDir
   * @param sFile
   * @return handle to [sFile]
   */
  public static String findFileInDirectory_OLD(String sDir, String sFile) {
    List<String> lRes = new ArrayList<>();
    String sBack = "";
    
    try {
      // Check for item [sFile] inside "sDir"
      Path dir = Paths.get(sDir);
      try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir, sFile)) {
        // Do we have a hit?        
        for (Path path : stream) {
          // Return the FIRST file we get
          sBack= path.toAbsolutePath().toString();
          break;
        }
      } catch(IOException e) {
        e.printStackTrace();
      }
      if (!sBack.isEmpty()) return sBack;
      // Process all directories inside [sDir]
      try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
        for (Path path : stream) {
          // Is this item a directory?
          if (path.toFile().isDirectory()) {
            // Look for items inside this directory
            String sTry = findFileInDirectory_OLD(path.toString(), sFile);
            if (!sTry.isEmpty()) {
              sBack = sTry;
              break;
            }
          }
        }
      } catch(IOException e) {
        e.printStackTrace();
      }
      // Getting here means we have no success
      return sBack;
    } catch(Exception e) {
      e.printStackTrace();
      return "";
    }
  }
  
  /**
   * findFileInDirectory
   *    Given directory sDir, find ONE (!!!) file named [sFile] in it
   *
   * 
   * @param sDir
   * @param sFile
   * @return handle to [sFile]
   */
  public static String findFileInDirectory(String sDir, String sFile) {  
    String sBack = "";
    try {
      // Initialize starting directory
      Path startDir = Paths.get(sDir);
      // Are we looking for a directory within a directory?
      if (sFile.contains(".")) {
        // Probably a file, so... Set a finder
        Finder finder = new Finder(sFile);
        // Walk the file tree
        // Files.walkFileTree(startDir, finder);

        Files.walkFileTree(startDir, EnumSet.of(FileVisitOption.FOLLOW_LINKS) , Integer.MAX_VALUE, finder);
        // Get the first result
        sBack = finder.found();
      } else {
        // Assume we are looking for a directory
        DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
          public boolean accept(Path file) throws IOException {
            return (file.toFile().isDirectory());
          }
        };
        sBack = FileUtil.getDirInDir(startDir, sFile, filter);
      }
      
      // Getting here means we have no success
      return sBack;
    } catch(Exception e) {
      Logger.getLogger(FileUtil.class.getName()).log(Level.SEVERE, 
                        "findFileInDirectory problem at dir [{0}], file [{1}]", 
                        new Object[]{sDir, sFile});
      e.printStackTrace();
      return "";
    }
  }
  
  static String getDirInDir(Path pStart, String sTarget, DirectoryStream.Filter<Path> filter) throws IOException  {
    // Look through the directory
    DirectoryStream<Path> stream = Files.newDirectoryStream(pStart, filter);
    for (Path path : stream) {
      if (path.getFileName().toString().equals(sTarget)) {
        return path.toAbsolutePath().toString();
      } else {
        // Try follow this link
        String sAttempt = FileUtil.getDirInDir(path, sTarget, filter);
        if (!sAttempt.isEmpty()) {
          return sAttempt;
        }
      }
    }
    // Getting here means: no success
    return "";
  }
  
  
  /**
   * decompressGzipFile -- 
   *    Decompress a .gz file into a normal file.
   * 
   * See: http://www.journaldev.com/966/java-gzip-example-compress-and-decompress-file-in-gzip-format-in-java
   * 
   * @param gzipFile
   * @param newFile 
   */
  public static void decompressGzipFile(String gzipFile, String newFile) {
    try {
      FileInputStream fis = new FileInputStream(gzipFile);
      GZIPInputStream gis = new GZIPInputStream(fis);
      FileOutputStream fos = new FileOutputStream(newFile);
      byte[] buffer = new byte[1024];
      int len;
      while((len = gis.read(buffer)) != -1){
          fos.write(buffer, 0, len);
      }
      //close resources
      fos.close();
      gis.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
  /**
   * decompressGzipString -- 
   *    Decompress a .gz file into a String.
   * 
   * See: http://www.journaldev.com/966/java-gzip-example-compress-and-decompress-file-in-gzip-format-in-java
   * 
   * @param gzipFile
   * @return String version of the file
   */
  public static String decompressGzipString(String gzipFile) {
    try {
      FileInputStream fis = new FileInputStream(gzipFile);
      ByteArrayOutputStream bos;
      try (GZIPInputStream gis = new GZIPInputStream(fis)) {
        bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while((len = gis.read(buffer)) != -1){
          bos.write(buffer, 0, len);
        } 
        //close resources
        bos.close();
      }
      // Convert byte output into string
      return bos.toString(defaultEncoding);
    } catch (IOException e) {
      e.printStackTrace();
      return "";
    }

  }
  
  /**
   * compressGzipFile --
   *    Compress a file into a .gz file
   * 
   * See: http://www.journaldev.com/966/java-gzip-example-compress-and-decompress-file-in-gzip-format-in-java
   * 
   * @param file
   * @param gzipFile 
   */
  public static void compressGzipFile(String file, String gzipFile) {
    try {
      // Remove destination file if it exists
      File fDst = new File(gzipFile);
      if (fDst.exists()) {
        fDst.delete();
      }
      try (FileInputStream fis = new FileInputStream(file); 
           FileOutputStream fos = new FileOutputStream(gzipFile)) {
        GZIPOutputStream gzipOS = new GZIPOutputStream(fos);
        byte[] buffer = new byte[1024];
        int len;
        while((len=fis.read(buffer)) != -1){
            gzipOS.write(buffer, 0, len);
        }
        //close resources
        gzipOS.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * compressGzipFile --
   *    Compress a file into a .gz file
   * 
   * See: http://www.journaldev.com/966/java-gzip-example-compress-and-decompress-file-in-gzip-format-in-java
   * 
   * @param sInput
   * @param gzipFile 
   */
  public static void compressGzipString(String sInput, String gzipFile) {
    try {
      try (InputStream fis = new ByteArrayInputStream(sInput.getBytes(StandardCharsets.UTF_8)); 
           FileOutputStream fos = new FileOutputStream(gzipFile); 
           GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {
        byte[] buffer = new byte[1024];
        int len;
        
        // Loop through buffer
        while((len=fis.read(buffer)) != -1){
            gzipOS.write(buffer, 0, len);
        }
        //close resources
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }  
  
  /**
   * Finder - Helper class to find files in a directory fast
   */
  public static class Finder
    extends SimpleFileVisitor<Path> {

    private final PathMatcher matcher;
    private int numMatches = 0;
    private String sFound = "";   // Full path of the file found

    Finder(String pattern) {
      matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
    }

    // Compares the glob pattern against
    // the file or directory name.
    boolean find(Path file) {
      Path name = file.getFileName();
      if (name != null && matcher.matches(name)) {
        sFound = file.toAbsolutePath().toString();
        numMatches++;
        return true;
      }
      return false;
    }
    
    String found() {
      return sFound;
    }

    // Prints the total number of
    // matches to standard out.
    void done() {
        System.out.println("Matched: "
            + numMatches);
    }

    // Invoke the pattern matching
    // method on each file.
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
      if (find(file))
        return TERMINATE;
      else
        return CONTINUE;
    }

    // Invoke the pattern matching
    // method on each directory.
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
      find(dir);
      return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
      System.err.println(exc);
      return CONTINUE;
    }
  }    
}


