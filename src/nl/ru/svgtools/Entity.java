/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.svgtools;

import java.util.UUID;
import javafx.scene.text.Font;

/**
 *
 * @author erwin
 */
public abstract class Entity {
  // ============== Fields ================
  protected boolean hovered = false;
  protected LithiumControl site;
  protected boolean isSelected = false;
  protected Font font = new Font("Verdana", 10F);
  // protected Pen blackPen = new Pen(Brushes.Black, 1f);
  // protected Pen redPen = new Pen(Brushes.Red, 2f);
  // protected Pen thickPen = new Pen(Color.BLACK, 1.7f);
  // protected Pen pen;  // Whatever pen you use instead of the black one
  protected String uid;  // Unique identifier
  public boolean visible = false;
  
  // ============ Properties =============
  public boolean IsSelected() { return this.isSelected; }
  public void IsSelected(boolean value) { this.isSelected = value; }
  public LithiumControl Site() { return this.site;}
  public void Site(LithiumControl value) { this.site = value;}
  public String UID() { return this.uid;}
  public void UID(String value) {this.uid = value;}
  public Font Font() { return this.font;}
  public void Font(Font value) { this.font = value;}
  
  // ============ Constructor ===============
  public Entity() { this.uid = UUID.randomUUID().toString();}
  public Entity(LithiumControl site) { this.site = site;}
  
  // ============ Methods ====================
  // public abstract void Paint(Graphics g); // Paint the enity on the control using the graphics object
  public abstract boolean Hit(Point p);   // Test whether the shape is hit by the mouse
  public abstract void Invalidate();      // Invalidate entity
  public abstract void Move(Point p);     // Move entity on canvas by [p], the shifting vector
}
