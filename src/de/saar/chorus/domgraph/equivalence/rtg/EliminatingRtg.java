package de.saar.chorus.domgraph.equivalence.rtg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.saar.chorus.domgraph.chart.RewritingRtg;
import de.saar.chorus.domgraph.chart.RtgFreeFragmentAnalyzer;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.equivalence.EquationSystem;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class EliminatingRtg extends RewritingRtg<String> {
	private static boolean DEBUG = false;
	
    protected EquationSystem eqs;
    private Map<String,List<Integer>> wildcardLabeledNodes;


	@Override
	protected String makeTopLevelNonterminal() {
		return null;
	}

	@Override
	protected Split<String> makeSplit(String previous, String root) {
		Split<String> ret = new Split<String>(root);
		
		for( String node : compact.getChildren(root, EdgeType.TREE )) {
			ret.addWcc(node, root);
		}
		
		return ret;
	}

	@Override
    protected boolean allowedSplit(String previousQuantifier, String currentRoot) {
    	if(DEBUG) System.err.print("(check split: " + currentRoot + " below " + previousQuantifier + ") ");
    	
    	// if there was no previous quantifier, all splits are allowed
        if( previousQuantifier == null ) {
            if(DEBUG)  System.err.print("[pq=null -> allowed] ");
            return true;
        }

        // If previousQuantifier < currentRoot, the split is allowed.
        // The order is the lexicographical order of (wildcard status, node name) where
        // wildcard status is 1 for wildcards and 2 for non-wildcards.
        int wildcardDiff = getWildcardStatus(previousQuantifier) - getWildcardStatus(currentRoot); 
        if( wildcardDiff < 0 ) {
        	if(DEBUG) System.err.println("[wc status -> allowed]");
        	return true;
        } else if( wildcardDiff == 0 ) {
        	if( previousQuantifier.compareTo(currentRoot) < 0 ) {
        		if(DEBUG) System.err.println("[pq smaller -> allowed]");
        		return true;
        	}
        }
        
        // Now check permutability. The quantifiers are permutable if they are co-free, and
        // either the eq system says they are permutable (with their connecting holes), or
        // the currentRoot is a wildcard.
        boolean permutable = isPermutable(previousQuantifier, currentRoot);
        if(DEBUG)  System.err.println("[perm: allowed=" + !permutable + "] ");
        return !permutable;
    }
    

    private boolean isPermutable(String u, String v) {
    	if( !analyzer.isCoFree(u, v) ) {
    		return false;
    	} else {
    		int vToU = indicesCompactToOriginal.get(v).get(analyzer.getReachability(v, u));
    		
    		if( wildcardLabeledNodes.containsKey(v) && wildcardLabeledNodes.get(v).contains(vToU)) {
    			return true;
    		} else {
    			return eqs.permutes(labels.getLabel(u), indicesCompactToOriginal.get(u).get(analyzer.getReachability(u, v)),
    					labels.getLabel(v), vToU);
    		}
    	}
    }


    
    
    
    
    
    public EliminatingRtg(DomGraph graph, NodeLabels labels, EquationSystem eqs, RtgFreeFragmentAnalyzer<?> analyzer) {
    	super(graph,labels,analyzer);
    	
    	this.eqs = eqs;
    	analyzeWildcards();
    }
    

	

    private void analyzeWildcards() {
        Set<String> roots = graph.getAllRoots();

        wildcardLabeledNodes = new HashMap<String,List<Integer>>();
        for( String node : roots ) {
            if( eqs.isWildcardLabel(labels.getLabel(node)) ) {
                List<Integer> holeIndices = new ArrayList<Integer>();
                wildcardLabeledNodes.put(node, holeIndices);

                for( int i = 0; i < compact.outdeg(node,EdgeType.TREE); i++ ) {
                    if( eqs.isWildcard(labels.getLabel(node), i) ) {
                        holeIndices.add(i);
                    }
                }
            }
        }
	}
    
    private int getWildcardStatus(String u) {
    	if( wildcardLabeledNodes.containsKey(u)) {
    		return 1;
    	} else {
    		return 2;
    	}
    }



}
