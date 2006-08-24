package de.saar.chorus.ubench.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.ubench.EdgeType;
import de.saar.chorus.ubench.Fragment;
import de.saar.chorus.ubench.JDomGraph;
import de.saar.chorus.ubench.NodeType;

public class ChartViewer extends JFrame implements CaretListener {

	
	private JTextPane prettyprint;
	private Chart chart;
	private DomGraph dg;
	private boolean splitMarked;
	private Color myGreen;
	
	ChartViewer(Chart c, DomGraph g, String title) {
		super("Chart of " + title);
		chart = c;
		splitMarked = false;
		myGreen = new Color(0,204,51);
		prettyprint = new JTextPane();
		prettyprint.addCaretListener(this);
		prettyprint.setContentType("text/html");
		String textchart = chartOnlyRootsHTML(g);
		StringBuffer htmlprint = new StringBuffer();
		
		textchart = textchart.replace("[","{");
		textchart = textchart.replace("]","}");
		
		htmlprint.append(textchart);
		prettyprint.setText(htmlprint.toString());
		
		prettyprint.setEditable(false);
		add(new JScrollPane(prettyprint));
		
		//TODO perhaps this isn't such a good idea...
		setAlwaysOnTop(true);
		pack();
		validate();
		setVisible(true);
	}
	
	
	 private String chartOnlyRootsHTML(DomGraph g) {
	        StringBuffer ret = new StringBuffer();
	        Set<String> roots = g.getAllRoots();
	        Set<Set<String>> visited = new HashSet<Set<String>>();
	        ret.append("<html><font face=\"Arial, Arial Black\" color=\"aqua\"><table border=\"0\">");
	        for( Set<String> fragset : chart.getToplevelSubgraphs() ) {
	            ret.append(corSubgraph(fragset, roots, visited));
	        }
	        ret.append("</table></font></html>");
	        return ret.toString();
	    }
	    


	    private String corSubgraph(Set<String> subgraph, Set<String> roots, Set<Set<String>> visited) {
	        Set<String> s = new HashSet<String>(subgraph);
	        StringBuffer ret = new StringBuffer();
	        boolean first = true;
	        String whitespace = "<td></td><td></td>";
	        Set<Set<String>> toVisit = new HashSet<Set<String>>();
	        
	        if( !visited.contains(subgraph )) {
	            visited.add(subgraph);
	            
	            s.retainAll(roots);
	            String sgs = s.toString();
	            
	            
	            if( chart.getSplitsFor(subgraph) != null ) {
	                ret.append("<tr>" + sgs + " <td>&#8594;</td><td> ");
	                for( Split split : chart.getSplitsFor(subgraph)) {
	                    if( first ) {
	                        first = false;
	                    } else {
	                        ret.append(whitespace);
	                    }
	                    
	                    ret.append(corSplit(split, roots) + "</td></tr>");
	                    toVisit.addAll(split.getAllSubgraphs());
	                }
	                
	                for( Set<String> sub : toVisit ) {
	                    ret.append(corSubgraph(sub, roots, visited));
	                }
	            }
	                
	            return ret.toString();
	        }
	        else {
	            return "";
	        }
	    }



	    private String corSplit(Split split, Set<String> roots) {
	        StringBuffer ret = new StringBuffer("&lt;" + split.getRootFragment());
	        Map<String,List<Set<String>>> map = new HashMap<String,List<Set<String>>> (); 
	        
	        for( String hole : split.getAllDominators() ) {
	            List<Set<String>> x = new ArrayList<Set<String>>();
	            map.put(hole, x);

	            for( Set<String> wcc : split.getWccs(hole) ) {
	                Set<String> copy = new HashSet<String>(wcc);
	                copy.retainAll(roots);
	                x.add(copy);
	            }
	        }
	        
	        
	        ret.append(" " + map);
	        ret.append("&gt;");
	        return ret.toString();
	    }
	
	    
	    public void caretUpdate(CaretEvent e) {
	    	String marked = prettyprint.getSelectedText();
	    	if((marked != null ) && marked.matches("[ \t\n\f\r]*<.*>")) {
	    		Ubench.getInstance().
				getVisibleTab().getGraph().setMarked(false);
	    		splitMarked = true;
	    		
	    		StringTokenizer tok = new StringTokenizer(marked," {},=<>\t\n\f\r");
	    		String root;
	    		List<String> remainingNodes = new ArrayList<String>();
	    		if( tok.countTokens() > 0 ) {
	    			root = tok.nextToken();
	    			while(tok.hasMoreTokens()) {
	    				remainingNodes.add(tok.nextToken());
	    			}
	    			
	    			// TODO move that anywhere else (Tab?)
	    			JDomGraph graph = Ubench.getInstance().
	    					getVisibleTab().getGraph();
	    			
	    			DefaultGraphCell rootNode = graph.getNodeForName(root);
	    			graph.markNode(rootNode
	    					, myGreen);
	    			
	    			for(DefaultEdge edg : graph.getOutEdges(rootNode)) {
	    				graph.markEdge(edg, myGreen);
	    				
	    			}
	    			
	    			Set<Fragment> toMark = new HashSet<Fragment>();
	    			
	    			for(String otherNode : remainingNodes) {
	    				DefaultGraphCell gc = graph.getNodeForName(otherNode);
	    				if(graph.getNodeData(gc).getType() != NodeType.unlabelled) {
	    					Fragment frag  = graph.findFragment(gc);
	    					toMark.add(frag);
	    				} else {
	    					graph.markNode(gc, Color.blue);
	    				}
	    				
		    			
		    			for(DefaultEdge edg : graph.getOutEdges(gc)) {
		    				graph.markEdge(edg, Color.blue);
		    				if(graph.getEdgeData(edg).getType() == 
		    					EdgeType.dominance) {
		    					Fragment tgt = graph.getTargetFragment(edg);
		    					if(tgt != null ) {
		    						toMark.add(tgt);
		    					}
		    				}
		    				
		    			}
	    			}
	    			
	    			for( Fragment frag : toMark ) {
	    				for( DefaultGraphCell gc : frag.getNodes() ) {
	    					graph.markNode(gc
			    					, Color.blue);
	    					for(DefaultEdge edg : graph.getOutEdges(gc)) {
	    						graph.markEdge(edg, Color.blue);
	    					}
	    				}
	    				
	    				
	    			}
	    			
	    			
	    			
	    			graph.computeLayout();
	    			graph.adjustNodeWidths();
	    			graph.setMarked(true);
	    			
	    		}
	    	} else {
	    		if( splitMarked ) {
	    			Ubench.getInstance().
						getVisibleTab().getGraph().setMarked(false);
	    			splitMarked = false;
	    		}
	    	}
	    	
	    	

	    }


		@Override
		public void setVisible(boolean b) {
			super.setVisible(b);
			Ubench.getInstance().
			getVisibleTab().getGraph().setMarked(false);
		}


	




}
