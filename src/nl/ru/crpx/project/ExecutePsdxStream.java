/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.project;

/**
 *
 * @author E.R.Komen
 */
/* ---------------------------------------------------------------------------
   Class:   ExecutePsdxStream
   Goal:    CRP execution for .psdx files forest-by-forest
   History:
   20/apr/2015   ERK Created
   --------------------------------------------------------------------------- */
public class ExecutePsdxStream extends ExecuteXml {
  // ============ Local variables =====================================
  
  /* ---------------------------------------------------------------------------
  Name:    ExecutePsdxStream
  Goal:    Perform initialisations needed for this class
  Note:    We go file by file, <forest> by <forest>, and execute the queries
              consecutively on this <forest> element.
           This method does *NOT* allow resolving references across the <forest> borders to be pursued
           This procedure also adds the RESULTS in *xml* form ONLY
           This procedure uses a StreamReader for the XML files
  History:
  20/apr/2015   ERK Created
  ------------------------------------------------------------------------------ */
  public ExecutePsdxStream() {
    // Do the initialisations for all Execute() classes
    super();
    
    
    // TODO: make sure we provide [intCurrentQCline] with the correct value!!
  }

  /* ---------------------------------------------------------------------------
     Name:    ExecuteQueries
     Goal:    Execute the queries in the given order for Xquery type processing
     History:
     18-03-2014  ERK Created with elements from [ExecuteQueriesXqueryFast] for .NET
     23/apr/2015 ERK Transformation into Java started
     --------------------------------------------------------------------------- */
  @Override
  public boolean ExecuteQueries() {
    try {
      // Perform general setup
      if (!super.ExecuteQueriesSetUp()) return false;
      // Perform setup part that is specifically for Xml/Xquery
      if (!super.ExecuteXmlSetup()) return false;
      
      // TODO: continue implementing the actual execution of the queries
      
      // Return positively
      return true;
    } catch (RuntimeException ex) {
      // Warn user
      DoError("ExecutePsdxStream/ExecuteQueries error: " + ex.getMessage() + "\r\n");
      // Return failure
      return false;
    }
  }
  

}
