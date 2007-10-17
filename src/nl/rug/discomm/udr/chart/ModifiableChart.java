package nl.rug.discomm.udr.chart;

import java.util.ArrayList;
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
	
	private List<Set<String>> orderedSubgraphs;
	
	public ModifiableChart() {
		super();
		orderedSubgraphs = null;
	}
	
	/**
	 * This provides the subgraph contained in this chart, ordered from
	 * toplevel subgraphs followed by the child graphs of their splits, and
	 * so on.
	 * For introducing new dominances, one needs this ordered subgraph list
	 * such that the splits are deleted in the right order. Without this ordering,
	 * some subgraphs that could actually be deleted are still referenced when
	 * deleting their last split.
	 * This method is to make sure that this list will be computed only once.
	 * 
	 * @return an ordered list of subgraphs in this chart.
	 */
	private List<Set<String>> getAllSubgraphs() {

		if(orderedSubgraphs == null) {
			orderedSubgraphs = new ArrayList<Set<String>>();
			for(Set<String> top : getToplevelSubgraphs()) {
				addOrderedSubgraphs(top);
			}
		}
		return orderedSubgraphs;
	}

	
	public void deleteSubgraph(Set<String> subgraph) {
		super.deleteSubgraph(subgraph);
		orderedSubgraphs.remove(subgraph);
	}
	
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

		List<Set<String>> copyOfSubgraphs = new ArrayList<Set<String>>(getAllSubgraphs());
		
		
		// check for all subgraphs whether they are concerned with the
		// new dominance edge.
		for( Set<String> subgraph : copyOfSubgraphs ) {

			// relevant subgraphs contain the source and
			// the target of the edge.
			if( subgraph.contains(src) && 
					subgraph.contains(tgt) ) {

				List<Split> splits = 
					getSplitsFor(subgraph);
				
				// for the new list of splits
				List<Split> modified = new ArrayList<Split>();
				
				/*
				 * A Split has to be deleted iff
				 * its root fragment is dominated by the new dominance edge 
				 * (provided that the source node is in the same subgraph) or
				 * its root fragment is not free anymore after introducing the edge.
				 */
				for(Split split : splits) {
					
					// if the dominance edge is within the subgraph,
					// a split rooted by the target node is not valid anymore.
					if(! split.getRootFragment().equals(tgt)) {
						boolean amStillFree = true;
						boolean foundOne = false;

						/*
						 * checking freeness of the root fragment.
						 * the root fragment is not free anymore iff
						 * two nodes of two different subgraphs have been
						 * connected by the dominance edge.
						 * (The assumption here is that all fragments are binary.
						 * TODO: check whether this holds also for fragments with
						 * higher arity.)
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

						if( amStillFree ) {
							// 'really' free fragment
							modified.add(split);
						}
					}
				}

				setSplitsForSubgraph(subgraph, modified);

			}
		}
	}

	private void addOrderedSubgraphs(Set<String> subgraph) {
		orderedSubgraphs.add(subgraph);
		if(containsSplitFor(subgraph)) {
			for(Split split : getSplitsFor(subgraph)) {
				for(Set<String> child : split.getAllSubgraphs()) {
					addOrderedSubgraphs(child);
				}
			}
		}
	}
 }
