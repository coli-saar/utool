package de.saar.chorus.domgraph.equivalence.rtg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.saar.chorus.domgraph.chart.GraphBasedNonterminal;
import de.saar.chorus.domgraph.chart.RegularTreeGrammar;
import de.saar.chorus.domgraph.chart.RtgFreeFragmentAnalyzer;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.equivalence.Equation;
import de.saar.chorus.domgraph.equivalence.EquationSystem;
import de.saar.chorus.domgraph.equivalence.FragmentWithHole;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;

public class EliminatingRtg extends RegularTreeGrammar<String> {
	private static final boolean DEBUG = true;
	
	private RtgFreeFragmentAnalyzer<?> analyzer;
	
    protected DomGraph graph; // original graph
    protected DomGraph compact; // compact version of the graph
    protected NodeLabels labels;
    protected EquationSystem eqs;
    
    private Map<String,List<Integer>> wildcardLabeledNodes;


    // The compactification deletes labelled leaves, so there may be a discrepancy
    // between the index of a hole in the compact graph and the (left-to-right dfs)
    // index of an unlabelled leaf in the original graph. This map here maps
    // the indices in the compact graph to the indices in the original graph.
    private final Map<String,Map<Integer,Integer>> indicesCompactToOriginal;

    private int currentHoleIdx;
    private int currentLeafIdx;
    
    
    

	@Override
	public List<String> getToplevelSubgraphs() {
		List<String> ret = new ArrayList<String>();
		ret.add(null);
		return ret;
	}
	
	@Override
	public List<Split<String>> getSplitsFor(String previousQuantifier, String currentRoot) {
		List<Split<String>> ret = new ArrayList<Split<String>>();
		
		if( allowedSplit(previousQuantifier, currentRoot) ) {
			ret.add(makeSplit(currentRoot));
		}
		
		return ret;
	}
	
	private Split<String> makeSplit(String root) {
		Split<String> ret = new Split<String>(root);
		
		for( String node : compact.getChildren(root, EdgeType.TREE )) {
			ret.addWcc(node, root);
		}
		
		return ret;
	}
	

    private boolean allowedSplit(String previousQuantifier, String currentRoot) {
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
    			FragmentWithHole f1 = new FragmentWithHole(labels.getLabel(u), indicesCompactToOriginal.get(u).get(analyzer.getReachability(u, v)));
    			FragmentWithHole f2 = new FragmentWithHole(labels.getLabel(v), vToU);

    			return eqs.contains(new Equation(f1,f2));
    		}
    	}
    }


	@Override
	public <F extends GraphBasedNonterminal> void prepareForIntersection(RegularTreeGrammar<F> other) {
		analyzer = new RtgFreeFragmentAnalyzer<F>(other);
		analyzer.analyze();
	}

	
	// because this grammar is only ever used as a filter grammar, the following RTG methods
	// are never called

	@Override
	public boolean containsSplitFor(String subgraph) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Set<String> getAllNonterminals() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRootForSingleton(String nt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Split<String>> getSplitsFor(String subgraph) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean isSingleton(String nt) {
		// TODO Auto-generated method stub
		return false;
	}
    
    
    
    
    
    
    
    
    
    public EliminatingRtg(DomGraph graph, NodeLabels labels, EquationSystem eqs) {
    	analyzer = null; 
    	
    	this.graph = graph;
        this.labels = labels;
        this.eqs = eqs;
        compact = graph.compactify();

        indicesCompactToOriginal = new HashMap<String,Map<Integer,Integer>>();
        computeIndexTable();
        
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

	/*
     * computation of the mapping from holes (in the compact graph)
     * to the children of the root (in the original graph).
     */
    private void computeIndexTable() {
        for( String root : graph.getAllRoots() ) {
            currentLeafIdx = 0;
            currentHoleIdx = 0;
            indicesCompactToOriginal.put(root, new HashMap<Integer,Integer>());

            for( String child : graph.getChildren(root, EdgeType.TREE) ) {
                indexTableDfs(root, child);
            }
        }
    }

    private void indexTableDfs(String root, String node) {
        // found an unlabelled leaf => enter it into the map
        if( graph.getData(node).getType() == NodeType.UNLABELLED ) {
            Map<Integer,Integer> thisHolesToChildren = indicesCompactToOriginal.get(root);
            thisHolesToChildren.put(currentHoleIdx++, currentLeafIdx);
        }

        List<String> children = graph.getChildren(node, EdgeType.TREE);

        if( children.isEmpty() ) {
            // if this was an (unlabelled or labelled) leaf, increase the
            // leaf counter
            currentLeafIdx++;
        } else {
            // otherwise, recurse into subtrees
            for( String child : children ) {
                indexTableDfs(root, child);
            }
        }
    }


}
