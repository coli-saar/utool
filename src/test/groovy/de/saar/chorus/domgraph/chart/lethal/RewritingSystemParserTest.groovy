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
        parser.read(new StringReader(testRewriting));
    }

    public static String testRewriting = """

// weakening rules
[+] every/2 > a/2
[-] a/2 > every/2

// equivalence rules
* = pron_rel/2

// annotator
start annotation: +
neutral annotation: 0

+: exists(+,+)
-: exists(-,-)
0: exists(0,0)

+: forall(-,+)
-: forall(+,-)
0: forall(0,0)


    """;
}

