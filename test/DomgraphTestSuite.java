import groovy.util.GroovyTestSuite;
import junit.framework.Test;
import junit.framework.TestSuite;


public class DomgraphTestSuite extends TestSuite {
	   // Since Eclipse launches tests relative to the project root,
    // declare the relative path to the test scripts for convenience
    private static final String TEST_ROOT = "test/";
    private static final String DOMGRAPH = "de/saar/chorus/domgraph/";
    private static final String SUFFIX = "Test.groovy";
    
    private static TestSuite suite = new TestSuite();
    private static GroovyTestSuite gsuite = new GroovyTestSuite();
    
    private static void addDomgraphTest(String name) throws Exception {
    	suite.addTestSuite(gsuite.compile(TEST_ROOT + DOMGRAPH + name + SUFFIX));
    }
    
    public static Test suite() throws Exception {
        
        //suite.addTestSuite(FooTest.class);  // non-groovy test cases welcome, too.
        
    	addDomgraphTest("codec/CodecManager");
    	addDomgraphTest("codec/basic/Chain");
    	addDomgraphTest("codec/domcon/DomconOzInputCodec");
        addDomgraphTest("graph/DomGraph");
        addDomgraphTest("chart/SplitComputer");
        addDomgraphTest("chart/SolvedFormIterator");
    	
        return suite;
    }
}
