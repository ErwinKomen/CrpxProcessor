/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
// This package is specifically for the command-line implementation of CRPP
package nl.ru.crpx.cmd;

// <editor-fold defaultstate="collapsed" desc="Import">
import java.io.*;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.project.CrpGlobal;
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
  // =================== public variables ====================================
  public static String strProject = "";   // Short name of the project to execute
  public static File flProject = null;    // The project we are working with
  public static File dirInput = null;     // Main directory where the psdx files are located
  // =================== instance variables ==================================
  // private CorpusResearchProject prjTmp = new CorpusResearchProject();
  // private static CrpGlobal objGen = new CrpGlobal();         // This is a local copy
  // private static CorpusResearchProject prjThis = objGen.getCrpx();
  private static CorpusResearchProject prjThis = new CorpusResearchProject();
  // =================== main code start =====================================
/* ---------------------------------------------------------------------------
   Name:    main
   Goal:    Main entry point of the processor
   Parameters:  @args - command line argument vector
   History:
   17/10/2014   ERK Created
   --------------------------------------------------------------------------- */
    public static void main(String[] args) throws Exception {
      // Try to configure
      try {
        BasicConfigurator.configure();
      } catch (Exception e) {
        // Nothing here
        logger.error("Log probelm", e);
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
      // Try to load the project
      if (!prjThis.Load(strProject)) {
        logger.error("Could not load project " + strProject);
        // Try to show the list of errors, if there is one
        String sMsg = prjThis.errHandle.getErrList().toString();
        logger.error("List of errors:\n" + sMsg);
        return;
      }
      
      // Show the project has been loaded
      logger.debug("Successfully loaded project " + 
              prjThis.getName() + " (type=" + prjThis.getProjectType() + ")");

      // Execute queries
      if (!prjThis.Execute()) {
        logger.error("The queries could not be executed");
      }
      // Check for interrupt
      if (prjThis.errHandle.bInterrupt) {
        logger.error("The program has been interrupted");
        return;
      }
      
      // Exit program normally
      logger.debug("Ready!");
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

}
