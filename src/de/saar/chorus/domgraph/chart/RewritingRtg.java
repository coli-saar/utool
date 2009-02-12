package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.saar.chorus.domgraph.graph.CompactificationRecord;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;

abstract public class RewritingRtg<E> extends RegularTreeGrammar<E> {
	protected RtgFreeFragmentAnalyzer<?> analyzer;
	
    protected DomGraph graph; // original graph
    protected DomGraph compact; // compact version of the graph
    protected NodeLabels labels;


    // The compactification deletes labelled leaves, so there may be a discrepancy
    // between the index of a hole in the compact graph and the (left-to-right dfs)
    // index of an unlabelled leaf in the original graph. This map here maps
    // the indices in the compact graph to the indices in the original graph.
    protected final Map<String,Map<Integer,Integer>> indicesCompactToOriginal;

    private int currentHoleIdx;
    private int currentLeafIdx;
    
    
    @Override
    public List<E> getToplevelSubgraphs() {
    	List<E> ret = new ArrayList<E>();
    	ret.add(makeTopLevelNonterminal());
    	return ret;
    }
	
	@Override
	public List<Split<E>> getSplitsFor(E previousQuantifier, String currentRoot) {
		List<Split<E>> ret = new ArrayList<Split<E>>();
		
		if( allowedSplit(previousQuantifier, currentRoot) ) {
			ret.add(makeSplit(previousQuantifier, currentRoot));
		}
		
		return ret;
	}
	
	abstract protected Split<E> makeSplit(E previous, String root);
    abstract protected boolean allowedSplit(E previousQuantifier, String currentRoot);
	abstract protected E makeTopLevelNonterminal();

    

/*

	@Override
	public <F extends GraphBasedNonterminal> void prepareForIntersection(RegularTreeGrammar<F> other) {
		analyzer = new RtgFreeFragmentAnalyzer<F>(other);
		analyzer.analyze();
	}
	*/

	
	// because this grammar is only ever used as a filter grammar, the following RTG methods
	// are never called

	@Override
	public boolean containsSplitFor(E subgraph) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Set<E> getAllNonterminals() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRootForSingleton(E nt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Split<E>> getSplitsFor(E subgraph) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean isSingleton(E nt) {
		// TODO Auto-generated method stub
		return false;
	}
    
    
    
    
    
    
    
    
    
    public RewritingRtg(DomGraph graph, NodeLabels labels, RtgFreeFragmentAnalyzer<?> analyzer) {
    	this.graph = graph;
        this.labels = labels;
        this.analyzer = analyzer;
        
        compact = graph.compactify(new CompactificationRecord());

        indicesCompactToOriginal = new HashMap<String,Map<Integer,Integer>>();
        computeIndexTable();
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
