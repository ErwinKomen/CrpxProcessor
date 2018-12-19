/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.project;

import nl.ru.util.json.JSONObject;

/**
 *
 * @author Erwin R. Komen
 */
public class CrpInfo {
  public static String sEtcProject = "/home/project";
  public static String sEtcCorpora = "/home/corpora";  
  
  public static void load(JSONObject properties) {
    sEtcProject = properties.getString("projectBase"); 
    sEtcCorpora = properties.getString("corpusBase"); 
  }
}
