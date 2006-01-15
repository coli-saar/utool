package de.saar.chorus.libdomgraph.chart;


import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;

import de.saar.chorus.libdomgraph.Chart;
import de.saar.chorus.libdomgraph.DomGraph;
import de.saar.chorus.libdomgraph.DomSolver;
import de.saar.chorus.libdomgraph.FragmentSetVector;
import de.saar.chorus.libdomgraph.Split;
import de.saar.chorus.libdomgraph.SplitVector;
import de.saar.chorus.libdomgraph.chart.gui.EnumeratorWindow;
import de.saar.chorus.ubench.DomGraphGXLCodec;
import de.saar.chorus.ubench.EdgeData;
import de.saar.chorus.ubench.EdgeType;
import de.saar.chorus.ubench.Fragment;
import de.saar.chorus.ubench.JDomGraph;
import de.saar.chorus.ubench.NodeData;
import de.saar.chorus.ubench.NodeType;
import de.saar.chorus.ubench.utool.DomGraphConverter;
import de.saar.chorus.ubench.utool.JDomGraphConverter;



public class Main {
	
	private static DomGraph graph;
	private static JDomGraph jDomGraph;
	private static DomSolver solver;
	private static Chart chart;
	private static int splitCount; //for debugging
	private static Set<Split> seen;
    private static int windowCounter = 0;
	private static List<JDomGraph> solvedForms;
	
	/**
	 * 
	 * @param fileName
	 * @return
	 */
	public static JDomGraph loadGraph(String fileName) {
		jDomGraph = new JDomGraph();
		try {
			File gxl = new File(fileName);	
			Reader input = new FileReader(gxl);
			DomGraphGXLCodec.decode(input, jDomGraph);
			for( Fragment frag : jDomGraph.getFragments() ) {
				System.out.println(frag);
			}
		} catch (IOException e ) {
			System.err.println("File can't be found");		
		} catch (Exception e) {
			System.err.println("Error while parsing " + fileName + ":");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		
		return jDomGraph;
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
			jDomGraph = conv.toJDomGraph();
			return jDomGraph;
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
					//FragmentSetVector splitSets = new FragmentSetVector();
					//lastSplit.getAllSubgraphs(splitSets);
					
					String rootName = graph.getData(lastSplit.getRoot()).getName();
					
					
				
					//recurcive printing the dependencies for every split
					
					if(! seen.contains(lastSplit)) {
						splitCount++;
						seen.add(lastSplit);
						chartPrint.append(printPath(0, chart, 
								lastSplit, new StringBuffer() ) ); 
					}
						
					
					
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
	private static StringBuffer printPath(int level, Chart chart, Split recentSplit, StringBuffer repr) {
		
		int newlevel = level;
		Split split = recentSplit;
		FragmentSetVector subgraphs = new FragmentSetVector();
		split.getAllSubgraphs(subgraphs);
		
		// switching to the next level for further
		// recursion steps
		newlevel++;
		
		StringBuffer toReturn = repr;
		
		// some graphical helpers (?)
		toReturn.append("------------------------" + System.getProperty("line.separator"));
		
		// printing the recursion level and the Node-Name of the
		// current Split-Root
		toReturn.append("LEVEL: " + level + System.getProperty("line.separator") + 
				"Root: " + graph.getData(split.getRoot()).getName() + " --> " 
				+ System.getProperty("line.separator"));
		
		
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
					Split nextSplit = newSplits.get(h);
					
					
					if(seen.contains(nextSplit)) {
						System.err.println("tadaaa.");
						
					} else {
						splitCount ++;
						seen.add(nextSplit);
						
						
						// next recursion step: the next level, the 
						// new split, consisting of the transmitted root & subgraphs,
						// the chart is always the same, the StringBuffer will be
						// continued.
						printPath(newlevel, chart, 
								nextSplit, toReturn );
						
						
						
					}
				} 
			}
		}
		return toReturn;
		
	}
	
	private static JDomGraph createEmptySolvedForm() {
		JDomGraph nextSolvedForm = new JDomGraph();
		
		for(DefaultGraphCell cell : jDomGraph.getNodes() ) {
			NodeData cellData = jDomGraph.getNodeData(cell);
			NodeData cloneData;
			if( cellData.getType().equals(NodeType.labelled)) {
				cloneData = new NodeData(NodeType.labelled, 
						cellData.getName(), cellData.getLabel(), nextSolvedForm); 
			} else {
				cloneData = new NodeData(NodeType.unlabelled, 
						cellData.getName(), nextSolvedForm); 
			}
			cloneData.addMenuItem(cell.toString(), 
					cell.toString());
			nextSolvedForm.addNode(cloneData);
		}
		
		for (DefaultEdge edge : jDomGraph.getSortedEdges() ) {
			
			EdgeData cellData = jDomGraph.getEdgeData(edge);
			EdgeData cloneData;
			
			if( cellData.getType().equals(EdgeType.solid)) {
				cloneData = new EdgeData(EdgeType.solid, 
						cellData.getName(), nextSolvedForm );
			} else {
				continue;
			}
			
			cloneData.addMenuItem(cellData.getMenuLabel(), cellData.getName());
			nextSolvedForm.addEdge(cloneData, nextSolvedForm.getNodeForName(
					jDomGraph.getNodeData(
							jDomGraph.getSourceNode(edge)).getName()), 
					        nextSolvedForm.getNodeForName(
					        		jDomGraph.getNodeData(jDomGraph.getTargetNode(edge)).getName()));
		}
		
		return nextSolvedForm;
	}
	
	public static List<JDomGraph> getSolvedForms() {
		return solvedForms;
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
		solvedForms = new ArrayList<JDomGraph>();
		
		
		FragmentSetVector fragSets = new FragmentSetVector();
		int numOfWccs = chart.computeWccFragmentSets(fragSets);
		
		EnumerationState enumState = new EnumerationState(chart, 
				WrapperTools.vectorToList(fragSets),
				jDomGraph);
		
		
		int sFormCounter = 0;
                
		//DomGraphConverter newConv = new DomGraphConverter(solver,graph);
		
		
		List<DomEdge> recentEdges ;
		while( ! enumState.isFinished() ) {
			
			enumState.findNextSolvedForm();
			if(enumState.representsSolvedForm()) {
				sFormCounter++;
				JDomGraph nextSolvedForm = createEmptySolvedForm();
				solvedForms.add(nextSolvedForm);
				
				recentEdges = new ArrayList<DomEdge>(
						enumState.extractDomEdges());
				
				for( DomEdge doEdge : recentEdges ) {
					doEdge.addToSolvedForm(nextSolvedForm);
				}
				
				
				System.out.println(sFormCounter + ". sF, " + 
						recentEdges.size() + " dominance edges");
			
				nextSolvedForm.computeFragments();
				
				
				
				/*JFrame debugWindow = new JFrame("Solved Form No " + sFormCounter);
                                windowCounter++;
				debugWindow.addWindowListener(new WindowAdapter() {
		            public void windowClosing(WindowEvent e) {
                                windowCounter--;
		                if(windowCounter == 0) 
				    System.exit(0);
		            }
		        });
				debugWindow.add(new JScrollPane(nextSolvedForm));

				debugWindow.pack();
				nextSolvedForm.computeLayout();
				debugWindow.validate();
				debugWindow.setVisible(true);*/
			}
			
		}
		
		EnumeratorWindow window = new EnumeratorWindow(solvedForms);
		window.setVisible(true);
		System.out.println("Computed solved forms: " + sFormCounter);
		System.err.println("Real number of solved forms: " + chart.countSolvedForms());
		//graph.setDominanceEdges();
	}
	
}
