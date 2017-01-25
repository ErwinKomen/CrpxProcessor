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
  private int bshift = 10;
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
      // Create 12 points into the point list
      lPoint.add(new Point(rectangle.X(), rectangle.Y()));
      lPoint.add(new Point(rectangle.X()+bshift, rectangle.Y()));
      lPoint.add(new Point(rectangle.Right()-bshift, rectangle.Y()));
      lPoint.add(new Point(rectangle.Right(), rectangle.Y()));
      lPoint.add(new Point(rectangle.Right(), rectangle.Y()+bshift));
      lPoint.add(new Point(rectangle.Right(), rectangle.Bottom()-bshift));
      lPoint.add(new Point(rectangle.Right(), rectangle.Bottom()));
      lPoint.add(new Point(rectangle.Right()-bshift, rectangle.Y()));
      lPoint.add(new Point(rectangle.X()+bshift, rectangle.Bottom()));
      lPoint.add(new Point(rectangle.X(), rectangle.Bottom()));
      lPoint.add(new Point(rectangle.X(), rectangle.Bottom()-bshift));
      lPoint.add(new Point(rectangle.X(), rectangle.Y()+bshift));
      
      // Determine the size of the box
      Size sMain = new Size(rectangle.Width(), rectangle.Height());
      
      // Build the inner shape
      region.append("<g >");
      /*
      region.append(g.renderBezier(lPoint.get(11), lPoint.get(0), lPoint.get(0), lPoint.get(1)));
      region.append(g.renderLine(lPoint.get(0), lPoint.get(2)));
      
      region.append(g.renderBezier(lPoint.get(2), lPoint.get(3), lPoint.get(3), lPoint.get(4)));
      region.append(g.renderLine(lPoint.get(4), lPoint.get(5)));
      
      region.append(g.renderBezier(lPoint.get(5), lPoint.get(6), lPoint.get(6), lPoint.get(7)));
      region.append(g.renderLine(lPoint.get(7), lPoint.get(8)));
      
      region.append(g.renderBezier(lPoint.get(8), lPoint.get(9), lPoint.get(9), lPoint.get(10)));
      region.append(g.renderLine(lPoint.get(10), lPoint.get(11)));
      */
      // New method
      
      region.append(g.renderRect(lPoint.get(0), 
              new Point(sMain), 
              new Point(bshift, bshift), "whitesmoke"));
      region.append("</g>");
      
      // Translate all the points
      for (Point p: lPoint) { p.setX(p.getX() + 5); p.setY(p.getY() + 5);}
      
      // Build the shadow
      shadow.append("<g >");
      shadow.append(g.renderRect(lPoint.get(0), 
              new Point(sMain), 
              new Point(bshift, bshift), "gainsboro"));
      shadow.append("</g>");
      
      

      return sb.toString();
    } catch (Exception ex) {
      logger.error("ShapeBase/renderSvg failed", ex);
      return "";
    }
  }
  
}
