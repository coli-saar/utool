package de.saar.chorus.oracle;

import junit.framework.*;


public class TestEvaluatingSearchSpace extends TestCase {
    private class StringEvaluator implements Evaluator<String> {
	public double evaluate(String info) {
	    return Double.parseDouble(info);
	}
    }

    
    StringEvaluator eval;
    private EvaluatingSearchSpace<String> space;

    protected void setUp() {
	eval = new StringEvaluator();
	space = new EvaluatingSearchSpace<String>(eval);

	space.addState( "root", "-2799", null );
	space.addState( "child1", "3.14", "root" );
	space.addState( "child2", "-7e2", "root" );
    }

    public void testCorrectEvaluation() {
	assertTrue( "root", space.evaluate("root") == -2799 );
	assertTrue( "child1", space.evaluate("child1") == 3.14 );
	assertTrue( "child2", space.evaluate("child2") == -7e2 );
    }

    public void testMinLocal() {
	StateEvaluation res = space.minLocal("root");

	assertTrue( "minLocal returns null", res != null );
	assertTrue( "minLocal returns wrong child (" + res.getStateName() + ")",
		    "child2".equals(res.getStateName()) );
	assertTrue( "minLocal returns wrong value", res.getEval() == -7e2 );
    }

    public void testMinGlobal() {
	StateEvaluation res = space.minGlobal();

	assertTrue( "minGlobal returns null", res != null );
	assertTrue( "minGlobal returns wrong child", "root".equals(res.getStateName()) );
	assertTrue( "minGlobal returns wrong value", res.getEval() == -2799 );
    }






    // default methods for junit

    public TestEvaluatingSearchSpace(String name) {
	super(name);
    }

    public static Test suite() {
	return new TestSuite(TestEvaluatingSearchSpace.class);
    }
}
