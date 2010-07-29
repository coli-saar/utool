/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.chorus.domgraph.chart.lethal

import org.junit.*;

import de.saar.chorus.term.*;
import java.util.*;

/**
 *
 * @author koller
 */
class CompoundWithIndexTest {
    @Test
    public void testCwiParsing() throws Exception {
        Term t = RewritingSystemParser.parseTerm("f#1(g(X))");

        assert t instanceof CompoundWithIndex;
        assert ((CompoundWithIndex) t).getLabel().equals("f");
        assert ((CompoundWithIndex) t).getIndex().equals("1");
        assert ((CompoundWithIndex) t).getSubterms().size() == 1;

        Term t1 = ((CompoundWithIndex) t).getSubterms().get(0);
        assert t1 instanceof Compound;
    }

    @Test
    public void testAssignIndices() throws Exception {
        Term t = RewritingSystemParser.parseTerm("f(g(X))");
        Map a = new HashMap();
        Map p = new HashMap();

        Term t1 = CompoundWithIndex.assignIndicesToTerm(t, a, p);

        assert t1 instanceof CompoundWithIndex;
        assert ((CompoundWithIndex) t1).getLabel().equals("f");
        assert ((CompoundWithIndex) t1).getIndex().equals("_i1");
        assert ((CompoundWithIndex) t1).getSubterms().size() == 1;

        Term t2 = ((CompoundWithIndex) t1).getSubterms().get(0);
        assert t2 instanceof CompoundWithIndex;
        assert ((CompoundWithIndex) t2).getLabel().equals("g");
        assert ((CompoundWithIndex) t2).getIndex().equals("_i2");
        assert ((CompoundWithIndex) t2).getSubterms().size() == 1;
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testAssignIndicesDuplicate() throws Exception {
        Term t = RewritingSystemParser.parseTerm("f(f(X))");
        Map a = new HashMap();
        Map p = new HashMap();

        Term t1 = CompoundWithIndex.assignIndicesToTerm(t, a, p);
    }

    @Test
    public void testAssignIndicesTwoTerms() throws Exception {
        Term t = RewritingSystemParser.parseTerm("f(g(X))");
        Map a = new HashMap();
        Map p = new HashMap();

        Term t1 = CompoundWithIndex.assignIndicesToTerm(t, a, p);

        Term u = RewritingSystemParser.parseTerm("g(f(X))");
        Map pp = new HashMap();

        Term u1 = CompoundWithIndex.assignIndicesToTerm(u, pp, a);

        assert u1 instanceof CompoundWithIndex;
        assert ((CompoundWithIndex) u1).getLabel().equals("g");
        assert ((CompoundWithIndex) u1).getIndex().equals("_i2");
        assert ((CompoundWithIndex) u1).getSubterms().size() == 1;

        Term u2 = ((CompoundWithIndex) u1).getSubterms().get(0);
        assert u2 instanceof CompoundWithIndex;
        assert ((CompoundWithIndex) u2).getLabel().equals("f");
        assert ((CompoundWithIndex) u2).getIndex().equals("_i1");
        assert ((CompoundWithIndex) u2).getSubterms().size() == 1;
    }

    @Test
    public void testCollectIndices() {
        Term t = RewritingSystemParser.parseTerm("f#1(g(X, a#foo2()))");
        Map s = new HashMap();

        CompoundWithIndex.collectAllIndices(t, s);
        assert s.keySet().equals(new HashSet(["1", "foo2"]));
    }

    @Test
    public void testCollectIndices2() {
        Term t = RewritingSystemParser.parseTerm("f#1(g(X, *[a#foo2()]))");
        Map s = new HashMap();

        CompoundWithIndex.collectAllIndices(t, s);
        assert s.keySet().equals(new HashSet(["1", "foo2"]));
    }
}

