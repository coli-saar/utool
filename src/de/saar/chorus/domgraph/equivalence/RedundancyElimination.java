/*
 * @(#)RedundancyElimination.java created 06.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.equivalence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org._3pq.jgrapht.Edge;
import org._3pq.jgrapht.util.ModifiableInteger;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;


/*
 * Some sanity checks:
 *  - There are no HNR_NO_CONNECTION entries in hypernormalReachability
 *    (because the graph is hnc). --> this is not true!
 *  - For every root, there are entries 0,1,... in indicesCompactToOriginal.
 */

public class RedundancyElimination {
    private DomGraph graph; // original graph
    private DomGraph compact; // compact version of the graph
    private NodeLabels labels;
    private EquationSystem eqs;
    
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
    private Map<String,Map<String,Integer>> hypernormalReachability;

    // The compactification deletes labelled leaves, so there may be a discrepancy
    // between the index of a hole in the compact graph and the (left-to-right dfs)
    // index of an unlabelled leaf in the original graph. This map here maps
    // the indices in the compact graph to the indices in the original graph.
    private Map<String,Map<Integer,Integer>> indicesCompactToOriginal;
    
    private Map<String,Map<String,ModifiableInteger>> numHolesToOtherRoot;
    
    private int currentHoleIdx;
    private int currentLeafIdx;

    
    public RedundancyElimination(DomGraph graph, NodeLabels labels, EquationSystem eqs) {
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
        
        System.err.println(hypernormalReachability);
    }
    
    
    /*
     * the main redundancy elimination algorithm
     */
    
    public void eliminate(Chart c) {
        Set<Set<String>> visited = new HashSet<Set<String>>();
        
        for( Set<String> subgraph : c.getToplevelSubgraphs() ) {
            eliminate(subgraph, c, visited);
        }
    }
    
    private void eliminate(Set<String> subgraph, Chart c, Set<Set<String>> visited) {
        List<Split> splits = c.getSplitsFor(subgraph);
        Split thePermutableSplit = null;

        if( !visited.contains(subgraph)) {
            visited.add(subgraph);
            
            if( splits != null ) { // i.e. not a singleton fragset 
                for( Split split : splits ) {
                    if( isPermutableSplit(split, subgraph)) {
                        // i.e. the split with this index is permutable => eliminate all others
                        thePermutableSplit = split;
                        break;
                    }
                }
                
                if( thePermutableSplit != null ) {
                    c.setSingleSplit(subgraph, thePermutableSplit);
                }
                
                // Whether we eliminated something from the complete subcompactGraph or not,
                // do recursive calls on _its_ subgraphs.
                splits = c.getSplitsFor(subgraph);
                for( Split split : splits ) {
                    for( Set<String> subsubgraph : split.getAllSubgraphs() ) {
                        eliminate(subsubgraph, c, visited);
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
        //System.err.println("table = " + indicesCompactToOriginal);
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
        // TODO: get this from n^3 to .5 n^3 with root list indices
        for( String root1 : compact.getAllRoots() ) {
            int holeidx1 = 0;
            
            for( String hole1 : compact.getChildren(root1, EdgeType.TREE) ) {
                for( String root2 : compact.getAllRoots() ) {
                    int holeidx2 = 0;
                    
                    if( !root1.equals(root2)) {
                        // count hole connections for possible dominators
                        visited.clear();
                        visited.add(root1);
                        if( isHnReachable(hole1, root2, visited, false)) {
                            ModifiableInteger x = numHolesToOtherRoot.get(root1).get(root2);
                            x.setValue(x.getValue()+1);
                        }
                        
                        
                        for( String hole2 : compact.getChildren(root2, EdgeType.TREE )) {
                            visited.clear();
                            visited.add(root1);
                            visited.add(root2);
                            
                            if( isHnReachable(hole1, hole2, visited, false)) {
                                // found a hn path from hole1 to hole2 that doesn't
                                // visit the roots
                                Integer old1 = hypernormalReachability.get(root1).get(root2); 
                                Integer old2 = hypernormalReachability.get(root2).get(root1);

                                System.err.println("hnc: " + root1 + "/" + holeidx1 + " -- " + root2 + "/" + holeidx2);

                                if( old1 == HNR_NO_CONNECTION ) {
                                    // Case 1: We have never seen a hn connection from
                                    // root1 to root2. In this case, we have also never
                                    // seen a connection from root2 to root1, as they
                                    // are always recorded together. We simply add both
                                    // hole indices to the table.
                                    hypernormalReachability.get(root1).put(root2, holeidx1);
                                    hypernormalReachability.get(root2).put(root1, holeidx2);
                                    System.err.println("  -- put");
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
                                    System.err.println("  -- two");
                                } else {
                                    // Case 3: We have seen the same connection before.
                                    System.err.println("  -- seen");
                                }
                            }
                            
                            holeidx2++;
                        }
                    }
                }
                
                holeidx1++;
            }
        }
        
        System.err.println("hnr: " + hypernormalReachability);
        
        /*
        
        for( String src : compact.getAllRoots() ) {
            holeIdx = 0;
            
            for( String hole : compact.getChildren(src, EdgeType.TREE )) {
                visited.clear();
                visited.add(src);
                
                hnReachDfs(hole, CameViaEdgeType.NONE, src, holeIdx++, visited);
            }
        }
        */
    }
    
    // Is there a hn path from src to target that doesn't use nodes in visited?
    private boolean isHnReachable(String src, String tgt, Set<String> visited, boolean previousEdgeWasUpDom) {
        if( visited.contains(src) ) {
            return false;
        } else if( src.equals(tgt) ) {
            return true;
        } else {
            visited.add(src);
            
            for( Edge edge : compact.getAdjacentEdges(src) ) {
                String neighbour = (String) edge.oppositeVertex(src);
                boolean isDomEdge = (compact.getData(edge).getType() == EdgeType.DOMINANCE);
                boolean isOutEdge = src.equals(edge.getSource());
                
                // skip outgoing dom edges if we came through an up dom edge
                if( isDomEdge && isOutEdge && previousEdgeWasUpDom ) {
                    continue;
                }
                
                if( isHnReachable(neighbour, tgt, visited, isDomEdge && isOutEdge) ) {
                    return true;
                }
            }
            
            return false;
        }
    }
    
    // root1 is p.d. of root2 iff it has exactly one hole that is connected
    // to root2 by a hn path that doesn't use root1.
    private boolean isPossibleDominator(String root1, String root2) {
        return numHolesToOtherRoot.get(root1).get(root2).getValue() == 1;
    }
    
    /*
    private void hnReachDfs(String node, CameViaEdgeType previousEdgeType, 
            String originalRoot, int holeIdx, Set<String> visited) {
        if( !visited.contains(node) ) {
            visited.add(node);
            
            // iterate over incoming edges
            for( Edge e : compact.getInEdges(node, null) ) {
                String src = (String) e.getSource();
                int idx = -1, i = 0;
                
                // If the parent is a root, then record the hypernormal path between
                // the starting node and this root
                if( compact.isRoot(src) && !visited.contains(src) ) {
                    assert compact.getData(e).getType() == EdgeType.TREE;
                    assert node.equals(e.getTarget());
                    
                    for( String tgt : compact.getChildren(src, EdgeType.TREE)) {
                        if( tgt.equals(node) ) {
                            idx = i;
                        }
                        i++;
                    }
                    
                    // idx is the index of the hole node below src in the compact graph
                    
                    System.err.println("hnc: " + originalRoot + "/" + holeIdx + " -- " + src + "/" + idx);
                    
                    // At this point, we have found a hn path from the edgeIdx-th hole
                    // of originalRoot to the the idx-th hole of src which doesn't use
                    // src.
                    if( hypernormalReachability.get(src).get(originalRoot)
                            == HNR_NO_CONNECTION ) {
                        // If we haven't seen a hn path between src and originalRoot yet,
                        // record it.
                        System.err.println("  -- put");
                        hypernormalReachability.get(originalRoot).put(src, holeIdx);
                        hypernormalReachability.get(src).put(originalRoot, idx);
                    } else if( (hypernormalReachability.get(originalRoot).get(src) != holeIdx)
                            || (hypernormalReachability.get(src).get(originalRoot) != idx) ) {
                        // Otherwise, record for both fragments that there is more than
                        // one hn connection between the two. In this case, one of the
                        // two fragments is entailed to dominate the other, so we don't
                        // have to distinguish which fragment used two different holes
                        // in the connections.
                        System.err.println("  -- two");
                        hypernormalReachability.get(originalRoot).put(src, HNR_TWO_CONNECTIONS);
                        hypernormalReachability.get(src).put(originalRoot, HNR_TWO_CONNECTIONS);
                    } else {
                        // otherwise we have just seen a path again which we already found earlier
                        System.err.println("  -- seen");
                    }
                }
                
                
                // recursive dfs call
                hnReachDfs(src,  
                        (compact.getData(e).getType() == EdgeType.TREE) ?
                                CameViaEdgeType.UP_TREE : CameViaEdgeType.UP_DOM,
                         originalRoot, holeIdx, visited);
            }

            // Iterate over outgoing edges
            for( Edge e : compact.getOutEdges(node, null)) {
                String tgt = (String) e.getTarget();
                
                // never use two dom edges out of the same node twice
                if( compact.getData(e).getType() == EdgeType.DOMINANCE ) {
                    if( previousEdgeType != CameViaEdgeType.UP_DOM ) {
                        hnReachDfs(tgt, CameViaEdgeType.DOWN_DOM,
                                originalRoot, holeIdx, visited);
                    }
                } else {
                    hnReachDfs(tgt, CameViaEdgeType.DOWN_TREE,
                            originalRoot, holeIdx, visited);
                }
            }
        }
    }
    */
    
    /*
     * permutability
     */
    
    private boolean isPermutableSplit(Split s, Set<String> subgraph) {
        String splitRoot = s.getRootFragment();
        
        for( String root : subgraph ) {
            if( graph.isRoot(root) &&  !root.equals(splitRoot) ) {
                if( isPossibleDominator(root, splitRoot)) {
                    if( !isPermutable(root, splitRoot) ) {
                        return false;
                    }
                } else {
                    System.err.println("skip " + root + " because it's not a p.d. of " + splitRoot);
                }
            }
        }
        
        return true;
    }


    private boolean isPermutable(String root1, String root2) {
        int n1n2 = hypernormalReachability.get(root1).get(root2),
            n2n1 = hypernormalReachability.get(root2).get(root1);

        if( (n1n2 < 0) || (n2n1 < 0) ) {
            System.err.println("automatically permutable " + root1 + "/" + root2 + "/" + n1n2 + "/" + n2n1);
            return true;
        } else {
            System.err.println("permutable lookup: " + root1 + "/" + n1n2);
            System.err.println("permutable lookup2: " + root2 + "/" + n2n1);
            System.err.println(indicesCompactToOriginal);
            
            FragmentWithHole f1 = 
                new FragmentWithHole(labels.getLabel(root1), indicesCompactToOriginal.get(root1).get(n1n2));
            FragmentWithHole f2 =
                new FragmentWithHole(labels.getLabel(root2), indicesCompactToOriginal.get(root2).get(n2n1));
            
            Equation eq = new Equation(f1,f2);
        
            return eqs.contains(eq);
        }
    }
}
