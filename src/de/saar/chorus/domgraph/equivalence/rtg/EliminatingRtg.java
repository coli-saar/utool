package de.saar.chorus.domgraph.equivalence.rtg;

import java.util.List;

import de.saar.chorus.domgraph.chart.RewritingRtg;
import de.saar.chorus.domgraph.chart.RtgFreeFragmentAnalyzer;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.equivalence.EquationSystem;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.CompactificationRecord.NodeChildPair;

public class EliminatingRtg extends RewritingRtg<String> {
	private static boolean DEBUG = false;
	
    protected EquationSystem eqs;

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
    		int uToV = analyzer.getReachability(u, v);
    		int vToU = analyzer.getReachability(v, u);
    		
    		List<NodeChildPair> pathInU = compactificationRecord.getRecord(u, uToV);
    		List<NodeChildPair> pathInV = compactificationRecord.getRecord(v, vToU);
    		
    		for( NodeChildPair ncpInU : pathInU ) {
    			for( NodeChildPair ncpInV : pathInV ) {
    				if( !eqs.permutes(labels.getLabel(ncpInU.node), ncpInU.childIndex, labels.getLabel(ncpInV.node), ncpInV.childIndex) ) {
    					return false;
    				}	
    			}
    		}

    		return true;
    	}
    }

    public EliminatingRtg(DomGraph graph, NodeLabels labels, EquationSystem eqs, RtgFreeFragmentAnalyzer<?> analyzer) {
    	super(graph,labels,analyzer);
    	
    	this.eqs = eqs;
    }
    
    /**
     * @param u a node name
     * @return
     */
    private int getWildcardStatus(String u) {
    	if( eqs.isWildcardLabel(labels.getLabel(u)) ) {
    		return 1;
    	} else {
    		return 2;
    	}
    }



}
