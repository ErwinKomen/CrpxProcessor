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
  private final String sLine = "<line x1='%1$.5f' y1='%2$.5f' x2='%3$.5f' y2='%4$.5f' stroke='black' stroke-width='1' />";
  private final String sBezier = "<path d='M%1$.2f %2$.2f C%3$.2f %4$.2f, %5$.2f %6$.2f, %7$.2f %8$.2f' stroke='black' fill='%9$s' />";
  private final String sRect = "<rect x='%1$d' y='%2$d' width='%3$d' height='%4$d' fill='%5$s' stroke='black' stroke-width='1' />";
  private final String sRectR = "<rect x='%1$d' y='%2$d' width='%3$d' height='%4$d' rx='%5$d' ry='%5$d' fill='%6$s' stroke='black' stroke-width='1' />";
  public String fill = "transparent";

  // ================== Methods
  @Override
  public String renderLine(Point p1, Point p2) {
    PointF pf1 = new PointF(p1.getX(), p1.getY());
    PointF pf2 = new PointF(p2.getX(), p2.getY());
    return renderLine(pf1, pf2);
  }
  @Override
  public String renderLine(PointF p1, PointF p2) {
    return String.format(sLine, p1.X(), p1.Y(), p2.X(), p2.Y());
  }

  @Override
  public String renderBezier(Point p1, Point p2, Point p3, Point p4) {
    PointF pf1 = new PointF(p1.getX(), p1.getY());
    PointF pf2 = new PointF(p2.getX(), p2.getY());
    PointF pf3 = new PointF(p3.getX(), p3.getY());
    PointF pf4 = new PointF(p4.getX(), p4.getY());
    return renderBezier(pf1, pf2, pf3, pf4);
  }
  @Override
  public String renderBezier(PointF p1, PointF p2, PointF p3, PointF p4) {
    // Make a Bezier Line through p1, p2, p3, p4
    // These are S, E, C1, C2
    // Where S = start, E = end, and C1, C2 are control points
    return String.format(sBezier, p1.X(), p1.Y(), p2.X(), p2.Y(), p3.X(), p3.Y(), p4.X(), p4.Y(), fill);
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
    return String.format(sRectR, p1.getX(), p1.getY(), p2.getX()-p1.getX(), p2.getY()-p1.getY(), r.getX(), r.getY(), fill);
  }
  @Override
  public String renderRect(Point p1, Point p2, Point r, String sColName) {
    this.fill = sColName;
    return this.renderRect(p1,p2,r);
  }
  
}
