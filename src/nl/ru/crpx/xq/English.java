/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.crpx.xq;

/**
 *
 * @author Erwin R. Komen
 */
public class English {
  // ====================== local constants ====================================
  private static final String SPEC_CHAR_IN = "tadegTADEG";
  private static final String SPEC_CHAR_OUT = "þæđëġÞÆĐËĠ";

  // -------------------------------------------------------------------------------------------------------
  // Name: VernToEnglish
  // Goal: Convert a vernacular OE text into intelligable English
  // Notes:
  //       - Special characters are defined as '+' + character:
  //         +t, +a,
  // History:
  // 27-11-2008   ERK Created for .NET
  // 19-12-2008   ERK Adapted for VB2005 TreeBank module
  // 30/apr/2015  ERK Adapted for Java
  // -------------------------------------------------------------------------------------------------------
  public static final String VernToEnglish(String strText) {
    String strOut = ""; // Output to be build up
    int intI;           // Position in the string
    int iSize;          // Size of the string

    // Determine size
    iSize = strText == null ? 0 : strText.length();
    // Check all characters of the input
    for (intI = 1; intI <= iSize; intI++) {
      // Check this character for a key
      switch (strText.substring(intI - 1, intI - 1 + 1)) {
        case "+": // This should be a special character
          // Goto next character
          intI++;
          // Copy the translation of the special character
          strOut += GetSpecChar(strText.substring(intI - 1, intI - 1 + 1));
          break;
        default: // Just copy the input
          strOut += strText.substring(intI - 1, intI - 1 + 1);
          break;
      }
    }
    // Return the string we have now made
    return strOut;
  }
  // -------------------------------------------------------------------------------------------------------
  // Name: GetSpecChar
  // Goal: Given a special character with a + sign, convert into "normal" English
  // History:
  // 27-11-2008   ERK Created for .NET
  // 30/apr/2015  ERK Adapted for Java
  // -------------------------------------------------------------------------------------------------------
  private static String GetSpecChar(String strIn) {
    String tempGetSpecChar = null;
    int intPos = 0;

    // Get position in input string
    intPos = SPEC_CHAR_IN.indexOf(strIn.substring(0, 1)) + 1;
    if (intPos > 0) {
      tempGetSpecChar = SPEC_CHAR_OUT.substring(intPos - 1, intPos - 1 + 1);
    } else {
      // Output the input with + sign
      tempGetSpecChar = "+" + strIn;
    }
    return tempGetSpecChar;
  }
}
