/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
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
  public static File dirOutput = null;    // Directory where the output of this query will be put
  public static File dirQuery = null;     // Directory where the queries (temporarily) are put
  public static File flDbase = null;      // Input file for databaseproject
  // =================== instance variables ==================================
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
      String sUser = "";        // Pass on user name (or keep empty)
      int iMaxParJobs=0;        // Max number of parallel XqF jobs
      
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
      // Initialise
      dirOutput = null; dirInput = null; dirQuery = null;
      // Parse the input parameters
      for (int i = 0; i < args.length; i++) {
        // Debugging
        logger.debug("Argument #" + i + " = [" + args[i].trim() + "]");
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
            case "dbase": // Specify a database as input file
              // Validate argument
              i++; if (i>args.length-1) {logger.error("Option --dbase should be followed by a database location"); usage(); return; }
              // Get the argument
              String sDbaseLoc = args[i];
              dirInput = new File(FileIO.getDirectory(sDbaseLoc));
              flDbase = new File(sDbaseLoc);
              break;
            case "outputdir":
              // We next expect the actual directory to follow
              i++;
              if (i>args.length-1) {
                logger.error("Option --outputdir should be followed by a directory");
                usage(); return;
              } else {
                // Get the input directory
                dirOutput = new File(args[i]);
                // Check it up
                if (!dirOutput.exists()) {
                // The project cannot be read/opened
                logger.error("Cannot open output directory: " + dirOutput);
                usage(); return;
                }
              }
              break;
            case "querydir":
              // We next expect the actual directory to follow
              i++;
              if (i>args.length-1) {
                logger.error("Option --querydir should be followed by a directory");
                usage(); return;
              } else {
                // Get the input directory
                dirQuery = new File(args[i]);
                // Check it up
                if (!dirQuery.exists()) {
                // The project cannot be read/opened
                logger.error("Cannot open query directory: " + dirQuery);
                usage(); return;
                }
              }
              break;
            case "maxparjobs":  // define the maximum number of XqF jobs in parallel
              // The number should follow
              i++;
              if (i>args.length-1) {
                logger.error("Option --maxparjobs should be followed by a number");
                usage(); return;
              } else {
                // Get the number
                iMaxParJobs = Integer.parseInt(args[i]);
              }
              break;
            case "user":
              // User name must follow
              i++;
              if (i>args.length-1) {
                logger.error("Option --user should be followed by a single name");
                usage(); return;
              } else {
                // Get the user name
                sUser = args[i];
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
      
      // Possibly adapt the max number of parallel jobs
      if (iMaxParJobs>0) {
        prjTypeManager.setMaxParJobs(iMaxParJobs);
      }
            
      // Handle the request
      if (!processRequest(indexName, strProject))  { logger.error("Could not process request"); return; }
      
      // Check on the progress of the 

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
    String sInputDir;
    String sOutputDir;
    String sQueryDir;

    try {
      // Create room for the project
      CorpusResearchProject prjThis = new CorpusResearchProject(true);
      // Initialize directories
      sInputDir = (dirInput == null) ? "" : dirInput.getAbsolutePath();
      sOutputDir = (dirOutput == null) ? "" : dirOutput.getAbsolutePath();
      sQueryDir = (dirQuery == null) ? "" : dirQuery.getAbsolutePath();
      errHandle.debug("inputdir=[" + sInputDir + "]");
      errHandle.debug("outputdir=[" + sOutputDir + "]");
      errHandle.debug("queryputdir=[" + sQueryDir + "]");
      // Load the project
      if (!prjThis.Load(strProject, sInputDir, sOutputDir, sQueryDir)) {
        errHandle.DoError("Could not load project " + strProject);
        // Try to show the list of errors, if there is one
        String sMsg = prjThis.errHandle.getErrList().toString();
        errHandle.DoError("List of errors:\n" + sMsg);
        return false;
      }
      // Is a database as input specified?
      if (flDbase != null) {
        prjThis.setSource(flDbase.toString());
      }
      // Show the project has been loaded
      errHandle.debug("Successfully loaded project " + 
              prjThis.getName() + " (type=" + prjThis.getProjectType() + ")");
      // Show the number of parallel jobs
      errHandle.debug("Maxparjobs = " + prjTypeManager.getMaxParJobs());
      // Get an object of the CrpxProcessor
      CrpxProcessor caller = new CrpxProcessor();
      // Elicit a response
      response = RequestHandler.handle(caller, indexName, prjThis);
      // Set the response's output type: we'll experiment with JSON
      DataFormat outputType = DataFormat.JSON;
      // DataFormat outputType = response.getOverrideType(); // some responses override the user's request (i.e. article XML)

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
    System.out.println("Usage:\n"
      + "  CrpxProcessor project [options]\n"
      + "\n"
      + "Options:\n"
      + "  --maxparjobs <n>   maximum number of parallel XqF jobs\n"
      + "  --inputdir <dir>   directory where psdx input files are located\n"
      + "  --outputdir <dir>  directory where psdx output files are put\n"
      + "  --querydir <dir>   directory where intermediate query files are kept\n"
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
