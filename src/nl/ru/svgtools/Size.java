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
public class Size {
  public static Size Empty;
  private int loc_height;
  private int loc_width;
  public Size(int width, int height) {
    this.loc_width = width;
    this.loc_height = height;
  }
  public Size(Point pt) {
    this.loc_width = pt.getX();
    this.loc_height = pt.getY();
  }
  public int getHeight() { return this.loc_height; }
  public int getWidth() { return this.loc_width; }
  public void setWidth(int value) { this.loc_width = value; }
  public void setHeight(int value) { this.loc_height = value; }
}