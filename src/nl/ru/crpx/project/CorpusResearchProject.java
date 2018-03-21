/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */

// My own name
package nl.ru.crpx.project;
// The external libraries that I need
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import nl.ru.crpx.dataobject.DataFormat;
import nl.ru.crpx.search.Job;
import nl.ru.crpx.search.SearchManager;
import nl.ru.crpx.search.WorkQueueXqF;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.crpx.tools.FileIO;
import nl.ru.crpx.tools.General;
import nl.ru.util.DateUtil;
import nl.ru.util.FileUtil;
import nl.ru.util.json.JSONArray;
import nl.ru.util.json.JSONObject;
import nl.ru.xmltools.XmlForest.ForType;
import static nl.ru.xmltools.XmlIO.WriteXml;
import org.apache.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/* ---------------------------------------------------------------------------
   Class:   CorpusResearchProject
   Goal:    Definition of and functions on a corpus research project (crp)
   History:
   17/10/2014   ERK Created
   --------------------------------------------------------------------------- */
public class CorpusResearchProject {
  // This class uses a logger
  private static final Logger logger = Logger.getLogger(CorpusResearchProject.class);
  // private static String sGregorianFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSXXX";
  // private static String sGregorianFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS";
  private static final String sGregorianFormat = "yyyy-MM-dd'T'HH:mm:ss";
  private static final String sSettings = "{'GenPrec': '2', 'GenInpExt': '.psdx', 'PrecNum': '2', "
      + "'FollNum': '1', 'ShowPsd': 'True', 'Locked': 'True', 'Stream': 'True', "
      + "'Language': '', 'Part': '', 'DbaseInput': 'False'}";
  private static final List<String> lCrpParts = Arrays.asList(
      "General", "QueryConstructor", "DbFeatList", "QwizList", "QueryList", "DefList", "VarList", "PeriodInfo");
  private static final List<String> lCrpDates = Arrays.asList(
      "DateCreated", "DateChanged");
  // ================== Enumerations in use ==================================
  public enum ProjType {
    ProjPsd, ProjPsdx, ProjNegra, ProjAlp, ProjFolia, Dbase, None;
    public static ProjType getType(String sName) {
      switch (sName.toLowerCase()) {
        case "xquery-psdx": return ProjType.ProjPsdx;
        case "negra-tig": return ProjType.ProjNegra;
        case "penn-psd": return ProjType.ProjPsd;
        case "folia-xml": return ProjType.ProjFolia;
        case "alpino-xml": return ProjType.ProjAlp;
        case "dbase": return ProjType.Dbase;
      }
      // Unidentified project
      return ProjType.None;
    }
    public static String getName(ProjType prjType) {
      switch(prjType) {
        case ProjPsd: return "Penn-Psd";
        case ProjPsdx: return "Xquery-Psdx";
        case ProjNegra: return "Negra-Tig";
        case ProjAlp: return "Alpino-Xml";
        case ProjFolia: return "Folia-Xml";
        case Dbase: return "Dbase";
        default: return "";
      }
    }
  }
  public enum ProjOut {ProjBasic, ProjDbase, ProjStat, ProjTimbl}
  // ================== Publically available fields ==========================
  public ProjType intProjType;
  public String sNodeNameSnt;       // Name of the "sentence-level" node
  public String sNodeNameCns;       // Name of the "constituent-level" node
  public String sNodeNamePrg;       // Name of "paragraph-level" node
  public String sNodeNameWrd;       // Name of "word-level" node
  public ErrHandle errHandle;       // My own error handler
  public boolean HasDbaseInput;     // True if this project takes a database as input
  // =================== private variables ====================================
  private String Location = "";     // Full filename and location of this project
  private String Name = "";         // Name of this project (w.o. extension)
  private String Source = "";       // Source database (if any)
  private String Goal = "";         // The goal of this project
  private String Comments = "";     // Comments about this project
  private String Author = "";       // Author of this project
  private String ProjectType = "";  // The type of project (one of several)
  private String SaveDate = "";     // The date + time when this CRP was last saved
  private String sHitsDir = "";     // Directory where hits are stored
  private String Language = "";     // The language this project focuses on
  private String Part = "";         // The particular part of the language it focuses on
  private String Rules = "";        // The rules to be used for selecting only part of the input
  private String xqInput = "";      // Xquery code that defines the input selection
  private File QueryDir = null;     // Location of temporal queries
  private File DstDir = null;       // Destination directory
  private File SrcDir = null;       // Directory where the psdx files are located
  private File flProject;           // The project we are working with (at [Location] )
  private File dirInput;            // Main directory where the psdx files are located
  private boolean ShowPsd = true;   // Show syntax of each result in PSD
  private boolean Locked = true;    // Lock project against synchronisation
  private boolean Stream = true;    // Execute line-by-line
  private boolean bXmlData = true;  // This project deals with XML data
  private boolean bShowPsd = true;  // The PSD syntax needs to be shown in the results
  private int PrecNum = 2;          // Number of preceding lines
  private int FollNum = 1;          // Number of following lines
  private ForType forProcType;      // The kind of processing type
  private Date dtCreated;           // Creation date of this project
  private Date dtChanged;           // Last change of this project
  private Document docProject;            // Project as XML document
  private DocumentBuilderFactory factory;
  private DocumentBuilder parser;
  private XPath xpath = XPathFactory.newInstance().newXPath();
  private Map mSet = new HashMap();       // Hash table with settings
  private SearchManager searchMan;        // The manager associated with this CRP
  private WorkQueueXqF workQueue;         // The work queue associated with this CRP
  private ExecutorService workExecutor;
  private PrjTypeManager prjTypeManager;  // Project type manager associated with this CRP
  private String userId;                  // ID of calling user
  private ExecuteXml objEx = null;           // Execution object
  private static Processor objSaxon = null;             // The saxon processor (for global reference)
  private static JSONObject oSetting = null;
  // Each project contains a number of lists
  List<JSONObject> lDefList = new ArrayList<>();
  List<JSONObject> lQueryList = new ArrayList<>();
  List<JSONObject> lQueryWiz = new ArrayList<>();         // ADDED: intermediate level of the query wizard (apr/2016)
  List<JSONObject> lQueryConstructor = new ArrayList<>();
  List<JSONObject> lDbFeatList = new ArrayList<>();
  List<JSONObject> lPeriodInfo = new ArrayList<>();
  List<JSONObject> lDivisionList = new ArrayList<>();
  List<JSONObject> lMemberList = new ArrayList<>();

  // ==================== Class initialisations ================================
  public CorpusResearchProject(boolean bUseSaxon) {
    // Set error handler
    errHandle = new ErrHandle(CorpusResearchProject.class);
    // Other class-level initialisations
    this.docProject = null;
    this.parser = null;
    this.factory = DocumentBuilderFactory.newInstance();
    this.HasDbaseInput = false;
    try {
      this.parser = this.factory.newDocumentBuilder();
    } catch (ParserConfigurationException ex) {
      logger.error("CorpusResearchProject: could not create documentbuilder", ex);
    }
    this.userId = "";
    this.dirInput = null;
    this.flProject = null;
    // Set default project type
    this.intProjType = ProjType.ProjPsdx;
    // Set default forest-processing type for this kind of project
    this.forProcType = ForType.PsdxPerForest;
    // Get the default settings
    oSetting = new JSONObject(sSettings);
    // Should we initialize saxon?
    if (bUseSaxon) {
      // Create a processor that is NOT schema-aware (so we use Saxon-B 9.1.0.8)
      if (objSaxon == null) objSaxon = new Processor(false);
    }
  }
  
  // =================== instance methods ======================================
  /* ---------------------------------------------------------------------------
   Name:    Load
   Goal:    Load a project. The name must be in @Location (see overloaded function)
   History:
   17/10/2014   ERK Created
   --------------------------------------------------------------------------- */
  public boolean Load(String sSrcDir, String sDstDir, String sQueryDir) {
    try {
      // Validate crucial settings ---------------------------------
      if (this.Location.equals("")) return(false);
      this.flProject = new File(this.Location);
      // Does it exist at all?
      if (!this.flProject.exists()) {
        return(errHandle.DoError("Non-existing CRP: " + this.flProject ));
      }
      if (!this.flProject.canRead()) {
        // The project cannot be read/opened
        return(errHandle.DoError("Cannot read [" + this.flProject + "]"));
      }    
      // Keep track of the save date
      this.SaveDate = General.getSaveDate(this.flProject);
      // Perform initialisations
      try {
        if (!DoInit()) return(errHandle.DoError("unable to initialize"));
      } catch (ParserConfigurationException ex) {
        return errHandle.DoError("Load crpx: could not configure parser", ex, CorpusResearchProject.class);
      }
      mSet.clear();
      try {
        // Read the CRPX project xml file
        this.docProject = this.parser.parse(this.flProject);
      } catch (SAXException ex) {
        return errHandle.DoError("Load crpx file: SAX problem", ex, CorpusResearchProject.class);
      } catch (IOException ex) {
        return errHandle.DoError("Load crpx file: IO problem", ex, CorpusResearchProject.class);
      }
      // Debugging
      logger.debug("Root node=" + this.docProject.getDocumentElement().getTagName());
      try {
        NodeList ndxList = (NodeList) xpath.compile("./descendant::Setting").evaluate( this.docProject, XPathConstants.NODESET);
        errHandle.debug("Settings of this crpx: " + ndxList.getLength());
      } catch (XPathExpressionException ex) {
        return errHandle.DoError("debug", ex, CorpusResearchProject.class);
      }
      // === Loading the settings assumes that this.docProject exists!!
      // Load the settings: strings
      this.Name = getSetting("Name");
      this.Source = getSetting("Source");
      this.Goal = getSetting("Goal");
      this.Comments = getSetting("Comments");
      this.Author = getSetting("Author");
      this.ProjectType = getSetting("ProjectType");
      this.intProjType = ProjType.getType(this.ProjectType);
      this.Language = getSetting("Language");
      this.Part = getSetting("Part");
      this.HasDbaseInput = getSetting("DbaseInput").equals("True");
      this.Rules = getSetting("Rules");
      this.xqInput = getSetting("xqInput");
      // Load the dates
      this.dtChanged = DateUtil.stringToDate(getDateSetting("Changed"));
      this.dtCreated = DateUtil.stringToDate(getDateSetting("Created"));
      // Determine the names of nodes available globally
      switch (this.intProjType) {
        case ProjPsdx:
          sNodeNameSnt = "forest"; sNodeNamePrg=""; sNodeNameWrd="eLeaf"; sNodeNameCns = "eTree"; 
          //  Original: this.forProcType = ForType.PsdxPerForest;
          this.forProcType = ForType.PsdxIndex;
          break;
        case ProjFolia:
          sNodeNameSnt = "s"; sNodeNamePrg="p"; sNodeNameWrd="w"; sNodeNameCns = "su"; 
          this.forProcType = ForType.FoliaIndex;
          break;
        case ProjNegra:
          sNodeNameSnt = "s"; sNodeNamePrg=""; sNodeNameWrd="t"; sNodeNameCns = ""; 
          // TODO: adapt when implementing Negra
          this.forProcType = ForType.NegraPerS;
          break;
        case ProjAlp:
          sNodeNameSnt = "node"; sNodeNamePrg=""; sNodeNameWrd="node"; sNodeNameCns = "node"; 
          // TODO: adapt when implementing Alpino
          this.forProcType = ForType.AlpPerS;
          break;
        case ProjPsd:
          // This should actually not be implemented??
          sNodeNameSnt = ""; sNodeNamePrg=""; sNodeNameWrd=""; sNodeNameCns = ""; 
          // TODO: adapt when implementing PSD treebank processing
          this.forProcType = ForType.PsdPerS;
          break;
        default:  // Default behaviour may need to be restored to an ERROR message?
          // For now: default is Psdx and PsdxPerForest
          sNodeNameSnt = "forest"; sNodeNamePrg=""; sNodeNameWrd="eLeaf"; sNodeNameCns = "eTree"; 
          this.forProcType = ForType.PsdxPerForest;
          break;
      }
      // Double check on the forest-processing-type
      switch (getSetting("ForType")) {
        case "PsdxPerForest": this.forProcType = ForType.PsdxPerForest; break;
        case "PsdxPerForgrp": this.forProcType = ForType.PsdxPerForgrp; break;
        case "PsdxWholeFile": this.forProcType = ForType.PsdxWholeFile; break;
        case "FoliaPerDiv": this.forProcType = ForType.FoliaPerDiv; break;
        case "FoliaPerPara": this.forProcType = ForType.FoliaPerPara; break;
        case "FoliaPerS": this.forProcType = ForType.FoliaPerS; break;
        case "FoliaWholeFile": this.forProcType = ForType.FoliaWholeFile; break;
        default:
          // No need to do anything for the default
          break;
      }

      // Calculate bXmlData
      switch(this.ProjectType.toLowerCase()) {
        case "xquery-psdx":
          this.bXmlData = true; break;
        default:
          this.bXmlData = true;
      }
      // Possibly adapt the setting directories
      if (sSrcDir.isEmpty()) sSrcDir = getSetting("SrcDir");
      if (sDstDir.isEmpty()) sDstDir = getSetting("DstDir");
      if (sQueryDir.isEmpty()) sQueryDir = getSetting("QueryDir");
      // Load the settings: directories
      this.QueryDir = new File(FileUtil.nameNormalize(sQueryDir));
      this.DstDir = new File(FileUtil.nameNormalize(sDstDir));
      this.SrcDir = new File(FileUtil.nameNormalize(sSrcDir));
      // Create alternative directories if we still have problems
      if (!this.DstDir.exists()) {
        // Create alternative dst dir
        sDstDir = "/etc/project/dst";
        this.DstDir = new File(FileUtil.nameNormalize(sDstDir));
        if (!this.DstDir.exists()) this.DstDir.mkdir();
      }
      if (!this.QueryDir.exists()) {
        // Create alternative query dir
        sQueryDir = "/etc/project/xq";
        this.QueryDir = new File(FileUtil.nameNormalize(sQueryDir));
        if (!this.QueryDir.exists()) this.QueryDir.mkdir();
      }
      // Load the list of definitions
      ReadCrpList(lDefList, "./descendant::DefList/child::Definition", 
                  "DefId;Name;File;Goal;Comment;Created;Changed", "Text", false);
      // Load the list of queries
      ReadCrpList(lQueryList, "./descendant::QueryList/child::Query", 
                  "QueryId;Name;File;Goal;Comment;Created;Changed", "Text", false);
      // Load the list of 'qwiz' elements
      ReadCrpList(lQueryWiz, "./descendant::QwizList/child::Qwiz", 
                  "QwizId;Name;Goal;Comment;Search;Created;Changed", "cns;cnd", true);
      // Load the list of QC items (query constructor)
      ReadCrpList(lQueryConstructor, "./descendant::QueryConstructor/child::QC", 
                  "QCid;Input;Query;Output;OutFeat;Result;Cmp;Mother;Goal;Comment", "", false);
      // Load the list of database features
      ReadCrpList(lDbFeatList, "./descendant::DbFeatList/child::DbFeat", 
                  "DbFeatId;Name;Pre;QCid;FtNum", "", false);
      // Load the list of divisions
      ReadCrpList(lDivisionList, "./descendant::PeriodInfo/child::Division", 
                  "DivisionId;Name;Descr", "", false);
      // Load the list of members
      ReadCrpList(lMemberList, "./descendant::Division/child::Member", 
                  "MemberId;DivisionId;PeriodId;Period;Group;Order", "Text", false);
      // Check directories: this is no longer needed
      //   since we WILL NOT be using these directories anyway...
      /*
      if (!this.QueryDir.isDirectory())  if (!this.QueryDir.mkdir()) return(errHandle.DoError("Could not create QueryDir [" + this.QueryDir.toString() + "]"));
      if (!this.DstDir.isDirectory())  if (!this.DstDir.mkdir()) return(errHandle.DoError("Could not create DstDir [" + this.DstDir.toString() + "]"));
      if (!this.SrcDir.isDirectory())  if (!this.SrcDir.mkdir()) return(errHandle.DoError("Could not create SrcDir [" + this.SrcDir.toString() + "]"));
      */

      // Check the project type
      logger.debug("CRP CHECK: " + this.ProjectType + "=" + ProjType.getType(this.ProjectType));
      switch(ProjType.getType(this.ProjectType)) {
        case ProjFolia:
        case ProjPsdx: // Okay, we are able to process this kind of project
          logger.debug("Processing type: " + this.ProjectType);
          break;
        case ProjAlp:
        case ProjNegra:
        case ProjPsd:
          // We will allow these types, but give a warning
          logger.debug("Processing of this type has not yet been implemented: " + this.ProjectType);
          break;
        default: 
          return(errHandle.DoError("Sorry, cannot process projects of type [" + this.ProjectType + "]"));
      }

      /*
      // Perform initialisations related to this project-type using the config file
      // Read it from the class path
      String configFileName = "crpp-settings.json";
      InputStream is = getClass().getClassLoader().getResourceAsStream(configFileName);
      if (is == null) {
        configFileName = "crpp-settings-default.json.txt";  // Internal default
        is = getClass().getClassLoader().getResourceAsStream(configFileName);
        if (is == null) {
          // We cannot continue...
          return(errHandle.DoError("Could not find " + configFileName + "!"));
        }
      }
      // Create configuration object
      JSONObject config;
      // Process input stream with configuration
      try {
        try {
          config = Json.read(is);
        } finally {
          is.close();
        }
      } catch (Exception e) {
        return(errHandle.DoError("Error reading JSON config file: " +  e.getMessage()));
      }
      
      // Create a new project-type manager on the basis of the configuration settings
      prjTypeManager = new PrjTypeManager(config);

      // Set the search manager
      searchMan = new SearchManager(config);
*/
      // Load the definitions

      // Close the project file

      // Return positively

      return(true);
    } catch (Exception ex) {
      errHandle.DoError("CorpusResearchProject will not load", ex, CorpusResearchProject.class);
      // Return failure
      return false;
    }
  }
  public boolean Load(String loc, String sSrcDir, String sDstDir, String sQueryDir) {
    this.Location = loc;
    return(Load(sSrcDir, sDstDir, sQueryDir));
  }
  
  /**
   * Create
   *    Create a new CRP structure, which exists in two realms:
   *    1) In an XML file
   *    2) Within this class
   *    Method: create an XML structure, and then load it
   *  
   * @param loc
   * @param sSrcDir
   * @param sDstDir
   * @param sQueryDir
   * @return 
   */
  public boolean Create(String loc, String sSrcDir, String sDstDir, String sQueryDir) {
    Element elGeneral = null;
    Element elDate = null;

    try {
      // Get the location for the file
      this.Location = loc;
      // Create an Xml structure
      this.docProject = this.parser.newDocument();
      this.docProject.setXmlStandalone(true);
      this.docProject.setXmlVersion("1.0");
      // Create root element node
      Element elRoot = this.docProject.createElement("CorpusResearchProject");
      this.docProject.appendChild(elRoot);
      // Add the children
      for (String sPart : this.lCrpParts) {
        Element elChild = this.docProject.createElement(sPart);
        elRoot.appendChild(elChild);
        if (sPart.equals("General")){
          elGeneral = elChild;
          for (String sDatePart : this.lCrpDates) {
            elDate = this.docProject.createElement(sDatePart);
            elGeneral.appendChild(elDate);
          }
        }
      }
      // General default settings
      Iterator keys = oSetting.keys();
      while (keys.hasNext()) {
        String sKey = keys.next().toString();
        this.setSetting(sKey, oSetting.getString(sKey));
      }
      // Get string for current date
      Date dNow = new Date();
      String sDateNow = DateUtil.dateToString(dNow);
      // Initialize general settings
      this.setName(Name);
      this.setAuthor(this.userId);
      this.setComments("<TODO: State details here>\n\n"
              + "History:\n"+this.userId+"\t"+sDateNow+"\tCreated\n");
      this.setGoal("<TODO: state the goal of this project here>");
      this.setDateSetting("Created", sDateNow);
      this.setDateSetting("Changed", sDateNow);
      this.setSetting("SrcDir", sSrcDir);
      this.setSetting("DstDir", sDstDir);
      this.setSetting("QueryDir", sQueryDir);
      this.setSetting("InputDir", sSrcDir);
      this.setSetting("OutputDir", sDstDir);

      String sProjType = ProjType.getName(intProjType);
      this.setProjectType(sProjType);
      
      // What is the extension belonging to intProjType?
      String sTextExt = getTextExt(ProjType.getType(sProjType));
      
      // Set the two settings needing this extension
      this.setSetting("GenInpExt", sTextExt);
      this.setSetting("Source", "*" + sTextExt);
      
      // Set the project file
      this.flProject = new File(this.Location);

      // Save what we have
      if (!this.Save()) return false;
      
      // Load what has been saved properly
      return(Load(sSrcDir, sDstDir, sQueryDir));      
    } catch (Exception ex) {
      errHandle.DoError("CorpusResearchProject will not create", ex, CorpusResearchProject.class);
      // Return failure
      return false;
    }
  }

  /* ---------------------------------------------------------------------------
   Name:    Save
   Goal:    Save a project as xml. 
            Use the project's location/name in this.Location
   History:
   20/04/2015   ERK Created
   --------------------------------------------------------------------------- */
  public boolean Save() {
    try {
      // Validate
      if (this.Location.isEmpty()) return false;
      if (this.docProject == null) return false;
      // Write as XML to the correct location
      boolean bFlag = WriteXml(this.docProject, this.Location);
      
      // Debugging:
      errHandle.debug("CRP save [" + this.Location + "] = " + bFlag);
      
      // Adapt the [SaveDate] property of myself
      this.SaveDate = General.getSaveDate(this.flProject);
      // Return the result of writing
      return bFlag;
    } catch (Exception ex) {
      errHandle.DoError("Could not perform [Save]", ex, CorpusResearchProject.class);
      return false;
    }
  }
  
  /* ---------------------------------------------------------------------------
   Name:    hasResults
   Goal:    Check if this CRP already has valid results
   History:
   27/07/2015   ERK Created
   --------------------------------------------------------------------------- */
  public boolean hasResults(JSONObject oQuery) {
    boolean bResultsValid = false;
    String sMsg = "";
    try {
      // Check if there is a status file
      File fStatus = new File(getResultFileName("status", "json"));
      if (!fStatus.exists()) {errHandle.debug("CACHE hasResults: no status.json"); return false;}
      // Get the contents of the status file (which is json)
      JSONObject oStatus = new JSONObject( (new FileUtil()).readFile(fStatus));
      // Check if the language and the dir coincide
      if (oStatus.getString("lng").equals(oQuery.getString("lng"))) {
        // Does the status contain a dir?
        if (oStatus.has("dir")) {
          // Requester must have "dir"
          if (oQuery.has("dir")) {
            // Are they equal?
            bResultsValid= (oStatus.getString("dir").equals(oQuery.getString("dir")));
            if (!bResultsValid)
              sMsg = "CACHE hasResults: 'dir' of requester differs from 'dir' of available status";
          } 
        } else {
          // Requester may not have "dir" either
          bResultsValid = (!oQuery.has("dir"));
          if (!bResultsValid)
            sMsg = "CACHE hasResults: available status has 'dir', but requester does not";
        }
      }
      if (!bResultsValid)
        sMsg = "CACHE hasResults: (1) lng not equal, or (2) status has 'dir' but requester not"; 
      // Possibly issue message
      if (!sMsg.isEmpty()) errHandle.debug(sMsg);
      return bResultsValid;
    } catch (Exception ex) {
      errHandle.DoError("hasResults error:", ex, CorpusResearchProject.class);
      return false;
    }
  }
  /**
   * getResultsOneTable
   *    Find and return the results table specified by the parameters
   * 
   * @param sType   The type can be: "results", "count", "table"
   * @return 
   */
  public String getResultsOneTable(String sType) {
    try {
      // Check if there is a status file
      File fResultsFile = new File(getResultFileName(sType, "json"));
      if (!fResultsFile.exists()) return "";
      // Read the file
      String sBack = (new FileUtil()).readFile(fResultsFile, "utf-8");
      // Return the results
      return sBack;
    } catch (Exception ex) {
      errHandle.DoError("getResultsOneTable error:", ex, CorpusResearchProject.class);
      return "";
    }
  }
  
  /* ---------------------------------------------------------------------------
   Name:    Execute
   Goal:    Handle the execution of this CRP
            - Do preparations, depending on the project type and execution type
            - Start up the actual Xq job to gather results
            - Return the results to the appropriate places
   History:
   20/04/2015   ERK Created
   --------------------------------------------------------------------------- */
  public boolean Execute(Job jobCaller, String sCallingUser) {
    boolean bFlag = true; // Flag with execution result
    boolean bXml = false; // Write results as XML also 
    
    try {
      // Reset any errors
      this.errHandle.clearErr();
      Job.errHandle.clearErr();
      // Determine status file location
      String sStatusFileLoc = getResultFileName("status", "json");
      File fStatusFile = new File(sStatusFileLoc);
      // Remove any old status file
      if (fStatusFile.exists()) fStatusFile.delete();
      // Set the starting time
      long startTime = System.currentTimeMillis();
      // Set the userid
      this.userId = sCallingUser;
      // Create an execution object instance
          // Check what kind of project this is
      switch (this.intProjType) {
        case ProjPsdx: // Okay, we are able to process this kind of project
          // Only one type of processing:
          objEx = new ExecutePsdxStream(this);          
          /*
          // Do we need to do streaming or not?
          if (this.getStream()) {
            // objEx = new ExecutePsdxStream(this, objGen);
            objEx = new ExecutePsdxStream(this);
          } else {
            // objEx = new ExecutePsdxFast(this, objGen);
            objEx = new ExecutePsdxFast(this);
          } */
          break;
        case ProjFolia:
          // Only one type of processing:
          objEx  = new ExecutePsdxStream(this);
          /*
          // Do we need to do streaming or not?
          if (this.getStream()) {
            // objEx  = new ExecuteFoliaStream(this, objGen);
            objEx  = new ExecuteFoliaStream(this);
          } else {
            // objEx  = new ExecuteFoliaFast(this, objGen);
            objEx  = new ExecuteFoliaFast(this);
          } */
          break;
        case ProjAlp:
        case ProjPsd:
        case ProjNegra:
        default: 
         errHandle.DoError("Sorry, cannot execute projects of type [" + this.getProjectType() + "]");
      }
      if (objEx == null) return false;
      // Execute the queries using the chosen method
      bFlag = objEx.ExecuteQueries(jobCaller);
      // Note finish time
      long stopTime = System.currentTimeMillis();
      long elapsedTime = stopTime - startTime; 
      // Log the time
      logger.debug("Query time: " + elapsedTime + " (ms)");
      if (bFlag) {
        // ========= Debugging =========
        // Get the job's OVERALL results
        String sResult = jobCaller.getJobResult();
        File fResultOut = new File (getResultFileName("results", "json"));
        FileUtil.writeFile(fResultOut, sResult);
        // Show where this is written
        logger.debug("Results are in: " + fResultOut.getAbsolutePath());
        // =============================
        // Get the counting results
        //   NOTE: an integer inside [toString()] provides pretty-printing
        //   NOTE: this information is probably superfluous -- skipping
        /*
        JSONObject objCount = new JSONObject();
        objCount.put("count", jobCaller.getJobCount());
        String sCount = objCount.toString(1);
        File fCountOut = new File(getResultFileName("count", "json"));
        FileUtil.writeFile(fCountOut, sCount);
        // Show where this is written
        logger.debug("Counts are in: " + fCountOut.getAbsolutePath());
        */
        // =============================
        // Get the table results
        String sTable = jobCaller.getJobTable().toString(DataFormat.JSON);
        File fTableOut = new File(getResultFileName("table", "json"));
        FileUtil.writeFile(fTableOut, sTable);
        // Show where this is written
        logger.debug("Table is in: " + fTableOut.getAbsolutePath());
        // =============================

        if (bXml) {
          // Write the table as an XML file (trial)
          String sTableXmlLoc = getResultFileName("table", "xml") ;
          BufferedWriter wHits = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sTableXmlLoc), "UTF-8"));
          jobCaller.getJobTable().serialize(wHits, DataFormat.XML, true);
          wHits.close();
          // Show where this is written
          logger.debug("Table (xml) is in: " + sTableXmlLoc);
        }

        // Write a status file to indicate the results are valid
        JSONObject oStatus = new JSONObject();
        String sQuery = jobCaller.getParameters().get("query");
        if (sQuery.isEmpty() || !sQuery.startsWith("{")) {
          // The "query" parameter is not a JSON object, so we cannot process it
          int m=1;
        } else  {
          JSONObject oQuery = new JSONObject(sQuery);
          oStatus.put("lng", oQuery.getString("lng"));
          if (oQuery.has("dir")) oStatus.put("dir", oQuery.getString("dir"));
          if (oQuery.has("dbase")) oStatus.put("dbase", oQuery.getString("dbase"));
          FileUtil.writeFile(fStatusFile, oQuery.toString());
        }
      }

      
      // =============================
      // Check if the query-execution resulted in an interrupt
      if (!bFlag || objEx.bInterrupt) { 
        errHandle.bInterrupt = true; 
        errHandle.debug("Interrupted"); 
        // Copy any error objects
        errHandle.DoError(jobCaller.getJobErrors());
        return false; 
      }
      // Is this a corpussearch project?
      /*
        ' Is this a corpussearch project?
        bXmlData = (InStr(strQengine, "xquery", CompareMethod.Text) > 0)
        ' Determine whether the syntax of the result should be shown or not
        bShowPsd = Me.chbShowPsd.Checked
        ' Show the results in the Results tab
        ShowResults(bXmlData, bShowPsd)
        ' Possibly show output file
        RuOutMessage()
        ' Possibly show lexicon file message
        RuLexMessage()
      */
      // Return positively
      return true; 
    } catch (Exception ex) {
      errHandle.DoError("Could not complete [Execute]", ex, CorpusResearchProject.class);
      return false;      
    }
  }
  
  /**
   * getResultFilename
   *    Produce result table and status file names in a uniform way
   * 
   * @param sType
   * @param sExt
   * @return 
   */
  private String getResultFileName(String sType, String sExt) {
    String sBack = this.DstDir + "/" + this.Name + "." + sType;
    if (!sExt.isEmpty()) sBack += "." + sExt;
    return sBack;
  }
  
  // ===================== Get and Set methods =================================
  public XPath getXPath() { return this.xpath;}
  public String getLocation() { return this.Location;}
  public String getName() { return this.Name;}
  public String getSource() { return this.Source;}
  public String getGoal() { return this.Goal;}
  public String getComments() { return this.Comments;}
  public String getAuthor() { return this.Author;}
  public String getProjectType() { return this.ProjectType;}
  public String getUserId() { return this.userId; }
  public String getSave() { return this.SaveDate; }
  public String getLanguage() { return this.Language;}
  public String getPart() { return this.Part;}
  public String getRules() { return this.Rules;}
  public String getXqInput() { return this.xqInput; }
  public String getDbaseInput() { String sResult = (this.HasDbaseInput) ? "True" : "False"; return sResult; }
  // Set string values
  public void setLocation(String sValue) { this.Location = sValue;}
  public void setProjectType(String sValue) { if (setSetting("ProjectType", sValue)) { this.ProjectType = sValue;}}
  // Forest (processing) type handling
  public ForType getForType() { return this.forProcType; }
  public void setForType(ForType ftNew) { this.forProcType = ftNew; }
  // Set string values and do that in the XML too
  public void setName(String sValue) { if (setSetting("Name", sValue)) { this.Name = sValue;}}
  public void setSource(String sValue) { if (setSetting("Source", sValue)) { this.Source = sValue;}}
  public void setGoal(String sValue) { if (setSetting("Goal", sValue)) { this.Goal = sValue;}}
  public void setComments(String sValue) { if (setSetting("Comments", sValue)) { this.Comments = sValue;}}
  public void setAuthor(String sValue) { if (setSetting("Author", sValue)) { this.Author = sValue;}}
  public void setLanguage(String sValue) { if (setSetting("Language", sValue)) { this.Language = sValue;} }
  public void setPart(String sValue) { if (setSetting("Part", sValue)) { this.Part = sValue;} }
  public void setRules(String sValue) { if (setSetting("Rules", sValue)) {this.Rules = sValue;}}
  public void setXqInput(String sValue) { if (setSetting("xqInput", sValue)) {this.xqInput = sValue;}}
  public void setDbaseInput(String sValue) { if (setSetting("DbaseInput", sValue)) { this.HasDbaseInput = (sValue.equals("True")); } }
  // ================ Directory and file names
  public File getQueryDir() { return this.QueryDir;}
  public File getDstDir() { return this.DstDir;}
  public File getSrcDir() { return this.SrcDir;}
  public File getInputDir() { return this.dirInput; }
  // Set file names
  public void setQueryDir(File fThis)  {if (setSetting("QueryDir", fThis.getAbsolutePath())) { this.QueryDir = fThis;}}
  public void setDstDir(File fThis)  {if (setSetting("DstDir", fThis.getAbsolutePath())) { this.DstDir = fThis;}}
  public void setSrcDir(File fThis)  {if (setSetting("SrcDir", fThis.getAbsolutePath())) { this.SrcDir = fThis;}}
  public void setInputDir(File fThis) {if (setSetting("InputDir", fThis.getAbsolutePath())) { this.dirInput = fThis;}}
  // ================ Booleans
  public boolean getShowPsd() { return this.ShowPsd;}
  public boolean getLocked() { return this.Locked;}
  public boolean getStream() { return this.Stream;}
  public boolean getIsXmlData() { return this.bXmlData;}
  // Set booleans
  public void setShowPsd(boolean bSet) { if (setSetting("ShowPsd", bSet ? "True" : "False")) {this.ShowPsd = bSet;}}
  public void setLocked(boolean bSet) { if (setSetting("Locked", bSet ? "True" : "False")) {this.Locked = bSet;}}
  public void setStream(boolean bSet) { if (setSetting("Stream", bSet ? "True" : "False")) {this.Stream = bSet;}}
  // Do NOT set [bXmlData]; it has to be calculated from the project type
  public void setIsXmlData(boolean bSet) { this.bXmlData = bSet;}
  // ================ Integers
  public int getPrecNum() {return this.PrecNum;}
  public int getFollNum() {return this.FollNum;}
  public void setPrecNum(int iValue) {if (setSetting("PrecNum", String.valueOf(iValue))) {this.PrecNum = iValue;}}
  public void setFollNum(int iValue) {if (setSetting("FollNum", String.valueOf(iValue))) {this.FollNum = iValue;}}
  // ================ Date/Time values
  public Date getDateChanged() {return this.dtChanged;}
  public Date getDateCreated() {return this.dtCreated;}
  public void setDateChanged(Date dValue) {if (setDateSetting("Changed", DateUtil.dateToString(dValue))) {this.dtChanged = dValue;}}
  public void setDateCreated(Date dValue) {if (setDateSetting("Created", DateUtil.dateToString(dValue))) {this.dtCreated = dValue;}}
  // ================ Query Constructor elements
  public int getListQcId(int iQCid) {
    for (int i=0;i<lQueryConstructor.size();i++) 
      if (lQueryConstructor.get(i).getInt("QCid") == iQCid) 
        return i;
    return -1;
  }
  public int getListQCsize() { return lQueryConstructor.size(); }
  public List<JSONObject> getListQC() { return lQueryConstructor;}
  public JSONObject getListQCitem(int iValue) {return lQueryConstructor.get(iValue); }
  public void setListQCitem(int iValue, JSONObject oItem) {lQueryConstructor.set(iValue, oItem);}
  // ================ Qwiz list elements ===============
  public int getListQwizId(int iQwizId) {
    for (int i=0;i<lQueryWiz.size();i++) 
      if (lQueryWiz.get(i).getInt("QwizId") == iQwizId) 
        return i;
    return -1;
  }
  public int getListQwizSize() { return lQueryWiz.size(); }
  public JSONObject getListQwizByName(String sQname) {
    for (int i=0;i<lQueryWiz.size();i++) 
      if (lQueryWiz.get(i).getString("Name").equals(sQname)) return lQueryWiz.get(i);
    return null;
  }
  public List<JSONObject> getListQwiz() { return lQueryWiz;}
  public JSONObject getListQwizItem(int iValue) {return lQueryWiz.get(iValue); }
  public void setListQwizItem(int iValue, JSONObject oItem) {lQueryWiz.set(iValue, oItem);}
  
  
  // ================ Query list elements
  public int getListQueryId(int iQueryId) {
    for (int i=0;i<lQueryList.size();i++) 
      if (lQueryList.get(i).getInt("QueryId") == iQueryId) 
        return i;
    return -1;
  }
  public JSONObject getListQueryByName(String sQname) {
    for (int i=0;i<lQueryList.size();i++) 
      if (lQueryList.get(i).getString("Name").equals(sQname)) return lQueryList.get(i);
    return null;
  }
  public int getListQueryIndex(JSONObject oItem) {
    String sQname = oItem.getString("Name");
    for (int i=0;i<lQueryList.size();i++) 
      if (lQueryList.get(i).getString("Name").equals(sQname)) return i;
    return -1;
  }
  public int getListQuerySize() { return lQueryList.size(); }
  public List<JSONObject> getListQuery() { return lQueryList;}
  public JSONObject getListQueryItem(int iValue) {return lQueryList.get(iValue); }
  public void setListQueryItem(int iValue, JSONObject oItem) {lQueryList.set(iValue, oItem);}
  public int addListQueryItem(JSONObject oItem) {
    int iIdx = getListQueryIndex(oItem);
    if (iIdx<0) { lQueryList.add(oItem);  renumberListQuery(); iIdx = getListQueryIndex(oItem); }
    if (iIdx>=0) return lQueryList.get(iIdx).getInt("QueryId");
    else return -1;
  }
  public void renumberListQuery() { for (int i=0;i<lQueryList.size(); i++) { lQueryList.get(i).put("QueryId", i+1);}}
  public boolean delListQueryItem(JSONObject oItem) {
    for (int i=0;i<lQueryList.size(); i++) { 
      if (lQueryList.get(i).equals(oItem)) { lQueryList.remove(i); renumberListQuery(); return true; }
    }
    return false;
  }
  // ================ Definition list elements
  public int getListDefId(int iDefId) {
    for (int i=0;i<lDefList.size();i++) 
      if (lDefList.get(i).getInt("DefId") == iDefId) 
        return i;
    return -1;
  }
  public JSONObject getListDefByName(String sDname) {
    for (int i=0;i<lDefList.size();i++) 
      if (lDefList.get(i).getString("Name").equals(sDname)) return lDefList.get(i);
    return null;
  }
  public int getListDefIndex(JSONObject oItem) {
    String sDname = oItem.getString("Name");
    for (int i=0;i<lDefList.size();i++) 
      if (lDefList.get(i).getString("Name").equals(sDname)) return i;
    return -1;
  }
  public int getListDefSize() { return lDefList.size(); }
  public List<JSONObject> getListDef() { return lDefList;}
  public JSONObject getListDefItem(int iValue) {return lDefList.get(iValue); }
  public void setListDefItem(int iValue, JSONObject oItem) {lDefList.set(iValue, oItem);}
  public int addListDefItem(JSONObject oItem) {
    int iIdx = getListDefIndex(oItem);
    if (iIdx<0) { lDefList.add(oItem);  renumberListDef(); iIdx = getListDefIndex(oItem); }
    if (iIdx>=0) return lDefList.get(iIdx).getInt("DefId");
    else return -1;
  }
  public void renumberListDef() { for (int i=0;i<lDefList.size(); i++) { lDefList.get(i).put("DefId", i+1);}}
  public boolean delListDefItem(JSONObject oItem) {
    for (int i=0;i<lDefList.size(); i++) { 
      if (lDefList.get(i).equals(oItem)) { lDefList.remove(i); renumberListDef(); return true; }
    }
    return false;
  }
  // ================ Database Feature list elements
  public int getListDbFeatId(int iDbFeatId) {
    for (int i=0;i<lDbFeatList.size();i++) 
      if (lDbFeatList.get(i).getInt("DbFeatId") == iDbFeatId) 
        return i;
    return -1;
  }
  public JSONObject getListDbFeatByName(String sDname) {
    for (int i=0;i<lDbFeatList.size();i++) 
      if (lDbFeatList.get(i).getString("Name").equals(sDname)) return lDbFeatList.get(i);
    return null;
  }
  public List<JSONObject> getListDbFeat() { return this.lDbFeatList;}
  public int getListDbFeatIndex(JSONObject oItem) {
    String sDname = oItem.getString("Name");
    for (int i=0;i<lDbFeatList.size();i++) 
      if (lDbFeatList.get(i).getString("Name").equals(sDname)) return i;
    return -1;
  }
  public int getListDbFeatSize() { return lDbFeatList.size(); }
  public int getListDbFeatSize(int iQC, boolean bOnlyCalculated) { 
    int iSize = 0;    // Size of the list
    // Walk the list
    for (int i=0;i< lDbFeatList.size();i++) {
      // Does this element pertain to the indicated QC?
      if (bOnlyCalculated) {
        if (lDbFeatList.get(i).getInt("QCid") == iQC && 
            lDbFeatList.get(i).getInt("FtNum") >0) iSize++;
      } else {
        if (lDbFeatList.get(i).getInt("QCid") == iQC) iSize++;
      }
    }
    // Return the total size
    return iSize;
  }
  public List<JSONObject> getListDbFeat(int iQC) {
    // Create a list
    List<JSONObject> lstBack = new ArrayList<>();
    // Validate
    int iSize = getListDbFeatSize(iQC, false);
    if (iSize>0) {
      // Walk the list
      for (int i=0;i< lDbFeatList.size();i++) {
        // Does this element pertain to the indicated QC?
        if (lDbFeatList.get(i).getInt("QCid") == iQC) {
          // Copy this element to the new (sorted) list
          lstBack.add(lDbFeatList.get(i));
        }          
      }
    }
    // Return the list that includes only elements for this QC
    return lstBack;
  }
  public JSONObject getListDbFeatItem(int iValue) {return lDbFeatList.get(iValue); }
  public void setListDbFeatItem(int iValue, JSONObject oItem) {lDbFeatList.set(iValue, oItem);}
  public int addListDbFeatItem(JSONObject oItem) {
    int iIdx = getListDbFeatIndex(oItem);
    if (iIdx<0) { lDbFeatList.add(oItem);  renumberListDef(); iIdx = getListDbFeatIndex(oItem); }
    if (iIdx>=0) return lDbFeatList.get(iIdx).getInt("DbFeatId");
    else return -1;
  }
  public void renumberListDbFeat() { for (int i=0;i<lDbFeatList.size(); i++) { lDbFeatList.get(i).put("DbFeatId", i+1);}}
  public boolean delListDbFeatItem(JSONObject oItem) {
    for (int i=0;i<lDbFeatList.size(); i++) { 
      if (lDbFeatList.get(i).equals(oItem)) { lDbFeatList.remove(i); renumberListDbFeat(); return true; }
    }
    return false;
  }
  // ================ Division list elements
  public List<JSONObject> getListDivision() { return lDivisionList; }
  public JSONObject getListDivisionItem(int iValue) {return lDivisionList.get(iValue); }
  // ================ Member list elements
  public List<JSONObject> getListMember() { return lMemberList; }
  public JSONObject getListMemberItem(int iValue) {return lMemberList.get(iValue); }
  // ================ Other objects ============================================
  public SearchManager getSearchManager() {return this.searchMan; }
  public WorkQueueXqF getWorkQueue() {return this.workQueue; }
  public ExecutorService getWorkExecutor() { return this.workExecutor; }
  public void setSearchManager(SearchManager oThis) {this.searchMan = oThis;}
  public void setWorkQueue(WorkQueueXqF oThis) {this.workQueue = oThis; }
  public void setWorkExecutor(ExecutorService oThis) {this.workExecutor = oThis; }
  public PrjTypeManager getPrjTypeManager() {return prjTypeManager;}
  public void setPrjTypeManager(PrjTypeManager oThis) { this.prjTypeManager = oThis;}
  public ExecuteXml getExe() { return this.objEx; }
  public Processor getSaxProc() { return this.objSaxon; }
  public String getHitsDir() {
    // Do we have a hitsdir defined?
    if (this.sHitsDir.isEmpty()) {
      // Calculate the directory where the .hits files are to be located for this project
      String sProjectFileName = Paths.get(this.getLocation()).getFileName().toString();
      sProjectFileName = sProjectFileName.substring(0, sProjectFileName.lastIndexOf("."));
      String sDir = this.getDstDir() + "/"+ sProjectFileName + "/hits";
      // Define our hitsdir
      this.sHitsDir = sDir;
    }
    // Return whatever value the HitsDir for this CRP has now
    return this.sHitsDir;
  }
  // ========== Location of the database, depending on the QC number
  public String getDbaseName(int iQCid) {
    return this.getHitsDir() + "/" + this.getName() + "_QC" + iQCid + "_Dbase.xml";
  }
  // ========== Location of the database, depending on the QC number and the filename
  public String getDbaseName(int iQCid, String sDbaseDir) {
    return sDbaseDir + "/" + this.getName()  + "_QC" + iQCid + "_Dbase.xml";
  }
  // ========== Location of the lexicon file, depending on the filename
  public String getLexName(String sFileName) {
    return this.getHitsDir() + "/" + sFileName + ".lex";
  }
  // =================== Text extension ========================================
  public String getTextExt() {
    return getTextExt(this.intProjType);
  }
  public static String getTextExt(ProjType ptThis) {
    switch (ptThis) {
      case ProjPsdx: return ".psdx";
      case ProjNegra: return ".negra";
      case ProjPsd: return ".psd";
      case ProjFolia: return ".folia.xml";
      case ProjAlp: return ".xml";
      case Dbase: return ".xml";
    }
    // Unidentified project
    return ".xml";
  }
  public static String getTextExt(String sExtType) {
    switch(sExtType.toLowerCase()) {
      case "psdx": return ".psdx";
      case "folia": return ".folia.xml";
      case "negra": return ".negra";
      case "psd": return ".psd";
      case "alp": return ".xml";
      case "dbase": return ".xml";
    }
    // Unidentified project
    return ".xml";
  }
  public static List<String> getTextExtList() {
    List<String> lBack = new ArrayList<>();
    // Make sure these are in decreasing size
    // Make sure to include GZ where needed
    lBack.add(getTextExt(ProjType.ProjFolia) + ".gz");
    lBack.add(getTextExt(ProjType.ProjFolia));
    lBack.add(getTextExt(ProjType.ProjNegra));
    lBack.add(getTextExt(ProjType.ProjPsdx));
    lBack.add(getTextExt(ProjType.ProjPsd));
    lBack.add(getTextExt(ProjType.ProjAlp));
    // NOTE: do not include Dbase
    return lBack;
  }
  public void setTextExt(ProjType ptThis) {
    this.intProjType = ptThis;
  }
  public String getTagPara() {
    return getTagPara(this.intProjType);
  }
  public static String getTagPara(ProjType ptThis) {
    switch (ptThis) {
      case ProjPsdx: return "@Para";
      case ProjNegra: return "";
      case ProjPsd: return "";
      case ProjFolia: return "p";
      case ProjAlp: return "";
      case Dbase: return "";
    }
    // Unidentified project
    return ".xml";
  }
  public String getTagHeader() {
    return getTagHeader(this.intProjType);
  }
  public static String getTagHeader(ProjType ptThis) {
    switch (ptThis) {
      case ProjPsdx: return "teiHeader";
      case ProjNegra: return "teiHeader";
      case ProjPsd: return "";
      case ProjFolia: return "metadata";
      case ProjAlp: return "";
      case Dbase: return "General";
    }
    // Unidentified project
    return ".xml";
  }
  public String getTagLine() {
    return getTagLine(this.intProjType);
  }
  public static String getTagLine(ProjType ptThis) {
    switch (ptThis) {
      case ProjPsdx: return "forest";
      case ProjNegra: return "s";
      case ProjPsd: return "";
      case ProjFolia: return "s";
      case ProjAlp: return "node";
      case Dbase: return "Result";
    }
    // Unidentified project
    return ".xml";
  }
  public String getNodeLine() {
    return getNodeLine(this.intProjType);
  }
  public static String getNodeLine(ProjType ptThis) {
    switch (ptThis) {
      case ProjPsdx: return "./descendant-or-self::forest[1]";
      case ProjNegra: return "./descendant-or-self::s[1]";
      case ProjPsd: return "";
      case ProjFolia: return "./descendant-or-self::s[1]";
      case ProjAlp: return "./descendant-or-self::node[1]";
      case Dbase: return "./descendant-or-self::Result[1]";
      default: return "";
    }
  }
  public String getNodeWords() {
    return getNodeWords(this.intProjType);
  }
  public static String getNodeWords(ProjType ptThis) {
    switch (ptThis) {
      case ProjPsdx: return "./descendant-or-self::eLeaf[@Type='Vern']";
      case ProjNegra: return "";  // TODO
      case ProjPsd: return "";
      case ProjFolia: return "./descendant-or-self::w[not(@class) or @class='Vern']";
      case ProjAlp: return "";    // TODO
      case Dbase: return "";      // TODO
      default: return "";
    }
  }
  
  public String getNodeLast() {
    return getNodeLast(this.intProjType);
  }
  public static String getNodeLast(ProjType ptThis) {
    switch (ptThis) {
      case ProjPsdx: return "./descendant::eTree[last()]";
      case ProjNegra: return "./descendant::su[last()]";
      case ProjPsd: return "";
      case ProjFolia: return "./descendant::su[last()]";
      case ProjAlp: return "./descendant::node[last()]";
      case Dbase: return "./descendant::Result[last()]";
      default: return "";
    }
  }
  public QName getAttrLineId() {
    return getAttrLineId(this.intProjType);
  }
  public static QName getAttrLineId(ProjType ptThis) {
    switch(ptThis) {
      case ProjPsdx: return new QName("", "", "forestId");
      case ProjFolia: return new QName("xml", "http://www.w3.org/XML/1998/namespace", "id");
      case ProjNegra: return new QName("", "", "");
      case ProjAlp: return new QName("", "", "");
      case ProjPsd: return new QName("", "", "");
      case Dbase: return new QName("", "", "ResId");
      default: return new QName("", "", "id");
    }
  }
  public QName getAttrConstId() {
    return getAttrConstId(this.intProjType);
  }
  public static QName getAttrConstId(ProjType ptThis) {
    switch(ptThis) {
      case ProjPsdx: return new QName("", "", "Id");
      case ProjFolia: return new QName("xml", "http://www.w3.org/XML/1998/namespace", "id");
      case ProjNegra: return new QName("", "", "");
      case ProjAlp: return new QName("", "", "");
      case ProjPsd: return new QName("", "", "");
      case Dbase: return new QName("", "", "forestId");
      default: return new QName("", "", "id");
    }
  }
  public QName getAttrPartId() {
    return getAttrConstId(this.intProjType);
  }
  public static QName getAttrPartId(ProjType ptThis) {
    switch(ptThis) {
      case ProjPsdx: return new QName("", "", "");
      case ProjFolia: return new QName("", "", "");
      case ProjNegra: return new QName("", "", "");
      case ProjAlp: return new QName("", "", "");
      case ProjPsd: return new QName("", "", "");
      case Dbase: return new QName("", "", "File");
      default: return new QName("", "", "");
    }
  }


  // =================== Compatibility with .NET: get 'table' ==================
  public List<JSONObject> getTable(String sName) {
    switch(sName) {
      case "Query":
        return lQueryList;
      case "Qwiz":
        return lQueryWiz;
      case "Definition":
        return lDefList;
      case "DbFeat":
        return lDbFeatList;
      case "QC":
        return lQueryConstructor;
      default:
        return null;
    }
  }
  // =================== private methods for internal use ======================
  /**
   * getSetting -- retrieve the value of 'Setting' named @sName
   * 
   * @param sName
   * @return 
   */
  private String getSetting(String sName) {
    String strValue = ""; // Default value
    
    try {
      String sExp = "./descendant::Setting[@Name='" + sName + "']";
      Node ndxThis = (Node) xpath.compile(sExp).evaluate(this.docProject, XPathConstants.NODE);
      if (ndxThis == null) return "";
      // Get the value of the attribute
      strValue = ndxThis.getAttributes().getNamedItem("Value").getNodeValue();
      // strValue = ndxThis.getNodeValue();
    } catch (XPathExpressionException ex) {
      errHandle.DoError("Problem with getSetting [" + sName + "]", ex);
    }
    // Return the result
    return strValue;
  }
  /**
   * setSetting -- set 'Setting' named @sName at value @sValue
   * 
   * @param sName
   * @param sValue
   * @return 
   */
  private boolean setSetting(String sName, String sValue) {
    
    try {
      Node ndxThis = (Node) xpath.evaluate("./descendant::Setting[@Name='" + sName + "']", 
                                           this.docProject, XPathConstants.NODE);
      // Validate
      if (ndxThis == null) {
        // TODO: this particular setting is not available, so it should be *added*
        Node ndNew = this.docProject.createElement("Setting");
        ((Element) ndNew).setAttribute("Name", sName);
        ((Element) ndNew).setAttribute("Value", sValue);
        Node ndParent = (Node) xpath.evaluate("./descendant::General[1]", 
                                           this.docProject, XPathConstants.NODE);
        ndParent.appendChild(ndNew);
        // Get the node again
        ndxThis = (Node) xpath.evaluate("./descendant::Setting[@Name='" + sName + "']", 
                                           this.docProject, XPathConstants.NODE);
        // Validate once more...
        if (ndxThis == null) {
          // SOmething is really wrong
          return false;
        }
      }
      // ndxThis.setNodeValue(sValue);
      ndxThis.getAttributes().getNamedItem("Value").setNodeValue(sValue);
    } catch (XPathExpressionException ex) {      
      errHandle.DoError("Problem with setSetting [" + sName + "]", ex);
    }
    // Return positively
    return true;
  }
  
  /**
   * getIdField -- get the name of the @id field
   * 
   * @param sItemType
   * @return 
   */
  private String getIdField(String sItemType) {
    switch(sItemType.toLowerCase()) {
      case "query": return "QueryId";
      case "qwiz": return "QwizId";
      case "definition": return "DefId";
      case "constructor": case "qc": return "QCid";
      case "dbfeat": return "DbFeatId";
      default: return "";
    }
  }
  /**
   * getItemTag
   *    Translate the item type into the name of a table
   * 
   * @param sItemType
   * @return 
   */
  private String getItemTag(String sItemType) {
    switch(sItemType.toLowerCase()) {
      case "query":       return "Query";
      case "qwiz":        return "Qwiz";
      case "definition":  return "Definition";
      case "constructor": return "QC";
      case "dbfeat":      return "DbFeat";
      default: return sItemType;
    }
  }
  /**
   * setItemValue -- set 'Setting' named @sName at value @sValue
   * 
   * @param sItemType
   * @param iItemId
   * @param sName
   * @param sValue
   * @return 
   */
  private boolean setItemValue(String sItemType, int iItemId, String sName, String sValue) {
    
    try {
      // Get the name of the @id field
      String sIdField = getIdField(sItemType);
      // Translate type into XML tag
      String sItemTag = getItemTag(sItemType);
      // Get the correct node from this list
      Node ndxThis = (Node) xpath.evaluate("./descendant::"+sItemTag+"[@" + sIdField+"="+iItemId+"]", 
                                           this.docProject, XPathConstants.NODE);
      // Validate
      if (ndxThis == null) {
        // SOmething is really wrong
        return false;
      }
      // Get attribute @sName
      Node ndAttr = ndxThis.getAttributes().getNamedItem(sName);
      if (ndAttr == null) {
        // It could be that we are dealing wiht a CHILD instead of an attribute
        NodeList ndList = ndxThis.getChildNodes();
        for (int i=0;i<ndList.getLength();i++) {
          Node ndItem = ndList.item(i);
          if (ndItem.getNodeName().equals(sName)) {
            // Found the child named @sName
            ndAttr = ndItem;
            // Set the text content of this child
            ndAttr.setTextContent(sValue);
            // Return positively
            return true;
          }
        }
        // Double check the outcome
        if (ndAttr == null) {
          errHandle.DoError("setItemValue error: (" + sItemType + ","+iItemId+","+sName+","+sValue+")");
          return false;
        }
      }
      // Set attribute named @sName with value [sValue]
      ndAttr.setNodeValue(sValue);
      // Return positively (see further down...)
    } catch (XPathExpressionException ex) {      
      errHandle.DoError("Problem with setItemValue ["+sItemType+"/"+iItemId+"/@" + sName + "]", ex);
      // TODO: why not return 'false' here??
    }
    // Return positively
    return true;
  }
  
  /**
   * setItemId -- set the id value of sItemType
   * 
   * @param sItemType
   * @param iItemId
   * @param sName
   * @param iValue
   * @return 
   */
  private boolean setItemId(String sItemType, int iItemId, String sValue) {
    String sIdField = "";
    try {
      // Get the name of the @id field
      sIdField = getIdField(sItemType);
      // Translate type into XML tag
      String sItemTag = getItemTag(sItemType);
      // Get the correct node from this list
      Node ndxThis = (Node) xpath.evaluate("./descendant::"+sItemTag+"[@" + sIdField+"="+iItemId+"]", 
                                           this.docProject, XPathConstants.NODE);
      // Validate
      if (ndxThis == null) {
        // SOmething is really wrong
        return false;
      }
      // Get attribute @sIdField
      Node ndAttr = ndxThis.getAttributes().getNamedItem(sIdField);
      if (ndAttr == null) {
        errHandle.DoError("setItemId error: (" + sItemType + ","+iItemId+","+sIdField+","+sValue+")");
        return false;
      }
      // Set attribute named @sName with value [sValue]
      ndAttr.setNodeValue(sValue);
      // Return positively (see further down...)
    } catch (XPathExpressionException ex) {      
      errHandle.DoError("Problem with setItemId ["+sItemType+"/"+iItemId+"/@" + sIdField + "]", ex);
      // TODO: why not return 'false' here??
    }
    // Return positively
    return true;
  }
  
  /**
   * getDateSetting -- get the date setting of [sName]
   * 
   * @param sName
   * @return 
   */
  private String getDateSetting(String sName) {
    String strValue = ""; // Default value
    Date dtThis;
    
    try {
      String sExp = "./descendant::Date"+sName;
      Node ndxThis = (Node) xpath.compile(sExp).evaluate(this.docProject, XPathConstants.NODE);
      if (ndxThis == null) return "";
      // Get the value of the node
      strValue = ndxThis.getTextContent();
    } catch (XPathExpressionException ex) {
      logger.error("Problem with getDateSetting [" + sName + "]", ex);
    }
    // Return the result
    return strValue;
  }
  private boolean setDateSetting(String sName, String sValue) {
    
    try {
      String sExp = "./descendant::Date"+sName;
      Node ndxThis = (Node) xpath.compile(sExp).evaluate(this.docProject, XPathConstants.NODE);
      // Validate
      if (ndxThis == null) return false;
      ndxThis.setTextContent(sValue);
    } catch (XPathExpressionException ex) {
      logger.error("Problem with setDateSetting [" + sName + "]", ex);
    }
    // Return positively
    return true;
  }

  /*
  public Date stringToDate(String s) throws ParseException {
    // Create the correct format of what we are expecting
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(sGregorianFormat);
   // Parse string to date
   return simpleDateFormat.parse(s);       
  }
  public String dateToString(Date dtThis) {
    // Create the format
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(sGregorianFormat);
    return simpleDateFormat.format(dtThis);
  }
  */
  
  /**
   * changeLocation
   *    Save the current CRP as XML in a location with filename @sNewName
   *    Then remove the file at the old location
   * 
   * @param sNewName
   * @return 
   */
  public boolean changeLocation(String sNewName) {
    String sLocOld = this.Location;
    String sLocNew = "";
    
    try {
      // Determine the new location name
      sLocNew = FileIO.getDirectory(sLocOld)+"/"+sNewName+".crpx";
      // Try to change file name
      File fOld = new File(sLocOld);
      File fNew = new File(sLocNew);
      if (!fOld.renameTo(fNew)) {
        // Provide a warning
        errHandle.DoError("changeLocation of CRP did not succeed");
        return false;
      }
      // Set the new location
      this.setLocation(sLocNew);
      errHandle.debug("changeLocation: CRP renamed");
      
      /*
      // Save XML file into new location
      if (WriteXml(this.docProject, sLocNew)) {
        // Set the new location
        this.setLocation(sLocNew);
        // Remove the file from the old location
        File fOld = new File(sLocOld);
        if (fOld.delete()) {
          // Everything went well!
          return true;
        }
      } */
      
      // Return success
      return true;
    } catch (Exception ex) {
      errHandle.DoError("changeLocation error", ex);
      return false;
    }
  }
  
  /**
   * doChange
   *    Change the value of [sKey] to [sValue]
   * 
   * @param sKey
   * @param sValue
   * @param iItemId
   * @return 
   */
  public boolean doChange(String sKey, String sValue, int iItemId) {
    boolean bChanged = false;
    try {
      // Validate
      if (sKey.isEmpty()) return false;
      // Determine the section we are in
      if (iItemId <0) {
        switch (sKey.toLowerCase()) {
          case "author": if (!this.getAuthor().equals(sValue)) {this.setAuthor(sValue); bChanged =true; } 
            break;
          case "comments": if (!this.getComments().equals(sValue)) {this.setComments(sValue);  bChanged =true; } 
            break;
          case "datechanged": 
            // Special treatment: don't signal change, otherwise date will get changed again
            this.setDateChanged(DateUtil.stringToDate(sValue)); bChanged=false; 
            break;
          case "datecreated": if (!DateUtil.dateToString(this.getDateCreated()).equals(sValue)) {this.setDateCreated(DateUtil.stringToDate(sValue));  bChanged =true; } 
            break;
          case "follnum": if (this.getFollNum() != Integer.parseInt(sValue)) {this.setFollNum(Integer.parseInt(sValue));  bChanged =true; } 
            break;
          case "fortype": if (this.getForType()!= ForType.forValue(sValue)) {this.setForType(ForType.forValue(sValue));  bChanged =true; } 
            break;
          case "goal": if (!this.getGoal().equals(sValue)) {this.setGoal(sValue);  bChanged =true; } 
            break;
          case "name": if (!this.getName().equals(sValue)) {
              // If the @Name feature changes, we need to change the filename associated with us
              this.changeLocation(sValue);
              this.setName(sValue);  
              bChanged =true; 
            } 
            break;
          case "precnum": if (this.getPrecNum() != Integer.parseInt(sValue)) {this.setPrecNum(Integer.parseInt(sValue));  bChanged =true; } 
            break;
          case "projecttype": 
            // Get the correct representation of [sValue]
            String sNewProjectType = ProjType.getName(ProjType.getType(sValue));
            // Only make changes if it is really necessary
            if (!this.getProjectType().equals(sNewProjectType)) {
              this.setProjectType(sNewProjectType); 
              // Change in project type also means: 
              // (1) change in "GenInpExt"
              String sTextExt = getTextExt(ProjType.getType(sNewProjectType));
              this.setSetting("GenInpExt", sTextExt);              
              // (2) change in "Source"
              this.setSetting("Source", "*" + sTextExt);              
              bChanged =true; 
            } 
            break;
          case "language": if (!this.getLanguage().equals(sValue)) {errHandle.debug("CrpChgDD language"); this.setLanguage(sValue);  bChanged =true; } 
            break;
          case "part": if (!this.getPart().equals(sValue)) {errHandle.debug("CrpChgDD part"); this.setPart(sValue);  bChanged =true; } 
            break;
          case "rules": if (!this.getRules().equals(sValue)) {errHandle.debug("CrpChgDD rules"); this.setRules(sValue);  bChanged =true; } 
            break;
          case "xqinput": if (!this.getXqInput().equals(sValue)) {errHandle.debug("CrpChgDD xqInput");  this.setXqInput(sValue);  bChanged =true; } 
            break;
          case "corpus":
            String[] arCrp = sValue.split(":");
            String sLng = arCrp[0];
            String sDir = "";
            if (arCrp.length>1) sDir = arCrp[1];
            if (!this.getLanguage().equals(sLng)) {this.setLanguage(sLng); errHandle.debug("CrpChgDD corpus:language");   bChanged =true; }
            if (!this.getPart().equals(sDir)) {this.setPart(sDir);  errHandle.debug("CrpChgDD corpus:part");  bChanged =true; }
            break;
          case "source":
            if (!this.getSource().equals(sValue)) {errHandle.debug("CrpChgDD source [" + this.getSource() + 
                    "] >> [" + sValue + "]"); this.setSource(sValue); bChanged = true; }
            break;
          case "dbaseinput": 
            // ========= Debugging =============
            errHandle.debug("CrpChg dbaseinput: " + 
                    (this.getDbaseInput().equals(sValue) ? "change" : "keep") + 
                    " (current=" + this.getDbaseInput() + ", new="+sValue+")");
            // =================================
            if (!this.getDbaseInput().equals(sValue)) {
              this.setDbaseInput(sValue); 
              bChanged = true;
              // Additional changes when the new sValue is 'false':
              if (sValue.equals("False")) {
                // Need to put the "source" value back up again
                this.setSource("*" + this.getTextExt());
                // ========= Debugging =============
                errHandle.debug("CrpChg dbaseinput: set source=" + this.getSource());
                // =================================
              }
            } 
            break;
          default:
            return errHandle.DoError("doChange: unknown key="+sKey, CorpusResearchProject.class);
        }
      } else {
        String[] arKey = sKey.split("[.]");
        // Validate
        if (arKey.length != 2) return errHandle.DoError("doChange: cannot handle key="+sKey, CorpusResearchProject.class);
        String sItemType = arKey[0];
        String sItemKey = arKey[1];
        int iIdx = -1;
        // Check for special keys: create, delete
        switch (sItemKey) {
          case "create":
            // Create specifics depend on the item
            switch(sItemType) {
              case "project":
                // Do not create just one new node, but create a whole new CRP

                break;
              case "query":
                if (createCrpListItem(lQueryList, "./descendant::QueryList", "Query", 
                                  "QueryId;Name;File;Goal;Comment;Created;Changed", "Text")) bChanged = true;
                break;
              case "qwiz":
                if (createCrpListItem(lQueryWiz, "./descendant::QwizList", "Qwiz", 
                                  "QwizId;Name;search;Goal;Comment;Created;Changed", "")) bChanged = true;
                break;
              case "definition":
                if (createCrpListItem(lDefList, "./descendant::DefList", "Definition", 
                                  "DefId;Name;File;Goal;Comment;Created;Changed", "Text")) bChanged = true;;
                break;
              case "constructor":
                if (createCrpListItem(lQueryConstructor, "./descendant::QueryConstructor", "QC", 
                                  "QCid;Input;Query;Output;Result;Cmp;Mother;Goal;Comment", "")) bChanged = true;;
                break;
              case "dbfeat":
                if (createCrpListItem(lDbFeatList, "./descendant::DbFeatList", "DbFeat", 
                                  "DbFeatId;Name;Pre;QCid;FtNum", "")) bChanged = true;;
                break;
            }            
            break;
          case "delete":
            // Delete specifics depend on the item
            switch(sItemType) {
              case "query":
                if (removeCrpListItem(lQueryList, "./descendant::QueryList/child::Query", "QueryId", iItemId)) bChanged = true;
                break;
              case "qwiz":
                if (removeCrpListItem(lQueryWiz, "./descendant::QwizList/child::Qwiz", "QwizId", iItemId)) bChanged = true;
                break;
              case "definition":
                if (removeCrpListItem(lDefList, "./descendant::DefList/child::Definition", "DefId", iItemId)) bChanged = true;;
                break;
              case "constructor":
                if (removeCrpListItem(lQueryConstructor, "./descendant::QueryConstructor/child::QC", "QCid", iItemId)) bChanged = true;;
                break;
              case "dbfeat":
                if (removeCrpListItem(lDbFeatList, "./descendant::DbFeatList/child::DbFeat", "DbFeatId", iItemId)) bChanged = true;;
                break;
            }            
            break;
          case "id":
            // THere is a change in the id value
            this.setItemId(sItemType, iItemId, sValue); bChanged = true;
            break;
          default:
            switch(sItemType) {
              case "query":
                iIdx = this.getListQueryId(iItemId);
                JSONObject oItemQry = this.getListQueryItem(iIdx);
                if (oItemQry.has(sItemKey)) {
                  if (!oItemQry.getString(sItemKey).equals(sValue)) {oItemQry.put(sItemKey, sValue); bChanged = true;} 
                } else {
                  return errHandle.DoError("doChange: cannot handle key="+sKey, CorpusResearchProject.class);
                }
                // Process changes
                if (bChanged) {
                  this.setListQueryItem(iIdx, oItemQry);
                  this.setItemValue("Query", iItemId, sItemKey, sValue);
                  // The change date of the definition needs to be adapted
                  this.setItemValue("Query", iItemId, "Changed", DateUtil.dateToString(new Date()));
                }
                break;
              case "qwiz":
                iIdx = this.getListQwizId(iItemId);
                JSONObject oItemQwiz = this.getListQwizItem(iIdx);
                if (oItemQwiz.has(sItemKey)) {
                  if (!oItemQwiz.getString(sItemKey).equals(sValue)) {oItemQwiz.put(sItemKey, sValue); bChanged = true;} 
                } else {
                  return errHandle.DoError("doChange[Qwiz]: cannot handle key="+sKey, CorpusResearchProject.class);
                }
                // Process changes
                if (bChanged) {
                  this.setListQwizItem(iIdx, oItemQwiz);
                  this.setItemValue("Qwiz", iItemId, sItemKey, sValue);
                  // The change date of the definition needs to be adapted
                  this.setItemValue("Qwiz", iItemId, "Changed", DateUtil.dateToString(new Date()));
                }
                break;
              case "definition":
                iIdx = this.getListDefId(iItemId);
                // Double check what we get
                if (iIdx<0) {
                  return errHandle.DoError("doChange: cannot handle getListDefId=-1 for iItemId="+iItemId, CorpusResearchProject.class);
                }
                JSONObject oItemDef = this.getListDefItem(iIdx);
                if (oItemDef.has(sItemKey)) {
                  if (!oItemDef.getString(sItemKey).equals(sValue)) {oItemDef.put(sItemKey, sValue); bChanged = true;} 
                } else {
                  return errHandle.DoError("doChange: cannot handle key="+sKey, CorpusResearchProject.class);
                }
                // Process changes
                if (bChanged) { 
                  this.setListDefItem(iIdx, oItemDef);
                  this.setItemValue("Definition", iItemId, sItemKey, sValue);
                  // The change date of the definition needs to be adapted
                  this.setItemValue("Definition", iItemId, "Changed", DateUtil.dateToString(new Date()));
                }
                break;
              case "constructor":
                iIdx = this.getListQcId(iItemId);
                JSONObject oItemQc = this.getListQCitem(iIdx);
                if (oItemQc.has(sItemKey)) {
                  if (!oItemQc.getString(sItemKey).equals(sValue)) {oItemQc.put(sItemKey, sValue); bChanged = true;} 
                } else {
                  return errHandle.DoError("doChange: cannot handle key="+sKey, CorpusResearchProject.class);
                }
                // Process changes
                if (bChanged) {
                  this.setListQCitem(iIdx, oItemQc);
                  this.setItemValue("QC", iItemId, sItemKey, sValue);
                }
                break;
              case "dbfeat":
                iIdx = this.getListDbFeatId(iItemId);
                JSONObject oItemDbf = this.getListDbFeatItem(iIdx);
                if (oItemDbf.has(sItemKey)) {
                  if (!oItemDbf.getString(sItemKey).equals(sValue)) {oItemDbf.put(sItemKey, sValue); bChanged = true;} 
                } else {
                  return errHandle.DoError("doChange: cannot handle key="+sKey, CorpusResearchProject.class);
                }
                // Process changes
                if (bChanged) {
                  this.setListDbFeatItem(iIdx, oItemDbf);
                  this.setItemValue("DbFeat", iItemId, sItemKey, sValue);
                }
                break;
              default: return errHandle.DoError("doChange: cannot handle key="+sKey, CorpusResearchProject.class);
            }
            break;
        }

      }
      // the change date of the whole CRP needs to be adapted
      if (bChanged) this.setDateChanged(new Date());
      // Return positively
      return bChanged;
    } catch (Exception ex) {
      errHandle.DoError("doChange error:", ex, CorpusResearchProject.class);
      return false;
    }
  }
  /**
   * doChange
   *    Change a key/value item within one of the lists
   * 
   * @param sList   - Can be: Query, Definition, Constructor, DbFeat
   * @param iListId - The id for the list's item
   * @param sKey    - The key name *within* the list
   * @param sValue  - The new value for the key *within* the list
   * @return 
   */
  public boolean doChange(String sList, int iListId, String sKey, String sValue) {
    List<JSONObject> lstThis = null;// The list of JSONObjects
    String sFeatId;                 // Name of the field where the list's id is kept

    try {
      // Validate
      if (sList.isEmpty() || iListId<0 || sKey.isEmpty()) return false;
      // Find the list
      switch (sList.toLowerCase() ) {
        case "query":       sFeatId = "QueryId";  lstThis = this.getListQuery();  break;
        case "qwiz":        sFeatId = "QwizId";   lstThis = this.getListQwiz();   break;
        case "definition":  sFeatId = "DefId";    lstThis = this.getListDef();    break;
        case "qc":          sFeatId = "QCid";     lstThis = this.getListQC();     break;
        case "dbfeat":      sFeatId = "DbFeatId"; lstThis = this.lDbFeatList;     break;
        default:
          errHandle.DoError("doChange: unknown list="+sList, CorpusResearchProject.class);
          return false;
      }
      // Get the item *within* the list
      for (JSONObject oThis : lstThis) {
        if (oThis.getInt(sFeatId) == iListId) {
          // Check if the key is in this item
          if (!oThis.has(sKey)) return  errHandle.DoError("doChange: unknown list key="+sKey, CorpusResearchProject.class);
          // Set the key with the new value
          oThis.put(sKey, sValue);
          // Save changes immediately and return the save status
          return this.Save();
        }
      }
      
      // Getting here means that we could not find the correct list/key combination
      return false;
    } catch (Exception ex) {
      errHandle.DoError("doChange error:", ex, CorpusResearchProject.class);
      return false;
    }
  }
 
  /* ---------------------------------------------------------------------------
   Name:    doSort
   Goal:    Sort the JSONObject ArrayList @sList on @sField
   History:
   21/apr/2015   ERK Created
   --------------------------------------------------------------------------- */
  public boolean doSort(String sList, String sField) { 
    // Check on the list
    switch(sList) {
      case "QClist":
        if (sField.equals("QCid")) {
          // Sort the QueryConstructor list on [QCid]
          Collections.sort(this.lQueryConstructor, new IntIdComparator("QCid") {});
        } else return false;
        break;
      case "QueryList":
        if (sField.equals("QueryId")) {
          // Sort the QueryConstructor list on [QueryId]
          Collections.sort(this.lQueryList, new IntIdComparator("QueryId") {});
        } else return false;
        break;
      case "QwizList":
        if (sField.equals("QwizId")) {
          // Sort the QueryConstructor list on [QueryId]
          Collections.sort(this.lQueryWiz, new IntIdComparator("QwizId") {});
        } else return false;
        break;
      case "DefList":
        if (sField.equals("DefId")) {
          Collections.sort(this.lDefList, new IntIdComparator("DefId") {});
        }
      case "DbFeatList":
        if (sField.equals("FtNum")) {
          Collections.sort(this.lDbFeatList, new IntIdComparator("FtNum") {});
        }
        break;
      default:
        return false;
    }
    // Return positively
    return true;
  }

  /* ---------------------------------------------------------------------------
     Name:     GetForTagName
     Goal:     Get the name of the <forest> equivalent tag for the current project
     History:
     21-09-2011  ERK Created for .NET CorpusStudio
     25/apr/2015 ERK transformed to Java CRPP
     --------------------------------------------------------------------------- */
  public String GetForTagName() {
    if (this.intProjType == ProjType.None) return "";
    switch(this.intProjType) {
      case ProjPsdx:
        return "<forest";
      case ProjNegra:
        return "<s";
      case ProjAlp:
        return "<node";
      case ProjFolia:
        return "<s";
      default:
        return "";
    }
  }


  // =================== Private methods ======================================
  /* ---------------------------------------------------------------------------
   Name:    DoInit
   Goal:    Any initialisations
   History:
   17/10/2014   ERK Created
   --------------------------------------------------------------------------- */
  private boolean DoInit() throws ParserConfigurationException {
    // Look for factory
    if (this.factory==null) return(false);
    // Set up factory
    this.factory.setIgnoringComments(true);  // Ignore comments
    this.factory.setCoalescing(true);        // Convert CDATA to Text nodes
    this.factory.setNamespaceAware(false);   // No namespace (default)
    this.factory.setValidating(false);       // Don't validate DTD
    // Use the factory to create a DOM parser
    parser = this.factory.newDocumentBuilder();
    // Return positively
    return(true);
  }

  /**
   * createCrpListItem
   *    Create a new XML node, appending it to the list in @sPath
   *    The new node gets attributes specified in @sAttribs
   *    It gets children specified in @sChildren
   * 
   * @param lThis
   * @param sPath
   * @param sTagName
   * @param sAttribs
   * @param sChildren
   * @return 
   */
  private boolean createCrpListItem(List<JSONObject> lThis, String sPath, String sTagName, String sAttribs, 
          String sChildren) {
    String sIdName="";        // Name of the id field 
    Node ndParent = null;     // Parent under which we will add
    int iIdValue = -1;        // Value of the new id
    
    try {
      // Get a list of nodes along sPath
      ndParent = (Node) xpath.evaluate(sPath, this.docProject, XPathConstants.NODE);
      // Validate: any result?
      if (ndParent == null) {
        // Getting here means there is no proper parent path
        ndParent = (Node) xpath.evaluate("/CorpusResearchProject", this.docProject, XPathConstants.NODE);
        if (ndParent == null) return false;
        String sParent = "";
        switch (sTagName) {
          case "Query": sParent = "QueryList"; break;
          case "Qwiz": sParent = "QwizList"; break;
          case "Definition": sParent = "DefList"; break;
          case "QC": sParent = "QueryConstructor"; break;
          case "DbFeat": sParent = "DbFeatList"; break;
          default: return false;
        }
          
        Element ndNew = this.docProject.createElement(sParent);
        ndParent.appendChild(ndNew);
        // Try again
        ndParent = (Node) xpath.evaluate(sPath, this.docProject, XPathConstants.NODE);
        if (ndParent == null) return false;
      }
      // Create a new Node element
      Element ndNew = this.docProject.createElement(sTagName);
      // Create a new object for the list
      JSONObject oNew = new JSONObject();
      // Add the attributes
      String[] arAttr = sAttribs.split("[;]");
      for (int i=1;i<arAttr.length; i++) {
        // Add this attribute
        ndNew.setAttribute(arAttr[i], "");
        // Add it in the JSON
        oNew.put(arAttr[i], "");
      }
      // Create a new id element
      sIdName = arAttr[0];
      iIdValue = getNewId(lThis, sIdName);
      ndNew.setAttribute(sIdName, String.valueOf(iIdValue));
      // Also add it to the object -- but as an integer
      oNew.put(sIdName, iIdValue);
      if (!sChildren.isEmpty()) {
        // Add the children
        String[] arChild = sChildren.split("[;]");
        for (String sChild : arChild) {
          // Create child
          Node ndChild = this.docProject.createElement(sChild);
          ndChild.setNodeValue("");
          // Add this child
          ndNew.appendChild(ndChild);
          // Add it in the JSON
          oNew.put(sChild, "");
        }      
      }
      // Append the new node to this path
      ndParent.appendChild(ndNew);
      // Append the new JSON object to the list
      lThis.add(oNew);      
      // Return positively -- this means changes need to be saved!!
      return true;
    } catch (Exception ex) {
      errHandle.DoError("createCrpListItem problem", ex);
      return false;
    }
  }
  
  /**
   * removeCrpListItem
   *    Remove the item with the indicated id from the list
   * 
   * @param lThis
   * @param sPath
   * @param sIdName
   * @param iItemId
   * @return 
   */
  private boolean removeCrpListItem(List<JSONObject> lThis, String sPath, String sIdName, int iItemId) {
    NodeList ndxList = null;  // List of all the nodes on the specified path
    Node ndParent = null;     // Parent node
    boolean bFound = false;   // Found node to be deleted

    try {
      // Get a list of all the nodes on @sPath
      ndxList = (NodeList) xpath.evaluate(sPath, this.docProject, XPathConstants.NODESET);
      // Double check
      if (ndxList == null) return false;
      // Walk through the list
      for (int i = 0; i < ndxList.getLength(); i++) {
        // Get the id of this item
        int iId = Integer.parseInt(ndxList.item(i).getAttributes().getNamedItem(sIdName).getNodeValue());
        // Is this the correct one?
        if (iId == iItemId) {
          // this is the one that needs to be deleted: proceed!
          ndParent = ndxList.item(i).getParentNode();
          ndParent.removeChild(ndxList.item(i));
          // Indicate success
          bFound = true;
          break;
        }
      }
      // Proceed if we found it
      if (bFound) {
        for (int i=0;i<lThis.size();i++) {
          // Get this item
          JSONObject oThis = lThis.get(i);
          // Is this the one?
          if (oThis.getInt(sIdName) == iItemId) {
            // This is the one: delete it from the list
            lThis.remove(i);
            // Return positively -- this means changes need to be saved!!
            return true;
          }
        }
      }  
      // Getting here means failure
      return false;
    } catch (Exception ex) {
      errHandle.DoError("removeCrpListItem problem", ex);
      return false;
    }
  }
  
  /* ---------------------------------------------------------------------------
   Name:    ReadCrpList
   Goal:    Read a list of objects into a JSON list
   History:
   17/apr/2015   ERK Created
   18/apr/2016   ERK Make sure that *all* the attributes of each of the 'sChildren'
                     is copied
   --------------------------------------------------------------------------- */
  private boolean ReadCrpList(List<JSONObject> lThis, String sPath, String sAttribs, 
          String sChildren, boolean bChildLists) {
    NodeList ndxList; // List of all the nodes on the specified path
    Node ndAttr;      // The attribute we are accessing
    String sVal;

    try {
      ndxList = (NodeList) xpath.evaluate(sPath, this.docProject, XPathConstants.NODESET);
    } catch (XPathExpressionException ex) {
      logger.error("ReadCrpList problem with [" + sPath + "]",ex);
      return false;
    }
    // Double check
    if (ndxList == null) return false;
    // Make sure list is empty
    lThis.clear();
    // Convert the [sAttribs] into an array
    String[] arAttribs = sAttribs.split(";");
    String[] arChildren = sChildren.split(";");
    // Walk the nodelist and transfer items into [lThis]
    for (int i = 0; i < ndxList.getLength(); i++) {
      // Create a new object for the stack
      JSONObject oThis = new JSONObject();
      try {
        // Copy the attributes
        for (String sName : arAttribs) { 
          ndAttr = ndxList.item(i).getAttributes().getNamedItem(sName);
          if (ndAttr == null) {
            // This attribute does not exist, so we either have to leave it open or supply a null value
            oThis.put(sName, "");
          } else {
            sVal = ndAttr.getNodeValue();
            // oThis.put(sName, ndxList.item(i).getAttributes().getNamedItem(sName).getNodeValue()); 
            oThis.put(sName, sVal);
          }
        }
      } catch (RuntimeException ex) {
        logger.error("ReadCrpList problem: " + ex.getMessage());
        return false;
      }
      // Copy the children -- possibly as lists!
      for (int j=0; j< arChildren.length; j++) { 
        String sName = arChildren[j];
        if (!sName.isEmpty()) {
          // CHeck if we are expecting a single child or multiple children + attributes
          if (bChildLists) {
            // We are expecting multiple children + attributes => make lists
            Node ndxChild;
            try {
              // Read the first child of this kind
              ndxChild = (Node) xpath.compile("./child::" + sName+"[1]").evaluate(ndxList.item(i), XPathConstants.NODE);
              JSONArray arThis = new JSONArray();
              // Loop through the children
              while (ndxChild != null) {
                // Add the information of this child to the JSON object
                JSONObject oQel = new JSONObject();
                NamedNodeMap attrs = ndxChild.getAttributes();
                for (int k=0;i<attrs.getLength(); k++) {
                  Attr attribute = (Attr)attrs.item(k);
                  oQel.put(attribute.getName(), attribute.getValue());
                }
                // Add the element to the array
                arThis.put(oQel);
                // Try to find the next child
                ndxChild = (Node) xpath.compile("./following-sibling::" + sName+"[1]").evaluate(ndxChild, XPathConstants.NODE);
              }
              // Add the array to the output object
              oThis.put(sName, arThis);
            } catch (XPathExpressionException ex) {
              logger.error("ReadCrpList: Cannot access child", ex);
              return false;
            }
            
          } else {
            // Expecting only one single child
            Node ndxChild;
            try {
              ndxChild = (Node) xpath.compile("./child::" + sName).evaluate(ndxList.item(i), XPathConstants.NODE);
              if (ndxChild != null) {
                oThis.put(sName, ndxChild.getTextContent());
              }
            } catch (XPathExpressionException ex) {
              logger.error("ReadCrpList: Cannot access child", ex);
              return false;
            }
          }
        }
      }
      // Add the JSON object to the list
      lThis.add(oThis);
    }
    // Return positively
    return true;
  }
  /* ---------------------------------------------------------------------------
   Name:    WriteCrpListItem
   Goal:    Write an attribute or child value into an item in a JSON list
            And also write the value to the underlying XML object [this.docProject]
   History:
   17/apr/2015   ERK Created
   --------------------------------------------------------------------------- */
  private boolean WriteCrpListItem(List<JSONObject> lThis, String sPath, int iItem, String sType, String sName, String sValue) {
    NodeList ndxList = null;  // List of all the nodes on the specified path
    Node ndxTarget = null;    // Working node
    try {
      ndxList = (NodeList) xpath.evaluate(sPath, this.docProject, XPathConstants.NODESET);
    } catch (XPathExpressionException ex) {
      logger.error("WriteCrpListItem problem with [" + sPath + "]",ex);
      if (ndxList == null) return false;
    }
    // Double check
    if (ndxList == null) return false;
    // Find the correct object in the list
    JSONObject oThis = lThis.get(iItem);
    // Set the attribute or child
    oThis.put(sName, sValue);
    // Set the value of the XML item
    switch (sType) {
      case "attribute":
        ndxList.item(iItem).getAttributes().getNamedItem(sName).setNodeValue(sValue);
        break;
      case "child":
        try {
          ndxTarget = (Node) xpath.evaluate("./child::" + sName, ndxList.item(iItem), XPathConstants.NODE);
        } catch (XPathExpressionException ex) {
          logger.error("WriteCrpListItem problem with [" + sPath + "/child::" + sName + "]",ex);
          if (ndxTarget == null) return false;
        } // Assign the value to this node
        ndxTarget.setNodeValue(sValue);
        break;
      default:
        return false;
    }
    
    // Return positively
    return true;
  }
  
  /**
   * getNewId
   *    Derive a new id value
   * 
   * @param oList
   * @param sIdField
   * @return 
   */
  private int getNewId(List<JSONObject> oList, String sIdField) {
    try {
      // Validate
      if (oList == null || sIdField.isEmpty()) return -1;
      // Consider the length
      if (oList.isEmpty()) return 1;
      // Look at the *last* value in the list!!
      int iLastId = oList.get(oList.size()-1).getInt(sIdField);
      // Return one higher than the last id
      return iLastId +1;
    } catch (Exception ex) {
      errHandle.DoError("CorpusResearchProject/getNewId problem", ex);
      return -1;
    }
  }

}


/* ---------------------------------------------------------------------------
 Class:   QCidComparator
 Goal:    Sort the QC list on @QCid
 History:
 21/apr/2015   ERK Created
 --------------------------------------------------------------------------- */
class QCidComparator implements Comparator {
  public int compare(JSONObject c1, JSONObject c2) {
    int iId1 = c1.getInt("QCid"); int iId2 = c2.getInt("QCid");
    if (iId1 == iId2) return 0; 
    else return (iId1 < iId2) ? -1 : 1;
  }

  @Override
  public int compare(Object t, Object t1) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  } 
}

/* ---------------------------------------------------------------------------
 Class:   IntIdComparator
 Goal:    Sort a JSONObject list on an integer element
 History:
 22/apr/2015   ERK Created
 --------------------------------------------------------------------------- */
class IntIdComparator implements Comparator {
  private String sIntId;
  public IntIdComparator(String sField) {
    sIntId = sField;
  }
  public int compare(JSONObject c1, JSONObject c2) {
    int iId1 = c1.getInt(sIntId); int iId2 = c2.getInt(sIntId);
    if (iId1 == iId2) return 0; 
    else return (iId1 < iId2) ? -1 : 1;
  }

  @Override
  public int compare(Object t, Object t1) {
    JSONObject c1 = (JSONObject) t;
    JSONObject c2 = (JSONObject) t1;
    return compare(c1,c2);
  } 
}
