/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.tools;
import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.project.Execute;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

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
  public static General objGen = new General();
  public static CorpusResearchProject prjThis = new CorpusResearchProject(objGen);
  // =================== main code start =====================================
/* ---------------------------------------------------------------------------
   Name:    main
   Goal:    Main entry point of the processor
   Parameters:  @args - command line argument vector
   History:
   17/10/2014   ERK Created
   --------------------------------------------------------------------------- */
    public static void main(String[] args) throws Exception {
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
                System.err.println("Option --inputdir should be followed by a directory");
                usage(); return;
              } else {
                // Get the input directory
                dirInput = new File(args[i]);
                // Check it up
                if (!dirInput.exists()) {
                // The project cannot be read/opened
                System.err.println("Cannot open input directory: " + dirInput);
                usage(); return;
                }
              }
              break;
            case "help":
              usage(); return;
            default:
              System.err.println("Unknown option --" + name);
              usage(); return;
          }
        } else if (i==0) {
          // Get the project file string
          strProject = arg;
        }
      }
      // Try to load the project
      if (!prjThis.Load(strProject)) {
        System.err.println("Could not load project " + strProject);
        return;
      }
      
      // Execute queries
      if (!prjThis.Execute()) {
        
      }
      
      // Return positively
      logger.debug("Successfully loaded project " + 
              prjThis.getName() + " (type=" + prjThis.getProjectType() + ")");
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
