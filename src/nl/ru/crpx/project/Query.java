/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.project;
import java.util.List;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;

/**
 *
 * @author E.R.Komen
 */
public class Query {
  // Local variables part of this class -- all can be accessed like that
  public int Line;               // The line of this query
  public int InputLine;          // The number of the input line
  public int OviewLine;          // The number of the line in the overview to be produced
                          //    (If there are "empty" lines, this may have effect)
  public int CatNum;             // Number of subcategories
  public boolean InputCmp;       // Whether the input from [InputLine] is "Cmp" (T) or "Out" (F)
  public boolean Cmp;            // Whether to produce a complement or not
  public boolean Mother;         // Store the results and the complement as a kind of "mother" file for later usage
  public boolean NoExmp;         // Only counting (T), or also include examples (F)
  public String Name;            // Name of the query
  public String InputFile;       // Input file or files
  public String QueryFile;       // The query file
  public String OutputFile;      // The output file
  public String ErrorFile;       // The error output file
  public String Descr;           // The description to appear in the HTML table's row header
  public String Args;            // Arguments for the JAVA CS process => EXTINCT??
  public String HtmlFile;        // Name of temporary html file containing output for this query
  public List<String> Examp;         // Collection of examples belonging to this QC step
  public String[][] CatExamp;    // Collection of subcategories in this QC step
  public XQueryExecutable Exe;   // The executable query
  public XQueryExpression Exp;   // Compiled Query
  public XQueryEvaluator Qeval;  // Query evaluator
  public String Qstring;         // The whole query as string
}


