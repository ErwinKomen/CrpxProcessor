/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.crpx.project;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;

/**
 *
 * @author E.R.Komen
 */
public class Query {
  // Local variables part of this class -- all can be accessed like that
  int Line;               // The line of this query
  int InputLine;          // The number of the input line
  int OviewLine;          // The number of the line in the overview to be produced
                          //    (If there are "empty" lines, this may have effect)
  int CatNum;             // Number of subcategories
  boolean InputCmp;       // Whether the input from [InputLine] is "Cmp" (T) or "Out" (F)
  boolean Cmp;            // Whether to produce a complement or not
  boolean Mother;         // Store the results and the complement as a kind of "mother" file for later usage
  boolean NoExmp;         // Only counting (T), or also include examples (F)
  String Name;            // Name of the query
  String InputFile;       // Input file or files
  String QueryFile;       // The query file
  String OutputFile;      // The output file
  String ErrorFile;       // The error output file
  String Descr;           // The description to appear in the HTML table's row header
  String Args;            // Arguments for the JAVA CS process => EXTINCT??
  String HtmlFile;        // Name of temporary html file containing output for this query
  String[] Examp;         // Collection of examples belonging to this QC step
  String[][] CatExamp;    // Collection of subcategories in this QC step
  XQueryExecutable Exe;   // The executable query
  XQueryEvaluator Qeval;  // Query evaluator
}


