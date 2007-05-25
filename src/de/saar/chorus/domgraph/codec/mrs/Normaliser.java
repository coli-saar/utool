package de.saar.chorus.domgraph.codec.mrs;

import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.graph.DomGraph;

public interface Normaliser {
	public void normalise(MrsCodec codec, DomGraph graph) throws MalformedDomgraphException;
}
