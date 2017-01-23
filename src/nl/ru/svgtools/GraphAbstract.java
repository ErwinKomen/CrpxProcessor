/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.svgtools;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author erwin
 */
public class GraphAbstract {
  protected String description = "No description";
  protected List<ShapeBase> shapes;
  protected List<Connection> connections;
  protected ShapeBase root;
  
  // Getters and setters
  public List<ShapeBase> Shapes() { return this.shapes; }
  public void Shapes(List<ShapeBase> value) {this.shapes = value; }
  public ShapeBase Root() { return this.root; }
  public void Root(ShapeBase value) {this.root = value; }
  
  // Constructor
  public GraphAbstract() {
    this.connections = new ArrayList<>();
    this.shapes = new ArrayList<>();
  }
}
