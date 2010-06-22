/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal

import org.junit.*

/**
 *
 * @author koller
 */
class RewritingSystemParserTest {
    @Test
    public void testParsing() throws Exception {
        RewritingSystemParser parser = new RewritingSystemParser();
        RewriteSystem weakening = new RewriteSystem(true);
        RewriteSystem equivalence = new RewriteSystem(false);
        Annotator ann = new Annotator();

        parser.read(new StringReader(testRewriting), weakening, equivalence, ann);
        System.out.println("weak=" + weakening);
        System.out.println("ann=" + ann);
    }

    public static String testRewriting = """

// weakening rules
[+] a(X, every(Y,Z)) -> every(Y, a(X,Z))
[-] every(Y, a(X,Z)) -> a(X, every(Y,Z))

// equivalence rules
pron_rel(X, *[Y]) = *[pron_rel(X,Y)]

// annotator
start annotation: +
neutral annotation: 0

+: a(+,+)
-: a(-,-)
0: a(0,0)

+: every(-,+)
-: every(+,-)
0: every(0,0)


    """;
}

