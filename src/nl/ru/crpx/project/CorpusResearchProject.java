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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
// import java.util.logging.Level;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import nl.ru.util.json.JSONObject;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/* ---------------------------------------------------------------------------
   Name:    CorpusResearchProject
   Goal:    Contains one corpus research project
   History:
   17/10/2014   ERK Created
   --------------------------------------------------------------------------- */
public class CorpusResearchProject {
  // This class uses a logger
  private static final Logger logger = Logger.getLogger(CorpusResearchProject.class);
  // ================== instance fields for the research project =============
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
      return(DoError("Cannot read [" + this.flProject + "]"));
    }    
    try {
      // Perform initialisations
      if (!DoInit()) return(DoError("unable to initialize"));
    } catch (ParserConfigurationException ex) {
      logger.error("Load crpx: could not configure parser", ex);
      return false;
    }
    mSet.clear();
    try {
      // Read the CRPX project xml file
      this.docProject = this.parser.parse(this.flProject);
    } catch (SAXException ex) {
      logger.error("Load crpx: SAX problem", ex);
      return false;
    } catch (IOException ex) {
      logger.error("Load crpx: IO problem", ex);
      return false;
    }
    // Debugging
    logger.debug("Root node=" + this.docProject.getDocumentElement().getTagName());
    try {
      NodeList ndxList = (NodeList) xpath.compile("./descendant::Setting").evaluate( this.docProject, XPathConstants.NODESET);
      logger.debug("Size = " + ndxList.getLength());
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
    // Load the settings: directories
    this.QueryDir = new File(getSetting("QueryDir"));
    this.DstDir = new File(getSetting("DstDir"));
    this.SrcDir = new File(getSetting("SrcDir"));
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
    if (!this.QueryDir.isDirectory())  if (!this.QueryDir.mkdir()) return(DoError("Could not create QueryDir [" + this.QueryDir + "]"));
    if (!this.DstDir.isDirectory())  if (!this.DstDir.mkdir()) return(DoError("Could not create DstDir [" + this.DstDir + "]"));
    if (!this.SrcDir.isDirectory())  if (!this.SrcDir.mkdir()) return(DoError("Could not create SrcDir [" + this.SrcDir + "]"));

    // Check the project type
    switch(this.ProjectType) {
      case "Xquery-psdx": // Okay, we are able to process this kind of project
        break;
      default: 
        return(DoError("Sorry, cannot process projects of type [" + this.ProjectType + "]"));
    }
    
    // Load the definitions
    
    // Return positively
    
    return(true);
  }
  public boolean Load(String loc) throws Exception {
    this.Location = loc;
    return(Load());
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
  // Set booleans
  public void setShowPsd(boolean bSet) { if (setSetting("ShowPsd", bSet ? "True" : "False")) {this.ShowPsd = bSet;}}
  public void setLocked(boolean bSet) { if (setSetting("Locked", bSet ? "True" : "False")) {this.Locked = bSet;}}
  public void setStream(boolean bSet) { if (setSetting("Stream", bSet ? "True" : "False")) {this.Stream = bSet;}}
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
  /* ---------------------------------------------------------------------------
   Name:    ReadCrpList
   Goal:    Read a list of objects into a JSON list
   History:
   17/apr/2015   ERK Created
   --------------------------------------------------------------------------- */
  private boolean ReadCrpList(List<JSONObject> lThis, String sPath, String sAttribs, String sChildren) {
    NodeList ndxList = null;  // List of all the nodes on the specified path
    try {
      ndxList = (NodeList) xpath.evaluate(sPath, this.docProject, XPathConstants.NODESET);
    } catch (XPathExpressionException ex) {
      logger.error("ReadCrpList problem with [" + sPath + "]",ex);
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
      // Copy the attributes
      for (String sName : arAttribs) { oThis.put(sName, ndxList.item(i).getAttributes().getNamedItem(sName).getNodeValue()); }
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
            logger.error("Cannot access child", ex);
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
  

  // =================== Private methods ======================================
  /* ---------------------------------------------------------------------------
   Name:    DoError
   Goal:    Show an error and return false
   History:
   17/10/2014   ERK Created
   --------------------------------------------------------------------------- */
  private boolean DoError(String msg) {
    System.err.println("CorpusResearchProject error:\n" + msg);
    return(false);
  }
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

}
