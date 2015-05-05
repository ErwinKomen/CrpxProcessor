/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.tools;

import javax.xml.xpath.XPathExpressionException;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.project.CrpGlobal;
import nl.ru.xmltools.XmlNode;
import org.w3c.dom.DOMException;
import org.w3c.dom.NodeList;

/**
 *
 * @author Erwin R. Komen
 */
public class Parse extends CrpGlobal {
  public Parse() {
    
  }
  // ----------------------------------------------------------------------------------------------------------
  // Name :  GetSeg
  // Goal :  Get the "original" text of the current sentence node
  //         Psdx:   Get the stuff that is between <seg>...</seg> 
  //         Negra:  Combine all the terminal nodes in the correct order
  // History:
  // 20-02-2010   ERK Created for .NET
  // 23/apr/2015  ERK Adapted for Java
  // ----------------------------------------------------------------------------------------------------------
  public String GetSeg(XmlNode ndThis, CorpusResearchProject crpThis) {
    XmlNode ndxOrg;   // The div/seg node we are looking for
    NodeList ndxTrm;  // List of terminal nodes
    String strProjType; // The project type we are dealing with
    
    try {
      // Validate: Does the node exist?
      if (ndThis == null) return "";
      // Action depends on project type
      strProjType = crpThis.getProjectType();
      switch(strProjType.toLowerCase()) {
        case "xquery-psdx":
          // Try find the 'org' segment
          ndxOrg = ndThis.SelectSingleNode("./div[@lang='org']/seg");
          // Check the reply
          if (ndxOrg == null) return "";
          // The node has been found, so return the innertext
          return ndxOrg.getNodeValue();
        case "folia-psdx":
          break;
        case "negra-tig":
          break;
        default:
          // Cannot handle this
          DoError("modParse/GetSeg: Can not handle project type " + strProjType, Parse.class);
          return "";
      }
      
      return "";
    } catch (DOMException ex) {
      // Warn user
      DoError("Parse/GetSeg DOM error", ex, Parse.class);
      // Return failure
      return "";
    } catch (XPathExpressionException ex) {
      DoError("Parse/GetSeg xpath error", ex, Parse.class);
      return "";
    }
  }
}
