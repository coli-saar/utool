package de.saar.theoremprovers.spass;

import java.util.Enumeration;
import java.util.Hashtable;

import de.saar.convenientprocess.ConvenientProcess;
import edu.mit.techniques.FOL.ConstantTerm;
import edu.mit.techniques.FOL.Function;
import edu.mit.techniques.FOL.Proposition;
import edu.mit.techniques.FOL.Sentence;
import edu.mit.techniques.FOL.Term;
import edu.mit.techniques.FOL.TermList;


public class Spass {
    private ConvenientProcess process;
    private Boolean foundResult;

    private String desc;

    private static int problemcounter = 1;


    public Spass(String desc, String logfile) {
	String spassCmdname = System.getProperty("spass.bin");

	process = new ConvenientProcess(spassCmdname + " -Stdin");
	process.setOutputLog(logfile + ".toSpass");
	process.setInputLog(logfile + ".fromSpass");

	foundResult = null;
	this.desc = desc;
    }

    public static boolean prove(String desc, Sentence[] axioms, Sentence theorem) {
	String logfile = "logs/chorusprob." + problemcounter;
	String realdesc;

	if( desc == null )
	    realdesc = "chorusprob." + problemcounter;
	else
	    realdesc = desc;

	problemcounter++;

	Spass s = new Spass(desc, logfile);

	s.send(axioms,theorem);
	return s.findResult();
    }

    private void send(Sentence[] axioms, Sentence theorem) {
	process.println("begin_problem(test).");

	process.println("list_of_descriptions.");
	process.println("name({*test*}).");
	process.println("author({*CHORUS-Demo*}).");
	process.println("status(unsatisfiable).");
	process.println("description({*" + desc + "*}).");
	process.println("end_of_list.\n");
		
	process.println("list_of_symbols.");
	send_symbols(axioms, theorem);
	process.println("end_of_list.");
		
	process.println("list_of_formulae(axioms).");
	for( int i = 0; i < axioms.length; i++ )
	    process.println("formula(" + axioms[i].toDFG() + ", " + (i+1) + ").");
	process.println("end_of_list.\n");
	
	process.println("list_of_formulae(conjectures).");
	process.println("formula(" + theorem.toDFG() + "," + (axioms.length+1) + ").");
	process.println("end_of_list.\n");
	
	process.println("end_problem.");

	process.closeWritingPipe();
	process.run();
    }

    private void send_symbols(Sentence[] axioms, Sentence theorem) {
	Hashtable predicates = new Hashtable(), functions = new Hashtable();
	String predstring, funcstring;
	boolean first = true;
	
	for( int i = 0; i < axioms.length; i++ )
	    collect_symbols(axioms[i], predicates, functions);
	collect_symbols(theorem, predicates, functions);

	if( functions.size() > 0 ) {
	    funcstring = "functions[";
	    first = true;

	    for( Enumeration ef = functions.keys(); ef.hasMoreElements(); ) {
		String key = (String) ef.nextElement();
		Integer arity = (Integer) functions.get(key);
		
		if( !first ) funcstring += ", ";
		funcstring += "(" + key + "," + arity + ")";
		
		first = false;
	    }
	    process.println(funcstring + "].");
	}

	if( predicates.size() > 0 ) {
	    predstring = "predicates[";
	    first = true;

	    for( Enumeration ep = predicates.keys(); ep.hasMoreElements(); ) {
		String key = (String) ep.nextElement();
		Integer arity = (Integer) predicates.get(key);

		if( !first ) predstring += ", ";
		predstring += "(" + key + "," + arity + ")";
		
		first = false;
	    }
	    process.println(predstring + "].");
	}
    }

    private void collect_symbols(Sentence sent, Hashtable predicates, Hashtable functions) {
	if( sent.isSentenceType("Proposition") ) {
	    Proposition sentAsProp = (Proposition) sent;

	    if( sentAsProp.getRelationSymbol() != null ) {
		// i.e. not Verum or Falsum
		predicates.put(sentAsProp.getRelationSymbol().name(), 
			       new Integer(sentAsProp.getArity()));
		
		TermList terms = sentAsProp.getTerms();
		while(terms != null) {
		    collect_term_symbols(terms.getFirst(), functions);
		    terms = terms.getRest();
		}
	    }
	} else {
	    Sentence[] subforms = sent.subformulas();
	    for( int i = 0; i < subforms.length; i++ )
		collect_symbols(subforms[i], predicates, functions);
	}
    }

    private void collect_term_symbols(Term t, Hashtable functions) {
	if( t.isTermType("ConstantTerm") ) {
	    functions.put(((ConstantTerm)t).getFunctionSymbol().name(), new Integer(0));
	} else if( t.isTermType("Function") ) {
	    Function ft = (Function) t;
	    functions.put( ft.getFunctionSymbol().name(), 
			   new Integer(ft.getTerms().length()) );
	}
    }


    


    // call this after process.run() has terminated.
    private boolean findResult() {
	String line;

	if( foundResult == null ) {
	    do {
		line = process.readLineStdout();

		if( line == null )
		    break;

		if( line.indexOf("SPASS beiseite") >= 0 )
		    break;
	    } while ( true );

	    if( line == null )
		foundResult = Boolean.FALSE;
	    else 
		if( line.indexOf("Proof") >= 0 )
		    foundResult = Boolean.TRUE;
		else
		    foundResult = Boolean.FALSE;
	}

	return foundResult.booleanValue();
    }

    /*
    public static void main(String[] args) {
	Spass m = new Spass();

	Sentence[] axioms = new Sentence[1];
	Sentence theorem;

	axioms[0] = Parser.parse("exists x p(x)");
	theorem = Parser.parse("p(a)");

	m.send(axioms, theorem);

	System.out.println("result: " + m.findResult());
    }
    
*/
	
}

