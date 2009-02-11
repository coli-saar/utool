package de.saar.chorus.domgraph.equivalence.rtg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org._3pq.jgrapht.util.ModifiableInteger;

import de.saar.chorus.domgraph.chart.GraphBasedNonterminal;
import de.saar.chorus.domgraph.chart.RegularTreeGrammar;
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
	
	private final Set<String> roots;
    private final Map<String,List<Integer>> wildcardLabeledNodes;
    
    protected DomGraph graph; // original graph
    protected DomGraph compact; // compact version of the graph
    protected NodeLabels labels;
    protected EquationSystem eqs;

    /*
     * The meanings of the special values in the hypernormalReachability map:
     */
    // first node = second node
    private static final Integer HNR_EQUAL = new Integer(-1);

    // no hn path through any of the holes of the first node
    private static final Integer HNR_NO_CONNECTION = new Integer(-2);

    // more than one hole is connected by a hn path
    private static final Integer HNR_TWO_CONNECTIONS = new Integer(-3);



    // This table maps a pair (u,v) of nodes in the compact graph to the index of the
    // child of u which is connected to v by a simple hypernormal path that doesn't use
    // u. Note that the index of the _child_ of u (in the original graph!) need not be
    // identical to the index of the _hole_ of u (in the compact graph!) that the hn.
    // path uses. It is important that we talk about children and not holes because
    // the permutability rules are phrased that way.
    protected final Map<String,Map<String,Integer>> hypernormalReachability;

    // The compactification deletes labelled leaves, so there may be a discrepancy
    // between the index of a hole in the compact graph and the (left-to-right dfs)
    // index of an unlabelled leaf in the original graph. This map here maps
    // the indices in the compact graph to the indices in the original graph.
    private final Map<String,Map<Integer,Integer>> indicesCompactToOriginal;

    private final Map<String,Map<String,ModifiableInteger>> numHolesToOtherRoot;

    // protected Map<String,Set<String>> possibleDominators;
    protected Map<String,Set<String>> oneWayDominator; //  a -> [...b...]: hnc path from a to root of b

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


	@Override
	public <F extends GraphBasedNonterminal> void prepareForIntersection(RegularTreeGrammar<F> other) {
		// TODO Auto-generated method stub
		
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
    	this.graph = graph;
        this.labels = labels;
        this.eqs = eqs;
        compact = graph.compactify();

        hypernormalReachability = new HashMap<String,Map<String,Integer>>();
        indicesCompactToOriginal = new HashMap<String,Map<Integer,Integer>>();

        numHolesToOtherRoot = new HashMap<String,Map<String,ModifiableInteger>>();
        for( String r1 : compact.getAllRoots() ) {
            Map<String,ModifiableInteger> thisMap = new HashMap<String,ModifiableInteger>();
            numHolesToOtherRoot.put(r1, thisMap);

            for( String r2 : compact.getAllRoots() ) {
                if( !r1.equals(r2)) {
                    thisMap.put(r2, new ModifiableInteger(0));
                }
            }
        }

        computeIndexTable();
        computeHypernormalReachability();

        /*
        System.err.println("\nHypernormal reachability table:");
        System.err.println(hypernormalReachability + "\n");
        */

        oneWayDominator = new HashMap<String,Set<String>>();
        computePossibleDominators();

        roots = graph.getAllRoots();

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


    /*
     * computation of the hypernormal reachability relation
     */
    private void computeHypernormalReachability() {
        Set<String> visited = new HashSet<String>();

        assert graph.isNormal();
        assert graph.isHypernormallyConnected();

        // initialise the table
        for( String src : compact.getAllRoots() ) {
            Map<String,Integer> map = new HashMap<String,Integer>();
            hypernormalReachability.put(src, map);

            for( String tgt : compact.getAllRoots() ) {
                if( src == tgt ) {
                    map.put(tgt, HNR_EQUAL);
                } else {
                    map.put(tgt, HNR_NO_CONNECTION);
                }
            }
        }

        // do the real dfs
        // EFFICIENCY NOTE: get this from n^3 to .5 n^3 with root list indices
        for( String root1 : compact.getAllRoots() ) {
            int holeidx1 = 0;

            for( String hole1 : compact.getChildren(root1, EdgeType.TREE) ) {
                for( String root2 : compact.getAllRoots() ) {
                    int holeidx2 = 0;

                    if( !root1.equals(root2)) {
                        // count hole connections for possible dominators
                        visited.clear();
                        visited.add(root1);
                        if( compact.isHypernormallyReachable(hole1, root2, visited)) {
                            ModifiableInteger x = numHolesToOtherRoot.get(root1).get(root2);
                            x.setValue(x.getValue()+1);
                        }


                        for( String hole2 : compact.getChildren(root2, EdgeType.TREE )) {
                            visited.clear();
                            visited.add(root1);
                            visited.add(root2);

                            if( compact.isHypernormallyReachable(hole1, hole2, visited)) {
                                // found a hn path from hole1 to hole2 that doesn't
                                // visit the roots
                                Integer old1 = hypernormalReachability.get(root1).get(root2);
                                Integer old2 = hypernormalReachability.get(root2).get(root1);

                                //System.err.println("hnc: " + root1 + "/" + holeidx1 + " -- " + root2 + "/" + holeidx2);

                                if( old1 == HNR_NO_CONNECTION ) {
                                    // Case 1: We have never seen a hn connection from
                                    // root1 to root2. In this case, we have also never
                                    // seen a connection from root2 to root1, as they
                                    // are always recorded together. We simply add both
                                    // hole indices to the table.
                                    hypernormalReachability.get(root1).put(root2, holeidx1);
                                    hypernormalReachability.get(root2).put(root1, holeidx2);
                                    //System.err.println("  -- put");
                                } else if( ((old1 >= 0) && (old1 != holeidx1))
                                        || ((old2 >= 0) && (old2 != holeidx2)) ) {
                                    // Case 2: We have seen a hn connection before,
                                    // and this new connection uses a different hole
                                    // on one of the two sides. This means that
                                    // one of the two fragments is not a possible
                                    // dominator of the other, and hence we will never
                                    // look at this pair in any direction when checking
                                    // permutability.
                                    hypernormalReachability.get(root1).put(root2, HNR_TWO_CONNECTIONS);
                                    hypernormalReachability.get(root2).put(root1, HNR_TWO_CONNECTIONS);
                                    //System.err.println("  -- two");
                                } else {
                                    // Case 3: We have seen the same connection before.
                                    //System.err.println("  -- seen");
                                }
                            }

                            holeidx2++;
                        }
                    }
                }

                holeidx1++;
            }
        }

    }


    // root1 is p.d. of root2 iff it has exactly one hole that is connected
    // to root2 by a hn path that doesn't use root1.
    protected boolean isPossibleDominator(String root1, String root2) {
        if( oneWayDominator.get(root2).contains(root1)) {
            return false;
        }

        return numHolesToOtherRoot.get(root1).get(root2).getValue() == 1;

        /*
        if( !possibleDominators.containsKey(root1)) {
            return false;
        } else {
            return possibleDominators.get(root1).contains(root2);
        }
        */
        //return numHolesToOtherRoot.get(root1).get(root2).getValue() == 1;
    }

	


    private void computePossibleDominators() {
        Set<String> avoid = new HashSet<String>();
        oneWayDominator.clear();

        for( String dominator : compact.getAllRoots() ) {
            Set<String> oneWayDominees = new HashSet<String>();
            oneWayDominator.put(dominator, oneWayDominees);

            for( String dominee : compact.getAllRoots()) {
                avoid.clear();
                avoid.addAll(compact.getChildren(dominee, EdgeType.TREE));

                if( compact.isHypernormallyReachable(dominator, dominee, avoid) ) {
                    oneWayDominees.add(dominee);
                }
            }
        }
    }
	
	

    private List<String> getPermutingWildcards(QuantifierMarkedNonterminal subgraph, Set<String> roots) {
        List<String> ret = new ArrayList<String>();


        for( String node : roots ) {
            if( isPermutingWildcard(node, subgraph, roots) ) {
                ret.add(node);
            }
        }

        // sort result list in ascending order
        Collections.sort(ret);

        return ret;
    }

    private boolean isPermutingWildcard(String node, QuantifierMarkedNonterminal subgraph, Set<String> roots) {
        // only wildcards can be permuting wildcards
        if( !wildcardLabeledNodes.containsKey(node) ) {
            return false;
        }

        // only free fragments can be permuting wildcards
        if( !roots.contains(node)) {
            return false;
        }


        // only fragments that are connected to all their possible dominators
        // by a wildcard hole can be permuting wildcards
        for( String other : roots ) {
            if( !other.equals(node) && isPossibleDominator(other, node) ) {
                int connectingHole = hypernormalReachability.get(node).get(other);

                if( !wildcardLabeledNodes.get(node).contains(connectingHole) ) {
                    return false;
                }
            }
        }

        return true;
    }
    
    
    private boolean allowedSplit(String previousQuantifier, String currentRoot) {
    	// if there was no previous quantifier, all splits are allowed
        if( previousQuantifier == null ) {
            if(DEBUG) {
                System.err.print("[pq=null -> allowed] ");
            }
            return true;
        }

        // if the previous quantifier was a wildcard, then it doesn't restrict the allowed splits
        if( wildcardLabeledNodes.containsKey(previousQuantifier)) {
            if(DEBUG) {
                System.err.print("[pq=wildcard -> allowed]");
            }
            return true;
        }



        // if the two quantifiers are in the right order (previous < here), then the split is allowed
        if( previousQuantifier.compareTo(currentRoot) < 0 ) {
            if(DEBUG) {
                System.err.println("[pq smaller -> allowed]");
            }
            return true;
        }

        // if the two quantifiers are permutable, then the split is not allowed
        if(DEBUG) {
            System.err.println("[perm: allowed=" + !isPermutable(previousQuantifier, currentRoot) + "] ");
        }
        return !isPermutable(previousQuantifier, currentRoot);
    }
    

    /*
     * permutability
     */

    protected boolean isPermutable(String root1, String root2) {
        // System.err.print("[pd " + root1 + "/" + root2 + ": " + isPossibleDominator(root1, root2) + "/" + isPossibleDominator(root2, root1) + "] ");

        if( !isPossibleDominator(root1, root2) || !isPossibleDominator(root2, root1)) {
            return false;
        } else {
            int n1n2 = hypernormalReachability.get(root1).get(root2),
            n2n1 = hypernormalReachability.get(root2).get(root1);

            assert (n1n2 >= 0) && (n2n1 >= 0);

            FragmentWithHole f1 =
                new FragmentWithHole(labels.getLabel(root1), indicesCompactToOriginal.get(root1).get(n1n2));
            FragmentWithHole f2 =
                new FragmentWithHole(labels.getLabel(root2), indicesCompactToOriginal.get(root2).get(n2n1));

            //System.err.println("     --> check permutability of " + root1 + " (" + f1
            //        + ") with " + root2 + " (" + f2 + ")");

            Equation eq = new Equation(f1,f2);

            return eqs.contains(eq);
        }
    }

}
