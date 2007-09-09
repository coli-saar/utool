package de.saar.chorus.domgraph.chart;

/**
 * An exception that signals that the chart solver doesn't understand the dominance
 * graph it is given to solve.
 * 
 * @author Alexander Koller
 *
 */
public class SolverNotApplicableException extends Exception {
	public SolverNotApplicableException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public SolverNotApplicableException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public SolverNotApplicableException(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public SolverNotApplicableException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = 6378294411682817605L;
}
