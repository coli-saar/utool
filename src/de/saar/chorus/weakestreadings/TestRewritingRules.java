
package de.saar.coli.chorus.weakestreadings;


class TestRewritingRules extends GenericTest {
    private void testOneEntailment(Entailment ent) {
	String[] contexts = Contexts.available();
	Rule[] rules = Rule.rules;

	System.out.println("Running tests for entailment: " + ent.getLabel());

	for( int i = 0; i < rules.length; i++ ) {
	    //1.5	for( Rule rule : Rule.rules ) {
	    System.out.println("");

	    for( int j = 0; j < contexts.length; j++ )
		//	    for( String context : contexts )
		checkRuleContextEntailment(rules[i], contexts[j], ent);
	    //		checkRuleContextEntailment(rule, context, ent);
	}
    }

    public void go(String[] args) {
	testOneEntailment(Entailment.withName(args[1]));

	//	testOneEntailment(Entailment.pi2InPlace);
	//	testOneEntailment(Entailment.piipTransitive);
	//		testOneEntailment(Entailment.projectTrans);
    }

    TestRewritingRules(Boolean printCountermodels) {
	super(printCountermodels);
    }
}
