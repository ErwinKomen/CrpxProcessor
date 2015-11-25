/*
 * This software has been developed at the "Meertens Instituut"
 *   for the CLARIN project "CorpusStudio-WebApplication".
 * The application is based on the "CorpusStudio" program written by Erwin R. Komen
 *   while working for the Radboud University Nijmegen.
 * The program and the source can be freely used and re-distributed.
 */
package nl.ru.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Erwin R. Komen
 */
public class DateUtil {
  private static final String sGregorianFormat = "yyyy-MM-dd'T'HH:mm:ss";
  /**
   * stringToDate
   *    Convert a string-date into a Date class object
   * 
   * @param s
   * @return
   * @throws ParseException 
   */
  public static Date stringToDate(String s) throws ParseException {
    // Create the correct format of what we are expecting
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(sGregorianFormat);
   // Parse string to date
   return simpleDateFormat.parse(s);       
  }
  
  /**
   * dateToString
   *    Convert a Date-class object into a string
   * 
   * @param dtThis
   * @return 
   */
  public static String dateToString(Date dtThis) {
    // Create the format
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(sGregorianFormat);
    return simpleDateFormat.format(dtThis);
  }
  public static String dateToString(long dtThis) {
    // Create the format
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(sGregorianFormat);
    return simpleDateFormat.format(dtThis);
  }
  
}
