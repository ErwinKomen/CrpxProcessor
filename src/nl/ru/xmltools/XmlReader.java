/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.xmltools;

import java.io.InputStream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;

/**
 *
 * @author E.R.Komen
 */
public class XmlReader {
  // This class uses a logger
  private static final Logger logger = Logger.getLogger(XmlReader.class);
  // ======================== variables for internal use
  private XMLInputFactory factory = null;
  private XMLEventReader reader = null;
  private XPath xpath = null;
  private XMLEvent eventLast = null;
  // Instantiation of the class
  public XmlReader() {
    // First, create a new XMLInputFactory
    factory = XMLInputFactory.newInstance();
    // Also create an xpath thingy (TODO: do we need it??)
    xpath = XPathFactory.newInstance().newXPath();
  }
  // Create an XML reader
  public void Create(InputStream is) throws XMLStreamException {
    reader = factory.createXMLEventReader(is);
  }
  // Read until the first occurrance of start element [sItem]
  public boolean ReadToFollowing(String sItem) {
    // Validate
    if (reader == null) return false;
    // Iterate through the reader
    while (reader.hasNext()) {
      XMLEvent event = (XMLEvent)reader.next();
      eventLast = event;
      // We are looking for a start element
      if (event.isStartElement()) {
        // Get the start element
        StartElement startElement = event.asStartElement();
        // Check if the start element is the one we are looking for
        if (startElement.getName().getLocalPart().equals(sItem)) {
          // We are there, so return
          return true;
        }
      }
    }
    // We haven't reached it, so return negatively
    return false;
  }
  // Read the complete content from start until end element as a string
  public String ReadOuterXml() {
    String sTag = "";   // the tag we are looking for
    int iNesting = 0;   // Nesting level
    // Get the current starting element
    if (reader == null) return "";
    XMLEvent event = eventLast;
    if (event.isStartElement()) {
      // Get the start element
      StartElement startElement = event.asStartElement();
      sTag = startElement.getName().getLocalPart();
      // Read until we reach this same tag as end element
      while (reader.hasNext()) {
        event = (XMLEvent)reader.next();
        eventLast = event;
        // Check on start or end element
        if (event.isEndElement() && event.asStartElement().getName().getLocalPart().equals(sTag)) {
          // Keep track of the nexting
          iNesting--;
          // Are we there?
          if (iNesting <= 0) {
            // Yes, we are there: return the string we found
            
          }
        } else if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals(sTag)) {
          // Keep track of the nesting level
          iNesting++;
        }
      }
    }
    return "";
  }
}
