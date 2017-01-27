/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.svgtools;

/**
 *
 * @author erwin
 */
public class GraphicsSvg extends GraphicsBase {
  // ================== Fields
  private final String sLine = "<line x1='%1$d' y1='%2$d' x2='%3$d' y2='%4$d' stroke='black' stroke-width='1' />\n";
  private final String sBezier = "<path d='M%1$d %2$d C%3$d %4$d, %5$d %6$d, %7$d %8$d' stroke='black' fill='%9$s' />\n";
  private final String sRect = "<rect x='%1$d' y='%2$d' width='%3$d' height='%4$d' fill='%5$s' stroke='black' stroke-width='1' />\n";
  private final String sRectR = 
          "<rect x='%1$d' y='%2$d' width='%3$d' height='%4$d' "+
          "rx='%5$d' ry='%5$d' fill='%6$s' stroke='black' stroke-width='1' />\n";
  public String fill = "transparent";

  // ================== Methods
  @Override
  public String renderLine(Point p1, Point p2) {
    return String.format(sLine, p1.getX(), p1.getY(), p2.getX(), p2.getY());
  }
  @Override
  public String renderLine(PointF p1, PointF p2) {
    Point pf1 = new Point(p1.X(), p1.Y());
    Point pf2 = new Point(p2.X(), p2.Y());
    return renderLine(pf1, pf2);
  }

  @Override
  public String renderBezier(Point p1, Point p2, Point p3, Point p4) {
    // Make a Bezier Line through p1, p2, p3, p4
    // These are S, E, C1, C2
    // Where S = start, E = end, and C1, C2 are control points
    return String.format(sBezier, p1.getX(), p1.getY(), p2.getX(), p2.getY(), 
            p3.getX(), p3.getY(), p4.getX(), p4.getY(), fill);
  }
  @Override
  public String renderBezier(PointF pf1, PointF pf2, PointF pf3, PointF pf4) {
    Point p1 = new Point(pf1.X(), pf1.Y());
    Point p2 = new Point(pf2.X(), pf2.Y());
    Point p3 = new Point(pf3.X(), pf3.Y());
    Point p4 = new Point(pf4.X(), pf4.Y());
    return renderBezier(p1, p2, p3, p4);
  }

  @Override
  public String renderRect(Point p1, Point p2) {
    // Make rectangle with p1 upper-left and p2 lower-right
    // Calculate 'width' and 'height' parameters
    return String.format(sRect, p1.getX(), p1.getY(), p2.getX()-p1.getX(), p2.getY()-p1.getY(), fill);
  }

  @Override
  public String renderRect(Point p1, Point p2, Point r) {
    // Make rectangle with p1 upper-left and p2 lower-right
    //    Calculate 'width' and 'height' parameters
    // Use 'r' as both 'rx' as well as 'ry'
    return String.format(sRectR, p1.getX(), p1.getY(), 
            p2.getX()-p1.getX(), p2.getY()-p1.getY(), r.getX(), r.getY(), fill);
  }
  @Override
  public String renderRect(Point p1, Point p2, Point r, String sColName, boolean bGradient) {
    if (bGradient) {
      // Use the gradient rect
      ShapeBase sbThis = new ShapeBase();
      sbThis.setColor(sColName);
      this.fill = "url(#" + sbThis.getColId() + ")";
    } else {
      // Use the normal fill color
      this.fill = sColName;
    }
    return this.renderRect(p1,p2,r);
  }
  
}
