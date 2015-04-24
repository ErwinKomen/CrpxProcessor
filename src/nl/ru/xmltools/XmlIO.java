/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.xmltools;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource; 
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import javax.xml.transform.OutputKeys;
import org.w3c.dom.Document;
import org.apache.log4j.Logger;

/**
 *
 * @author E.R.Komen
 */
public class XmlIO {
  // This class uses a logger
  private static final Logger logger = Logger.getLogger(XmlIO.class);
/* ---------------------------------------------------------------------------
   Name:    WriteXml
   Goal:    Write a document as xml. 
            Use the project's location/name in this.Location
   History:
   20/04/2015   ERK Created
   --------------------------------------------------------------------------- */
  public static boolean WriteXml(Document dThis, String sFile) {
    // Validate
    if (dThis == null) return false;
    // Get the file name from this.Location
    if (sFile.equals("")) return(false);
    // Create a file entry point
    File fFile = new File(sFile);
    
    // Use a Transformer for output
    TransformerFactory tFactory = TransformerFactory.newInstance();
    Transformer transformer;
    try {
      transformer = tFactory.newTransformer();
      // Make sure indentation is used
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");

      // Define soure and result
      DOMSource source = new DOMSource(dThis);
      StreamResult result = new StreamResult(fFile);
      // Write from source to result
      transformer.transform(source, result);
    } catch (TransformerConfigurationException tce) {
      String sErr = "WriteXML: Transformer Factory error" + " " + tce.getMessage();
      logger.error(sErr, tce);
      return false;
    } catch (TransformerException te) {
      String sErr = "WriteXML: Transformation error" + " " + te.getMessage();
      logger.error(sErr, te);
      return false;
    } 
    // Return positively
    return true;
  }
}
