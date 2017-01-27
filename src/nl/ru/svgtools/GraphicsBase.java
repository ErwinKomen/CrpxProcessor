/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.svgtools;

/**
 * RenderBase
 *    Base (abstract) class for rendering
 * @author erwin
 */
public abstract class GraphicsBase {
  // ============ Methods ====================
  public abstract String renderLine(Point p1, Point p2);
  public abstract String renderLine(PointF p1, PointF p2);
  public abstract String renderRect(Point p1, Point p2);
  public abstract String renderRect(Point p1, Point p2, Point r);
  public abstract String renderRect(Point p1, Point p2, Point r, String colName, boolean bGradient);
  public abstract String renderBezier(Point p1, Point p2, Point p3, Point p4);
  public abstract String renderBezier(PointF p1, PointF p2, PointF p3, PointF p4);
}
