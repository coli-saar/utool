package de.saar.chorus.oracle;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class TestSortedEvaluatingSearchSpace extends TestCase {
    private class StringEvaluator implements Evaluator<String> {
	public double evaluate(String info) {
	    return Double.parseDouble(info);
	}
    }

    
    StringEvaluator eval;
    SortedEvaluatingSearchSpace<String> space;

    protected void setUp() {
	eval = new StringEvaluator();
	space = new SortedEvaluatingSearchSpace<String>(eval);

	space.addState( "root", "-2799", null );
	space.addState( "child1", "3.14", "root" );
	space.addState( "child2", "700", "root" );
	space.addState( "child3", "3.14", "child2" );
    }


    public void testChooseGlobal() {
	int foundPiStates = 0;

	assertTrue( "first remove", "root".equals(space.chooseGlobal()) );

	String piState1 = space.chooseGlobal();
	if( "child1".equals(piState1) ) {
	    foundPiStates = 1;
	} else if( "child3".equals(piState1) ) {
	    foundPiStates = 2;
	} else {
	    assertTrue( "second remove not c1 or c3", false );
	}

	String piState2 = space.chooseGlobal();
	if( (foundPiStates == 1) && "child3".equals(piState2) )
	    ;
	else if( (foundPiStates == 2) && "child1".equals(piState2) )
	    ;
	else
	    assertTrue ("third remove not the other c1 or c3", false );

	assertTrue("fourth remove", "child2".equals(space.chooseGlobal()));

	assertTrue("only four queue elements",
		   !space.hasUnseenStates());
    }

    public void testAddFront() {
	space.addState( "test", "-10000", "child1" );

	assertTrue( "test comes first", "test".equals(space.chooseGlobal()) );
	assertTrue( "root comes second", "root".equals(space.chooseGlobal()) );
    }

    public void testAddSecond() {
	space.addState( "test", "0.1", "root" );

	assertTrue( "root comes first", "root".equals(space.chooseGlobal()) );
	assertTrue( "test comes second", "test".equals(space.chooseGlobal()) );
    }

    
    


    // default methods for junit

    public TestSortedEvaluatingSearchSpace(String name) {
	super(name);
    }

    public static Test suite() {
	return new TestSuite(TestSortedEvaluatingSearchSpace.class);
    }
}
