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
public class PointF {
  public static PointF Empty;
  private float loc_x;
  private float loc_y;
  public PointF(float x, float y) {
    this.loc_x = x;
    this.loc_y = y;
  }
  public PointF(Size sz) {
    this.loc_x = sz.getWidth();
    this.loc_y = sz.getHeight();
  }
  public float X() { return this.loc_x; }
  public float Y() { return this.loc_y; }
  public void X(float value) {this.loc_x = value;}
  public void Y(float value) {this.loc_y = value;}}
