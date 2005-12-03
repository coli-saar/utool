package de.saar.chorus.libdomgraph.chart;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import de.saar.chorus.libdomgraph.Chart;
import de.saar.chorus.libdomgraph.DomGraph;
import de.saar.chorus.libdomgraph.DomSolver;
import de.saar.chorus.libdomgraph.FragmentSetVector;
import de.saar.chorus.libdomgraph.NodeData;
import de.saar.chorus.libdomgraph.SWIGTYPE_p_Node;
import de.saar.chorus.libdomgraph.Split;
import de.saar.chorus.libdomgraph.SplitVector;
import de.saar.chorus.ubench.DomGraphGXLCodec;
import de.saar.chorus.ubench.Fragment;
import de.saar.chorus.ubench.JDomGraph;
import de.saar.chorus.ubench.utool.DomGraphConverter;
import de.saar.chorus.ubench.utool.JDomGraphConverter;



public class Main {
	
	private static DomGraph graph;
	private static DomSolver solver;
	private static Chart chart;
	/**
	 * 
	 * @param fileName
	 * @return
	 */
	public static JDomGraph loadGraph(String fileName) {
		JDomGraph loadedGraph = new JDomGraph();
		try {
			File gxl = new File(fileName);	
			Reader input = new FileReader(gxl);
			DomGraphGXLCodec.decode(input, loadedGraph);
			for( Fragment frag : loadedGraph.getFragments() ) {
				System.out.println(frag);
			}
		} catch (IOException e ) {
			System.err.println("File can't be found");		
		} catch (Exception e) {
			System.err.println("Error while parsing " + fileName + ":");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		
		return loadedGraph;
	}
	
	/**
	 * 
	 * @param filename
	 * @return
	 */
	public static JDomGraph importGraph(String filename) {
		solver = new DomSolver();
		boolean ok = solver.loadGraph(filename);
		
		if( !ok ) {
			JOptionPane.showMessageDialog(new JFrame(),
					"An error occurred while loading this graph\n(perhaps the file " +
					"doesn't exist,\nor the input codec couldn't be determined or was " +
					"unable to parse the graph).",
					"Error during import",
					JOptionPane.ERROR_MESSAGE);
			return null;
		} else {
			DomGraphConverter conv = new DomGraphConverter(solver, solver.getGraph());
			return conv.toJDomGraph();
		}
	}    
	
	
	/**
	 * 
	 * @param filename
	 * @return
	 */
	public static DomGraph genericLoadGraph(String filename) {
		try {
            System.loadLibrary("DomgraphSwig");
            
        } catch(UnsatisfiedLinkError e) {
            System.err.println("Error while loading libdomgraph library: " + e.getMessage());
            
        }
		
		if( filename.endsWith(".xml") ) {
			solver = new DomSolver();
			JDomGraphConverter jconv = new JDomGraphConverter(solver);
			jconv.toDomGraph(loadGraph(filename));
			return jconv.getGraph();
		} else {
			solver = new DomSolver();
			boolean ok = solver.loadGraph(filename);
			
			if( !ok ) {
				JOptionPane.showMessageDialog(new JFrame(),
						"An error occurred while loading this graph\n(perhaps the file " +
						"doesn't exist,\nor the input codec couldn't be determined or was " +
						"unable to parse the graph).",
						"Error during import",
						JOptionPane.ERROR_MESSAGE);
				return null;
			} else {
				return solver.getGraph();
			}
		}
	}
	
	/**
	 * Creates a <code>StringBuffer</code> containing a 
	 * representation of the current <code>Chart</code>
	 * 
	 * @return the readable <code>Chart</code> 
	 */
	public static StringBuffer chartToString() {
		
		//StringBuffer for the representation
		StringBuffer chartPrint = new StringBuffer();
		
		chartPrint.append("chart edges: \n");
		
		// the chart has to be instantiated 
		if( chart != null ) {
			
			// will contain the first wccs as FragmentSets
			FragmentSetVector fragSets = new FragmentSetVector();
			int numOfWccs = chart.computeWccFragmentSets(fragSets);
			
			// first recurcion step here: in normal caes
			// just 1 iteration?!
			for( int i = 0; i< fragSets.size(); i++ ) {
				
				// the splits for the recently considered wcc
				SplitVector recentSplits = chart.getEdgesFor(fragSets.get(i));
				
				// it should be 1 step in general. otherwise the
				// original Graph consists of two seperate graphs.
				chartPrint.append("STEP " + (i+1) + System.getProperty("line.separator"));
				
				// iterating over the splits 
				for( int h = 0; h < recentSplits.size(); h++) {
					Split lastSplit = recentSplits.get(h);
					
					//determining the subgraphs of the split
					FragmentSetVector splitSets = new FragmentSetVector();
					lastSplit.getAllSubgraphs(splitSets);
					
					//recurcive printing the dependencies for every split
					chartPrint.append(printPath(0, chart, lastSplit.getRoot(), 
							splitSets, new StringBuffer() ) ); 	
				}
			}
			
			chartPrint.append(System.getProperty("line.separator"));
		
		}
		
		return chartPrint;
	}
	
	/**
	 * Recursive auxiliary method to represent the dependencies
	 * of the chart with a String.
	 * 
	 * @param level the current recursion level
	 * @param chart the <code>Chart</code> we are dealing with
	 * @param root the root of the recent <code>Split</Code>
	 * @param subgraphs the subgraphs of the recent <code>Split</Code>
	 * @param repr the (sucessive growing) textual representation
	 * @return the textual representation of the whole initial Split
	 */
	private static StringBuffer printPath(int level, Chart chart, SWIGTYPE_p_Node root, 
			FragmentSetVector subgraphs, StringBuffer repr) {
		
		int newlevel = level;
		
		// switching to the next level for further
		// recursion steps
		newlevel++;
		
		StringBuffer toReturn = repr;
		
		// some graphical helpers (?)
		toReturn.append("------------------------" + System.getProperty("line.separator"));
		
		// printing the recursion level and the Node-Name of the
		// current Split-Root
		toReturn.append("LEVEL: " + level + System.getProperty("line.separator") + 
				"Root: " + graph.getData(root).getName() + " --> " 
				+ System.getProperty("line.separator"));
		
		// printing the number of Subgraphs to consider
		toReturn.append(subgraphs.size() + " subgraph(s): " + System.getProperty("line.separator"));
		
		// iterating through the Subgraphs to get the Splits
		// for them
		for( int i = 0; i < subgraphs.size(); i++) {
			
			// asking the chart for the Splits of the currently
			// considered subgraph (=FragmentSet)
			SplitVector newSplits = chart.getEdgesFor(subgraphs.get(i));
			
			// in case there are any...
			if( newSplits != null ) {
				
				// iterate over them to handle each of them
				for( int h = 0; h < newSplits.size(); h++) {
					
					// the recent new split
					Split recentSplit = newSplits.get(h);
					
					// for the subgraphs of the recent new split
					FragmentSetVector allSubgraphs = new FragmentSetVector();
					
					// filling the subgraph-vector
					recentSplit.getAllSubgraphs(allSubgraphs);
					
					// next recursion step: the next level, the 
					// new split, consisting of the transmitted root & subgraphs,
					// the chart is always the same, the StringBuffer will be
					// continued.
					printPath(newlevel, chart, 
							recentSplit.getRoot(), allSubgraphs, toReturn );
				}
			} else {
				// if there are no Splits for the recent FragmentSet,
				// it should consist of a leaf.
				/*
				 * TODO: figure out how to get the nodes out of a 
				 *       FragmentSet.  
				 */
				toReturn.append("(leaf) at LEVEL " + newlevel + System.getProperty("line.separator") );
			}
		}
		return toReturn;
		
	}
	
	/**
	 * This loads a <code>DomGraph</code> whose file representation
	 * is read from the commandline, solves the graph and
	 * puts its <code>String</code> represenation on screen.
	 * @param args
	 */
	public static void main(String[] args) {
		
		if( args != null ) {
			graph = genericLoadGraph( args[0] );
		} else 
			System.err.println("Please put a filename on " +
			"command line!");
		
		solver.solve();
		chart = solver.getChart();
		System.out.print(chartToString());
	}
	
}
