package edu.mit.techniques.PL;

import java.io.*;
import java.util.*;

/** A set of utilities for dealing with propositional logic sentences
 *  in conjunctive normal form (CNF). */
 
public final class CNF {

  static char vee = 'v';
  static char wedge = '^';
  static char neg = '~';

  /** Parses a <code>String</code> into a <code>Conjunction</code>. This
	  Parser does very little error checking.  Unparsable sentences
	  will either produce garbage or a runtime <code>Exception</code>.
	  Whitespace is insignificant. */
  public static Conjunction parse(String sentence){
	return parseConjunction(sentence);
  }


  
  /** Parses a textfile into a <code>Conjunction</code>. If there is an
	  IO Error, this method will print an error message and returns
	  null.  Otherwise, this Parser does very little error checking.
	  Unparsable sentences will either produce garbage or a runtime
	  <code>Exception</code>.  Whitespace in the file is
	  insignificant. */
  public static Conjunction parse(File infile){
	try{/* read in the file */

	  StringBuffer sb = new StringBuffer();
	  Reader reader = new BufferedReader(new FileReader(infile));
	  int c = -1;

	  while((c = reader.read()) != -1)
		sb.append((char) c);
	  return parse(sb.toString());
	}
	catch(IOException e){
	  System.out.println("Error reading file: " + infile);
	  return null;
	}
  }

  /**
     Given a file containing a CNF sentence, this function
     outputs the same sentence, in DIMACS format, to a file.
     Also, produces a file containing a map of the original
     Variable names to the new DIMACS names.
  */
  public static String pl2dimacs( File cnfFile ){
    Conjunction c = parse( cnfFile );
    return pl2dimacs( c, cnfFile.toString()+".dimacs" );
  }
  /**
     Given a file containing a CNF sentence,this function
     outputs the same sentence, in DIMACS format, to a file.
     Also, produces a file containing a map of the original
     Variable names to the new DIMACS names.
   */
  public static String pl2dimacs( File cnfFile, String dimacsFile ){
    Conjunction c = parse( cnfFile );
    return pl2dimacs( c, dimacsFile );
  }
  /**
     Given a CNF sentence,this function
     outputs the same sentence, in DIMACS format, to a file.
     Also, produces a file containing a map of the original
     Variable names to the new DIMACS names.
   */
  public static String pl2dimacs( Sentence cnfSentence, String dimacsFile ){
    String mapFile = dimacsFile+".map" ;
    if( cnfSentence instanceof Conjunction ){
      Conjunction cnf = (Conjunction)cnfSentence;
      Set vars = cnf.getVariables();
      List disjunctions = cnf.getClauses();
      int numVars = vars.size();
      int numDisjunctions = disjunctions.size();
      HashMap nameToInt = new HashMap();
      
      String valenceStr = "";
      String varName = "";
      int counter = 1;
      
      String dimacsString = "c original CNF sentence: \n";
      //dimacsString += "c "+cnf.toString()+"\nc\nc\n";
      dimacsString += "p cnf "+numVars+" "+numDisjunctions+" \n";
      String mapString = "";
      
      for( int i = 0; i<numDisjunctions; i++){
	List units = ( (Disjunction)disjunctions.get(i) ).getClauses();
	int numUnits = units.size();
	for( int j = 0; j< numUnits; j++ ){
	  Sentence v = (Sentence)units.get(j);
	  if( v instanceof Variable ){
	    varName = ((Variable)v).toString();
	    valenceStr = "";
	  }
	  else if( v instanceof Negation ){
	    varName = ((((Negation)v).getVariables().toArray())[0]).toString();
	    valenceStr = "-";
	  }
	  Object intName = nameToInt.get( varName );
	  if( intName == null ){
	    intName = new Integer( counter );
	    nameToInt.put( varName, intName);
	    mapString += varName + " " + counter+ "\n";
	    counter++;
	  }
	  dimacsString += ""+ valenceStr + intName.toString() +" ";
	}
	dimacsString += "0 \n"; //end the clause
      }
      
      
      try{
	Writer writer = new BufferedWriter(new FileWriter(dimacsFile));
	writer.write(dimacsString);
	writer.close();
      }
      catch(IOException e){ System.out.println("IO Error writing file " + dimacsFile + ".");}
      
      try{
	Writer writer = new BufferedWriter(new FileWriter(mapFile));
	writer.write(mapString);
	writer.close();
      }
      catch(IOException e){ System.out.println("IO Error writing file " + mapFile + ".");}
      
    }
    else{
      System.out.println(" Didn't get a conjunction. Exiting. ");
      return null;
    }
    return mapFile;
  }

  /**
     Given the a fileName containing the standard output of a zChaff invocation,
     and the fileName of the mapFile generated by CNF.pl2dimacs(),
     create an Interpretation that maps PL.Variables (as specified in the mapFile)
     to the assignments reported by zChaff. 
   */
  public static Interpretation zchaffToInterpretation( String zchaffOutputFile, String mapFile ){

    BufferedReader in;		
    String line =null;
    int space = -1;

    HashMap names = new HashMap();
    Boolean assignment = null;
    Integer intName = null;
    String asmt = "";
    Interpretation interp= new Interpretation(); 
    
    // read in the map of the names
    try{
      in = new BufferedReader(new FileReader( mapFile ));
      line = in.readLine();
      while( line != null ){
	space = line.indexOf(' ');
	String name = line.substring(0,space);
	intName =new Integer( Integer.parseInt( line.substring( space+1 )));
	names.put( intName , name );
	line = in.readLine();
      }
    }catch(IOException e){
      System.out.println("Error reading file: " + mapFile);
      return null;
    }
    

    // read in the zchaff output
    try{
    in = new BufferedReader(new FileReader( zchaffOutputFile ));
    line = in.readLine();
    while( line != null ){
      if( line.startsWith( "Verify Solution successful" ) ){//next line contains the assignment
	line = in.readLine();
	space = line.indexOf(' ');
	
	asmt = line.substring( 0,space );
	intName = new Integer( Math.abs( Integer.parseInt( asmt )) );
	assignment = getAssignment( asmt, intName );
	interp.put( new Variable( (String)names.get( intName )), assignment );
	
	line = line.substring( space+1 );
	space = line.indexOf(' ');
	while( space != -1 ){ 

	  asmt = line.substring( 0,space );
	  intName =  new Integer( Math.abs( Integer.parseInt( asmt )) );
	  assignment = getAssignment( asmt, intName );
	  interp.put( new Variable(  (String)names.get( intName )), assignment );
	  //System.out.println( " putting "+intName+" / "+assignment );
	  line = line.substring( space+1 );//next iteration
	  space = line.indexOf(' ');
	}
      }
      
      line = in.readLine();
    }
    }catch(IOException e){
      System.out.println("Error reading file: " + zchaffOutputFile);
      return null;
    }
    if( interp.isEmpty() )
      return null;
    
    return interp;
  }
  private static Boolean getAssignment( String asmt, Integer intNameOut ){
    int numValue = Integer.parseInt( asmt );
    Boolean assignment = null;
    if( numValue < 0 ){
      intNameOut = new Integer( Math.abs( numValue ));
      assignment = Boolean.FALSE;
    }
    else{
      intNameOut = new Integer( Math.abs( numValue ));
      assignment = Boolean.TRUE;
    }
    return assignment;
  }

  /* attempts to parse a conjunction. */
  private static Conjunction parseConjunction(String sentence) {
	sentence = trimParens(sentence);
	List list = new ArrayList();
	int index = sentence.indexOf(wedge);
	
	if (index == -1){
	  list.add(parseDisjunction(sentence));
	  sentence = "";
	}

	while(index != -1){
	  list.add(parseDisjunction(sentence.substring(0, index)));
	  sentence = trimParens(sentence.substring(index+1));
	  index = sentence.indexOf(wedge);
	}

	if (!(sentence.length() == 0))
	  list.add(parseDisjunction(sentence));
	
	return new Conjunction(list);
  }
	
  /* attempts to parse a disjunction. */
  private static Sentence parseDisjunction(String sentence) {
	sentence = trimParens(sentence);
	List list = new ArrayList();
	int index = sentence.indexOf(vee);
	
	if (index == -1){
	  list.add(parseLiteral(sentence));
	  sentence = "";
	}

	while(index != -1){
	  list.add(parseLiteral(sentence.substring(0, index)));
	  sentence = trimParens(sentence.substring(index+1));
	  index = sentence.indexOf(vee);
	}

	if (!(sentence.length() == 0))
	  list.add(parseLiteral(sentence));

	return new Disjunction(list);
  }

  /* attempts to parse a Literal.  If none is present, causes a 
   * runtime exception. */
  private static Sentence parseLiteral(String sentence){
	sentence = trimParens(sentence);
	int index = sentence.indexOf(' ');

	/* error check */
	if(sentence.length() == 0)
	  throw new RuntimeException("Parse Error: Missing Variable.");
	
	boolean negated = (sentence.charAt(0) == neg);

	if(negated)
	  sentence = trimParens(sentence.substring(1));

	if(sentence.length() < 1 || !Character.isLetter(sentence.charAt(0)))
	  throw new RuntimeException("Parse Error: unexpected literal " 
								 + sentence);
	Variable v = new Variable(sentence);
	if(negated)
	  return new Negation(v);
	else
	  return v;
  }

  private static String trimParens(String input){
	input = input.trim();

	while(input.startsWith("(") && input.endsWith(")")){
	  int depth = 1;
	  boolean matched = true;
	  for(int i = 1; i < input.length()-1; i++){
		char c = input.charAt(i); 
		if(c == '(')
		  depth++;
		else if (c == ')')
		  depth--;
		if(depth == 0)
		  matched = false;
	  }
	  
	  if(depth == 1 && matched)
		input = input.substring(1, input.length()-1).trim();
	  else if (depth == 1)
		break;
	  else if (depth != 1)
		throw new RuntimeException("Parse Error: Parenthesis Mismatch.");
	}

	return input;
  }

  private static final int NUMVARS = 26;

  /** A command-line accessible version of the
	 <code>randInstance()</code> method.  It takes one mandatory and
	 one optional argument.  The first argument is a real number
	 indicating the ratio of clauses to variables in the returned
	 instance.  The second (optional) argument is a filename in which
	 to write the results. If no second argument is supplied, the
	 sentence will be sent to standard out. 

	 <p>For example:

     <p><code> % java techniques.PL.CNF 2.2 mysentence.cnf</code>

	 <p>will result in a sentence with 57 clauses (about 2.2*26),
	 written to a file named <code>mysentence.cnf</code>. See 
     <a href="http://www.ai.mit.edu/courses/6.825/hw1/javadocs/techniques/PL/CNF.html#randInstance(int)">randInstance()</a>
	 for more documentation.*/

  public static void main(String[] args){
	if(args.length != 1 && args.length != 2){
	  System.out.println("Usage:");
	  System.out.print("\t java techniques.PL.CNF ");
	  System.out.println("<clause/var ratio> " + "filename.cnf");
	  return;
	}
	double ratio = Double.parseDouble(args[0]);
	int numClauses = Math.max(1, (int) Math.round(ratio * NUMVARS));
	String instance = randInstance(numClauses);

	if(args.length == 1){
	  System.out.println(instance);
	  return;
	}
	try{
	  Writer writer = new BufferedWriter(new FileWriter(args[1]));
	  writer.write(instance);
	  writer.close();
	}
	catch(IOException e){
	  System.out.println("IO Error writing file " + args[1] + ".");
	}
  }

  /** Returns a randomly generated 3-SAT instance, as a String.  All
	 sentences are drawn from a world of 26 variables, A-Z.
	 <code>numClauses</code> is the number of disjunctive clauses to
	 generate. */
  public static String randInstance(int numClauses){
	StringBuffer sb = new StringBuffer(randDisjunction());
	for(int i = 1; i < numClauses; i++)
	  sb.append(" " + CNF.wedge + "\n" + randDisjunction());
	return sb.toString();
  }

  /** Returns a disjunction of three literals chosen by randLiteral() */
  public static String randDisjunction(){
	return "(" + randLiteral() 
	  + " " + CNF.vee + " " + randLiteral() 
	  + " " + CNF.vee + " " + randLiteral() + ")"; 
  }

  /** Returns a random literal, as a string.  The variable of the
      literal is drawn uniformly from 'A'-'Z', and negated with
      probability 0.5. */
  public static String randLiteral(){
	return (coinFlip() ? "~" : "") + (char)('A' + randInt(0, 25));
  }

  /** Returns true with probability 0.5. */
  public static boolean coinFlip(){
	return (Math.random() < 0.5);
  }

  /** returns a random integer between a and b, inclusive. */
  public static int randInt(int a, int b) {
	return((int)(Math.floor(Math.random()*(b-a+1))+a));
  }

}
