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
	
	public static StringBuffer chartToString() {
		
		StringBuffer chartPrint = new StringBuffer();
		
		if( chart != null ) {
					chartPrint.append("chart edges: \n");
					/*ChartEdgeMap chartEdges = chart.getEdges();
				for ( int i = 0; i< chartEdges.size(); i++ ) {
					chartPrint.append( i+1 );
					chartPrint.append(") --> Splits: \n");
					
					
				}*/
				
				
				chartPrint.append(System.getProperty("line.separator"));
			
			
			chartPrint.append("The SWIG representation: " +
					System.getProperty("line.separator"));
			chartPrint.append(chart.toString());
			
		}
		
		return chartPrint;
	}
	
	/**
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
