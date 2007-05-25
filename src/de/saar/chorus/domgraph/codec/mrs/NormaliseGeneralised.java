package de.saar.chorus.domgraph.codec.mrs;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org._3pq.jgrapht.Edge;

import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;

public class NormaliseGeneralised extends NormaliseNets {

	public void normalise(MrsCodec codec, DomGraph graph)
		throws MalformedDomgraphException
	{
		// XXX -- only correct for compact graphs; for non-compact graphs,
		// we have to consider subgraphs with all non-holes of the current
		// fragment deleted
		Set<String> subgraph = new HashSet<String>(graph.getAllNodes());

		for (String root : graph.getAllRoots()) {
			Collection<Edge> weakEdges = graph.getOutEdges(root, EdgeType.DOMINANCE);
		
			if (weakEdges.size() > 1) {
				subgraph.remove(root);
				
				Map<String,Integer> wccMap = graph.computeWccMap(graph.wccs(subgraph));

				for (Edge weakEdge : weakEdges) {
					String target = (String) weakEdge.getTarget();
					Integer targetWcc = wccMap.get(target);
					
					for (String hole : graph.getHoles(root)) {
						Integer holeWcc = wccMap.get(hole);
						
						if (targetWcc.equals(holeWcc)) {
							// XXX
							codec.addDomEdge(hole, target);
							// XXX
							graph.remove(weakEdge);
						}
					}
				}
				
				subgraph.add(root);
			}
		}
		super.normalise(codec, graph);
	}

}
