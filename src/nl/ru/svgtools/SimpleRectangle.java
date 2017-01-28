/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.svgtools;

import java.util.ArrayList;
import java.util.List;

/**
 * SimpleRectangle
 *    A simple rectangular shape
 * 
 * @author erwin
 */
public class SimpleRectangle extends ShapeBase {
  // ======== Fields
  private String plus = "";
  private int bshift = 3;
  // ======== Constructor
  public SimpleRectangle(LithiumControl s) {
    // Perform the standard initialization of a ShapeBase
    super(s);
    // this.site = s;
  }

  // ======== Methods
  /**
   * renderSvg
   *    Render this shape as SVG
   * 
   * @param g
   * @return 
   */
  @Override
  public String renderSvg(GraphicsBase g) {
    StringBuilder sb = new StringBuilder();
    StringBuilder region = new StringBuilder(); // Main region
    StringBuilder shadow = new StringBuilder(); // Shadow of main region
    Rectangle toggleNode = Rectangle.Empty;     // The [+] or [-]
    List<Point> lPoint = new ArrayList<>();
    
    try {
      // Put main points into the point list
      lPoint.add(new Point(rectangle.X(), rectangle.Y()));
      lPoint.add(new Point(rectangle.Right(), rectangle.Bottom()));
      
      // Build the shadow: translated (b,b)
      shadow.append(g.renderRect(lPoint.get(0).translate(bshift,bshift), 
              lPoint.get(1).translate(bshift,bshift), 
              new Point(bshift, bshift), "gainsboro", false));
      
      // Render shadow and region SVG to the stringbuilder
      sb.append(shadow.toString());
      
      // Build the inner shape: 'region'      
      region.append(g.renderRect(lPoint.get(0), lPoint.get(1), 
              new Point(bshift, bshift), this.shapeColName, true));
      
      // Render the region, encapsulating it in a 
      sb.append(String.format("<g id='node_%1$s' class='lithium-node'>", this.NodeId));
      sb.append(region.toString());
      
      // Do we need to add the text to the shape?
      if (!this.text.isEmpty()) {
        // Yes, add the text
        sb.append(g.renderText(lPoint.get(0).translate(5,2 + font.getSize()), 
                font.getName(), font.getSize(), "black", this.text));
      }
      sb.append("</g>\n");
      
      // Need to draw the [+]?
      if (this.childNodes.size() > 0) {
        // Yes, we have children: prepare the rectangle for the +/-
        switch(site.layoutDirection) {
          case Vertical:
            toggleNode = new Rectangle(Left() + Width()/2 - 5, Bottom(), 10, 10);
            break;
          case Horizontal:
            toggleNode = new Rectangle(Right(), Top() + Height()/2-5, 10, 10);
            break;
        }
        // Draw [  ] (the rectangle for the +/-)
        g.fill = "white";
        sb.append(String.format("<g id='toggle_%1$s' class='lithium-toggle'>", this.NodeId));
        sb.append(g.renderRect(toggleNode));
        
        // Draw - (horizontal bar)
        int x=toggleNode.X();
        int y=toggleNode.Y();
        int h=toggleNode.Height();
        int w=toggleNode.Width();
        sb.append(g.renderLine(new Point(x + 1, y + (h/2)), 
                               new Point(x + w-1, y + (h/2))));
        
        // If expanded: also draw the vertical bar | (forming a plus)
        if (!this.expanded) {
          // Yes, draw |
          sb.append(g.renderLine(new Point(x + w/2, y+1), new Point(x + w/2, y + h - 1)));
        }
        sb.append("</g>\n");
        
      }
      
      // Return the combined string
      return sb.toString();
    } catch (Exception ex) {
      logger.error("ShapeBase/renderSvg failed", ex);
      return "";
    }
  }
  
}
