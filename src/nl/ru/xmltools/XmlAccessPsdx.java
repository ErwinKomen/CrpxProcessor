/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.xmltools;

import java.util.List;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import nl.ru.crpx.dataobject.DataObject;
import nl.ru.crpx.dataobject.DataObjectList;
import nl.ru.crpx.dataobject.DataObjectMapElement;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.xq.English;
import nl.ru.crpx.xq.Extensions;
import nl.ru.crpx.xq.RuBase;
import nl.ru.util.ByRef;
import nl.ru.util.json.JSONArray;
import nl.ru.util.json.JSONObject;

/**
 * XmlAccessPsdx
 *    Provide access to XML coded files in a way that hides the actual XML
 *    functions.
 *    This "extends" class implements the interface for PSDX-coded files
 * 
 * @author Erwin R. Komen
 * @history 21/jul/2015 Created
 */
public class XmlAccessPsdx extends XmlAccess {
  // ========================================== Constants ======================
  protected static final QName loc_xq_forestId = new QName("", "", "forestId");  
  protected static final QName loc_xq_Location = new QName("", "", "Location");  
  protected static final QName loc_xq_TextId = new QName("", "", "TextId");  
  protected static final QName loc_xq_Text = new QName("", "", "Text");  
  protected static final QName loc_xq_pos = new QName("", "", "Label");  
  protected static final String loc_path_Forest = "./descendant-or-self::forest[1]";
  protected static final String loc_path_TeiHeader = "./descendant-or-self::teiHeader[1]";

  // ==========================================================================================================
  // Class instantiation
  public XmlAccessPsdx(CorpusResearchProject crpThis, XmlDocument pdxDoc, String sFileName) {
    super(crpThis, pdxDoc, sFileName);
  }
  
  
  /**
   * getHitLine
   *    Given the sentence in [ndxSentence] get a JSON representation of 
   *    this sentence that includes:
   *    { 'pre': 'text preceding the hit',
   *      'hit': 'the hit text',
   *      'fol': 'text following the hit'}
   * 
   * @param sLngName
   * @param sLocw
   * @return 
   */
  @Override
  public JSONObject getHitLine(String sLngName, String sLocs, String sLocw) {
    JSONObject oBack = new JSONObject();
    String sPre = "";
    String sHit = "";
    String sFol = "";
    
    try {
      // Make sure the indicated sentence is read
      if (!readSent(sLocs)) return null;
      // Validate
      if (ndxSent == null) return null;
      // Get word nodes of the whole sentence
      List<XmlNode> arWords = this.ndxSent.SelectNodes("./descendant::eLeaf[(@Type = 'Vern' or @Type = 'Punct')]");
      // Walk the results
      int i=0;
      // Get the preceding context
      while(i < arWords.size() && !hasAncestor(arWords.get(i), "id", sLocw))  {
        // Double check for CODE ancestor
        if (!hasAncestor(arWords.get(i), "pos", "CODE"))
          sPre += arWords.get(i).getAttributeValue(loc_xq_Text) + " ";
        i++;
      }
      // Get the hit context
      while(i < arWords.size() && hasAncestor(arWords.get(i), "id", sLocw))  {
        // Double check for CODE ancestor
        if (!hasAncestor(arWords.get(i), "pos", "CODE"))
          sHit += arWords.get(i).getAttributeValue(loc_xq_Text) + " ";
        i++;
      }
      // Get the following context
      while(i < arWords.size() && !hasAncestor(arWords.get(i), "id", sLocw))  {
        // Double check for CODE ancestor
        if (!hasAncestor(arWords.get(i), "pos", "CODE"))
          sFol += arWords.get(i).getAttributeValue(loc_xq_Text) + " ";
        i++;
      }
      // Possible corrections depending on language
      switch(sLngName) {
        case "eng_hist":
          // Convert OE symbols
          sPre = English.VernToEnglish(sPre);
          sHit = English.VernToEnglish(sHit);
          sFol = English.VernToEnglish(sFol);
          break;
      }
      
      // Construct object
      oBack.put("pre", sPre.trim());
      oBack.put("hit", sHit.trim());
      oBack.put("fol", sFol.trim());
      return oBack;
    } catch (Exception ex) {
      logger.error("getHitLine failed", ex);
      return null;
    }
  }
  
  /**
   * getHitSyntax
   *    Given the sentence in [ndxSentence] get a JSON representation of 
   *    this sentence that includes:
   *    { "all": {
   *        "main": "IP-MAT-SPE",
   *        "children": [
   *          { "pos": "CONJ", "txt": "and"}, {} ]},
   *      "hit": {
   *        "main": "IP-MAT-SPE",
   *        "children": [ { "pos": "CONJ", "txt": "and"},{}]}
   *    }
   * 
   * @param sLngName
   * @param sLocw
   * @return 
   */
  @Override
  public DataObject getHitSyntax(String sLngName, String sLocs, String sLocw) {
    XmlNode ndxTop;         // Top constituent
    XmlNode ndxHit;         // The target constituent from sLocs
    String sConvType = "";  // Kind of additional ru:conv()
    DataObjectMapElement oBack = new DataObjectMapElement();
    
    try {
      // Make sure the indicated sentence is read
      if (!readSent(sLocs)) return null;
      // Validate
      if (ndxSent == null) return null;
      // Possible corrections depending on language
      switch(sLngName) {
        case "eng_hist":
          // Convert OE symbols
          sConvType = "OE";
          break;
      }
      // Get the hit constituent
      ndxHit = this.ndxSent.SelectSingleNode("./descendant::eTree[@Id=" + sLocw + "]");
      if (ndxHit==null) return null;
      // Get constituent nodes of the whole sentence
      ndxTop = ndxHit.SelectSingleNode("./ancestor-or-self::eTree[parent::forest]");
      if (ndxTop == null) return null;
      // Process this information
      DataObjectMapElement oAll = new DataObjectMapElement();
      oAll.put("main", ndxTop.getAttributeValue(loc_xq_pos));
      DataObjectList arAll = new DataObjectList("all");
      // Get the top's children
      List<XmlNode> arConst = ndxTop.SelectNodes("./child::eTree[(@Label != 'CODE')]");
      for (int i=0;i<arConst.size();i++) {
        DataObjectMapElement oOneConst = new DataObjectMapElement();
        oOneConst.put("pos", arConst.get(i).getAttributeValue(loc_xq_pos));
        oOneConst.put("txt", RuBase.RuNodeText(crpThis, arConst.get(i).getNode(), sConvType).trim());
        arAll.add(oOneConst);
      }
      oAll.put("children", arAll);
      // Process the "hit" syntax information
      DataObjectMapElement oHit = new DataObjectMapElement();
      oHit.put("main", ndxHit.getAttributeValue(loc_xq_pos));
      DataObjectList arHit = new DataObjectList("hit");
      // Get the top's children
      arConst = ndxHit.SelectNodes("./child::eTree[(@Label != 'CODE')]");
      for (int i=0;i<arConst.size();i++) {
        DataObjectMapElement oOneConst = new DataObjectMapElement();
        oOneConst.put("pos", arConst.get(i).getAttributeValue(loc_xq_pos));
        oOneConst.put("txt", RuBase.RuNodeText(crpThis, arConst.get(i).getNode(), sConvType).trim());
        arHit.add(oOneConst);
      }
      oHit.put("children", arHit);
      // Prepare the object to be returned
      oBack.put("all", oAll);
      oBack.put("hit", oHit);
      return oBack;
    } catch (Exception ex) {
      logger.error("getHitSyntax failed", ex);
      return null;
    }    
  }
  
  /**
   * getHitContext
   *    Given the sentence in [ndxSentence] get a JSON representation of 
   *    this sentence that includes:
   *    { 'pre': 'text preceding the hit',
   *      'hit': 'the hit text',
   *      'fol': 'text following the hit'}

   * @param sLngName
   * @param sLocw
   * @param iPrecNum
   * @param iFollNum
   * @return 
   */
  @Override
  public JSONObject getHitContext(String sLngName, String sLocs, String sLocw, 
          int iPrecNum, int iFollNum) {
    XmlNode ndxWork;  // Working node
    
    try {
      // Get the hit context of the target line
      JSONObject oBack = getHitLine(sLngName, sLocs, sLocw);
      String sPre = " [" + ndxSent.getAttributeValue(loc_xq_Location) + "] " + oBack.getString("pre");
      String sFol = oBack.getString("fol");
      String sHit = oBack.getString("hit");
      // Get the preceding sentences
      for (int i=0; i< iPrecNum; i++) {
        if (readOffsetSent(-1)) {
          // Got anything?
          if (ndxSent != null) {
            // Add this preceding context
            sPre = "[" + ndxSent.getAttributeValue(loc_xq_Location) + "] " + getOneSent(ndxSent) + sPre;
          }
        }
      }
      // Return to the sentence we had
      if (!readSent(sLocs)) return null;
      // Get the sentences following upon the hit
      for (int i=0; i< iFollNum; i++) {
        if (readOffsetSent(1)) {
          // Got anything?
          if (ndxSent != null) {
            // Add this preceding context
            sFol += " [" + ndxSent.getAttributeValue(loc_xq_Location) + "] " + getOneSent(ndxSent);
          }
        }
      }
      // Re-combine
      oBack.put("pre", sPre);
      oBack.put("fol", sFol);
      oBack.put("hit", sHit);
      // Return our result
      return oBack;
    } catch (Exception ex) {
      logger.error("getHitContext failed", ex);
      return null;
    }
  }
  
  /**
   * hasAncestor
   *    Check if the indicated node has an ancestor of type @sType with value @sValue
   * 
   * @param ndThis
   * @param sType
   * @param sValue
   * @return 
   */
  @Override
  public boolean hasAncestor(XmlNode ndThis, String sType, String sValue) {
    String sPath;
    try {
      // Validate
      if (ndThis == null) return false;
      // Action depends on the type
      switch (sType) {
        case "id":
          sPath = "./ancestor::eTree[@Id = " + sValue + "] ";
          break;
        case "pos":
          sPath = "./ancestor::eTree[@Label = '" + sValue + "'] ";
          break;
        default:
          return false;
      }
      // Check if the ancestor exists
      return (ndThis.SelectSingleNode(sPath) != null);
    } catch (Exception ex) {
      // Warn user
      logger.error("XmlAccessPsdx/hasAncestor error: ", ex);
      // Return failure
      return false;
    }
  }
  /**
   * getOneSent
   *    Given a node, get the sentence associated with that node
   * 
   * @param ndxThis
   * @return 
   */
  private String getOneSent(XmlNode ndxThis) {
    String sBack = "";
      
    try {
      // Validate
      if (ndxThis == null || ndxThis.isAtomicValue()) return "";
      XmlNode ndxSeg = ndxThis.SelectSingleNode("./ancestor-or-self::forest/child::div[@lang='org']/child::seg");
      // Validate
      if (ndxSeg != null) 
        sBack = ndxSeg.getNodeValue();
      return sBack;
    } catch (Exception ex) {
      logger.error("getOneSent failed", ex);
      return "";
    }
  }  
  
  /**
   * readSent
   *    Read the sentence with indicated id
   * 
   * @param sSentId
   * @return 
   */
  private boolean readSent(String sSentId) {
    try {
      // Do we already have this one?
      if (!sSentId.equals(sCurrentSentId)) {
        // Get the String representation 
        String sSentLine = objXmlRdr.getOneLine(sSentId);
        // Convert this into an XmlNode
        pdxDoc.LoadXml(sSentLine);
        // Get the root node
        // this.ndxSent = new XmlNode(pdxDoc.getNode(), this.objSaxon);
        this.ndxSent = pdxDoc.SelectSingleNode("./descendant-or-self::forest[1]");
        // Keep track of the id
        sCurrentSentId = sSentId;
      }
      // Return positively
      return true;
    } catch (Exception ex) {
      logger.error("readSent failed", ex);
      return false;
    }      
  }
  /**
   * readOffsetSent
   *    Read a sentence before or after the currently loaded one
   * 
   * @param iOffset
   * @return 
   */
  private boolean readOffsetSent(int iOffset) {
    ByRef<String> oSentId = new ByRef("");
    try {
      // Validate
      if (iOffset == 0) return true;
      if (sCurrentSentId.isEmpty() || ndxSent == null) return false;
      // Get the String representation 
      String sSentLine = objXmlRdr.getRelativeLine(iOffset, oSentId);
      // Validate the result
      if (sSentLine.isEmpty()) {
        // Just return with failure, but do NOT adapt the currentSentId and the ndxSent
        return false;
      }
      // Convert this into an XmlNode
      pdxDoc.LoadXml(sSentLine);
      // Get the sentence node
      //this.ndxSent = new XmlNode(pdxDoc.getNode(), this.objSaxon);
      this.ndxSent = pdxDoc.SelectSingleNode("./descendant-or-self::forest[1]");
      // Keep track of the id
      sCurrentSentId = oSentId.argValue;
      // Return positively
      return true;
    } catch (Exception ex) {
      logger.error("readSent failed", ex);
      return false;
    }      
  }
}
