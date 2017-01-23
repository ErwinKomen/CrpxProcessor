/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.svgtools;

// import java.awt.FontMetrics;
// import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import nl.ru.xmltools.XmlForest;

/**
 *
 * @author erwin
 */
public class ShapeBase extends Entity {
  // This class uses a logger
  protected final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ShapeBase.class);
  // ================= Fields ==============
  protected Rectangle rectangle;                  // Rectangle on which any shape lives
  public Color shapeColor = Color.STEELBLUE;      // Back color of the shapes
  // protected Brush shapeBrush;                  // Brush corresponding to the backcolor
  public String text = "";                        // The text on the shape
  public String NodeName = "";                    // Name of the node
  public String NodeId = "";                      // Unique ID of this node
  public boolean isRoot = false;                  // Whether this shape is the root
  public List<ShapeBase> childNodes;              // Collection of child nodes
  public boolean pickup = false;
  public ShapeBase parentNode = null;             // Points to the unique parent of this shape, unless it's the root and then Null
  public boolean expanded = false;                // If expanded, all the child nodes will have visible=true
  public Connection connection = null;            // Unique link to the parent 
  public boolean visited = false;                 // Used by the visiting pattern and tags whether this shape has been visited already
  public int level = -1;                          // Level of the shape in the hierarchy
  
  // ================ Class initializer
  public ShapeBase() {
    this.Init();
  }
  public ShapeBase(LithiumControl site) {
    this.site = site;
    this.Init();
  }
  
  // ================ Properties getters and setters ==========
  public String Text() { return this.text; }
  public void Text(String t) {
    this.text = t; 
    // Fit();
    // site.DrawTree();
    // site.Invalidate();
  }
  public int Width() { return this.rectangle.Width(); }
  public void Width(int value) {
    this.Resize(value, this.Height());
    this.site.DrawTree();
    this.site.Invalidate();
  }
  public int Height() { return this.rectangle.Height(); }
  public void Height(int value) {
    this.Resize(this.Width(), value);
    this.site.DrawTree();
    this.site.Invalidate();
  }
  public int X() { return this.rectangle.X(); }
  public void X(int value) { this.rectangle.X(value); }
  public int Y() { return this.rectangle.Y(); }
  public void Y(int value) { this.rectangle.Y(value); }
  public int Left() { return this.rectangle.X(); }
  public Point Location() { return new Point(this.rectangle.X(), this.rectangle.Y());}
  public void Location(Point value) {
    //we use the move method but it requires the delta value, not an absolute position!
    Point p = new Point(value.getX()-this.rectangle.X(),value.getY()-this.rectangle.Y());
    //if you'd use this it would indeed move the shape but not the connector s of the shape
    //this.rectangle.X = value.X; this.rectangle.Y = value.Y; Invalidate();
    this.Move(p);
  }
  

  /**
   * Expand
   *    Expand the children -- if any
   * @return 
   */
  public boolean Expand() {
    try {
      this.expanded = true;
      this.visible = true;
      for (int k=0;k<this.childNodes.size();k++) {
        ShapeBase chThis = childNodes.get(k);
        chThis.visible = true;
        chThis.connection.visible = true;
        if (chThis.expanded) {
          chThis.Expand();
        }
      }
      
      return true;
    } catch (Exception ex) {
      logger.error("ShapeBase/Expand failed", ex);
      return false;
    }
  }
  
  /**
   * Collapse
   *    Collapse the children underneath this shape
   * 
   * @param bChange
   * @return 
   */
  public boolean Collapse(boolean bChange) {
    try {
      if (bChange) this.expanded = false;
      for (int k=0;k<this.childNodes.size();k++) {
        ShapeBase chThis = childNodes.get(k);
        chThis.visible = false;
        chThis.connection.visible = false;
        if (chThis.expanded) {
          chThis.Collapse(false);
        }
      }
      
      return true;
    } catch (Exception ex) {
      logger.error("ShapeBase/Collapse failed", ex);
      return false;
    }
  }
  
  /**
   * 
   * @param sText
   * @return 
   */
  public ShapeBase AddChild(String sText) {
    try {
      SimpleRectangle shape = new SimpleRectangle(this.site);
      shape.Location(new Point(this.Width()/2 + 50, this.Height()/2 + 50));
      // Set the standard size of the shape: 50 * 25
      shape.Width(50);
      shape.Height(25);
      shape.Text(sText);
      shape.NodeId = "";
      shape.NodeName = "";
      shape.shapeColor = Color.LINEN;
      shape.isRoot = false;
      shape.parentNode = this;
      shape.Font(this.font);
      shape.level = this.level + 1;
      shape.Fit();  // Fit the child
      
      // Add the shape to the collections
      site.graphAbstract.shapes.add(shape);
      this.childNodes.add(shape);
      
      // Add a connection; from=child, to=parent
      Connection con = new Connection(shape, this);
      this.site.Connections().add(con);
      con.site = this.site;
      shape.connection = con;
      
      // Work on the implications of visibility
      if (this.visible) {
        this.Expand();
      } else {
        shape.visible = false;
        con.visible = false;
      }
      
      // Return the shape we have made
      return shape;
    } catch (Exception ex) {
      logger.error("ShapeBase/AddChild failed", ex);
      return null;
    }
  }
  
  public void Resize(int width, int height) {
    this.rectangle.Height(height);
    this.rectangle.Width(width);
    this.site.Invalidate();
  }
  
  /**
   * Fit
   *    Resize the shape's rectangle in function of the containing text
   */
  public void Fit() {
    // Determine the size of the text
    Size s = new Size(LithiumControl.MeasureString(this.text, this.font));
    // Adapt the rectangle to include this text
    this.rectangle.Width(s.getWidth() + 10);
    this.rectangle.Height(s.getHeight() + 8);
    this.Invalidate();
  }

  @Override
  public boolean Hit(Point p) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void Invalidate() {
    // THis is not really used
  }

  /**
   * Move
   *    Moves the shape with the given shift
   *    Note that [p] represents a shift-vector, not the absolute position
   * @param p 
   */
  @Override
  public void Move(Point p) {
    this.rectangle.X(p.getX() + this.rectangle.X());
    this.rectangle.Y(p.getY() + this.rectangle.Y());
    this.Invalidate();
  }
  
  /**
   * Init
   *     Summarizes the initialization used by the constructors
   */
  private void Init() {
    this.childNodes= new ArrayList<>();
    this.rectangle = new Rectangle(0,0,100,70);
    // The following is not used for SVG drawing:
    // this.SetBrush();
  }  
}
