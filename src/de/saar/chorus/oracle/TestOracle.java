
package de.saar.chorus.oracle;

import java.util.*;
import junit.framework.*;


/* NOTE: Oracles create new spaces whenever a new client
   makes a connection. This (i.e. there is a space, it's a 
   different space than before, etc.) should be tested somewhere.
*/

public class TestOracle extends TestCase {
    private Oracle<SearchSpace<String>,String> oracle;

    protected void setUp() {
        oracle = new Oracle<SearchSpace<String>, String>() {
            protected SearchSpace<String> generateNewSpace() {
                return new SearchSpace<String>();
            }

            protected String xmlToDomain(String elt) {
		// strip off < and />, i.e. the data for this
		// kind of search space is just the tag name
		// of the (single) element in the data description.
		return elt.substring(1, elt.length()-2);
            }
        };
    }

  

    protected void checkConfirmMessage(Message m) {
        assertTrue("result message is confirm", m.getType().equals("confirm"));
    }
        
    
    public void testProcessReset() {
        Message res = oracle.processMessage(new Message("reset", null));

        assertTrue("empty", oracle.getSpace().isEmpty());
        
        checkConfirmMessage(res);
    }

    public void testProcessInit() {
        Message res = oracle.processMessage(new Message("init", null));

        checkConfirmMessage(res);
    }

    protected Message newMessage(String id, String parentid, String desc) {
        return new Message("<new id=\'" + id + "\' parentid=\'" +
                           parentid + "\'><" + desc + "/></new>");
    }

    public void testProcessNew() {
	SearchSpace<String> space = oracle.getSpace();
        Message res = 
            oracle.processMessage(newMessage("root", "root", "rootinfo"));
        checkConfirmMessage(res);

        Message res2 = oracle.processMessage(newMessage("child", "root", "childinfo"));
        checkConfirmMessage(res2);

        assertTrue( "root info", "rootinfo".equals(space.getStateForName("root")) );
        assertTrue( "child info", "childinfo".equals(space.getStateForName("child")) );

        assertTrue( "root parent", space.getParentName("root") == null );
        assertTrue( "child parent", "root".equals(space.getParentName("child")) );
    }

    public void testReject() {
        Message res = oracle.rejectMessage();
        assertTrue( res.getType().equals("reject") );
    }

    public void testConfirm() {
        Message res = oracle.confirmMessage();
        checkConfirmMessage(res);
    }




    // default methods for junit

    public TestOracle(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestOracle.class);
    }
}    
