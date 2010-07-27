/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.chorus.domgraph.chart.lethal;

import de.saar.chorus.contexttransducer.PairState;
import de.saar.chorus.domgraph.chart.ConcreteRegularTreeGrammar;
import de.saar.chorus.domgraph.chart.DecoratedNonterminal;
import de.saar.chorus.domgraph.chart.RegularTreeGrammar;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.uni_muenster.cs.sev.lethal.factories.TreeFactory;
import de.uni_muenster.cs.sev.lethal.grammars.generic.GenRTG;
import de.uni_muenster.cs.sev.lethal.states.State;
import de.uni_muenster.cs.sev.lethal.symbol.common.BiSymbol;
import de.uni_muenster.cs.sev.lethal.symbol.common.RankedSymbol;
import de.uni_muenster.cs.sev.lethal.symbol.standard.InnerSymbol;
import de.uni_muenster.cs.sev.lethal.symbol.standard.LeafSymbol;
import de.uni_muenster.cs.sev.lethal.symbol.standard.StdNamedRankedSymbol;
import de.uni_muenster.cs.sev.lethal.tree.common.Tree;
import de.uni_muenster.cs.sev.lethal.treeautomata.easy.EasyFTA;
import de.uni_muenster.cs.sev.lethal.treeautomata.easy.EasyFTAOps.StdStateBuilder;
import de.uni_muenster.cs.sev.lethal.treeautomata.generic.GenFTA;
import de.uni_muenster.cs.sev.lethal.treeautomata.generic.GenFTARule;
import de.uni_muenster.cs.sev.lethal.utils.StateBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author koller
 */
public class ChartToLethal {

    private static final StdStateBuilder<String> ssb = new StdStateBuilder<String>();
    private static TreeFactory tf = TreeFactory.getTreeFactory();
    private static Pattern nodeNameExtractorPattern = Pattern.compile(".*_([^_]+)");
    private static final String CHART_ROOT_DECORATION = "root";

    public static GenRTG<RankedSymbol, State> convertToRtg(RegularTreeGrammar<SubgraphNonterminal> chart, DomGraph graph, NodeLabels labels) {
        if (chart.getToplevelSubgraphs().size() != 1) {
            throw new UnsupportedOperationException("Cannot convert chart with multiple start symbols!");
        }

        // initialize RTG with start symbol
        List<State> startStates = new ArrayList<State>();
        startStates.add(makeState(chart.getToplevelSubgraphs().get(0).getNodes(), graph));
        GenRTG<RankedSymbol, State> ret = new GenRTG<RankedSymbol, State>(startStates);

        // now add rules to RTG
        for (SubgraphNonterminal nt : chart.getAllNonterminals()) {
            State ntState = makeState(nt.getNodes(), graph);

            for (Split<SubgraphNonterminal> split : chart.getSplitsFor(nt)) {
                ret.addRule(ntState, convertSplit(split.getRootFragment(), split, graph, labels));
            }
        }

        return ret;

    }

    public static EasyFTA convertToFta(RegularTreeGrammar<SubgraphNonterminal> chart, DomGraph graph, NodeLabels labels) {
        GenRTG<RankedSymbol, State> ret = convertToRtg(chart, graph, labels);
        return new EasyFTA(ret, new TrivialConverter());
    }

    public static RegularTreeGrammar<DecoratedNonterminal<SubgraphNonterminal, String>> convertFtaToChart(GenFTA<RankedSymbol, PairState<State, String>> fta, DomGraph graph) {
        ConcreteRegularTreeGrammar<DecoratedNonterminal<SubgraphNonterminal, String>> ret = new ConcreteRegularTreeGrammar<DecoratedNonterminal<SubgraphNonterminal, String>>();
        Map<PairState<State, String>, List<DecoratedNonterminal<SubgraphNonterminal, String>>> statesToNontermLists = new HashMap<PairState<State, String>, List<DecoratedNonterminal<SubgraphNonterminal, String>>>();
        Map<PairState<State, String>, List<String>> nodesInFragment = new HashMap<PairState<State, String>, List<String>>();
        final List<GenFTARule<RankedSymbol, PairState<State, String>>> rules = fta.getRulesInBottomUpOrder();

        Set<String> allNodesExceptHoles = new HashSet<String>(graph.getAllNodes());
        for( String node : graph.getAllNodes() ) {
            if( graph.isHole(node)) {
                allNodesExceptHoles.remove(node);
            }
        }

        for (GenFTARule<RankedSymbol, PairState<State, String>> rule : rules) {
            String node = extractNode(rule.getSymbol());

            if (graph.isRoot(node)) {
                Split<DecoratedNonterminal<SubgraphNonterminal, String>> split = new Split<DecoratedNonterminal<SubgraphNonterminal, String>>(node);
                List<String> holes = graph.getHoles(node);
                List<DecoratedNonterminal<SubgraphNonterminal, String>> nontermsForHoles = new ArrayList<DecoratedNonterminal<SubgraphNonterminal, String>>();
                Set<String> subgraph = new HashSet<String>();

                subgraph.add(node);

                for (PairState<State, String> childState : rule.getSrcStates()) {
                    nontermsForHoles.addAll(statesToNontermLists.get(childState));

                    subgraph.addAll(nodesInFragment.get(childState));

                    for( DecoratedNonterminal<SubgraphNonterminal,String> nt : statesToNontermLists.get(childState)) {
                        subgraph.addAll(nt.getNodes());
                    }
                }

                assert holes.size() == nontermsForHoles.size();

                for (int i = 0; i < holes.size(); i++) {
                    split.addWcc(holes.get(i), nontermsForHoles.get(i));
                }

                String decoration = (subgraph.containsAll(allNodesExceptHoles)) ? CHART_ROOT_DECORATION : rule.getDestState().getSecond();
                DecoratedNonterminal<SubgraphNonterminal, String> nt = new DecoratedNonterminal<SubgraphNonterminal, String>(new SubgraphNonterminal(subgraph), decoration);
                ret.addSplit(nt, split);

                List<DecoratedNonterminal<SubgraphNonterminal, String>> ntt = new ArrayList<DecoratedNonterminal<SubgraphNonterminal, String>>();
                ntt.add(nt);
                statesToNontermLists.put(rule.getDestState(), ntt);
                nodesInFragment.put(rule.getDestState(), new ArrayList<String>());

                // NB: holes are never added to fragments. Perhaps this is not a problem.
            } else {
                List<DecoratedNonterminal<SubgraphNonterminal, String>> nonterminals = new ArrayList<DecoratedNonterminal<SubgraphNonterminal, String>>();
                List<String> nodesHere = new ArrayList<String>();

                // this code only works because the fta is bottom-up deterministic inside fragments

                nodesHere.add(node);

                for (PairState<State, String> childState : rule.getSrcStates()) {
                    nonterminals.addAll(statesToNontermLists.get(childState));
                    nodesHere.addAll(nodesInFragment.get(childState));
                }

                statesToNontermLists.put(rule.getDestState(), nonterminals);
                nodesInFragment.put(rule.getDestState(), nodesHere);
            }
        }

        Set<String> toplevelSubgraph = new HashSet<String>(graph.getAllNodes());
        for (String node : graph.getAllNodes()) {
            if (graph.isHole(node)) {
                toplevelSubgraph.remove(node);
            }
        }
        ret.addToplevelSubgraph(new DecoratedNonterminal<SubgraphNonterminal, String>(new SubgraphNonterminal(toplevelSubgraph), CHART_ROOT_DECORATION));

        return ret;
    }

    private static Tree<BiSymbol<RankedSymbol, State>> convertSplit(String node, Split<SubgraphNonterminal> split, DomGraph graph, NodeLabels labels) {
        List<SubgraphNonterminal> wccs = split.getWccs(node);

        if (wccs != null) {
            // node is a hole
            if (wccs.size() != 1) {
                throw new UnsupportedOperationException("Encountered hole with too many or too few wccs: " + node);
            } else {
                BiSymbol<RankedSymbol, State> s = new LeafSymbol<RankedSymbol, State>(makeState(wccs.get(0).getNodes(), graph));
                return tf.makeTreeFromSymbol(s);
            }
        } else {
            // inner node
            List<String> children = graph.getChildren(node, EdgeType.TREE);
            List<Tree<BiSymbol<RankedSymbol, State>>> childTrees = new ArrayList<Tree<BiSymbol<RankedSymbol, State>>>();
            BiSymbol<RankedSymbol, State> sym = new InnerSymbol<RankedSymbol, State>(new StdNamedRankedSymbol<String>(labels.getLabel(node) + "_" + node, children.size()));

            for (String ch : children) {
                childTrees.add(convertSplit(ch, split, graph, labels));
            }

            return tf.makeTreeFromSymbol(sym, childTrees);
        }
    }

    private static State makeState(Set<String> subgraph, DomGraph graph) {
        StringBuffer ret = new StringBuffer("q");
        List<String> sortedSubgraphs = new ArrayList<String>(subgraph);
        Collections.sort(sortedSubgraphs);

        for (String s : sortedSubgraphs) {
            if (graph.isRoot(s)) {
                ret.append("_" + s);
            }
        }

        return ssb.convert(ret.toString());
    }

    private static String extractNode(RankedSymbol symbol) {
        Matcher m = nodeNameExtractorPattern.matcher(symbol.toString());

        if (!m.matches()) {
            throw new RuntimeException("Couldn't extract node from symbol " + symbol);
        } else {
            return m.group(1);
        }
    }

    private static class TrivialConverter extends StateBuilder<Object> {

        @Override
        public State convert(Object a) {
            if (a instanceof State) {
                return (State) a;
            } else {
                return super.convert("qq_" + a.toString());
//                throw new RuntimeException("This should never happen: " + a);
            }
        }
    }
}
