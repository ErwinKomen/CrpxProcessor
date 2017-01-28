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
public class Point {
  public static Point Empty;
  private int loc_x;
  private int loc_y;
  public Point(int x, int y) {
    this.loc_x = x;
    this.loc_y = y;
  }
  public Point(double x, double y) {
    this.loc_x = (int) Math.ceil(x);
    this.loc_y = (int) Math.ceil(y);
  }
  public Point(Size sz) {
    this.loc_x = sz.getWidth();
    this.loc_y = sz.getHeight();
  }
  public Point translate(int x, int y) {
    return new Point(this.loc_x + x, this.loc_y+y);
  }
  public int getX() { return this.loc_x; }
  public int getY() { return this.loc_y; }
  public void setX(int value) {this.loc_x = value;}
  public void setY(int value) {this.loc_y = value;}
}
