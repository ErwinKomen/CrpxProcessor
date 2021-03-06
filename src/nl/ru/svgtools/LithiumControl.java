/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.svgtools;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
// import javafx.scene.paint.Color;

/**
 * LithiumControl
 *    Conversion of an XML parsed tree into an SVG tree
 * 
 * @author Erwin R. Komen
 */
public class LithiumControl {
  // <editor-fold desc="Enumerators"> 
  // =================== ENUMs ==================================
  public enum ShapeTypes { Rectangular, Oval, TextLabel; }    // Shape types available in assembly
  public enum TreeDirection { Vertical, Horizontal;   }       // Direction in which tree layout spreads the diagram
  public enum ConnectionType {Default, Traditional, Bezier; } // Types of connections in this assembly
  // This class uses a logger
  protected final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(LithiumControl.class);
  // </editor-fold>
  
  // <editor-fold desc="Fields"> 
  // ================= Parameters ===============================
  protected TreeDirection layoutDirection = TreeDirection.Vertical;
  protected int wordSpacing = 12;                   // Space between nodes
  protected int branchHeight = 70;                  // Height between branches
  protected boolean layoutEnabled = true;           // Tree layout algorithm does its work by default
  protected final String defaultRootName = "Root";  // Default name when root is added
  protected GraphAbstract graphAbstract;            // Abstract representation of the graph
  // protected Entity hoveredEntity;                   // Entity hovered by the mouse
  protected Entity selectedEntity;                  // Unique entity currently selected
  protected int selectedId;
  protected int hoveredId;
  protected boolean tracking = false;
  protected Point refp;
  protected Font font = new Font("Verdana", Font.PLAIN, 12);
  public int width = 1025;                          // My own width
  public int height = 631;                          // My own height
  public int minWidth = width;                      // Minimal width  of the tree that has been made
  public int minHeight = height;                    // Minimal height of the tree that has been made
  public int fontheight = 10;
  // protected ContextMenu menu;
  // protected Random rnd;
  // protected Proxy proxy;
  protected boolean globalMove = false;
  protected Size gridSize = new Size(10,10);
  // protected Connection neoCon = null;
  protected ShapeBase memChild = null;
  protected ShapeBase memParent = null;
  protected ConnectionType connectionType = ConnectionType.Traditional; // TYpe of connection
  int marginLeft = 10;			
  // ================= Local to this class ======================
  private ShapeBase root = null;
  private FontRenderContext frc;
  
  // ======================= Getters and setters
  public List<Connection> Connections() {return this.graphAbstract.connections; }
  public void Connections(List<Connection> value) {this.graphAbstract.connections = value; }
  public List<ShapeBase> Shapes() { return this.graphAbstract.shapes; }
  // </editor-fold>
   
  // <editor-fold desc="Class constructor">  
  public LithiumControl() {
    // Init the graph abstract
    this.graphAbstract  = new GraphAbstract();
    BufferedImage image = new BufferedImage(100,100, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = image.createGraphics();
    frc = g2d.getFontRenderContext();
  }
  // </editor-fold>

  // <editor-fold desc="Public methods">  
  /**
   * NewDiagram
   *    Starts a new diagram and forgets about everything
   *    Call the Save method before this if the current diagram
   *      needs to be kept
   * @return 
   */
  public boolean NewDiagram() {
    try {
      this.graphAbstract.clear();
      this.AddRoot(defaultRootName);
      CenterRoot();
      Invalidate();      
      return true;
    } catch (Exception ex) {
      logger.error("LithiumControl/NewDiagram failed", ex);
      return false;
    }
  }
  
  public ShapeBase Root() {
    try {
      return this.graphAbstract.Root();
    } catch (Exception ex) {
      logger.error("LithiumControl/Root failed", ex);
      return null;
    }
  }
  
  /**
   * CenterRoot
   *    Center the root on the control's canvas
   * 
   */
  public void CenterRoot() {
    this.graphAbstract.root.rectangle.Location(new Point(this.width/2, this.height/2));
    this.DrawTree();
  }
  
  public boolean Draw() {
    try {
      // TODO: provide code
      
      return true;
    } catch (Exception ex) {
      logger.error("LithiumControl/Draw failed", ex);
      return false;
    }
  }
  
  /**
   * MeasureString
   *    Measure how much width and height the indicated string should take up
   * @param sText
   * @param fntThis
   * @return 
   */
  public Point MeasureString(String sText, Font fntThis) {
    int iAddW = 15;
    int iAddH = 10;
    
    TextLayout layout = new TextLayout(sText, font, frc);
    Rectangle2D bounds = layout.getBounds();
    
    Point pBack = new Point (bounds.getWidth() + iAddW, font.getSize() + iAddH);
    
    this.fontheight = pBack.getY();
    // Point pBack = new Point(iWidth, iHeight);
    return pBack;
  }
  
  /**
   * Fit
   *    Resize shape to fit the text
   * @param shape
   */
  public void Fit(ShapeBase shape) {
    Size s = new Size(this.MeasureString(shape.Text(), this.font));
    shape.Width(s.getWidth() + 20);
    shape.Height(s.getHeight() + 8);
  }
  
  public ShapeBase AddRoot(String rootText) {
    try {
      // Validate
      if (this.Shapes().size() > 0) {
        // Cannot set the root unless the diagram is empty
        return null;
      }
      SimpleRectangle rootLocal = new SimpleRectangle(this);
      rootLocal.Location(new Point(this.width/2+50, this.height/2+50));
      rootLocal.Width(50);
      rootLocal.Height(25);
      rootLocal.Text(rootText);
      rootLocal.Fit();
      rootLocal.setColor("steelblue");
      rootLocal.isRoot= true;
      rootLocal.font = this.font;
      rootLocal.level = 0;
      Fit(rootLocal);
      
      // Set the root of the diagram
      this.graphAbstract.Root(rootLocal);
      this.Shapes().add(rootLocal);
      
      return rootLocal;
    } catch (Exception ex) {
      logger.error("LithiumControl/Draw failed", ex);
      return null;
    }
  }  
  
  /**
   * DrawTree
     Generic entry point to layout the diagram on the canvas.
     The default LayoutDirection is vertical. If you wish to layout the tree in a certain
     direction you need to specify this property first. Also, the direction is global, you cannot have 
     different parts being drawn in different ways though it can be implemented.
   * 
   * @return 
   */
  public boolean DrawTree() {
    try {
      // Validation
      if (!this.layoutEnabled) return false;
      if (this.graphAbstract == null) return false;
      if (this.graphAbstract.root == null) return false;
      // The shift vector difference between the original and the moved root
      Point p = Point.Empty;
      
      // Start the recursion
      // The layout will move the root bit it's reset to its original position
      switch(this.layoutDirection) {
        case Vertical:
          p = new Point(graphAbstract.Root().X(), graphAbstract.Root().Y());
          VerticalDrawTree(graphAbstract.Root(),false,this.marginLeft,this.graphAbstract.Root().Y());
          p = new Point(p.getX() - graphAbstract.Root().X(), p.getY() - graphAbstract.Root().Y());
          MoveDiagram(p);
          break;
        case Horizontal:
          // We do not implement that
          break;
      }
      int maxY = 0;
      for (int i=0;i<this.Shapes().size();i++) {
        // Access this shape
        ShapeBase shape = this.Shapes().get(i);
        // Look at color and calculate the maxY
        if (shape.shapeColName.equals("ivory")) {
          if (shape.visible) {
            // Possibly adapt the maxY value
            if (shape.Y() > maxY) maxY = shape.Y();
          }
        }
      }
      for (int i=0;i<this.Shapes().size();i++) {
        // Access this shape
        ShapeBase shape = this.Shapes().get(i);
        // Look at color and calculate the maxY
        if (shape.shapeColName.equals("ivory")) {
          if (shape.visible) {
            // Move in the Y-direction
            shape.Move(new Point(0, maxY - shape.Y()));
          }
        }
      }
      
      // The following is not necessary for XML-to-SVG conversion:
      CalculateScrollBars();
      
      Invalidate();
      
      return true;
    } catch (Exception ex) {
      logger.error("LithiumControl/DrawTree failed", ex);
      return false;
    }
  }
  
  public void dumpShapes() {
    try {
      for (int i=0;i<this.Shapes().size();i++) {
        StringBuilder sb = new StringBuilder();
        // Access this shape
        ShapeBase shape = this.Shapes().get(i);
        // Show where it is
        // (1) Shape number and text
        sb.append(Integer.toString(i)).append(" ").append(shape.Text()).append(": ");
        // (2) Shape location
        sb.append("[").append(shape.X()).append(",").append(shape.Y()).append("]");
        // (3) Shape size
        sb.append(" size=[").append(shape.Width()).append(",").append(shape.Height()).append("]");
        // (4) Visibility
        sb.append(" " + ((shape.visible ) ? "+" : "-"));
        // (5) Color
        sb.append(" " + shape.shapeColName);
        logger.debug(sb.toString());
      }
    } catch (Exception ex) {
      logger.error("LithiumControl/dumpShapes failed", ex);
      return ;
    }      

  }
  
  /**
   * renderSvg
   *    Render the currently selected tree as SVG
   * 
   * @return 
   */
  public String renderSvg() {
    StringBuilder sb = new StringBuilder();
    GraphicsSvg g = new GraphicsSvg();
    boolean bDebug = false;
    
    try {
      if (bDebug) {
        sb.append("<?xml-stylesheet type=\"text/css\" href=\"style8.css\"?>\n");
      }
      // Start the SVG xml object--make sure the namespace is there
      sb.append(String.format("<svg width='%1$spx' height='%2$spx' xmlns='%3$s' xmlns:xlink='%4$s'>\n",
                this.minWidth, this.minHeight, 
                "http://www.w3.org/2000/svg", 
                "http://www.w3.org/1999/xlink"));
      // Add all color-gradient definitions
      sb.append("<defs>").append(this.graphAbstract.root.getSvgDefs()).append("</defs>\n");
      
      // Visit all the shapes recursively
      sb.append(doShapeSvg(g, this.Root(), "tree"));

      // Finish the SVG xml object
      sb.append("</svg>\n");
      return sb.toString();
    } catch (Exception ex) {
      logger.error("LithiumControl/renderSvg failed", ex);
      return "";
    }
  }
  
  /**
   * doShapeSvg
   *    Render ONE shape recursively to SVG
   * 
   * @param g
   * @param shpThis
   * @return 
   */
  private String doShapeSvg(GraphicsSvg g, ShapeBase shpThis, String sArea) {
    StringBuilder sb = new StringBuilder();
    String sRoot = "";
    
    try {
      // What we do depends on the area
      switch(sArea) {
        case "tree":
          // Check if this is the root
          if (shpThis.isRoot) sRoot = " lithium-root";
          // Start a <g> element
          sb.append(String.format("<g id='%1$s' class='lithium-tree%2$s'>\n", 
                  "tree_"+shpThis.NodeId, sRoot));
          break;
      }
      
      // Next: walk all children
      for (ShapeBase shpChild : shpThis.childNodes) {
        // Add the result of this child
        sb.append(doShapeSvg(g, shpChild, sArea));
      }
      
      // What we do depends on the area
      switch(sArea) {
        case "tree":
          // Now draw the connection
          // CHeck if there is a connection attached to this shape
          if (shpThis.connection != null) {
            // Render the connection
            sb.append(shpThis.connection.renderSvg(g));
          }
          // And now add the shape
          SimpleRectangle srMe = (SimpleRectangle) shpThis;
          sb.append(srMe.renderSvg(g));
          // Finish the <g> object
          sb.append("</g>\n");
          break;
      }      

      // Return the total
      return sb.toString();
    } catch (Exception ex) {
      logger.error("LithiumControl/doShapeSvg failed", ex);
      return "";
    }
  }

  
  /**
   * MoveDiagram
   *    Move with the given vector [p]
   * @param p 
   */
  public void MoveDiagram(Point p) {
    try {
      // Move the whole diagram
      for (int i=0;i < this.Shapes().size(); i++) {
        // Move this shape
        ShapeBase shape = this.Shapes().get(i);
        shape.Move(p);
        Invalidate();
      }
    }catch (Exception ex) {
      logger.error("LithiumControl/MoveDiagram failed", ex);
    }
  } 

  /**
   * Invalidate
   *    In C# this should force a re-drawing
   */
  public void Invalidate() {
    // We do not do anything here
    int i = 0;
  }
  // </editor-fold>
  
  // <editor-fold desc="Private Methods">
  private void CalculateScrollBars() {
    try {
      Point minPoint = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
      Size maxSize = new Size(0,0);
      for (ShapeBase shape : this.Shapes()) {
        if (shape.visible) {
          int iNewWidth = shape.X() + shape.Width();
          int iNewHeight = shape.Y() + shape.Height();
          
          // Look for max Width / max Height
          if (iNewWidth > maxSize.getWidth())
            maxSize.setWidth(iNewWidth);
          if (iNewHeight > maxSize.getHeight())
            maxSize.setHeight(iNewHeight);
          
          // Look for minimum size
          if (shape.X() < minPoint.getX())
            minPoint.setX(shape.X());
          if (shape.Y() < minPoint.getY())
            minPoint.setY(shape.Y());
        }
      }
      
      // Move the whole diagram, taking into account [minPoint] (upper left point)
      this.MoveDiagram(new Point(50 - minPoint.getX(), 50 - minPoint.getY()));
      
      // Adapt maxSize again
      maxSize.setWidth(maxSize.getWidth() - minPoint.getX() + 100);
      maxSize.setHeight(maxSize.getHeight() - minPoint.getY() + 100);
      this.minWidth = maxSize.getWidth();
      this.minHeight = maxSize.getHeight();
    } catch (Exception ex) {
      logger.error("LithiumControl/CalculateScrollBars failed", ex);
    }
  }
  
  /**
   * VerticalDrawTree
   *    Positions everything underneath the node and returns the total width of the children
   * 
   * @param containerNode
   * @param first
   * @param shiftLeft
   * @param shiftTop
   * @return 
   */
  private int VerticalDrawTree(ShapeBase containerNode, boolean first, int shiftLeft, int shiftTop) {
    boolean isFirst = false;
    int childrenWidth = 0;
    int thisX, thisY;
    int returned = 0;

    try {
      boolean isParent = containerNode.childNodes.size() > 0 ? true : false;
      int verticalDelta = this.branchHeight; // The applied vertical shift of the child depends on the Height of the containernode
      
      // <editor-fold defaultstate="collapsed" desc="Children width">
      for (int i=0;i<containerNode.childNodes.size();i++) {
        // Determine the width of the label
        isFirst = (i==0);
        if (containerNode.childNodes.get(i).visible) {
          if ( (branchHeight - containerNode.Height()) < 30) // If too close to the child, shift it with 40 units
            verticalDelta = containerNode.Height() + 40;
          returned = VerticalDrawTree(containerNode.childNodes.get(i), isFirst, shiftLeft + childrenWidth, shiftTop + verticalDelta);
          childrenWidth += returned;
        }
      }
      if(childrenWidth>0 && containerNode.expanded) {
        //in case the length of the containerNode is bigger than the total length of the children
        childrenWidth=Math.max((int) Math.ceil(childrenWidth + (containerNode.Width()-childrenWidth)/2), childrenWidth); 
      }
      // </editor-fold>
      
      if (childrenWidth ==0) // THere are no children; this is the branch end
        childrenWidth = containerNode.Width() + this.wordSpacing;
      
      // <editor-fold defaultstate="collapsed" desc="Positioning">
      thisY = shiftTop;
      if (containerNode.childNodes.size() > 0 && containerNode.expanded) {
        if(containerNode.childNodes.size()==1) {
          thisX = (int) Math.ceil(containerNode.childNodes.get(0).X()+containerNode.childNodes.get(0).Width()/2 - containerNode.Width()/2);
        } else {
          float firstChild = containerNode.childNodes.get(0).Left()+ containerNode.childNodes.get(0).Width()/2;
          float lastChild = containerNode.childNodes.get(containerNode.childNodes.size()-1).Left() +
                  containerNode.childNodes.get(containerNode.childNodes.size()-1).Width()/2;
          //the following max in case the containerNode is larger than the childrenWidth
          float dW = containerNode.Width();
          float dMax = Math.max(firstChild + (lastChild - firstChild - dW)/2, firstChild);
          thisX = (int) Math.ceil(dMax);
        }
      } else {
        thisX = shiftLeft;
      }
      containerNode.rectangle.X(thisX);
      containerNode.rectangle.Y(thisY);
      // </editor-fold>

      // Return the width as we have calculated it
      return childrenWidth;
    } catch (Exception ex) {
      logger.error("LithiumControl/VerticalDrawTree failed", ex);
      return -1;
    }
  }
  
  // </editor-fold>
  
}