
package de.saar.coli.chorus.oracle.sxdg;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import de.saar.coli.chorus.oracle.SortedEvaluatingSearchSpace;


public class TestBilexicalEvaluator extends TestCase {

    private BilexicalEvaluator eval;
    private SxdgReflection reflDirect;
    private SortedEvaluatingSearchSpace<SxdgReflection> space;

    private File testdata;

    // fixture creation
    protected void setUp() {
        testdata = new File("testdata.xml");
        writeTestFile(testdata);

        eval = new BilexicalEvaluator("testdata.xml");
        space = new SortedEvaluatingSearchSpace<SxdgReflection>(eval);
        reflDirect = null;

        try {
            String doc = "<description mode=\'complete\'><node id='1'><entry id='every' /><entry id='every2' /><mother id='2' label='det' /><mother id='4' label='gen'/><mother id='3' label='det' /></node><node id='2'><entry id='researcher' /><mother id='6' label='subj' /></node><node id='3'><entry id='of' /><mother id='2' label='gen' /></node><node id='4'><entry id='a' /><mother id='5' label='det' /></node><node id='5'><entry id='company' /><mother id='3' label='genobj' /></node><node id='6'><entry id='sees' /><mother id='9' label='root' /></node><node id='7'><entry id='some' /><mother id='8' label='det' /></node><node id='8'><entry id='sample' /><mother id='6' label='obj' /></node><node id='9'><entry id='.' /></node></description>";
            reflDirect = new SxdgReflection(doc, space);
        } catch(Exception e) {
            e.printStackTrace(System.err);
        }
    }

    protected void tearDown() {
        testdata.delete();
    }


    
    public void testNotNull() {
        assertTrue( "eval", eval != null );
        assertTrue( "refl", reflDirect != null );
    }

    public void testEvalRefl() {
        // I didn't check that this is indeed the correct value for the test data.
        assertEquals( eval.evaluate(reflDirect), 1034.0892032001082 );
    }



    // default methods for junit

    public TestBilexicalEvaluator(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestBilexicalEvaluator.class);
    }



    private void writeTestFile(File file) {
        try {
            PrintWriter testWriter = new PrintWriter(new FileWriter(file));

            testWriter.println("<probabilities>");
            testWriter.println("<root lexentry=\"every\" count=\"12\" />");
            testWriter.println("<root lexentry=\"every2\" count=\"120\" />");
            testWriter.println("<root lexentry=\"researcher\" count=\"5\" />");
            testWriter.println("<root lexentry=\"of\" count=\"23\" />");
            testWriter.println("<root lexentry=\"a\" count=\"58\" />");
            testWriter.println("<root lexentry=\"company\" count=\"1\" />");
            testWriter.println("<root lexentry=\"sees\" count=\"21207\" />");
            testWriter.println("<root lexentry=\"some\" count=\"129\" />");
            testWriter.println("<root lexentry=\"sample\" count=\"4\" />");

            testWriter.println("<labelling lexentry=\"researcher\" edgelabel=\"det\" count=\"2987\" />");
            testWriter.println("<labelling lexentry=\"researcher\" edgelabel=\"gen\" count=\"2987\" />");
            testWriter.println("<labelling lexentry=\"of\" edgelabel=\"det\" count=\"2987\" />");
            testWriter.println("<labelling lexentry=\"of\" edgelabel=\"genobj\" count=\"2987\" />");
            testWriter.println("<labelling lexentry=\"a\" edgelabel=\"gen\" count=\"2987\" />");
            testWriter.println("<labelling lexentry=\"company\" edgelabel=\"det\" count=\"2987\" />");
            testWriter.println("<labelling lexentry=\"sees\" edgelabel=\"subj\" count=\"2987\" />");
            testWriter.println("<labelling lexentry=\"sees\" edgelabel=\"obj\" count=\"2987\" />");
            testWriter.println("<labelling lexentry=\"sample\" edgelabel=\"det\" count=\"2987\" />");
            testWriter.println("<labelling lexentry=\".\" edgelabel=\"root\" count=\"2987\" />");

            testWriter.println("<dependency lexentryMother=\"researcher\" lexentryDaughter=\"every\"  edgelabel=\"det\" count=\"982734\" />");
            testWriter.println("<dependency lexentryMother=\"researcher\" lexentryDaughter=\"every2\"  edgelabel=\"det\" count=\"982734\" />");
            testWriter.println("<dependency lexentryMother=\"researcher\" lexentryDaughter=\"of\" edgelabel=\"gen\" count=\"982734\" />");
            testWriter.println("<dependency lexentryMother=\"of\" lexentryDaughter=\"every\" edgelabel=\"det\" count=\"982734\" />");
            testWriter.println("<dependency lexentryMother=\"of\" lexentryDaughter=\"every2\" edgelabel=\"det\" count=\"982734\" />");
            testWriter.println("<dependency lexentryMother=\"of\" lexentryDaughter=\"company\" edgelabel=\"genobj\" count=\"982734\" />");
            testWriter.println("<dependency lexentryMother=\"a\" lexentryDaughter=\"every\" edgelabel=\"gen\" count=\"982734\" />");
            testWriter.println("<dependency lexentryMother=\"a\" lexentryDaughter=\"every2\" edgelabel=\"gen\" count=\"982734\" />");
            testWriter.println("<dependency lexentryMother=\"company\" lexentryDaughter=\"a\" edgelabel=\"det\" count=\"982734\" />");
            testWriter.println("<dependency lexentryMother=\"sees\" lexentryDaughter=\"researcher\" edgelabel=\"subj\" count=\"982734\" />");
            testWriter.println("<dependency lexentryMother=\"sees\" lexentryDaughter=\"sample\" edgelabel=\"obj\" count=\"982734\" />");
            testWriter.println("<dependency lexentryMother=\"sample\" lexentryDaughter=\"some\" edgelabel=\"det\" count=\"982734\" />");
            testWriter.println("<dependency lexentryMother=\".\" lexentryDaughter=\"sees\" edgelabel=\"root\" count=\"982734\" />");
            testWriter.println("</probabilities>");
            testWriter.close();
        } catch(Exception e) {}
    }

}
