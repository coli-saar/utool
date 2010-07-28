package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.domgraph.graph.DomGraph;

/**
 * An iterator over the different solved forms represented by
 * a {@link Chart}. The chart is passed to the constructor of
 * an object of this class. Then you can iterate over the solved
 * forms of this chart using <code>hasNext()</code> and <code>next()</code>
 * as usual.<p>
 *
 * Each successful call to <code>next()</code> will return an object
 * of class <code>List<{@link DomEdge}></code>, i.e. a list of
 * dominance edge representations. This list can e.g. be passed
 * to the <code>encode</code> method of {@link de.saar.chorus.domgraph.codec.OutputCodec} or
 * one of its subclasses.<p>
 *
 * This class implements a transition system for states consisting
 * of an agenda of subgraphs that must currently be resolved, and
 * a stack of splits that still need to be processed. This algorithm
 * is dramatically faster than a naive algorithm which simply computes
 * the sets of solved forms of a graph by computing the Cartesian
 * product of the sets of solved forms of its subgraphs, but quite
 * a bit more complicated.
 *
 * @author Alexander Koller
 * @author Michaela Regneri
 *
 */
public class SolvedFormIterator<E extends GraphBasedNonterminal> implements Iterator<SolvedFormSpec> {

    private RegularTreeGrammar<E> chart;
    private Agenda agenda;
    private Stack<EnumerationStackEntry> stack;
    private Set<String> roots;
    private String rootForThisFragset;
    // the solved form which will be returned by the next call
    // to next()
    private SolvedFormSpec nextSolvedForm;
    // a cached list of solved forms for get(int)
    private List<SolvedFormSpec> solvedForms;
    // the iterator used for computing the solved forms
    private SolvedFormIterator<E> iteratorForGet;
    private boolean chartIsEmpty, returnedSfForEmptyChart;

    // I need the graph in order to determine the fragments: I need to
    // know the roots of singleton fragsets to create the dom edge.
    public SolvedFormIterator(RegularTreeGrammar<E> ch, DomGraph graph) {
        this(ch, graph, true);
    }

    private SolvedFormIterator(RegularTreeGrammar<E> ch, DomGraph graph, boolean makeIteratorForGet) {
        chart = ch;
        agenda = new Agenda();
        stack = new Stack<EnumerationStackEntry>();
        solvedForms = new ArrayList<SolvedFormSpec>();

        if (chart.getToplevelSubgraphs().isEmpty()) {
            // If the chart has no top-level subgraph, we will generate a single (empty)
            // solved form.  (If such a chart comes from a solvable graph, the graph must
            // have been empty.)
            chartIsEmpty = true;
            returnedSfForEmptyChart = false;
        } else {

            if (makeIteratorForGet) {
                iteratorForGet = new SolvedFormIterator<E>(ch, graph, false);
            } else {
                iteratorForGet = null;
            }

            roots = graph.getAllRoots();


            for (E fragset : chart.getToplevelSubgraphs()) {
                //if( (fragset.size() > 0) ) { // TODO - must I??
                agenda.add(new AgendaEntry(null, fragset));
                //}
            }

            //Null-Element on Stack
            stack.push(new EnumerationStackEntry(null, new ArrayList<Split<E>>(), null));

            updateNextSolvedForm();
        }
    }

    private void updateNextSolvedForm() {
        if (isFinished()) {
            nextSolvedForm = null;
        } else {
            findNextSolvedForm();

            if (representsSolvedForm()) {
                nextSolvedForm = extractSolvedFormSpec();
            } else {
                nextSolvedForm = null;
            }
        }
    }

    private boolean representsSolvedForm() {
        return (agenda.isEmpty() && stack.size() > 0);
    }

    private boolean isFinished() {
        return (agenda.isEmpty() && stack.isEmpty());
    }

    private SolvedFormSpec extractSolvedFormSpec() {
        SolvedFormSpec toReturn = new SolvedFormSpec();

        for (EnumerationStackEntry ese : stack) {
            toReturn.addAllDomEdges(ese.getEdgeAccu());

            if (ese.getCurrentSplit() != null) {
                toReturn.addSubstitution(ese.getCurrentSplit().getSubstitution());
            }
        }

        return toReturn;
    }

    private void findNextSolvedForm() {
        if (!isFinished()) {
            do {
                step();
            } while (!agenda.isEmpty());

            if (isFinished()) {
                agenda.clear();
            }
        }
    }

    private void addSplitToAgendaAndAccu(EnumerationStackEntry ese) {
        Split<E> split = ese.getCurrentSplit();

        // iterate over all dominators
        for (String node : split.getAllDominators()) {
            List<E> wccs = split.getWccs(node);
            for (int i = 0; i < wccs.size(); i++) {
                E wcc = wccs.get(i);
                addFragsetToAgendaAndAccu(wcc, node, ese);
            }
        }

    }

    private void addFragsetToAgendaAndAccu(E fragSet, String dominator, EnumerationStackEntry ese) {
        //if( fragSet.isSingleton(roots) ) {
        if (chart.isSingleton(fragSet)) {
            // singleton fragsets: add directly to ese's domedge list
            //DomEdge newEdge = new DomEdge(dominator, fragSet.getRootIfSingleton());
            DomEdge newEdge = new DomEdge(dominator, chart.getRootForSingleton(fragSet));
            ese.addDomEdge(newEdge);
        } else {
            // larger fragsets: add to agenda
            AgendaEntry newEntry = new AgendaEntry(dominator, fragSet);
            agenda.add(newEntry);
        }
    }

    private void step() {
        EnumerationStackEntry top = stack.peek();
        AgendaEntry agTop;
        E topFragset;
        String topNode;

        // 1. Apply (Up) as long as possible
        if (agenda.isEmpty()) {
            while (top.isAtLastSplit()) {
                stack.pop();
                if (stack.isEmpty()) {
                    return;
                } else {
                    top = stack.peek();
                }
            }
        }

        // 2. Apply (Step) or (Down) as appropriate.
        // Singleton fragments are put directly into the accu, rather
        // than the agenda (i.e. simulation of Singleton).
        if (agenda.isEmpty()) {
            // (Step)
            top.clearAccu();
            top.nextSplit();

            if (top.getDominator() != null) {
                DomEdge newEdge =
                        new DomEdge(top.getDominator(), top.getCurrentSplit().getRootFragment());
                top.addDomEdge(newEdge);
            }

            if (!top.getAgendaCopy().isEmpty()) {
                agenda.addAll(top.getAgendaCopy());
            }

            addSplitToAgendaAndAccu(top);
        } else {
            // (Down)
            agTop = agenda.pop();
            topNode = agTop.getDominator();
            topFragset = agTop.getFragmentSet();

            //if( !topFragset.isSingleton(roots) ) {
            if (!chart.isSingleton(topFragset)) {
                // if topFragset is a singleton, then it was a wcc of the entire graph
                // that only contained a single fragment; hence we don't need to do anything here
                List<Split<E>> sv = chart.getSplitsFor(topFragset);

                EnumerationStackEntry newTop =
                        new EnumerationStackEntry(topNode, sv, agenda);

                if (topNode != null) {
                    DomEdge newEdge =
                            new DomEdge(topNode, newTop.getCurrentSplit().getRootFragment());
                    newTop.addDomEdge(newEdge);
                }

                stack.push(newTop);
                addSplitToAgendaAndAccu(newTop);
            }
        }
    }

    /**** convenience methods for implementing Iterator ****/
    public boolean hasNext() {
        if (chartIsEmpty) {
            return !returnedSfForEmptyChart;
        } else {
            return nextSolvedForm != null;
        }
    }

    public SolvedFormSpec next() {
        if (chartIsEmpty) {
            if (returnedSfForEmptyChart) {
                return null;
            } else {
                returnedSfForEmptyChart = true;
                return new SolvedFormSpec();
            }
        }

        SolvedFormSpec ret = nextSolvedForm;

        if (ret != null) {
            updateNextSolvedForm();
            return ret;
        } else {
            return null;
        }
    }

    /**
     * This returns a solved form represented by a List of <code>DomEdge</code>
     * objects. The form is accessed via its index.
     * Forms with indices exceeding the range of <code>int</code> have to be
     * extracted manually by calling <code>next()</code> as often as necessary.
     *
     * @param sf index of the solved form to extract
     * @return the solved form
     */
    public SolvedFormSpec getSolvedForm(int sf) {
        for (int i = solvedForms.size(); i <= sf; i++) {
            if (!iteratorForGet.hasNext()) {
                return null;
            } else {
                solvedForms.add(iteratorForGet.next());
            }
        }

        return solvedForms.get(sf);
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public RegularTreeGrammar<E> getChart() {
        return chart;
    }

    /**** classes for the agenda and the enumeration stack ****/
    private class EnumerationStackEntry {

        private final String dominator;
        private final List<DomEdge> edgeAccu;
        private Split currentSplit;
        private final List<Split<E>> splits; // points into chart
        private Split lastElement;
        private final Agenda agendaCopy;
        private Iterator<Split<E>> splitIterator;

        EnumerationStackEntry(String dom, List<Split<E>> spl, Agenda agenda) {
            dominator = dom;
            splits = spl;
            agendaCopy = new Agenda();

            if (agenda != null) {
                agendaCopy.addAll(agenda);
            }

            if ((spl != null) && (!spl.isEmpty())) {

                splitIterator = splits.iterator();
                currentSplit = splitIterator.next();

                lastElement = splits.get(splits.size() - 1);

            }

            edgeAccu = new ArrayList<DomEdge>();
        }

        public void nextSplit() {
            currentSplit = splitIterator.next();
        }

        public boolean isAtLastSplit() {
            if (splitIterator == null) {
                return true;
            }
            return ((!splitIterator.hasNext()) || currentSplit.equals(lastElement));
        }

        public void addDomEdge(DomEdge edge) {
            edgeAccu.add(edge);
        }

        public void clearAccu() {
            edgeAccu.clear();
        }

        /**
         * @return Returns the dominator.
         */
        public String getDominator() {
            return dominator;
        }

        /**
         * @return Returns the edgeAccu.
         */
        public List<DomEdge> getEdgeAccu() {
            return edgeAccu;
        }

        /**
         * @return Returns the currentSplit.
         */
        public Split getCurrentSplit() {
            return currentSplit;
        }

        /**
         * @return Returns the agendaCopy.
         */
        public Agenda getAgendaCopy() {
            return agendaCopy;
        }

        @Override
        public String toString() {
            StringBuilder ret = new StringBuilder();

            ret.append("<ESE dom=" + dominator + ", accu=" + edgeAccu);
            ret.append(", agendacopy=" + agendaCopy + ", splits=");
            for (Split split : splits) {
                if (split == currentSplit) {
                    ret.append(split.toString().toUpperCase());
                } else {
                    ret.append(split);
                }
                ret.append(",");
            }

            return ret.toString();
        }
    }

    private class AgendaEntry {

        String dominator;
        E fragmentSet;

        AgendaEntry(String source, E target) {
            dominator = source;
            fragmentSet = target;
        }

        public String getDominator() {
            return dominator;
        }

        public E getFragmentSet() {
            return fragmentSet;
        }

        @Override
        public String toString() {
            return "<Ag dom=" + dominator + ", fs=" + fragmentSet + ">";
        }
    }

    private class Agenda extends Stack<AgendaEntry> {

        /**
         *
         */
        private static final long serialVersionUID = 7426236767126350134L;
    }
}
