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
        RewriteSystem weakening = new RewriteSystem();
        Annotator ann = new Annotator();

        parser.read(new StringReader(testRewriting), weakening, null, ann);
        System.out.println("weak=" + weakening);
        System.out.println("ann=" + ann);
    }

    public static String testRewriting = """

// weakening rules
[+] a/2 > every/2
[-] every/2 > a/2

// equivalence rules
* = pron_rel/2

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

