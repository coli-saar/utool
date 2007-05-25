package de.saar.chorus.domgraph.codec.mrs;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org._3pq.jgrapht.Edge;

import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;

public class NormaliseNets implements Normaliser {

	public void normalise(MrsCodec codec, DomGraph graph)
		throws MalformedDomgraphException 
	{
		for (String root : graph.getAllRoots()) {
			Collection<Edge> edges = graph.getOutEdges(root, EdgeType.DOMINANCE);

			if (edges.size() > 0) {

				// check that the dominance children of the edges are pairwise
				// connected by hypernormal paths

				Object[] edgeArray = edges.toArray();

				for (int i = 0; i < edgeArray.length; ++i) {
					for (int j = i + 1; j < edgeArray.length; ++j) {
						String ni = (String) ((Edge) edgeArray[i]).getTarget();
						String nj = (String) ((Edge) edgeArray[j]).getTarget();

						Set<String> rootSet = new TreeSet<String>();
						rootSet.add(root);

						if (!graph.isHypernormallyReachable(ni, nj, rootSet)) {
							throw new MalformedDomgraphException(
									"The dominance children "
											+ ni
											+ " and "
											+ nj
											+ " of the root "
											+ root
											+ " are not hypernormally connected with each other.",
									ErrorCodes.NOT_HYPERNORMALLY_CONNECTED);
						}
					}
				}

				Collection<String> holes = graph.getOpenHoles(root);

				if (holes.size() == 1) {
					for (Edge edge : edges) {
						for (String hole : holes) {
							codec.addDomEdge(hole, (String) edge.getTarget());
						}
					}

					for (Edge edge : edges) {
						graph.remove(edge);
					}
				}
			}
		}
		
		StringBuffer errorText = new StringBuffer();
		int errorCode = 0;

		if (! graph.isWeaklyNormal())
			throw new MalformedDomgraphException("The graph is not weakly normal.\n", ErrorCodes.NOT_WEAKLY_NORMAL);

		if (! graph.isNormal()) {
			errorCode |= ErrorCodes.NOT_NORMAL;
			errorText.append("The graph is not normal.\n");
		} 
		if (! graph.isLeafLabelled()) {
			errorCode |= ErrorCodes.NOT_LEAF_LABELLED;
			errorText.append("The graph is not leaf-labelled.\n");
		}
		if (! graph.isHypernormallyConnected()) {
			errorCode |= ErrorCodes.NOT_HYPERNORMALLY_CONNECTED;
			errorText.append("The graph is not hypernormally connected.\n");
		}

		if (errorCode != 0)
			throw new MalformedDomgraphException(errorText.toString(), errorCode);
	}
}
