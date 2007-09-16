import groovy.util.GroovyTestSuite;
import junit.framework.Test;
import junit.framework.TestSuite;


public class RondaneTestSuite extends TestSuite {
    private static TestSuite suite = new TestSuite();
    private static GroovyTestSuite gsuite = new GroovyTestSuite();
    
    public static Test suite() throws Exception {
    	suite.addTestSuite(gsuite.compile("test/testsuite/rondane/RondaneTestsuite.groovy"));
    	
        return suite;
    }
}
