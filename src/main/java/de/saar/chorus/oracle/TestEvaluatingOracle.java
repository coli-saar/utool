
package de.saar.chorus.oracle;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestEvaluatingOracle extends TestCase {
    private class StringEvaluator implements Evaluator<String> {
        public double evaluate(String info) {
	    return Double.parseDouble(info);
        }
    }

    private EvaluatingOracle<EvaluatingSearchSpace<String>,String> oracle;
    private  StringEvaluator eval;
    

    protected void setUp() {
        eval = new StringEvaluator();
        
        oracle = new EvaluatingOracle<EvaluatingSearchSpace<String>, String>() {
            protected EvaluatingSearchSpace<String> generateNewSpace() {
                return new EvaluatingSearchSpace<String>(eval);
            }


            protected String xmlToDomain(String elt) {
		return elt;
            }
        };

        oracle.getSpace().addState( "root", "-2799", null );
        oracle.getSpace().addState( "child1", "3.14", "root" );
        oracle.getSpace().addState( "child2", "-7e2", "root" );
    }
        
    

    // representative for all inherited message processor cases
    public void testProcessReset() {
        Message res = oracle.processMessage(new Message("reset", null));

        assertTrue("empty", oracle.getSpace().isEmpty());
        
        checkConfirmMessage(res);
    }

    public void testProcessEvaluate() {
        Map<String,String> map = new HashMap<String,String>();
        map.put("id", "child1");
        Message m = new Message("evaluate", map);

        Message res = oracle.processMessage(m);

        assertTrue("evaluated Message", "evaluated".equals(res.getType()) );
        assertTrue("correct value", "3.14".equals(res.getArgument("value")));
    }

    public void testProcessMinLocal() {
        Map<String,String> map = new HashMap<String,String>();
        map.put("parentid", "root");
        Message m = new Message("minLocal", map);

        Message res = oracle.processMessage(m);
        
        assertTrue("minLocalResult message", "minLocalResult".equals(res.getType()) );
        assertTrue("correct value", "-700.0".equals(res.getArgument("value")));
        assertTrue("correct child", "child2".equals(res.getArgument("childid")));

    }

    public void testProcessMinGlobal() {
        Message m = new Message("minGlobal", null);
        Message res = oracle.processMessage(m);

        assertTrue("minGlobalResult message", "minGlobalResult".equals(res.getType()) );
        assertTrue("correct value", "-2799.0".equals(res.getArgument("value")));
        assertTrue("correct node", "root".equals(res.getArgument("id")));

    }
    

    




    protected void checkConfirmMessage(Message m) {
        assertTrue("result message is confirm", m.getType().equals("confirm"));
    }
        


    // default methods for junit

    public TestEvaluatingOracle(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestEvaluatingOracle.class);
    }
}    
    
