
package de.saar.chorus.weakestreadings;

import edu.mit.techniques.FOL.*;


class TestEntDirection extends GenericTest {
    public void go(String[] args) {
	String[] contexts = Contexts.available();
	Rule[] rules = Rule.rules;


	for( int i = 0; i < rules.length; i++ ) {
	    System.out.println("");

	    for( int j = 0; j < contexts.length; j++ ) {
		Rule r = rules[i];


		Sentence fmla = Contexts.insert(r.getLHS(),
						contexts[j]);
		String desc = r.getLabel() + " LHS / " + contexts[j];

		System.out.println(desc + ": " + bidirectionalCheck(desc,
						      Presuppositions.projectedPol(fmla, true),
						      new Conjunction(fmla,
								      Presuppositions.pi(fmla)),
						      true));



		//		System.err.println("\n\n\n-------------------------\n\n");


		Sentence fmla2 = Contexts.insert(r.getRHS(),
						contexts[j]);
		String desc2 = r.getLabel() + " RHS / " + contexts[j];

		System.out.println(desc2 + ": " + bidirectionalCheck(desc2,
						      Presuppositions.projectedPol(fmla2, true),
						      new Conjunction(fmla2,
								      Presuppositions.pi(fmla2)),
						      true));


	    }
	}
    }

    TestEntDirection(Boolean printCountermodels) {
	super(printCountermodels);
    }
}
