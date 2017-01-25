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
public class Connection extends Entity {
  // This class uses a logger
  protected final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Connection.class);
  // ================= Fields
  protected boolean visible = false;
  private ShapeBase from;
  private ShapeBase to;
  // ================= Class initializer
  public Connection(ShapeBase from, ShapeBase to) {
    this.from = from;
    this.to = to;
  }
  
  /**
   * renderSvg
   *    Render this connection as SVG
   *    The 'from' part is alwasy the child node while the 'to' part is
   *    always the parent node.
   *    Vertical: Parent->Child <=> Top->Bottom
   * 
   * @param g
   * @return 
   */
  public String renderSvg(GraphicsBase g) {
    StringBuilder sb = new StringBuilder();
    PointF p1, p2, p3, p4;  // Intermediate Floating points
    
    try {
      // Only render if visible
      if (this.visible) {
        switch(this.site.connectionType) {
          case Default:
            switch(this.site.layoutDirection) {
              case Vertical:
                p1 = new PointF(from.Left() + from.Width() / 2, from.Top() );
                p2 = new PointF(to.Left() + to.Width() / 2, to.Bottom() + 5);
                // Make a line from [p1] to [p2]
                sb.append(g.renderLine(p1, p2));
                break;
              case Horizontal:
                break;
            }
            break;
          case Traditional:
            switch(this.site.layoutDirection) {
              case Vertical:
                p1 = new PointF(from.Left() + from.Width() / 2, from.Top() - (from.Top() - to.Bottom())/2);
                p2 = new PointF(to.Left() + to.Width() / 2, from.Top() - (from.Top() - to.Bottom())/2);
                PointF pStart = new PointF(from.X(), from.Y());
                PointF pEnd = new PointF(to.X(), to.Y());
                // Make a line from [Start] to [p1]
                sb.append(g.renderLine(pStart, p1));
                // Make a line from [p1] to [p2]
                sb.append(g.renderLine(p1, p2));
                // Make a line from [End] to [p2]
                sb.append(g.renderLine(pEnd, p2));
                break;
              case Horizontal:
                break;
            }
            break;
          case Bezier:
            switch(this.site.layoutDirection) {
              case Vertical:
                p1 = new PointF(from.Left()+from.Width()/2,from.Top());
                p2 = new PointF(from.Left() + from.Width()/2, from.Top() - (from.Top() - to.Bottom())/2); 
                p3 = new PointF(to.Left() + to.Width()/2, from.Top() - (from.Top() - to.Bottom())/2);
                p4 = new PointF(to.Left()+to.Width()/2,to.Bottom());
                // Make a Bezier Line through p1, p2, p3, p4
                // These are S, E, C1, C2
                // Where S = start, E = end, and C1, C2 are control points
                sb.append(g.renderBezier(p1,p2,p3,p4));
                break;
              case Horizontal:
                break;
            }
            break;
        }
      }

      return sb.toString();
    } catch (Exception ex) {
      logger.error("Connection/renderSvg failed", ex);
      return "";
    }
  }
  

  @Override
  public boolean Hit(Point p) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void Invalidate() {
    // This function is not used
  }

  @Override
  public void Move(Point p) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}
