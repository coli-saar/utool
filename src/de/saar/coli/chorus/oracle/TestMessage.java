
package de.saar.coli.chorus.oracle;

import junit.framework.*;

import java.util.*;
import org.w3c.dom.*;



public class TestMessage extends TestCase {
    private String foobarbaz, abcd;
    private Message fbbMessage, mapMessage;
    private Map<String,String> map;

    // fixture creation
    protected void setUp() {
        foobarbaz = "<foo bar=\"baz\" bar2=\"baz2\"><fooo/></foo>";
        abcd = "<abcd a=\"b\" c=\"d\" />";

        fbbMessage = new Message(foobarbaz);

        map = new HashMap<String,String>();
        map.put("x", "y");
        map.put("z", "w");

        mapMessage = new Message("xyzw", map);

    }



    // the tests
    public void testCreationNonNull() {
        assertTrue(fbbMessage != null );
    }

    public void testComponentsNonNull() {
        assertTrue("type", fbbMessage.getType() != null );
        assertTrue("bar", fbbMessage.getArgument("bar") != null );
        assertTrue("bar2", fbbMessage.getArgument("bar2") != null );
    }   

    public void testConstructorDecoding() {
        assertTrue("type", fbbMessage.getType().equals("foo"));
        assertTrue("bar", fbbMessage.getArgument("bar").equals("baz"));
        assertTrue("bar2", fbbMessage.getArgument("bar2").equals("baz2"));
    }

    public void testDirectDecoding() {
        fbbMessage.decode(abcd);

        assertTrue("type", fbbMessage.getType().equals("abcd"));
        assertTrue("a", fbbMessage.getArgument("a").equals("b"));
        assertTrue("c", fbbMessage.getArgument("c").equals("d"));
    }

    public void testXmlEncodingWithData() {
        Message m2 = new Message(fbbMessage.encode());

        assertTrue("type", m2.getType().equals("foo"));
        assertTrue("bar", m2.getArgument("bar").equals("baz"));
        assertTrue("bar2", m2.getArgument("bar2").equals("baz2"));
    }

    public void testXmlEncodingWithoutData() {
        Message m2 = new Message(new Message(abcd).encode());

        assertTrue("type", m2.getType().equals("abcd"));
        assertTrue("a", m2.getArgument("a").equals("b"));
        assertTrue("c", m2.getArgument("c").equals("d"));
    }

    public void testMapComponentsNonNull() {
        assertTrue("type", mapMessage.getType() != null);
        assertTrue("x", mapMessage.getArgument("x") != null);
        assertTrue("z", mapMessage.getArgument("z") != null);
    }   

    public void testMapComponentsCorrect() {
        assertTrue("type", mapMessage.getType().equals("xyzw"));
        assertTrue("x", mapMessage.getArgument("x").equals("y"));
        assertTrue("z", mapMessage.getArgument("z").equals("w"));
    }

    public void testMapIndependentOfExternalChanges() {
        map.put("x", "12345");
        assertTrue("x", mapMessage.getArgument("x").equals("y"));
    }

    public void testDataCorrectValue() {
        assertTrue( "<fooo/>".equals(fbbMessage.getData()) );
    }
                



    // default methods for junit

    public TestMessage(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestMessage.class);
    }
}
