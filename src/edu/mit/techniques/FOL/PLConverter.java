package edu.mit.techniques.FOL;

import java.util.*;
import java.io.*;

import edu.mit.techniques.FOL.parser.Parser;


/** Contains static methods to convert first-order to propositional logic.
 */

public class PLConverter {

	private PLConverter(){} // don't make one of these

	/** <p>Converts a <code>Sentence</code> to the propositional
	 subset of first-order logic, given a finite "universe" of
	 <code>Objects</code>.  It does not mutate either the
	 <code>Sentence</code> or the <code>Universe</code>.  We encourage
	 you to use Strings as the Objects in your universe, though it is
	 not strictly necessary. We will be using the "unique names"
	 hypothesis, and thus the objects in your universe should return
	 distinct names via their toString() method.  It is these names
	 that will be used when translating the "Equals" relation between
	 two terms.  See the note on "Equals" below for more information
	 about this.

	<p>This converter <em>does not</em> handle Functions, thus the
	only terms in your FOL setences should be constant terms and
	variables.  In practice, the FOL parser parses all symbols as
	<code>Variables</code>, so if you create a sentence in some way
	other than using the parser, you should just use Variables for
	your ConstantTerms --- The converter treats all free variables
	(i.e. variables not introduced by a Quantifier) as constant terms.

	<p> The conversion process works as follows: The parse tree of the
	Sentence is traversed.  Nodes which represent syntactic element
	that are legal in propositional logic are trivially translated
	(Negation, Connectives, and AtomicPropositions). Thus,
	AtomicPropositions in first-order logic are equivalent to the
	Variables of propositional logic.

    <p>The nodes that need explicit translation are Quantifiers and
    CompoundPropositions (relations).  Quantifiers over variables are
    translated into Conjunctions and Disjunctions by replacing the
    variable of quantification with constant terms from the universe.
    
	<p>Quantified variables are <em>typed</em>.  In an untyped system,
	quantified variables range over every element in the universe.  In
	our system, quantified variables only range over elements in the
	universe with the same type. To keep a simple syntax, we will use
	the following convention to indicate types: A variable ranges over
	exactly those elements in the universe which share the same, case
	insensitive, letter as the variable.  Thus, in "all t p(t)", the
	"t" will only be filled in with elements beginning with "t" or
	"T".  If <em>none</em> of the elements in the universe share the
	same initial letter with a variable, it is assumed to range over
	all elements in the universe. A good convention would be to use
	capital letters for constant terms, and lowercase letters for
	variables.
	
    <p>CompoundPropositions/Relations are translated into
    AtomicPropositions by gathering up the terms of the relation into
    the name of the AtomicProposition.  Thus, you should think of the
    atomic proposition "P_Object1_Object2" as holding only in
    (first-order) universes where P(Object1, Object2) holds.

	<p>The compound proposition "Equals" has special treatment, and
	rather than being converted into an AtomicProposition, it is
	converted into either the TrueProposition or the
	FalseProposition. This conversion is possible because all variable
	terms will be eliminated by the translation of Quantifiers.  Thus,
	we only need to translate "Equals" between two constant
	terms. Constant terms, (either pure constant terms or variables
	bound to elements in the universe) are considered equal if their
	names are equal.  */


	public static Sentence convert(Sentence input, Set universe){
		Sentence result = internalConvert(input, universe, new HashMap());
		System.out.println("Created " + propMap.size() + " unique propositions.");
		propMap.clear();
		return result;
	}
	
	private static Sentence internalConvert(Sentence input, 
											Set universe,
											Map bindings){
		if(input instanceof CompoundProposition){
			CompoundProposition prop = (CompoundProposition) input;
			String propName = prop.getRelationSymbol().toString();
			TermList list = prop.getTerms();

			if("Equals".equals(propName))
				return handleEquals(list, bindings);
			else
				return handleProposition(propName, list, bindings);
		}
		else if(input instanceof Quantifier){
			Quantifier quantifier = (Quantifier) input;
			Variable variable = quantifier.v;
			Sentence sentence = quantifier.s;
			char vChar = variable.toString().toLowerCase().charAt(0);

			// gather up the clauses
			Object[] elements = universe.toArray();
			char[] types = new char[elements.length];
			List clauseList = new ArrayList();

			// first, check to see if this is an untyped variable
			boolean typed = false;
			for(int i = 0; i < elements.length; i++){
				types[i] = elements[i].toString().toLowerCase().charAt(0);
				typed |= (vChar == types[i]);
			}

			for(int i = 0; i < elements.length; i++){
				// This is how we type variables!
				if(!typed || vChar == types[i]){
					Sentence copy = (Sentence) sentence.clone();
					bindings.put(variable, elements[i]);
					clauseList.add(internalConvert(copy, universe, bindings));
					bindings.remove(variable);
				}
			}

			// Otherwise place them into a Conjunction/Disjunction
			Sentence[] clauses = 
				(Sentence[]) clauseList.toArray(new Sentence[clauseList.size()]);
			Sentence base = clauses[0];

			if(quantifier instanceof Universal)
				for(int i = 1; i<clauses.length; i++)
					base = new Conjunction(clauses[i], base);

			else if(quantifier instanceof Existential)
				for(int i = 1; i<clauses.length; i++)
					base = new Disjunction(clauses[i], base);

			return base;
		}
		else if(input instanceof Proposition){
			return (Proposition) input.clone();
		}
		else if(input instanceof Negation){
			Negation neg = (Negation) input.clone();
			neg.s1 = internalConvert(neg.s1, universe, bindings);
			return neg;
		}
		else{ // must be connective
 			Connective con = (Connective) input.clone();
			con.s1 = internalConvert(con.s1, universe, bindings);
			con.s2 = internalConvert(con.s2, universe, bindings);
			return con;
		}
	} 


	////////////////////////////////////////////////////////////////////
	// Code for handling propositions

	private static Object lookup(Term t, Map bindings){
		if(t instanceof Function)
			throw new RuntimeException("Can't convert Function " + t);
		Object o = bindings.get(t);
		return (o == null)
			? t.toString()
			: o;
	}

	private static Sentence handleEquals(TermList list, Map bindings){
		Term t1 = (Term) list.getFirst();
		Term t2 = (Term) list.getRest().getFirst();

		if(lookup(t1, bindings).toString()
		   .equals(lookup(t2, bindings).toString()))
			return Sentence.TRUE;
		else
			return Sentence.FALSE;
	}
			

	// This will hopefully save us some memory
	private static Map propMap = new HashMap();
	private static Sentence handleProposition(String propName, 
											  TermList list, Map bindings){
		int length = list.length();
		StringBuffer sb = new StringBuffer();
		sb.append(propName);
		
		for(int i=0; i<length; i++){
			sb.append("_" + lookup((Term) list.getFirst(), bindings));
			list = list.getRest();
		}

		// See if we've already allocated this proposition
		String name = sb.toString();
		Proposition prop = (Proposition) propMap.get(name);
		if(prop == null){ // allocate a new one
			prop = new AtomicProposition(name);
			propMap.put(name, prop);
		}
		return prop;
	}

	////////////////////////////////////////////////////////////////////
	// Code for end-to-end conversion

	/** Converts a <code>Sentence</code> into a
		<code>techniques.PL.Sentence</code>.  The input sentence must
		<em>not</em> contain non-propositional subcomponents. 
		</ul> */

    public static edu.mit.techniques.PL.Sentence CNFconvert(Sentence s){
		ClauseList clauseList = fullSimplify(s);

		System.out.println("Converting to PL data structure...");
		
		// Convert to techniques.PL
		List clauses = new ArrayList();
		List literals = new ArrayList();
		
		for(int i=0; i < clauseList.size(); i++){
			Clause clause = clauseList.clauseAt(i);
			literals.clear();
			
			for(int j=0; j < clause.size(); j++){
				Literal literal = clause.literalAt(j);
				Proposition prop = literal.proposition();

				edu.mit.techniques.PL.Variable var = 
					new edu.mit.techniques.PL.Variable(prop.toString());
				
				if(literal.isNegative())
					literals.add(new edu.mit.techniques.PL.Negation(var));
				else
					literals.add(var);
			}
		    
			clauses.add(new edu.mit.techniques.PL.Disjunction(literals));
			
		}
		System.out.println("done.");
		return new edu.mit.techniques.PL.Conjunction(clauses);
    }
	
    /** Given an input file in the FOL language, and a universe,
		writes an output file in the PL language. */
    public static void convertFile(String inPath, String outPath, Set universe){
		Sentence s = Parser.parseFile(inPath);
		
		System.out.print("Converting to propositions...");
		s = convert(s, universe);
		System.out.print("done");		

		if (s == null)
			System.out.println("Empty sentence");
		
		ClauseList clauseList = fullSimplify(s);
		
		try {
			System.out.print("Writing file...");
			PrintStream ps = new PrintStream(new FileOutputStream(outPath));
			clauseList.print(ps);
			System.out.println("done.");	
		} 
		catch(IOException e){ 
			System.out.println("File write failed:" + e);
		}
    }


	private static ClauseList fullSimplify(Sentence s){
		System.out.print("Eliminating Equivalences...");
		s = s.eliminateEquivalences();
		System.out.println("done.");
		
		System.out.print("Eliminating Implications...");
		s = s.eliminateImplications();
		System.out.println("done.");
		
		System.out.print("Driving Negations...");
		s = s.driveInNegations();
		System.out.println("done.");
		
		System.out.print("Simplifying...");
		s = s.simplify();
		System.out.println("done.");
		
		System.out.print("Clausifying...");
		ClauseList clauseList = s.makeClauses();
		System.out.println("done.");

		return clauseList;
	}

	/** Prints out the results of conversion on a sentence.  The
        sentence is passed in as a String.  You are encouraged to use
        this method to play around with the conversion process and
        understand what it is doing. */
	public static void verboseTest(String s, Set universe){
		Sentence sentence = edu.mit.techniques.FOL.parser.Parser.parse(s);
		Sentence convert = convert(sentence, universe);
		System.out.println("The Universe:\t" + universe);
		System.out.println("FOL:\t" + sentence);
		System.out.println("PL:\t" + convert);
		System.out.println("CNF-ified:\t" + CNFconvert(convert));
		System.out.println();
	}

	public static void test(String s, Set universe){
		Sentence sentence = edu.mit.techniques.FOL.parser.Parser.parse(s);
		Sentence convert = convert(sentence, universe);
		System.out.println("FOL:\t" + sentence);
		System.out.println();
		CNFconvert(convert);
		System.out.println();
	}
	


}
