/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.xq;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Erwin R. Komen
 */
public class LexDict {
  public List<LexEl> lDict = null;// The dictionary for this line
  public int QC;                  // The QC line for this dict
  // ================ Class initializer =====================
  public LexDict(int iQC) {
    // Initialize a new list
    lDict = new ArrayList<>();
    this.QC = iQC;
  }
  /**
   * Add
   *    Add a word/pos combination to the list
   * 
   * @param sWord
   * @param sPos 
   */
  public void Add(String sWord, String sPos) {
    // Validate
    if (lDict == null) return;
    // Check if this combination is in the list
    for (int i=0;i<lDict.size();i++) {
      if (lDict.get(i).Word.equals(sWord) && lDict.get(i).Pos.equals(sPos)) {
        // Change the frequency
        lDict.get(i).Freq++;
        return;
      }
    }
    // We did not find it: add an entry with fequency 1
    lDict.add(new LexEl(sWord, sPos, 1));
  }
  /**
   * getWord
   *    Get the word from position i
   * 
   * @param i
   * @return 
   */
  public String getWord(int i) { if (i>=0 && i < lDict.size()) return lDict.get(i).Word; else  return "";}
  /**
   * getPos
   *    Get the pos from position i
   * 
   * @param i
   * @return 
   */
  public String getPos(int i) {  if (i>=0 && i < lDict.size()) return lDict.get(i).Pos; else return "";}
  /**
   * getFreq
   *    Get the freq from position i
   * 
   * @param i
   * @return 
   */
  public int getFreq(int i) {  if (i>=0 && i < lDict.size()) return lDict.get(i).Freq; else return 0;}
}
// Define LexEl elements
class LexEl {
  public String Word;   // The word of this element
  public String Pos;    // The POS (or other) category for this word
  public int Freq;      // The frequency of occurrence for this word/pos combination
  // ================ Class initializer =====================
  public LexEl(String sWord, String sPos, int iFreq) {
    this.Word = sWord; this.Pos = sPos; this.Freq = iFreq;
  }
}
