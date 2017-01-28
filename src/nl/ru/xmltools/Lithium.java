/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.xmltools;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import net.sf.saxon.s9api.QName;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.xq.English;
import nl.ru.svgtools.GraphAbstract;
import nl.ru.svgtools.LithiumControl;
import nl.ru.svgtools.Point;
import nl.ru.svgtools.ShapeBase;
import nl.ru.svgtools.Size;
import nl.ru.util.ByRef;

/**
 *
 * @author erwin
 */
public class Lithium {
  // =================== ENUMs ==================================
  public enum ShapeTypes { Rectangular, Oval, TextLabel; }    // Shape types available in assembly
  public enum TreeDirection { Vertical, Horizontal;   }       // Direction in which tree layout spreads the diagram
  public enum ConnectionType {Default, Traditional, Bezier; } // Types of connections in this assembly
  // This class uses a logger
  protected final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Lithium.class);
  // ================= Parameters ===============================
  protected TreeDirection layoutDirection = TreeDirection.Vertical;
  protected int wordSpacing = 20;                   // Space between nodes
  protected int branchHeight = 60;                  // Height between branches
  protected boolean layoutEnabled = true;           // Tree layout algorithm does its work by default
  protected final String defaultRootName = "Root";  // Default name when root is added
  protected GraphAbstract graphAbstract;            // Abstract representation of the graph
  // protected Entity hoveredEntity;                   // Entity hovered by the mouse
  // protected Entity selectedEntity;                  // Unique entity currently selected
  protected int selectedId;
  protected int hoveredId;
  protected boolean tracking = false;
  protected Point refp;
  // protected ContextMenu menu;
  // protected Random rnd;
  // protected Proxy proxy;
  protected boolean globalMove = false;
  protected Size gridSize = new Size(10,10);
  // protected Connection neoCon = null;
  protected ShapeBase memChild = null;
  protected ShapeBase memParent = null;
  protected ConnectionType connectionType = ConnectionType.Default; // TYpe of connection
  // ================= Local to this class ======================
  // private ShapeBase root = null;
  private String loc_strSelId = "";   // ID of the selected node
  private XmlNode ndxSelTree = null;  // The selected tree (if any is selected)
  private CorpusResearchProject.ProjType iPrj = null;
  private QName sAttrConstId;
  private QName sAttrLineId;
  private List<ShapeBase> Shapes = new ArrayList<>();
  private List<String> colCollapsedConst = new ArrayList<>();
  private LithiumControl objLitTree = null;
  private String sConvType = "";
  
  public Lithium(CorpusResearchProject.ProjType iType, String sConv) {
    this.iPrj = iType;
    this.sAttrConstId = CorpusResearchProject.getAttrConstId(iType);
    this.sAttrLineId = CorpusResearchProject.getAttrLineId(iType);
    this.objLitTree = new LithiumControl();
    this.sConvType = sConv;
  }

  /**
   * MakeLitTree
   *    Convert the tree starting at [ndxThis] to an SVG element
   * 
   * @param objLitTree
   * @param ndxTree
   * @param ndxSel
   * @return String
   */
  public String MakeLitTree(XmlNode ndxTree, XmlNode ndxSel) {
    String sBack = "";  // The string to be returned
    
    try {
      // Start making a new Lithium tree
      objLitTree.NewDiagram();
      // Set the root
      ShapeBase objRoot = objLitTree.Root();
      objRoot.NodeName = ndxTree.getNodeName().toString();
      objRoot.visible = true;
      switch(this.iPrj) {
        case ProjPsdx:
          switch (objRoot.NodeName) {
            case "forest":
              // Show the location
              objRoot.text = ndxTree.getAttributeValue("TextId") + " " + 
                      ndxTree.getAttributeValue("Location");
              objRoot.NodeId = ndxTree.getAttributeValue("forestId");
              break;
            case "eTree":
              // Show the POS label
              objRoot.text = ndxTree.getAttributeValue("Label");
              objRoot.NodeId = ndxTree.getAttributeValue("Id");
              break;
          }
          break;
        case ProjFolia:
          // TODO: implement FoLiA processing
          break;
      }
      objRoot.Fit();
      objRoot.Site().CenterRoot();
      // Set the selected tree
      ndxSelTree = ndxSel;
      this.loc_strSelId = (ndxSel == null) ? "" : ndxSel.getAttributeValue(sAttrConstId);
      // Recursively add the nodes
      ByRef<ShapeBase> objTree = new ByRef(null);
      objTree.argValue = objRoot;
      if (!WalkTree(objTree, ndxTree.SelectNodes("./child::*"))) {
        sBack = ""; return sBack;
      }
      // Expand the result
      objRoot = objTree.argValue;
      objRoot.Expand();
      // Collapse what needs be
      for (int i=0;i<objLitTree.Shapes().size();i++) {
        ShapeBase objItem = objLitTree.Shapes().get(i);
        switch(this.iPrj) {
          case ProjPsdx:
            // Check if this is a constituent that should be collapsed
            if (objItem.NodeName.equals("eTree") && colCollapsedConst.contains(objItem.NodeId) ) {
              objItem.Collapse(true);
            }
            break;
          case ProjFolia:
            break;
        }
      }
      // Calculate the positions of the tree elements
      objLitTree.DrawTree();
      
      // Show the tree
      // objLitTree.dumpShapes();
      
      // Convert the tree to SVG
      sBack = objLitTree.renderSvg();
      
      return sBack;
    } catch (Exception ex) {
      logger.error("MakeLitTree failed", ex);
      return sBack;
    }
  }
 
  /**
   * WalkTree
   *    Walk the constituent tree
   * 
   * @param oShape
   * @param lNodes
   * @return 
   */
  private boolean WalkTree(ByRef<ShapeBase> oShape, List<XmlNode> lNodes) {
    XmlNode ndxThis;          // One of the elements from [lNodes]
    ByRef<ShapeBase> objChildShape;  // SHape of one child
    
    try {
      // Validate
      if (lNodes == null || oShape == null || oShape.argValue == null) return false;
      
      // ==================== DEBUG =================
      // this.objLitTree.dumpShapes();
      // ============================================
      
      // Create the childshape
      objChildShape = new  ByRef(null);
      // Walk all the children
      for (int i=0;i<lNodes.size();i++) {
        // Get this node from the list
        ndxThis = lNodes.get(i);
        // Action depends on project type
        switch(this.iPrj) {
          case ProjPsdx:
            // Check the kind of node
            switch(ndxThis.getNodeName().toString()) {
              case "eLeaf": // THis is a terminal node -- but what kind?
                String strToken = ndxThis.getAttributeValue("Text");
                // Further action depends on the "TYpe" of eleaf
                switch (ndxThis.getAttributeValue("Type")) {
                  case "Vern":
                  case "Punct":
                    switch(this.sConvType) {
                      case "OE":
                        strToken = English.VernToEnglish(strToken);
                        break;
                    }
                    break;
                  case "Zero":
                  case "Star":
                    // No transformations needed
                    break;
                }
                // Create and add a child to the Shape object
                objChildShape.argValue = oShape.argValue.AddChild(strToken);
                // Set the features of this child
                objChildShape.argValue.setColor(NodeColor(ndxThis, false));
                objChildShape.argValue.NodeName = "eLeaf";
                objChildShape.argValue.NodeId = "leaf_" + ndxThis.SelectSingleNode("./parent::eTree").getAttributeValue("Id");
                break;
              case "eTree": // We do NOT want to look at certain nodes...
                String strLabel = ndxThis.getAttributeValue("Label");
                switch (strLabel) {
                  case "CODE": case "META": case "METADATA":
                    // skip this one
                    break;
                  default:
                    // This one is part of the clause
                    objChildShape.argValue = oShape.argValue.AddChild(strLabel);
                    // Is this node within the selection?
                    if (this.loc_strSelId.isEmpty())
                      this.loc_strSelId = "-1";
                    if (ndxThis.SelectSingleNode("ancestor-or-self::eTree[@Id=" + this.loc_strSelId + "]") == null) {
                      // Not selected -- default text
                      objChildShape.argValue.setColor(NodeColor(ndxThis, false));
                    } else {
                      // This is a selected node
                      objChildShape.argValue.setColor(NodeColor(ndxThis, true));
                      objChildShape.argValue.IsSelected(true);
                    }
                    objChildShape.argValue.NodeName = "eTree";
                    objChildShape.argValue.NodeId = ndxThis.getAttributeValue("Id");
                    // Now address the children
                    if (!WalkTree(objChildShape, ndxThis.SelectNodes("./child::*"))) return false;
                    break;
                }
                break;
              default:
                // Just skip other nodes!
                break;
            }
            break;
          case ProjFolia:
            break;
        }
      }

      return true;
    } catch (Exception ex) {
      logger.error("Lithium/WalkTree failed", ex);
      return false;
    }
  }
  
  /**
   * NodeColor
   *    Figure out which color this node should be
   * 
   * @param ndxThis
   * @param bSelected
   * @return 
   */
  private String NodeColor(XmlNode ndxThis, boolean bSelected) {
    String strNodeLabel = "";
    try {
      // Validate 
      if (ndxThis==null) return "white";
      // Get the name of the node
      String strNodeName = ndxThis.getNodeName().toString();
      // Action depends on type and node name
      switch(this.iPrj) {
        case ProjPsdx:
          // Find out what kind of node we need to take care of 
          switch(strNodeName) {
            case "eLeaf":
              // Get a nodelabel (type)
              strNodeLabel = ndxThis.getAttributeValue("Type");
              switch(strNodeLabel) {
                case "Vern": case "Punct":
                  return (bSelected) ? "lightblue" : "ivory";
                case "Zero": case "Star":
                  return (bSelected) ? "darkgreen" : "lightgreen";
              }
              break;
            case "eTree":
              // Get a nodelabel (type)
              strNodeLabel = ndxThis.getAttributeValue("Label");
              switch(strNodeLabel) {
                case "META": case "CODE":
                  return "lightgray";
                default:
                  return (bSelected) ? "darkgoldenrod" : "steelblue";
              }
            case "forest":
              return (bSelected) ? "darkgoldenrod" : "steelblue";
          }
          break;
        case ProjFolia:
          // TODO: determine which colors should be used for FOLIA
          
          break;
      }
      // Getting here means we have not detected anything
      return "white";
    } catch (Exception ex) {
      logger.error("Lithium/NodeColor failed", ex);
      return "";
    }
  }

}




