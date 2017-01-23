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
  // ================= Fields
  protected boolean visible = false;
  private ShapeBase from;
  private ShapeBase to;
  // ================= Class initializer
  public Connection(ShapeBase from, ShapeBase to) {
    this.from = from;
    this.to = to;
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
