/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
// This package is specifically for the command-line implementation of CRPP
package nl.ru.crpx.cmd;

// <editor-fold defaultstate="collapsed" desc="Import">
import java.io.*;
import java.nio.file.Files;
import java.util.Properties;
import nl.ru.crpx.dataobject.DataFormat;
import nl.ru.crpx.dataobject.DataObject;
import nl.ru.crpx.dataobject.DataObjectPlain;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.project.PrjTypeManager;
import nl.ru.crpx.search.SearchManager;
import nl.ru.crpx.search.SearchParameters;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.tools.FileIO;
import nl.ru.util.FileUtil;
import nl.ru.util.Json;
import nl.ru.util.json.JSONObject;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
// </editor-fold>
/* ---------------------------------------------------------------------------
   Name:    CrpxProcessor
   Goal:    Process "crpx" files that contain a corpus research project (xml)
   History:
   17/10/2014   ERK Created
   --------------------------------------------------------------------------- */
public class CrpxProcessor {
  // This class uses a logger
  private static final Logger logger = Logger.getLogger(CrpxProcessor.class);
  private static final ErrHandle errHandle = new ErrHandle(CrpxProcessor.class);
  // =================== public variables ====================================
  // public static String strProject = "";   // Short name of the project to execute
  public static File flProject = null;    // The project we are working with
  public static File dirInput = null;     // Main directory where the psdx files are located
  // =================== instance variables ==================================
  // private CorpusResearchProject prjTmp = new CorpusResearchProject();
  // private static CrpGlobal objGen = new CrpGlobal();         // This is a local copy
  // private static CorpusResearchProject prjThis = objGen.getCrpx();
  // private static CorpusResearchProject prjThis;
  // private static String userId;
  private static JSONObject config;           // Configuration object
  private static SearchManager searchManager; // The search manager we make
  private static PrjTypeManager prjTypeManager;
  // =================== Simple getters =======================================
  public SearchManager getSearchManager() {return searchManager;}
  public PrjTypeManager getPrjTypeManager() { return prjTypeManager;}
  public JSONObject getConfig() { return config;}
  // =================== main code start =====================================
/* ---------------------------------------------------------------------------
   Name:    main
   Goal:    Main entry point of the processor
   Parameters:  @args - command line argument vector
   History:
   17/10/2014   ERK Created
   --------------------------------------------------------------------------- */
    public static void main(String[] args) throws Exception {
      String indexName = "";    // The request that is being made
      String strProject = "";   // Name of the project to execute
      
      // Try to configure
      try {
        BasicConfigurator.configure();
      } catch (Exception e) {
        // Nothing here
        logger.error("Log problem", e);
      }
      // Check for no arguments
      if (args.length==0) {
        // Tell the user that there is a problem
        System.err.println("Passing parameter to CrpxProcessor: argument needed!");
        usage(); return;
      }
      // Parse the input parameters
      for (int i = 0; i < args.length; i++) {
        // Isolate and trim this argument
        String arg = args[i].trim();
        if (i > 0 && arg.startsWith("--")) {
          // Get the option name following --
          String name = arg.substring(2);
          // Check all possible options
          switch (name.toLowerCase()) {
            case "inputdir":
              // We next expect the actual directory to follow
              i++;
              if (i>args.length-1) {
                logger.error("Option --inputdir should be followed by a directory");
                usage(); return;
              } else {
                // Get the input directory
                dirInput = new File(args[i]);
                // Check it up
                if (!dirInput.exists()) {
                // The project cannot be read/opened
                logger.error("Cannot open input directory: " + dirInput);
                usage(); return;
                }
              }
              break;
            case "help":
              usage(); return;
            default:
              logger.error("Unknown option --" + name);
              usage(); return;
          }
        } else if (i==0) {
          // Get the project file string
          strProject = arg;
        }
      }
      
      // Preliminary: say the request is "execute"
      indexName = "execute";
      
      // Initialize
      if (!init()) { logger.error("Could not initialize"); return; }
            
      // Handle the request
      if (!processRequest(indexName, strProject))  { logger.error("Could not process request"); return; }

      // Exit program normally
      logger.debug("Ready!");
    }
    
/* ---------------------------------------------------------------------------
   Name:    init
   Goal:    Read the configuration file
   History:
   12/may/2015   ERK Created
   --------------------------------------------------------------------------- */
  private static boolean init() {
    // Perform initialisations related to this project-type using the config file
    // Read it from the class path
    String configFileName = "crpp-settings.json";
    // InputStream is = getClass().getClassLoader().getResourceAsStream();
    InputStream is = FileIO.getProjectDirectory(CrpxProcessor.class, configFileName);
    // InputStream is = FileUtil.getInputStream(configFileName);
    if (is == null) {
      configFileName = "crpp-settings-default.json.txt";  // Internal default
      // is = FileUtil.getInputStream(configFileName);
      is = FileIO.getProjectDirectory(CrpxProcessor.class, configFileName);
      if (is == null) {
        // We cannot continue...
        return errHandle.DoError("Could not find " + configFileName + "!");
      }
    }
    // Process input stream with configuration
    try {
      try {
        config = Json.read(is);
      } finally {
        is.close();
      }
    } catch (Exception e) {
      return errHandle.DoError("Error reading JSON config file: " +  e.getMessage());
    }

    // Create a new search manager
    searchManager = new SearchManager(config);
    
    // Create a new project type manager
    prjTypeManager = new PrjTypeManager(config);
    
    // Show that we are ready
    logger.info("CrpxProcessor is initialized.");

    // Return positively
    return true;
  }
  
/* ---------------------------------------------------------------------------
   Name:    processRequest
   Goal:    Process the request
   History:
   12/may/2015   ERK Created
   --------------------------------------------------------------------------- */
  private static boolean processRequest(String indexName , String strProject) {
    DataObject response;  // The response of this program
    boolean prettyPrint = true;
    String callbackFunction = "";

    try {
      // Create room for the project
      CorpusResearchProject prjThis = new CorpusResearchProject();
      // Load the project
      if (!prjThis.Load(strProject)) {
        errHandle.DoError("Could not load project " + strProject);
        // Try to show the list of errors, if there is one
        String sMsg = prjThis.errHandle.getErrList().toString();
        errHandle.DoError("List of errors:\n" + sMsg);
        return false;
      }
      // Show the project has been loaded
      errHandle.debug("Successfully loaded project " + 
              prjThis.getName() + " (type=" + prjThis.getProjectType() + ")");
      // Get an object of the CrpxProcessor
      CrpxProcessor caller = new CrpxProcessor();
      // Elicit a response
      response = RequestHandler.handle(caller, indexName, prjThis);
      // Determine response output type
      DataFormat outputType = response.getOverrideType(); // some responses override the user's request (i.e. article XML)

      String rootEl = "crpxResponse";
      // TODO: handle DataObjectPlain
      if (response instanceof DataObjectPlain && !((DataObjectPlain) response).shouldAddRootElement()) {
        // Plain objects sometimes don't want root objects (e.g. because they're 
        // full XML documents already)
        rootEl = null;
      }
      // Write the response
      OutputStreamWriter out = new OutputStreamWriter(System.out, "utf-8");
      // Set some default parameters
      response.serializeDocument(rootEl, out, outputType, prettyPrint, callbackFunction);
      out.flush();

      // Return positively
      return true;
    } catch (RuntimeException | IOException ex) {
      return errHandle.DoError("Unable to process request", ex, CrpxProcessor.class);
    }
  }
/* ---------------------------------------------------------------------------
   Name:    usage
   Goal:    Show the user how CrpxProcessor should be called
   History:
   17/10/2014   ERK Created
   --------------------------------------------------------------------------- */
  private static void usage() {
    System.out
    .println("Usage:\n"
      + "  CrpxProcessor project [options]\n"
      + "\n"
      + "Options:\n"
      + "  --inputdir <dir>   directory where psdx input files are located\n"
      + "  --help             this help information\n");
  }
  /**
   * Get the search-related parameters from the request object.
   *
   * This ignores stuff like the requested output type, etc.
   *
   * Note also that the request type is not part of the SearchParameters, so from looking at these
   * parameters alone, you can't always tell what type of search we're doing. The RequestHandler subclass
   * will add a jobclass parameter when executing the actual search.
   *
   * @param request the kind of action
   * @return the unique key
   */
  public SearchParameters getSearchParameters(String request) {
    try {
      // Set up a parameters object
      SearchParameters param = new SearchParameters(searchManager);
      Properties arProp = System.getProperties();

      // Walk all relevant search parameters
      for (String name: searchManager.getSearchParameterNames()) {
        String value = "";  // Default value
        switch (name) {
          case "resultsType": value = "XML"; break;
          case "waitfortotal": value= "no"; break;
          case "tmpdir": 
            // Create temporary file
            File fTmp = File.createTempFile("tmp", ".txt");
            // Get the path of this file
            String sPath = fTmp.getAbsolutePath();
            value = sPath.substring(0,sPath.lastIndexOf(File.separator));
            // value = Files.createTempDirectory("tmpdir").toString(); // System.getProperty("tmpdir");
        }
        // Check if it has some kind of value
        if (value.length() == 0) continue;
        // Since it really has a value, add it to the parameters object
        param.put(name, value);
      }
      // Return the object that contains the parameters
      return param;
    } catch (Exception ex) {
      errHandle.DoError("could not get search parameters", ex, CrpxProcessor.class);
      return null;
    }
  }

}
