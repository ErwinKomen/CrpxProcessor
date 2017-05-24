package nl.ru.crpx.search;
/*
 * This software has been developed at the "Radboud University"
 *   in order to support the CLARIAH project "ACAD".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import nl.ru.crpx.dataobject.DataObject;
import nl.ru.crpx.dataobject.DataObjectList;
import nl.ru.crpx.dataobject.DataObjectMapElement;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.util.FileUtil;
import nl.ru.util.Json;
import nl.ru.util.json.JSONArray;
import nl.ru.util.json.JSONObject;
import nl.ru.xmltools.Parse;
import nl.ru.xmltools.XmlForest;

/**
 * A "RunTxtList" handles processing of one file for one corpus research project 
 * 
 * @author Erwin R. Komen
 */
public class RunTxtList extends RunAny {
// <editor-fold defaultstate="collapsed" desc="Variables">
  // ========================= Constants =======================================
  static String sCorpusBase = "/etc/corpora/";  // Base directory where corpora are stored
  // ========== Variables needed for txtlist creation job ========================
  Job parentXqJob;                          // The XQ job we are 'under'
  String loc_sLng = "";
  String loc_sPart = "";
  String loc_sExt = "";
// </editor-fold>
  
// <editor-fold defaultstate="collapsed" desc="Class initializer">
  public RunTxtList(ErrHandle oErr, Job jParent, 
          String userId, SearchParameters par) {
    // Make sure the class I extend is initialized
    super(oErr, par);
    // Then make my own initialisations
    try {
      this.jobStatus = "creating";
      this.jobName = "txtlist";
      // Get the parameters from [par]
      loc_sLng = par.getString("lng");
      loc_sExt = par.getString("ext");
      if (par.containsKey("dir")) {
        loc_sPart = par.getString("dir");
      } else {
        loc_sPart = "";
      }
      this.jobStatus = "created";
    } catch (Exception ex) {
      errHandle.DoError("RunTxtList - Initialisation of class fails: ", ex);
    }
  }
// </editor-fold>
  


  /**
   * close
   *    
   * Close all the resources related to this job
   */
  @Override
  public void close() {
    try {
      this.jobStatus = "closed";
    } catch (Exception ex) {
      // Show the error
      errHandle.DoError("RunTxtList: close problem", ex, RunTxtList.class);
    } 
  }

  /**
   * run
   *    
   * Start collecting the txtlist information
   */
  @Override
  public void run() {
    DataObject oBack = null;
    
    try {
      this.jobStatus = "working";
      // Start it up and store the results
      oBack = getTextList(this.loc_sLng, this.loc_sPart, this.loc_sExt);
      if (oBack == null) {
        this.jobBack = null;
        this.jobResult = "empty textlist";
        this.jobStatus = "error";
      } else {
        this.jobBack = oBack;
        // Indicate we are finished
        this.jobStatus = "finished";
      }
    } catch (Exception ex) {
      // Show the error
      errHandle.DoError("RunTxtList: run problem", ex, RunTxtList.class);
    } 
  }
  
  /**
   * getTextList
   *    Get a list of texts from language [sLng], corpus part [sPart],
   *       and extension type [sExtType] (folia or psdx)
   * 
   * @param sLng
   * @param sPart
   * @param sExtType
   * @return 
   */
  public DataObject getTextList(String sLng, String sPart, String sExtType) {
    String sPartPath = "";  // Path to the Lng/Part
    Deque<Path> stack = new ArrayDeque<>();
    JSONObject oTextList = null;
    JSONObject oProg = new JSONObject();
    String sSearch = "*";
    DataObjectMapElement oBack = new DataObjectMapElement();
    String sExtFind = "";
    int iTexts = 0;     // Total number of texts found
    
    try {
      // Make sure we look for what is needed
      if (!sExtType.isEmpty()) sExtFind = CorpusResearchProject.getTextExt(sExtType);   
      
      // We need to have a corpus research project to continue...
      CorpusResearchProject crpThis = new CorpusResearchProject(true);
      // And the one thing that needs to be set in the project is the type
      switch (sExtType) {
        case "psdx":
          crpThis.setForType(XmlForest.ForType.PsdxIndex);
          crpThis.setTextExt(CorpusResearchProject.ProjType.ProjPsdx);
          break;
        case "folia":
          crpThis.setForType(XmlForest.ForType.FoliaIndex);
          crpThis.setTextExt(CorpusResearchProject.ProjType.ProjFolia);
          break;
        default:
          errHandle.DoError("getTextList: unknown extension type ["+sExtType+"]");
          return null;
      }
      // Get a Parse object
      Parse prsThis = new Parse(crpThis, this.errHandle);
      
      // Get the directory from where to search
      Path pRoot = Paths.get(FileUtil.nameNormalize(sCorpusBase), sLng);
      // If [part] is specified, then we need to get a sub directory
      if (sPart != null && !sPart.isEmpty()) {
        // Find the first sub directory containing [sPart] under [pRoot]
        pRoot =Paths.get(FileUtil.findFileInDirectory(pRoot.toString(), sPart));
      }
      // Check to see if a file-list .json file already exists
      String sTextListName = (sExtType.isEmpty()) ? "textlist-all" : "textlist-" + sExtType;
      Path pJsonTextList = Paths.get(pRoot.toString(), sTextListName+".json");
      if (!Files.exists(pJsonTextList)) {
        // Create an array that will hold all
        JSONArray arDir = new JSONArray();
        // Create an array for the Genre and the Subtype items
        JSONArray arGenre = new JSONArray();
        JSONArray arSubtype = new JSONArray();
        int iItemId =-1; 
        // We need to have an idea of the file extensions that are possible
        // (this is needed when sExtType or sExtFind is empty)
        List<String> lExtList = CorpusResearchProject.getTextExtList();
        // Start creating this list
        stack.push(pRoot);
        while (!stack.isEmpty()) {
          // Get all the items inside "dir"
          Path pThis = stack.pop();
          try(DirectoryStream<Path> streamSub = Files.newDirectoryStream(pThis, sSearch)) {
            // Create an object for this directory
            JSONObject oDirContent = new JSONObject();
            JSONArray arContent = new JSONArray();
            
            int iDirCount = 0;
            oDirContent.put("count", iDirCount);
            oDirContent.put("path", pThis.toAbsolutePath().toString());
            oDirContent.put("list", arContent);
            // FInd the correct extension for this sub directory
            String sExt;
            // Walk all these items
            for (Path pathSub : streamSub) {
              if (Files.isDirectory(pathSub)) {
                stack.push(pathSub);
              } else {
                String sFile = pathSub.getFileName().toString();
                // Check if the file has an extension in the list of allowed ones
                if (sExtType.isEmpty() || sExtFind.isEmpty()) 
                  sExt = getExtensionInList(lExtList, sFile);
                else
                  sExt = (sFile.endsWith(sExtFind)) ? sExtFind : "";
                if (!sExt.isEmpty()) {
                  // We found a match -- get the complete path
                  String sSubThis = pathSub.toAbsolutePath().toString();
                  String sName = sFile.substring(0, sFile.length() - sExt.length());
                  // Add this to the list
                  JSONObject oFile = new JSONObject();
                  oFile.put("name", sName);
                  oFile.put("ext", sExt);
                  // Get the metadata information from this file:
                  //   title, genre, author, date, subtype, size
                  JSONObject oMeta = prsThis.getMetaInfo(sSubThis);
                  // Add all the metadata to [oFile]
                  Iterator keys = oMeta.keys();
                  while (keys.hasNext()) {
                    String sKey = keys.next().toString();
                    String sValue = "";
                    switch(sKey) {
                      case "size":
                        oFile.put(sKey, oMeta.getInt(sKey));
                        break;
                      case "subtype":
                        sValue = oMeta.getString(sKey);
                        iItemId = getArrayIndex(arSubtype, sValue);
                        oFile.put(sKey, iItemId);
                        break;
                      case "genre":
                        sValue = oMeta.getString(sKey);
                        iItemId = getArrayIndex(arGenre, sValue);
                        oFile.put(sKey, iItemId);
                        break;
                      default:
                        oFile.put(sKey, oMeta.getString(sKey));
                        break;
                    }
                  }
                  
                  // Global text counter
                  iTexts++;                  
                  // Add this item to the array
                  arContent.put(oFile);
                  iDirCount++;
                  // Adapt the status object
                  oProg.put("total", iTexts);
                  oProg.put("dirs", iDirCount);
                  oProg.put("last", sName);
                  oProg.put("ext", sExt);
                  this.setJobCount(oProg);
                }
              }
            }
            // CHeck if anything has been added in this directory
            if (iDirCount > 0) {
              // Then adapt this object
              oDirContent.put("count", iDirCount);
              oDirContent.put("list", arContent);
              // Add this object to the list
              arDir.put(oDirContent);
            }
          }
        }
        // Create a json object with the contents
        JSONObject oTotal = new JSONObject();
        oTotal.put("paths", arDir.length());
        oTotal.put("texts", iTexts);
        oTotal.put("genre", arGenre);
        oTotal.put("subtype", arSubtype);
        oTotal.put("list", arDir);
        // Store this object into a file
        Json.write(oTotal, pJsonTextList.toFile());
        
      }
      this.jobStatus = "reading";
      // We now should have the correct file stored -- load it
      oTextList = Json.read(pJsonTextList.toFile());
      
      // Transform this list into a DataObject
      int iPaths = oTextList.getInt("paths");
      oBack.put("paths", iPaths);
      oBack.put("texts", oTextList.getInt("texts"));
      
      // Treat the list of genre
      DataObjectList lGenre = new DataObjectList("genre");
      JSONArray arGenre = oTextList.getJSONArray("genre");
      for (int i=0;i<arGenre.length();i++ ) { lGenre.add(arGenre.getString(i)); }
      oBack.put("genre", lGenre);
      
      // Treat the list of subtype
      DataObjectList lSubtype = new DataObjectList("subtype");
      JSONArray arSubtype = oTextList.getJSONArray("subtype");
      for (int i=0;i<arSubtype.length();i++ ) { lSubtype.add(arSubtype.getString(i)); }
      oBack.put("subtype", lSubtype);
      
      // Treat the list of file-info-objects
      DataObjectList arList = new DataObjectList("list");
      iTexts = 0;
      // Fill this list
      for (int i=0; i < iPaths; i++) {
        JSONObject oDir = oTextList.getJSONArray("list").getJSONObject(i);
        DataObjectMapElement oDataDir = new DataObjectMapElement();
        oDataDir.put("count", oDir.getInt("count"));
        oDataDir.put("path", oDir.getString("path"));
        // Get a list of items for this path
        DataObjectList arDirList = new DataObjectList("list");
        for (int j=0;j< oDir.getInt("count"); j++) {
          JSONObject oFile = oDir.getJSONArray("list").getJSONObject(j);
          DataObjectMapElement oDataFile = new DataObjectMapElement();
          
          iTexts += 1;
          // Extract all the metadata from [oFile]
          Iterator keys = oFile.keys();
          while (keys.hasNext()) {
            String sKey = keys.next().toString();
            switch (sKey) {
              case "size":
              case "genre":
              case "subtype":
                oDataFile.put(sKey, oFile.getInt(sKey));
                break;
              default:
                // All the other keys are of type STRING
                oDataFile.put(sKey, oFile.getString(sKey));
                break;
            }
          }
          
          // Adapt the status object
          oProg.put("total", iTexts);
          oProg.put("dirs", i);
          oProg.put("last", oDataFile.get("name"));
          this.setJobCount(oProg);
          
          // oDataFile.put("name", oFile.getString("name"));
          // oDataFile.put("ext", oFile.getString("ext"));
          // Add this file object to the list
          arDirList.add(oDataFile);
        }        
        // Add the lis tof items to the current datadir object
        oDataDir.put("list", arDirList);        
        // Add this directory content to the overall list
        arList.add(oDataDir);
      }
      
      // Place the overal list into the dataobject
      oBack.put("list", arList);
      
      // Return the back object
      return oBack;      
    } catch (Exception ex) {
      errHandle.DoError("Could not get a list of texts", ex, RunTxtList.class);
      return null;
    }
  }
  
  private int getArrayIndex(JSONArray arThis, String sValue) {
    int i;
    
    try {
      // Check if it is already in the list
      for (i=0;i<arThis.length();i++) {
        if (arThis.getString(i).equals(sValue)) {
          return i;
        }
      }
      // It is not in the list, so add it
      arThis.put(sValue);
      // REturn the index, which is the size minus one
      return (arThis.length()-1);
    } catch (Exception ex) {
      errHandle.DoError("RunTxtList.getArrayIndex problem", ex, RunTxtList.class);
      return -1;
    }
  }

  /**
   * getExtensionInList -- 
   *    Check if [sName] has an extension in the list [lExtList]
   *    If this is so, then return that extension
   *    Otherwise return ""
   * 
   * @param lExtList
   * @param sName
   * @return 
   */
  private String getExtensionInList(List<String> lExtList, String sName)  {
    boolean bMatch = false;
    String sFound = "";
    
    try {
      // Check if the file extension matches
      for (String sExt : lExtList) {
        if (sName.endsWith(sExt)) {
          bMatch = true; 
          sFound = sExt;
          break;
        }
      }   
      return sFound;      
    } catch (Exception ex) {
      errHandle.DoError("Could not determine the extension", ex, RunTxtList.class);
      return sFound;
    }
  }
  
  
  
}
