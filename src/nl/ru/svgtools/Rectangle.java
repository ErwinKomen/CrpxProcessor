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
public class Rectangle {
  // ========== Fields
  public static Rectangle Empty;
  private int loc_x;
  private int loc_y;
  private int loc_width;
  private int loc_height;
  // ========== Constructor
  public Rectangle(Point location, Size size) {
    this.loc_x = location.getX();
    this.loc_y = location.getY();
    this.loc_width = size.getWidth();
    this.loc_height = size.getHeight();
  }
  public Rectangle(int x, int y, int width, int height) {
    this.loc_x = x;
    this.loc_y = y;
    this.loc_width = width;
    this.loc_height = height;
  }
  // ========== Getters.setters
  public int X() {return this.loc_x; }
  public void X(int value) {this.loc_x = value;}
  public int Y() {return this.loc_y; }
  public void Y(int value) {this.loc_y = value;}
  public int Width() {return this.loc_width; }
  public void Width(int value) {this.loc_width = value;}
  public int Height() {return this.loc_height; }
  public void Height(int value) {this.loc_height = value;}
  /**
   * Location
   *    Gets or sets the coordinates of the upper-left corner of this Rectangle structure.
   * @param value 
   */
  public void Location(Point value) {
    this.loc_x = value.getX();
    this.loc_y = value.getY();
  }
  public Point Location() { return new Point(this.loc_x, this.loc_y);}
}
