package nl.rug.discomm.udr.modelcheck;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolverNotApplicableException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class ModelCheck {


	
	public static boolean solves(DomGraph sf, NodeLabels sfl, DomGraph dg, NodeLabels dgl) {
		if(! sf.isSolvedForm() ) {
			System.err.println("The solved form is not a tree!");
			return false;
		} else {
			try {
			return ChartSolver.solve(dg, new Chart(), new ModelSplitSource(dg,dgl,sf,sfl));
			} catch(SolverNotApplicableException e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	
	
}
