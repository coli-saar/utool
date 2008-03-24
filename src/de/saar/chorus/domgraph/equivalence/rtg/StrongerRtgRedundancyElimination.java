package de.saar.chorus.domgraph.equivalence.rtg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.RegularTreeGrammar;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.rtgparser.StringNonterminal;
import de.saar.chorus.domgraph.equivalence.EquationSystem;
import de.saar.chorus.domgraph.equivalence.RedundancyElimination;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class StrongerRtgRedundancyElimination<E> extends RedundancyElimination<E> {
	public StrongerRtgRedundancyElimination(DomGraph graph, NodeLabels labels, EquationSystem eqs) {
		super(graph, labels, eqs);
	}



    public RegularTreeGrammar eliminateStrong(Chart chart) {
		RegularTreeGrammar filterGrammar = null;
		List<String> quants = new ArrayList<String>();
		List<String> leaves = new ArrayList<String>();

		for( String root : compact.getAllRoots() ) {
		    if( compact.outdeg(root, EdgeType.TREE) > 0 ) {
		        quants.add(root);
		    } else {
		        leaves.add(root);
		    }
		}

		for( String quant : quants ) {
		    filterGrammar = intersectFilterGrammar(filterGrammar, quant, quants, leaves);
		    //filterGrammar = intersectWildcardFilterGrammar(filterGrammar, quant, quants, leaves);
		}

		System.err.println("filter grammar:" + filterGrammar);

		return chart.relabel(makeSelfLabels(graph)).intersect(filterGrammar);
	}

	private int getArity(String node) {
	    return compact.outdeg(node, EdgeType.TREE);
	}

	@SuppressWarnings("unchecked")
    private RegularTreeGrammar intersectWildcardFilterGrammar(RegularTreeGrammar filterGrammar, String quant, List<String> quants, List<String> leaves) {
	    RegularTreeGrammar ret = filterGrammar;
	    List<String> largerWildcardNodes = new ArrayList<String>();

	    for( String q : quants ) {

	    }


	    for( int hole : eqs.getIndicesForLabel(labels.getLabel(quant))) {
	        if( eqs.isWildcard(labels.getLabel(quant), hole)) {
	            RegularTreeGrammar<StringNonterminal> filter = new RegularTreeGrammar<StringNonterminal>();

	            StringNonterminal defaultState = new StringNonterminal("q_" + quant + "_0"),
                markedState = new StringNonterminal("q_" + quant + "_1");

	            // top-down final states
	            ret.addToplevelSubgraph(defaultState);

	            // terminal productions
	            ret.setFinal(defaultState);
	            ret.setFinal(markedState);
	            for( String leaf : leaves ) {
	                ret.addSplit(defaultState, new Split<StringNonterminal>(leaf));
	                ret.addSplit(markedState, new Split<StringNonterminal>(leaf));
	            }

	            for( String upperQuant : quants ) {
	                if( upperQuant.equals(quant)) {
	                    // rules for wildcard: fail if in marked state
	                    ret.addSplit(defaultState, makeSplit(quant, -1, defaultState, markedState));
	                } else if( upperQuant.compareTo(quant) > 0 ) {
	                    // smaller quantifier: change into marked state
	                    ret.addSplit(defaultState, makeSplit(upperQuant, -1, markedState, markedState));
	                    ret.addSplit(markedState, makeSplit(upperQuant, -1, markedState, markedState));
	                } else {
	                    // all other quantifiers: pass on markedness
	                    ret.addSplit(defaultState, makeSplit(upperQuant, -1, defaultState, defaultState));
                        ret.addSplit(markedState, makeSplit(upperQuant, -1, markedState, markedState));
	                }
	            }

	            if( ret == null ) {
	                ret = filter;
	            } else {
	                ret = ret.intersect(filter);
	            }
	        }
	    }

	    return ret;
	}



	private Split<StringNonterminal> makeSplit(String node, int markedHole, StringNonterminal defaultState, StringNonterminal markedState) {
	    int arity = getArity(node);
	    Split<StringNonterminal> ret = new Split<StringNonterminal>(node);

	    for( int i = 0; i < arity; i++ ) {
	        ret.addWcc(new Integer(i).toString(), i == markedHole ? markedState : defaultState);
	    }

	    return ret;
    }


    @SuppressWarnings("unchecked")
    private RegularTreeGrammar intersectFilterGrammar(RegularTreeGrammar filterGrammar, String quant, List<String> quants, List<String> leaves) {
	    RegularTreeGrammar ret = new RegularTreeGrammar<StringNonterminal>();
	    String label = labels.getLabel(quant);
	    StringNonterminal defaultState = new StringNonterminal("q_" + quant + "_0"),
	                      markedState = new StringNonterminal("q_" + quant + "_1");
	    Collection<String> permutingLabels = new HashSet<String>();

	    // top-down final states
	    ret.addToplevelSubgraph(defaultState);

	    // terminal productions
	    ret.setFinal(defaultState);
	    ret.setFinal(markedState);
	    for( String leaf : leaves ) {
	        ret.addSplit(defaultState, new Split<StringNonterminal>(leaf));
	        ret.addSplit(markedState, new Split<StringNonterminal>(leaf));
	    }

        for( int hole : eqs.getIndicesForLabel(label)) {
            // switch to marked state
            ret.addSplit(defaultState, makeSplit(quant, hole, defaultState, markedState));
            ret.addSplit(markedState, makeSplit(quant, hole, defaultState, markedState));

            // TODO - this is not strictly speaking correct
            permutingLabels.addAll(eqs.getPermutingLabels(label, hole));
        }


	    for( String thisQuant : quants ) {
	        if( thisQuant.compareTo(quant) < 0 && permutingLabels.contains(labels.getLabel(thisQuant)) ) {
	            ret.addSplit(defaultState, makeSplit(thisQuant, -1, defaultState, defaultState));
	        } else if( !thisQuant.equals(quant)){
	            ret.addSplit(markedState, makeSplit(thisQuant, -1, markedState, markedState));
	            ret.addSplit(defaultState, makeSplit(thisQuant, -1, defaultState, defaultState));
	        }
	    }

	    // intersect with old grammar
	    if( filterGrammar == null ) {
	        return ret;
	    } else {
	        return filterGrammar.intersect(ret);
	    }
    }


    private NodeLabels makeSelfLabels(DomGraph graph) {
		NodeLabels ret = new NodeLabels();

		for( String node : graph.getAllNodes() ) {
			ret.addLabel(node, node);
		}

		return ret;
	}


	@Override
	public List<Split<E>> getIrredundantSplits(E subgraph, List<Split<E>> allSplits) {
		// TODO Auto-generated method stub
		return null;
	}

}
