package nl.rug.discomm.udr.chart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.Split;


/**
 * A class extending the <code>Chart</code> for <code>DomGraph</code> solving.
 * This class additionally provides methods to introduce new dominances to the
 * existing (and solved) chart by deleting splits. This is needed to allow for
 * the (more efficient) successive disambiguation of a dominance graph.
 * 
 * @author Michaela Regneri
 *
 */
public class ModifiableChart extends Chart {
	
	

	
	/**
	 * This introduces a dominance edge from the given source to the given target node
	 * by deleting the splits from the chart which become invalid because of this dominance
	 * edge.
	 * It's not possible to introduce a dominance edge which makes the graph unsolvable.
	 * If this happens, an <code>UnsupportedOperationException</code> will be thrown at
	 * some point. The changes to the chart made by such an invalid call are not defined!
	 * 
	 * @param src the source node of the new dominance edge
	 * @param tgt the target node of the new dominance edge
	 * @throws UnsupportedOperationException if the graph would become unsolvable with the new edge
	 */
	public void addDominance(String src, String tgt) throws UnsupportedOperationException {

		long time = System.currentTimeMillis();
		
		// recursively iterating through the chart and deleting splits which
		// are not valid anymore.
		for( Set<String> subgraph : getToplevelSubgraphs() ) {
			restrictSubgraph(subgraph, src, tgt, new HashSet<Set<String>>());
		}
	}
	
	
	/**
	 * Indicates whether or not a split has to be deleted when introducing the
	 * dominance edge between the given nodes.
	 *  A Split has to be deleted iff
	 * its root fragment is dominated by the new dominance edge 
	 * (provided that the source node is in the same subgraph) or
	 * its root fragment is not free anymore after introducing the edge.
	 * 
	 * @param split the split to check
	 * @param src the source of the new dominance edge
	 * @param tgt the target of the new dominance edge
	 * @return true if the dominance edge makes this split invalid 
	 */
	private boolean delete(Split split, String src, String tgt) {
		
		
		// if the dominance edge is within the subgraph,
		// a split rooted by the target node is not valid anymore.
		if(! split.getRootFragment().equals(tgt)) {
			boolean amStillFree = true;
			boolean foundOne = false;
			

			/*
			 * checking freeness of the root fragment.
			 * the (formerly free) root fragment is not free anymore iff
			 * two nodes of two different subgraphs have been
			 * connected by the dominance edge.
			 */
			for (Set<String> children :
				split.getAllSubgraphs()) {
				if(children.contains(src)) {
					if(foundOne) {
						amStillFree = false;
						break;
					} else {
						foundOne = true;
					}
				} else if (children.contains(tgt)) {
					if(foundOne) {
						amStillFree = false;
						break;
					} else {
						foundOne = true;
					}
				}
			}
			return  ! amStillFree;
		} else {
			return true;
		}
	}
	

	/**
	 * Introduces a dominance edge into a given subgraph and deletes the splits
	 * which become invalid. This method treats the given subgraph and recursively
	 * the subgraphs of the (still valid) splits.
	 * 
	 * @param subgraph the subgraph to check
	 * @param src the source of the new dominance edge
	 * @param tgt the target of the new dominance edge
	 * @param visited the subgraphs already seen
	 */
	private void restrictSubgraph(Set<String> subgraph, String src, String tgt,
			Set<Set<String>> visited) {

		if(! visited.contains(subgraph)) {

			visited.add(subgraph);

			// only subgraphs containing the edge are concerned
			if( subgraph.contains(src) && subgraph.contains(tgt) ) {

				// for the new list of splits
				List<Split> modified = new ArrayList<Split>();
				boolean changed = false;

				if(containsSplitFor(subgraph)) {
					
					// check splits whether they are still valid
					for(Split split : getSplitsFor(subgraph) ) {
						if(! delete(split, src, tgt)) {
							modified.add(split);
							
							/*
							 * If a split is valid, keep it and check its child
							 * subgraphs.
							 * 
							 *  TODO find out whether this can cause split deletion
							 *  in the wrong order (i.e. deletion of still-referenced
							 *  subgraphs). Up to now, this didn't happen, but I don't
							 *  see why it should not happen.
							 */
							for(Set<String> child : split.getAllSubgraphs() ) {
								restrictSubgraph(child, src, tgt,visited);
							}
						} else {
							changed = true;
						}
					}

					if(changed) {
						setSplitsForSubgraph(subgraph, modified);
					}

					// this is the alternative for treating the remaining splits immediately.
					// it's not much slower, but slower, though.
				/*	for(Split split : modified ) {
						for(Set<String> child : split.getAllSubgraphs() ) {
							restrictSubgraph(child, src, tgt,visited);
						}
					}*/
				}
			}

		}

	}
	



 }
