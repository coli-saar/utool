package de.saar.coli.chorus.oracle.sxdg;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import de.saar.coli.chorus.oracle.Evaluator;
import de.saar.coli.chorus.oracle.SortedEvaluatingSearchSpace;


public class TestSxdgReflection extends TestCase {
    private class DummyEvaluator implements Evaluator<SxdgReflection> {
	public double evaluate(SxdgReflection info) {
	    return 1;
	}
    }
	    

    private SxdgReflection reflDirect, reflDiff;
    private SortedEvaluatingSearchSpace<SxdgReflection> space;
    private DummyEvaluator eval;

    // fixture creation
    protected void setUp() {
	eval = new DummyEvaluator();
	space = new SortedEvaluatingSearchSpace<SxdgReflection>(eval);
	reflDirect = null;

	String doc = "<description mode=\'complete\'><node id='1'><entry id='every' /><entry id='every2' /><mother id='2' label='det' /><mother id='4' label='gen'/><mother id='3' label='det' /></node><node id='2'><entry id='researcher' /><mother id='6' label='subj' /></node><node id='3'><entry id='of' /><mother id='2' label='gen' /></node><node id='4'><entry id='a' /><mother id='5' label='det' /></node><node id='5'><entry id='company' /><mother id='3' label='genobj' /></node><node id='6'><entry id='sees' /><mother id='9' label='root' /></node><node id='7'><entry id='some' /><mother id='8' label='det' /></node><node id='8'><entry id='sample' /><mother id='6' label='obj' /></node><node id='9'><entry id='.' /></node></description>";
	reflDirect = new SxdgReflection(doc, space);
	space.addState( "root", reflDirect, null );

	String diffString = "<description mode=\'diff\'><node id='1'><entry id='every' /><mother id='3' label='det' /></node><node id='2'></node><node id='3'></node><node id='4'></node><node id='5'><mother id='3' label='genobj'/></node><node id='6'></node><node id='7'></node><node id='8'></node><node id='9'></node></description>";
	reflDiff = new SxdgReflection(diffString, space, reflDirect);
	space.addState( "diff", reflDiff, "root" );
    }


    // reflDirect does _something_
    public void testDirectNotNull() {
	assertTrue( reflDirect != null );
    }


    // reflDirect contains correct info
    public void testNodeSet() {
	Set<String> expected = new HashSet<String>();
	expected.add("1");
	expected.add("2");
	expected.add("3");
	expected.add("4");
	expected.add("5");
	expected.add("6");
	expected.add("7");
	expected.add("8");
	expected.add("9");

	assertTrue(reflDirect.getNodeSet().equals(expected));
    }

    public void testLexEntries() {
	Set<String> entries1 = new HashSet<String>(),
	    entries5 = new HashSet<String>();

	entries1.add("every");
	entries1.add("every2");
	entries5.add("company");

	assertTrue( "node 1", entries1.equals(reflDirect.getLexEntries("1") ) );
	assertTrue( "node 5", entries5.equals(reflDirect.getLexEntries("5") ) );
    }

    public void testAllEdgeLabels() {
	Set<String> expected = new HashSet<String>();
	expected.add("det");
	expected.add("subj");
	expected.add("gen");
	expected.add("genobj");
	expected.add("root");
	expected.add("obj");

	assertTrue( expected.equals(reflDirect.getAllEdgeLabels()) );
    }

    public void testIncomingEdges() {
	Set<String> expected1Det = new HashSet<String>();
	expected1Det.add("2");
	expected1Det.add("3");
	assertTrue( "1 det", expected1Det.equals(reflDirect.getIncomingEdges("1", "det")));

	Set<String> expected5Genobj = new HashSet<String>();
	expected5Genobj.add("3");
	assertTrue( "5 genobj", expected5Genobj.equals(reflDirect.getIncomingEdges("5", "genobj")));
    }

    public void testIncomingEdgeLabels() {
	Set<String> expected = new HashSet<String>();
	expected.add("det");
	expected.add("gen");

	assertTrue( expected.equals(reflDirect.getIncomingEdgeLabels("1")) );
    }
	

    // diff works
    public void testDiffNodes() {
	Set<String> expected = new HashSet<String>();

	expected.add("1");
	expected.add("2");
	expected.add("3");
	expected.add("4");
	expected.add("5");
	expected.add("6");
	expected.add("7");
	expected.add("8");
	expected.add("9");

	assertTrue(reflDiff.getNodeSet().equals(expected));
    }

    public void testDiffLexentries() {
	Set<String> entries1 = new HashSet<String>(),
	    entries5 = new HashSet<String>();

	entries1.add("every2");
	entries5.add("company");

	assertTrue( "node 1", entries1.equals(reflDiff.getLexEntries("1") ) );
	assertTrue( "node 5", entries5.equals(reflDiff.getLexEntries("5") ) );
    }

    public void testDiffAllEdgeLabels() {
	Set<String> expected = new HashSet<String>();
	expected.add("det");
	expected.add("subj");
	expected.add("gen");
	expected.add("root");
	expected.add("obj");

	assertTrue( expected.equals(reflDiff.getAllEdgeLabels()) );
    }

    public void testDiffIncomingEdges() {
	// 1 had incoming edges <det, 2>, <gen, 4>, <det, 3>
	// diff deleted the edge <det, 3>.
	Set<String> expected1Det = new HashSet<String>();
	expected1Det.add("2");
	assertTrue( "1 det", expected1Det.equals(reflDiff.getIncomingEdges("1", "det")));

	// 5 had incoming edges <genobj, 3>
	// diff deleted this edge
	assertTrue( "5 genobj",
		    reflDiff.getIncomingEdges("5", "genobj").isEmpty() );

	// 2 had incoming edges <gen, 3>
	// diff didn't delete this edge.
	Set<String> expected3Gen = new HashSet<String>();
	expected3Gen.add("2");
	assertTrue( "3 gen", 
		    expected3Gen.equals(reflDiff.getIncomingEdges("3", "gen")) );

    }

    public void testDiffIncomingEdgeLabels() {
	Set<String> expected1 = new HashSet<String>();
	expected1.add("det");
	expected1.add("gen");
	assertTrue( "1",
		    expected1.equals(reflDiff.getIncomingEdgeLabels("1")) );

	Set<String> expected5 = new HashSet<String>();
	assertTrue( "5",
		    expected5.equals(reflDiff.getIncomingEdgeLabels("5")) );
    }

    


    //// reproduce former bugs here ////


    // Former bug: getIncomingEdges(nodename, edgelabel) makes
    // sure that there is an (empty) entry for edgelabel in 
    // incomingEdges[nodename]. getIncEdgeLabels checks only
    // for which edgelabels there are entries, without checking
    // whether they are actually nonempty sets.
    public void testIncomingEdgeLabelsAfterQuery() {
	Set<String> foo = reflDirect.getIncomingEdges("1", "genobj");
	assertEquals( 2, reflDirect.getIncomingEdgeLabels("1").size() );
    }




    // default methods for junit

    public TestSxdgReflection(String name) {
	super(name);
    }

    public static Test suite() {
	return new TestSuite(TestSxdgReflection.class);
    }
}
