/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.xmltools;

import java.util.List;
import net.sf.saxon.s9api.QName;
import nl.ru.crpx.dataobject.DataObject;
import nl.ru.crpx.dataobject.DataObjectList;
import nl.ru.crpx.dataobject.DataObjectMapElement;
import nl.ru.crpx.project.CorpusResearchProject;
import nl.ru.crpx.xq.English;
import nl.ru.crpx.xq.RuBase;
// import nl.ru.crpx.xq.RuBase;
import nl.ru.util.ByRef;
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
  protected static final QName loc_xq_TextType = new QName("", "", "Type");  
  protected static final QName loc_xq_pos = new QName("", "", "Label");  
  protected static final QName loc_xq_feat_type = new QName("", "", "type");  
  protected static final QName loc_xq_feat_name = new QName("", "", "name");  
  protected static final QName loc_xq_feat_val = new QName("", "", "value");  
  protected static final QName loc_xq_feat_forest = new QName("", "", "lang");  
  protected static final String loc_path_PsdxSent = "./descendant-or-self::forest[1]";
  protected static final String loc_path_PsdxHeader = "./descendant-or-self::teiHeader[1]";
  protected static final String loc_path_PsdxFeat = "./child::fs/child::f";
  protected static final String loc_path_PsdxParentF = "./parent::fs";
  protected static final String loc_path_PsdxChild = "./child::eTree[(@Label != 'CODE')]";
  protected static final String loc_path_PsdxLeaf = "./child::eLeaf";

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
   * @param sLocs
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
   * @param sLocs
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
        oOneConst.put("txt", objBase.RuNodeText(crpThis, arConst.get(i).getNode(), sConvType).trim());
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
        oOneConst.put("txt", objBase.RuNodeText(crpThis, arConst.get(i).getNode(), sConvType).trim());
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
   * getHitTree
   *    Given the sentence in [ndxSentence] get a JSON 
   *    HIERARCHICAL representation of node [locs,locw] that includes:
   *    { "all": {
   *        "main": "IP-MAT-SPE",
   *        "children": [
   *          { "pos": "NP-SBJ", 
   *            "children": [...], 
   *            "f": [{"name": "aap", "value": "noot"}, {}, ...]  
   *          },
   *          { "pos": "CONJ", "txt": "and"}, {} ]
   *      },
   *      "hit": {
   *        "main": "IP-MAT-SPE",
   *        "children": [ { "pos": "CONJ", "txt": "and"},{}]
   *      }
   *    }
   * 
   * @param sLngName
   * @param sLocs
   * @param sLocw
   * @return 
   */
  @Override
  public DataObject getHitTree(String sLngName, String sLocs, String sLocw) {
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
      // Get constituent nodes of the whole sentence: INCLUDING THE FOREST
      ndxTop = ndxHit.SelectSingleNode("./ancestor-or-self::forest");
      if (ndxTop == null) return null;
      // Prepare the object to be returned
      oBack.put("all", getTreeNode(ndxTop, sConvType));
      oBack.put("hit", getTreeNode(ndxHit, sConvType));
      return oBack;
    } catch (Exception ex) {
      logger.error("getHitTree failed", ex);
      return null;
    }    
  }
  
  /**
   * getTreeNode
   *    Convert xml node ndxThis into a tree node 
   *    THis is a recursive function
   * 
   * @param ndxThis     - Node, which may be <eTree> or <forest>
   * @param sConvType
   * @return 
   */
  private DataObjectMapElement getTreeNode(XmlNode ndxThis, String sConvType) {
    DataObjectMapElement oBack = new DataObjectMapElement();
    DataObjectList lChild = new DataObjectList("child");
    String sPos = "";

    try {
      // Add the details of this node to the oBack
      switch (ndxThis.getNodeName().toString()) {
        case "forest":
          sPos = ndxThis.getAttributeValue(loc_xq_Location);
          break;
        case "eTree":
          sPos = ndxThis.getAttributeValue(loc_xq_pos);
          break;
        case "eLeaf":
          sPos = "ERROR: leaf";
          break;
      }
      oBack.put("pos",sPos);
      oBack.put("f", getTreeNodeFeatures(ndxThis));
      // Get the top's children, excluding CODE
      List<XmlNode> arConst = ndxThis.SelectNodes(loc_path_PsdxChild);
      // Are there any children?
      if (arConst.isEmpty()) {
        // There are no children, so this is an end-eTree-node
        String sText = "";
        String sType = "";
        // The text is in the one-and-only <eLeaf> child
        XmlNode ndxLeaf = ndxThis.SelectSingleNode(loc_path_PsdxLeaf);
        if (ndxLeaf != null) {
          sText = ndxLeaf.getAttributeValue(loc_xq_Text);
          if (!sConvType.isEmpty()) sText = RuBase.RuConv(sText, sConvType);
          sType = ndxLeaf.getAttributeValue(loc_xq_TextType);
        }
        oBack.put("txt", sText);
        oBack.put("type", sType);
        // oBack.put("txt", objBase.RuNodeText(crpThis, ndxThis.getNode(), sConvType).trim());
      } else {
        // Walk the children
        for (int i=0;i<arConst.size();i++) {
          XmlNode ndxChild = arConst.get(i);
          lChild.add(getTreeNode(ndxChild, sConvType));
        }
        // Add the list of children
        oBack.put("child", lChild);
      }
      
      return oBack;
    } catch (Exception ex) {
      logger.error("getTreeNode failed", ex);
      return null;
    }
  }
  
  /**
   * getTreeNodeFeatures
   *    Get the list of feature key-values for this node
   * 
   * @param ndxThis
   * @return 
   */
  DataObject getTreeNodeFeatures(XmlNode ndxThis) {
    DataObjectMapElement oBack = new DataObjectMapElement();
    List<XmlNode> lFeats = null;
    XmlNode ndxFeat = null;
    String sKey = "";
    String sValue = "";
    
    try {
      // Add the details of this node to the oBack
      switch (ndxThis.getNodeName().toString()) {
        case "forest":
          lFeats = ndxThis.SelectNodes("./child::div");
          for (int i=0;i<lFeats.size();i++) {
            // Get this <div> node
            ndxFeat = lFeats.get(i);
            // Feature 'key' is the @lang property of the div
            sKey = ndxFeat.getAttributeValue(loc_xq_feat_forest);
            // Feature 'value' is the <seg> content
            sValue = ndxFeat.SelectSingleNode("./child::seg").getNodeValue();
            oBack.put(sKey, sValue);
          }
          // Also add any other sentence-level features
          lFeats = ndxThis.SelectNodes(loc_path_PsdxFeat);
          for (int i=0;i<lFeats.size();i++) {
            // Get this feature
            ndxFeat = lFeats.get(i);
            // Get feature type, key and value
            String sType = ndxFeat.SelectSingleNode(loc_path_PsdxParentF).getAttributeValue(loc_xq_feat_type);
            sKey = ndxFeat.getAttributeValue(loc_xq_feat_name);
            if (!sType.isEmpty()) sKey = sType + "." + sKey;
            sValue = ndxFeat.getAttributeValue(loc_xq_feat_val);
            oBack.put(sKey, sValue);
          }
          break;
        default:
          lFeats = ndxThis.SelectNodes(loc_path_PsdxFeat);
          for (int i=0;i<lFeats.size();i++) {
            // Get this feature
            ndxFeat = lFeats.get(i);
            // Get feature type, key and value
            String sType = ndxFeat.SelectSingleNode(loc_path_PsdxParentF).getAttributeValue(loc_xq_feat_type);
            sKey = ndxFeat.getAttributeValue(loc_xq_feat_name);
            if (!sType.isEmpty()) sKey = sType + "." + sKey;
            sValue = ndxFeat.getAttributeValue(loc_xq_feat_val);
            oBack.put(sKey, sValue);
          }
          break;
      }      
      return oBack;
    } catch (Exception ex) {
      logger.error("getTreeNodeFeatures failed", ex);
      return null;
    }
  }

  
  /**
   * getHitSvg
   *    Given the sentence in [ndxSentence] get a JSON representation of 
   *    this sentence that includes:
   *    { "all": {<svg>...</svg>},
   *      "hit": {<svg>...</svg>}
   *    }
   * 
   * @param sLngName
   * @param sLocs
   * @param sLocw
   * @return 
   */
  @Override
  public DataObject getHitSvg(String sLngName, String sLocs, String sLocw) {
    XmlNode ndxTop;         // Top constituent
    XmlNode ndxHit;         // The target constituent from sLocs
    String sConvType = "";  // Kind of additional ru:conv()
    DataObjectMapElement oBack = new DataObjectMapElement();
    
    try {
      // Make sure the indicated sentence is read
      if (!readSent(sLocs)) return null;
      // Validate: we expect the sentence to be within [ndxSent]
      if (ndxSent == null) return null;
      // Get the hit constituent
      ndxHit = this.ndxSent.SelectSingleNode("./descendant::eTree[@Id=" + sLocw + "]");
      if (ndxHit==null) { logger.error("getHitSvg: ndxHit is empty");  return null;}
      // Possible corrections depending on language
      switch(sLngName) {
        case "eng_hist":
          // Convert OE symbols
          sConvType = "OE";
          break;
      }
      
      // Get a tree for this hit in the form of an SVG string
      String sHitG = PsdxToSvg(sConvType, ndxHit, null);
      
      // Get a tree of the whole sentence
      String sAllG = PsdxToSvg(sConvType, ndxSent, ndxHit);
      
      // Prepare the object to be returned
      oBack.put("all", sAllG);
      oBack.put("hit", sHitG);      
      
      return oBack;
    } catch (Exception ex) {
      logger.error("getHitSvg failed", ex);
      return null;
    }    
  }  
  
  /**
   * getOffsetNode
   *    Get the sentence that is [iOffset] away from the current one
   * 
   * @param sLocs
   * @param iOffset
   * @return 
   */
  @Override
  public XmlNode getOffsetNode(String sLocs, int iOffset) {
    try {
      // Should at least read current sentence
      if (!readSent(sLocs)) return null;
      // Check if this is not the current sentence
      if (iOffset!=0) {
        if (!readOffsetSent(iOffset)) return null;
      }
      // Return the result
      return ndxSent;

    } catch (Exception ex) {
      logger.error("getOffsetNode failed", ex);
      return null;
    }
  }
  @Override
  public XmlNode getTopNode(String sLocs) {
    try {
      // Read the indicated sentence
      if (!readSent(sLocs)) return null;
      
      // Return the topmost <eTree>
      return ndxSent.SelectSingleNode("./descendant::eTree[@Label != 'CODE'][1]");

    } catch (Exception ex) {
      logger.error("getOffsetNode failed", ex);
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
   * @param sLocs
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
  
  @Override
  public void close() {
    if (bUseRa) {
      this.objXmlRaRdr.close();
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
        String sSentLine  = "";
        if (bUseRa) {
          sSentLine = objXmlRaRdr.getOneLine(sSentId);
        } else {
          sSentLine = objXmlRdr.getOneLine(sSentId);
        }
        // If we don't get anything, then return false
        sSentLine = sSentLine.trim();
        if (sSentLine.isEmpty()) {
          logger.error("readSent: empty line for sentence id ["+sSentId+"]");
          return false;
        }
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
      String sSentLine;
      if (bUseRa) {
        sSentLine = objXmlRaRdr.getRelativeLine(iOffset, oSentId);
      } else {
        sSentLine = objXmlRdr.getRelativeLine(iOffset, oSentId);
      }
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
  
  /**
   * PsdxToSvg
   *    Convert the tree starting at [ndxThis] to an SVG element
   *    Then return this SVG element as a string
   * 
   * @param ndxTree
   * @param ndxSel
   * @return String
   */
  private String PsdxToSvg(String sConvType, XmlNode ndxTree, XmlNode ndxSel) {
    String sBack = "";  // The string to be returned
    Lithium litThis = new Lithium(CorpusResearchProject.ProjType.ProjPsdx, sConvType);
        
    try {
      // Start making a new Lithium tree
      sBack = litThis.MakeLitTree(ndxTree, ndxSel);
      return sBack;
    } catch (Exception ex) {
      logger.error("PsdxToSvg failed", ex);
      return sBack;
    }
  }
}
