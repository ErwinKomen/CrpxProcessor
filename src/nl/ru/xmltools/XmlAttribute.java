/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;

import org.w3c.dom.Node;

/**
 *
 * @author Erwin
 */
public abstract class XmlAttribute implements Node {
  public String Value = this.getNodeValue();
  /*
  public String Value() {
    return this.getNodeValue();
  } */
  

}
