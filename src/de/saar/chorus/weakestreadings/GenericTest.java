
package de.saar.chorus.weakestreadings;

import de.saar.theoremprovers.mace.Mace;
import de.saar.theoremprovers.mace.Model;
import de.saar.theoremprovers.mace.ModelTextGUI;
import de.saar.theoremprovers.spass.Spass;
import edu.mit.techniques.FOL.Implication;
import edu.mit.techniques.FOL.Sentence;


class GenericTest {
    protected boolean printCountermodels;


    // The three possible outcomes of SpassAndMace calls.
    public static final int SAM_PROOF = 1;
    public static final int SAM_COUNTERMODEL = 2;
    public static final int SAM_NEITHER = 3;


    protected static String prettyFormulas(Sentence[] lhs, Sentence rhs) {
	StringBuffer sb = new StringBuffer("Axioms:\n");

	for( int i = 0; i < lhs.length; i++ )
	    sb.append( "(" + (i+1) + ") " + lhs[i] + "\n");

	sb.append("Thm:\n" + rhs + "\n");

	return sb.toString();
    }


    protected int callSpassAndMace( String desc, Sentence[] lhs,
				    Sentence rhs, boolean expected ) {
	Model counter;
	Sentence[] empty = new Sentence[0];
	Sentence thm = Auxiliary.universalClosure(new Implication(Auxiliary.listAnd(lhs), rhs));
	//Sentence thm = new Implication(Auxiliary.universalClosure(Auxiliary.listAnd(lhs)), 
	//		       Auxiliary.universalClosure(rhs));

	String formulas = prettyFormulas(lhs, rhs);

	if( Spass.prove(desc + "\n\n" + formulas + "\n", empty, thm) )
	    return SAM_PROOF;
	else {
	    counter = Mace.computeCountermodel(desc, empty, thm);
	    if( counter == null )
		return SAM_NEITHER;
	    else {
		if( expected && printCountermodels ) {
		    // this direction _should_ be provable, but we
		    // found a countermodel

		    System.out.println("\nCountermodel for " + desc);
		    System.out.println(formulas);

		    ModelTextGUI.displayModel(System.out, counter);
		}

		return SAM_COUNTERMODEL;
	    }
	}
    }

    protected int callSamWithEntailment( Entailment ent,
					 String desc, Sentence[] axioms,
					 Sentence thm, boolean expected ) {
	Sentence axiomsInOne = Auxiliary.listAnd(axioms);
	Sentence rhs = Auxiliary.listAnd(ent.rhs(axiomsInOne, thm));
	Sentence[] lhs = ent.lhs(axiomsInOne, thm);

	return callSpassAndMace( desc, lhs, rhs, expected );
    }

    protected String arrowpoint( int samResult, String point ) {
	switch(samResult) {
	case SAM_PROOF: return point;
	case SAM_COUNTERMODEL: return "*";
	case SAM_NEITHER: return "?";
	default: return "-";
	}
    }

    protected Sentence insertContextEntailment(Rule r, String context, Entailment ent,
					       boolean computeLhs, boolean contextPres) {
	Sentence lhs = r.getLHS(), rhs = r.getRHS();

	if( contextPres ) {
	    lhs = Contexts.insert( lhs, context );
	    rhs = Contexts.insert( rhs, context );
	}

	//	System.err.println("\n\n\nEntailment for LHS:");
	Sentence lhsWithPre = Auxiliary.listAnd(ent.lhs(lhs, rhs));

	//	System.err.println("\n\n\nEntailment for RHS:");
	Sentence rhsWithPre = Auxiliary.listAnd(ent.rhs(lhs, rhs));

	if( !contextPres ) {
	    lhsWithPre = Contexts.insert( lhsWithPre, context );
	    rhsWithPre = Contexts.insert( rhsWithPre, context );
	}

	if( computeLhs )
	    return lhsWithPre;
	else
	    return rhsWithPre;
    }

    /*

	// Cheating: Compute presupps first, then insert into context
	Sentence lhsWithPre = Auxiliary.listAnd(ent.lhs(r.getLHS(), r.getRHS()));
	Sentence rhsWithPre = Auxiliary.listAnd(ent.rhs(r.getLHS(), r.getRHS()));

	Sentence lhsPreContext = Contexts.insert(lhsWithPre, context);
	Sentence rhsPreContext = Contexts.insert(rhsWithPre, context);

	// Less cheating: Insert into context first, then compute presupps
	Sentence lhsInContext = Contexts.insert(r.getLHS(), context);
	Sentence rhsInContext = Contexts.insert(r.getRHS(), context);

	Sentence entLHS = Auxiliary.listAnd(ent.lhs(lhsInContext, rhsInContext));
	Sentence entRHS = Auxiliary.listAnd(ent.rhs(lhsInContext, rhsInContext));
    */
	    


    protected void checkRuleContextEntailment(Rule rule, String context, Entailment ent ) {
	boolean cheat = false; // i.e. don't compute presupps of contexts

	Rule r;
	if( Contexts.negative(context) ) {
	    r = rule.invert();
	} else {
	    r = rule;
	}

	Sentence lhs = insertContextEntailment(r, context, ent, true, !cheat);
	Sentence rhs = insertContextEntailment(r, context, ent, false, !cheat);

	String label = pad(r.getLabel() + " / " + context, 15);
	String desc = ent.getLabel() + ": " + r.getLabel() + " / " + context;

	System.out.println( label + ":  " +
			    bidirectionalCheck(desc, lhs, rhs,
					       !Contexts.negative(context)) );
    }

    protected String bidirectionalCheck( String desc, Sentence lhs,
					 Sentence rhs, boolean lToRExpected ) {
	String resultString = "";

	resultString += arrowpoint( callSpassAndMace(desc + " <-",
						     new Sentence[] { rhs },
						     lhs,
						     !lToRExpected),
				    "<" );
	resultString += "-";
	resultString += arrowpoint( callSpassAndMace(desc + " ->",
						     new Sentence[] { lhs }, 
						     rhs,
						     lToRExpected),
				    ">" );

	return resultString;
    }






    protected static String pad(String s, int toLength) {
	StringBuffer b = new StringBuffer(s);

	for( int i = 0; i < toLength - s.length(); i++ )
	    b.append(" ");

	return b.toString();
    }


    GenericTest(Boolean printCountermodels) {
	this.printCountermodels = printCountermodels.booleanValue();
    }

    GenericTest() {
	printCountermodels = false;
    }

    public static void main(String args[]) {
	boolean printCountermodels = false;
	String className = "GenericTest";

	// command-line parameters
	for( int i = 0; i < args.length; i++ )
	    if( args[i].equals("-cm") )
		printCountermodels = true;
	    else {
		className = args[i];
		break;
	    }

	GenericTest test = newInstance( className, printCountermodels );
	test.go(args);
    }


    // This is an ugly workaround. Because main() is static and I don't want
    // to provide a new implementation in every offspring class, I'm passing
    // the class name here as a string, and recover the appropriate class object.
    protected static GenericTest newInstance(String className, boolean printCountermodels) {
	try {
	    Class c = Class.forName(className);
	    Class[] constructorArgClasses = {Boolean.class};
	    Object[] constructorArgs = {new Boolean(printCountermodels)};
	    
	    GenericTest inst = (GenericTest)
		c.getDeclaredConstructor(constructorArgClasses).
		newInstance(constructorArgs);

	    return inst;
	} catch(Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	    return null;
	}
    }
    
    // override me
    public void go(String[] args) {
	System.err.println("GenericTest.go() called! Please override this method in your implementation.");
    }
}
