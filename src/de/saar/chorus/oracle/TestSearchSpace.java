
package de.saar.chorus.oracle;

import junit.framework.*;

import java.util.*;



public class TestSearchSpace extends TestCase {
    // test fixture
    private SearchSpace<String> space;

    protected void setUp() {
	space = new SearchSpace<String>();

	space.addState("root", "rootinfo", null);
	space.addState("child1", "childinfo1", "root");
	space.addState("child2", "childinfo2", "root");
    }



    // the tests

    public void testRetrievalNonNull() {
	assertTrue( "root", space.getStateForName("root") != null );
	assertTrue( "first child", space.getStateForName("child1") != null );
	assertTrue( "second child", space.getStateForName("child2") != null );
    }

    public void testRetrievalValues() {
	assertTrue( "root",  space.getStateForName("root").equals("rootinfo") );
	assertTrue( "first child", space.getStateForName("child1").equals("childinfo1") );
	assertTrue( "second chlid", space.getStateForName("child2").equals("childinfo2") );

	assertTrue( "root-is-root", space.getRootName().equals("root") );
    }

    public void testParent() {
	assertTrue( space.getParentName("root") == null );
	assertTrue( "root".equals(space.getParentName("child1")) );
	assertTrue( "root".equals(space.getParentName("child2")) );
    }

    public void testRootChildrenNotNull() {
	assertTrue( space.getChildren("root") != null );
    }

    public void testRootChildren() {
	List<String> children = space.getChildren("root");
	int found1 = 0, found2 = 0;
	for( String state : children ) {
	    if( state.equals("child1") ) found1++;
	    else if( state.equals("child2") ) found2++;
	}

	assertTrue( found1 == 1 );
	assertTrue( found2 == 1 );
    }

    public void testRootGrandchildrenIsNull() {
	List<String> grandchildren = space.getChildren("child1");
	assertTrue( grandchildren.size() == 0 );
    }




		



    // default methods for junit

    public TestSearchSpace(String name) {
	super(name);
    }

    public static Test suite() {
	return new TestSuite(TestSearchSpace.class);
    }
}
