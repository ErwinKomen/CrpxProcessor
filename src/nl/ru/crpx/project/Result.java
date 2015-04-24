/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.project;
import java.util.ArrayList;
import java.util.List;
import static nl.ru.crpx.project.CorpusResearchProject.lQueryConstructor;
import nl.ru.crpx.tools.General;
import nl.ru.util.json.JSONObject;
import org.apache.log4j.Logger;

/**
 *
 * @author E.R.Komen
 */
/* ---------------------------------------------------------------------------
   Class:   Result
   Goal:    The results of executing a CRP
   History:
   21/apr/2015   ERK Created
   --------------------------------------------------------------------------- */
public class Result {
  // This class uses a logger
  private static final Logger logger = Logger.getLogger(Result.class);
  protected General objGen;

  // Each result contains a number of lists
  static List<JSONObject> lCat = new ArrayList<>();
  static List<JSONObject> lOview = new ArrayList<>();
  // ==================== Class initialisations ================================
  public Result(General oGen) {
    this.objGen = oGen;
  }
  // ================ Cat elements =============================================
  public int getListCatSize() { return lCat.size(); }
  public List<JSONObject> getListCat() { return lCat;}
  public JSONObject getListCatItem(int iValue) {return lCat.get(iValue); }
  // ================ Oview elements =============================================
  public int getListOviewSize() { return lOview.size(); }
  public List<JSONObject> getListOview() { return lOview;}
  public JSONObject getListOviewItem(int iValue) {return lOview.get(iValue); }
}
