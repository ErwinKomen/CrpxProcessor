/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
// My own name
package nl.ru.crpx.project;
// The external libraries that I need
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import nl.ru.crpx.tools.ErrHandle;
import nl.ru.util.FileUtil;
import nl.ru.util.Json;
import nl.ru.util.json.JSONObject;
import static nl.ru.xmltools.XmlIO.WriteXml;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
  // ================== Enumerations in use ==================================
  public enum ProjType {
    ProjPsd, ProjPsdx, ProjNegra, ProjAlp, ProjFolia, None;
    public static ProjType getType(String sName) {
      switch (sName.toLowerCase()) {
        case "xquery-psdx": return ProjType.ProjPsdx;
        case "negra-tig": return ProjType.ProjNegra;
        case "penn-psd": return ProjType.ProjPsd;
        case "folia-xml": return ProjType.ProjFolia;
        case "alpino-xml": return ProjType.ProjAlp;
      }
      // Unidentified project
      return ProjType.None;
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
  // ================== instance fields for the research project =============
  private PrjTypeManager prjTypeManager;
  // =================== private variables ====================================
  private String Location = "";     // Full filename and location of this project
  private String Name = "";         // Name of this project (w.o. extension)
  private String Source = "";       // Source database (if any)
  private String Goal = "";         // The goal of this project
  private String Comments = "";     // Comments about this project
  private String Author = "";       // Author of this project
  private String ProjectType = "";  // The type of project (one of several)
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
  private XMLGregorianCalendar dtCreated; // Creation date of this project
  private XMLGregorianCalendar dtChanged; // Last change of this project
  private Document docProject;            // Project as XML document
  private DocumentBuilderFactory factory;
  private DocumentBuilder parser;
  private XPath xpath = XPathFactory.newInstance().newXPath();
  private Map mSet = new HashMap(); // Hash table with settings
  // Each project contains a number of lists
  static List<JSONObject> lDefList = new ArrayList<>();
  static List<JSONObject> lQueryList = new ArrayList<>();
  static List<JSONObject> lQueryConstructor = new ArrayList<>();
  static List<JSONObject> lDbFeatList = new ArrayList<>();
  static List<JSONObject> lPeriodInfo = new ArrayList<>();

  // ==================== Class initialisations ================================
  public CorpusResearchProject() {
    // Set error handler
    errHandle = new ErrHandle(CorpusResearchProject.class);
    // Other class-level initialisations
    this.docProject = null;
    this.parser = null;
    this.factory = DocumentBuilderFactory.newInstance();
    try {
      this.parser = this.factory.newDocumentBuilder();
    } catch (ParserConfigurationException ex) {
      logger.error("CorpusResearchProject: could not create documentbuilder", ex);
    }
    this.dirInput = null;
    this.flProject = null;
    // Set default project type
    this.intProjType = ProjType.ProjPsdx;
  }
  
  // =================== instance methods ======================================
  /* ---------------------------------------------------------------------------
   Name:    Load
   Goal:    Load a project. The name must be in @Location (see overloaded function)
   History:
   17/10/2014   ERK Created
   --------------------------------------------------------------------------- */
  public boolean Load() {
    // Validate crucial settings ---------------------------------
    if (this.Location.equals("")) return(false);
    this.flProject = new File(this.Location);
    if (!this.flProject.canRead()) {
      // The project cannot be read/opened
      return(errHandle.DoError("Cannot read [" + this.flProject + "]"));
    }    
    try {
      // Perform initialisations
      if (!DoInit()) return(errHandle.DoError("unable to initialize"));
    } catch (ParserConfigurationException ex) {
      logger.error("Load crpx: could not configure parser", ex);
      return false;
    }
    mSet.clear();
    try {
      // Read the CRPX project xml file
      this.docProject = this.parser.parse(this.flProject);
    } catch (SAXException ex) {
      logger.error("Load crpx file: SAX problem", ex);
      return false;
    } catch (IOException ex) {
      logger.error("Load crpx file: IO problem", ex);
      return false;
    }
    // Debugging
    logger.debug("Root node=" + this.docProject.getDocumentElement().getTagName());
    try {
      NodeList ndxList = (NodeList) xpath.compile("./descendant::Setting").evaluate( this.docProject, XPathConstants.NODESET);
      logger.debug("Settings of this crpx: " + ndxList.getLength());
    } catch (XPathExpressionException ex) {
      logger.error("debug", ex);
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
    // Determine the names of nodes available globally
    switch (this.intProjType) {
      case ProjPsdx:
        sNodeNameSnt = "forest"; sNodeNamePrg=""; sNodeNameWrd="eLeaf"; sNodeNameCns = "eTree"; break;
      case ProjFolia:
        sNodeNameSnt = "s"; sNodeNamePrg="p"; sNodeNameWrd="w"; sNodeNameCns = "su"; break;
      case ProjNegra:
        sNodeNameSnt = "s"; sNodeNamePrg=""; sNodeNameWrd="t"; sNodeNameCns = ""; break;
      case ProjAlp:
        sNodeNameSnt = "node"; sNodeNamePrg=""; sNodeNameWrd="node"; sNodeNameCns = "node"; break;
      case ProjPsd:
        sNodeNameSnt = ""; sNodeNamePrg=""; sNodeNameWrd=""; sNodeNameCns = ""; break;
      default:
    }

    // Calculate bXmlData
    switch(this.ProjectType.toLowerCase()) {
      case "xquery-psdx":
        this.bXmlData = true; break;
      default:
        this.bXmlData = true;
    }
    // Load the settings: directories
    this.QueryDir = new File(FileUtil.nameNormalize(getSetting("QueryDir")));
    this.DstDir = new File(FileUtil.nameNormalize(getSetting("DstDir")));
    this.SrcDir = new File(FileUtil.nameNormalize(getSetting("SrcDir")));
    // Load the list of definitions
    ReadCrpList(lDefList, "./descendant::DefList/child::Definition", 
                "DefId;Name;File;Goal;Comment;Created;Changed", "Text");
    // Load the list of queries
    ReadCrpList(lQueryList, "./descendant::QueryList/child::Query", 
                "QueryId;Name;File;Goal;Comment;Created;Changed", "Text");
    // Load the list of QC items (query constructor)
    ReadCrpList(lQueryConstructor, "./descendant::QueryConstructor/child::QC", 
                "QCid;Input;Query;Output;Result;Cmp;Mother;Goal;Comment", "");
    // Load the list of database features
    ReadCrpList(lDbFeatList, "./descendant::DbFeatList/child::DbFeat", 
                "DbFeatId;Name;Pre;QCid;FtNum", "");
    // Check directories
    if (!this.QueryDir.isDirectory())  if (!this.QueryDir.mkdir()) return(errHandle.DoError("Could not create QueryDir [" + this.QueryDir.toString() + "]"));
    if (!this.DstDir.isDirectory())  if (!this.DstDir.mkdir()) return(errHandle.DoError("Could not create DstDir [" + this.DstDir.toString() + "]"));
    if (!this.SrcDir.isDirectory())  if (!this.SrcDir.mkdir()) return(errHandle.DoError("Could not create SrcDir [" + this.SrcDir.toString() + "]"));

    // Check the project type
    switch(this.ProjectType) {
      case "Xquery-psdx": // Okay, we are able to process this kind of project
        break;
      default: 
        return(errHandle.DoError("Sorry, cannot process projects of type [" + this.ProjectType + "]"));
    }
    
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
    
    // Load the definitions
    
    // Close the project file
    
    // Return positively
    
    return(true);
  }
  public boolean Load(String loc) throws Exception {
    this.Location = loc;
    return(Load());
  }
  
  /* ---------------------------------------------------------------------------
   Name:    Save
   Goal:    Save a project as xml. 
            Use the project's location/name in this.Location
   History:
   20/04/2015   ERK Created
   --------------------------------------------------------------------------- */
  public boolean Save() {
    // Validate
    if (this.Location.isEmpty()) return false;
    if (this.docProject == null) return false;
    // Write as XML to the correct location
    boolean bFlag = WriteXml(this.docProject, this.Location);
    // Return the result of writing
    return bFlag;
  }
  
  public boolean Execute() {
    Execute objEx = null;
    boolean bFlag = true;
    // Create an execution object instance
        // Check what kind of project this is
    switch (this.intProjType) {
      case ProjPsdx: // Okay, we are able to process this kind of project
        // Do we need to do streaming or not?
        if (this.getStream()) {
          // objEx = new ExecutePsdxStream(this, objGen);
          objEx = new ExecutePsdxStream(this);
        } else {
          // objEx = new ExecutePsdxFast(this, objGen);
          objEx = new ExecutePsdxFast(this);
        }
        break;
      case ProjFolia:
        // Do we need to do streaming or not?
        if (this.getStream()) {
          // objEx  = new ExecuteFoliaStream(this, objGen);
          objEx  = new ExecuteFoliaStream(this);
        } else {
          // objEx  = new ExecuteFoliaFast(this, objGen);
          objEx  = new ExecuteFoliaFast(this);
        }
        break;
      case ProjAlp:
      case ProjPsd:
      case ProjNegra:
      default: 
       errHandle.DoError("Sorry, cannot execute projects of type [" + this.getProjectType() + "]");
    }
    if (objEx == null) return false;
    // Execute the queries using the chosen method
    bFlag = objEx.ExecuteQueries();
    // Check if the query-execution resulted in an interrupt
    if (!bFlag || objEx.bInterrupt) { errHandle.bInterrupt = true; return false; }
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
  }
  
  // ===================== Get and Set methods =================================
  public String getLocation() { return this.Location;}
  public String getName() { return this.Name;}
  public String getSource() { return this.Source;}
  public String getGoal() { return this.Goal;}
  public String getComments() { return this.Comments;}
  public String getAuthor() { return this.Author;}
  public String getProjectType() { return this.ProjectType;}
  // Set string values
  public void setLocation(String sValue) { this.Location = sValue;}
  public void setProjectType(String sValue) { this.ProjectType = sValue;}
  // Set string values and do that in the XML too
  public void setName(String sValue) { if (setSetting("Name", sValue)) { this.Name = sValue;}}
  public void setSource(String sValue) { if (setSetting("Source", sValue)) { this.Source = sValue;}}
  public void setGoal(String sValue) { if (setSetting("Goal", sValue)) { this.Goal = sValue;}}
  public void setComments(String sValue) { if (setSetting("Comments", sValue)) { this.Comments = sValue;}}
  public void setAuthor(String sValue) { if (setSetting("Author", sValue)) { this.Author = sValue;}}
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
  public XMLGregorianCalendar getDateChanged() {return this.dtChanged;}
  public XMLGregorianCalendar getDateCreated() {return this.dtCreated;}
  public void setPrecNum(XMLGregorianCalendar dValue) {if (setSetting("DateChanged", String.valueOf(dValue))) {this.dtChanged = dValue;}}
  public void setFollNum(XMLGregorianCalendar dValue) {if (setSetting("DateCreated", String.valueOf(dValue))) {this.dtCreated = dValue;}}
  // ================ Query Constructor elements
  public int getListQCsize() { return lQueryConstructor.size(); }
  public List<JSONObject> getListQC() { return lQueryConstructor;}
  public JSONObject getListQCitem(int iValue) {return lQueryConstructor.get(iValue); }
  // ================ Query list elements
  public int getListQuerySize() { return lQueryList.size(); }
  public List<JSONObject> getListQuery() { return lQueryList;}
  public JSONObject getListQueryItem(int iValue) {return lQueryList.get(iValue); }
  // ================ Definition list elements
  public int getListDefSize() { return lDefList.size(); }
  public List<JSONObject> getListDef() { return lDefList;}
  public JSONObject getListDefItem(int iValue) {return lDefList.get(iValue); }
  // ================ Database Feature list elements
  public int getListDbFeatSize() { return lDbFeatList.size(); }
  public JSONObject getListDbFeatItem(int iValue) {return lDbFeatList.get(iValue); }
  // =================== Compatibility with .NET: get 'table' ==================
  public List<JSONObject> getTable(String sName) {
    switch(sName) {
      case "Query":
        return lQueryList;
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
      logger.error("Problem with getSetting [" + sName + "]", ex);
    }
    // Return the result
    return strValue;
  }
  private boolean setSetting(String sName, String sValue) {
    
    try {
      Node ndxThis = (Node) xpath.evaluate("./descendant::Setting[@Name='" + sName + "']", 
                                           this.docProject, XPathConstants.NODE);
      ndxThis.setNodeValue(sValue);
    } catch (XPathExpressionException ex) {
      logger.error("Problem with setSetting [" + sName + "]", ex);
    }
    // Return positively
    return true;
  }

  // Make the project-type manager for this project available to others
  public PrjTypeManager getPrjTypeManager() {
    return prjTypeManager;
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
      case "DefList":
        if (sField.equals("DefId")) {
          Collections.sort(this.lDefList, new IntIdComparator("DefId") {});
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

  /* ---------------------------------------------------------------------------
   Name:    ReadCrpList
   Goal:    Read a list of objects into a JSON list
   History:
   17/apr/2015   ERK Created
   --------------------------------------------------------------------------- */
  private boolean ReadCrpList(List<JSONObject> lThis, String sPath, String sAttribs, String sChildren) {
    NodeList ndxList = null;  // List of all the nodes on the specified path
    Node ndAttr;              // The attribute we are accessing
    String sVal;

    try {
      ndxList = (NodeList) xpath.evaluate(sPath, this.docProject, XPathConstants.NODESET);
    } catch (XPathExpressionException ex) {
      logger.error("ReadCrpList problem with [" + sPath + "]",ex);
      return false;
    }
    // Double check
    if (ndxList == null) return false;
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
      // Copy the children
      for (int j=0; j< arChildren.length; j++) { 
        String sName = arChildren[j];
        if (sName != "") {
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
