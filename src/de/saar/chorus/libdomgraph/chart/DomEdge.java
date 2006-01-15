package de.saar.chorus.libdomgraph.chart;

import org.jgraph.graph.DefaultGraphCell;

import de.saar.chorus.libdomgraph.DomGraph;
import de.saar.chorus.libdomgraph.SWIGTYPE_p_Node;
import de.saar.chorus.ubench.EdgeData;
import de.saar.chorus.ubench.EdgeType;
import de.saar.chorus.ubench.JDomGraph;

public class DomEdge {
	
	private String src;
	private String tgt;
	
	DomEdge(SWIGTYPE_p_Node source, SWIGTYPE_p_Node target,
				DomGraph cGraph, JDomGraph jGraph) {
		src = cGraph.getData(source).getName();
		tgt = cGraph.getData(target).getName();
		
	   System.out.println("Source: " +src +
			   " -> Target: " + tgt ); //debug
	}
	
	DomEdge(DefaultGraphCell source, DefaultGraphCell target, JDomGraph jGraph) {
		src = jGraph.getNodeData(source).getName();
		tgt = jGraph.getNodeData(target).getName();
	}
	
	/* (non-Javadoc)
	 * @see java.util.Map.Entry#getKey()
	 */
	public String getSource() {
		// TODO Auto-generated method stub
		return src;
	}
	
	/**
	 * TODO put this in a more appropriate place
	 * @param sF
	 */
	public void addToSolvedForm( JDomGraph  sF ) {
		DefaultGraphCell source = sF.getNodeForName(src);
    	DefaultGraphCell target = sF.getNodeForName(tgt);
    	
    	EdgeData domEdgData = new EdgeData(EdgeType.dominance, src + " -> " + tgt, sF);
    	domEdgData.addMenuItem(domEdgData.getName(), domEdgData.getName());
    	sF.addEdge(domEdgData, source, target);
	}

	/* (non-Javadoc)
	 * @see java.util.Map.Entry#getValue()
	 */
	public String getTarget() {
		// TODO Auto-generated method stub
		return tgt;
	}

	/* (non-Javadoc)
	 * @see java.util.Map.Entry#setValue(V)
	 */
	/*public DefaultGraphCell setValue(DefaultGraphCell arg0) {
		src =  arg0;
		return arg0;
	}*/
	
	public boolean equals(Object o) {
		boolean equal = (src.equals(((DomEdge) o).getSource()) && 
				tgt.equals(((DomEdge) o).getTarget()));
			return equal;
	}

}
