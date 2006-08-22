package de.saar.chorus.ubench.gui;

import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.graph.DomGraph;

public class ChartViewer extends JFrame {

	private JTextPane prettyprint;
	private Chart chart;
	private DomGraph dg;
	
	
	ChartViewer(Chart c, DomGraph g, String title) {
		super("Chart of " + title);
		chart = c;
		prettyprint = new JTextPane();
		prettyprint.setContentType("text/html");
		String textchart = chartOnlyRootsHTML(c,g);
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
	
	
	 private String chartOnlyRootsHTML(Chart ch, DomGraph g) {
	        StringBuffer ret = new StringBuffer();
	        Set<String> roots = g.getAllRoots();
	        Set<Set<String>> visited = new HashSet<Set<String>>();
	        ret.append("<html><font face=\"Arial, Arial Black\" color=\"aqua\"><table border=\"0\">");
	        for( Set<String> fragset : ch.getToplevelSubgraphs() ) {
	            ret.append(corSubgraph(fragset, ch, roots, visited));
	        }
	        ret.append("</table></font></html>");
	        return ret.toString();
	    }
	    


	    private String corSubgraph(Set<String> subgraph, Chart ch, Set<String> roots, Set<Set<String>> visited) {
	        Set<String> s = new HashSet<String>(subgraph);
	        StringBuffer ret = new StringBuffer();
	        boolean first = true;
	        String whitespace = "<td></td><td></td>";
	        Set<Set<String>> toVisit = new HashSet<Set<String>>();
	        
	        if( !visited.contains(subgraph )) {
	            visited.add(subgraph);
	            
	            s.retainAll(roots);
	            String sgs = s.toString();
	            
	            
	            if( ch.getSplitsFor(subgraph) != null ) {
	                ret.append("<tr>" + sgs + " <td>&#8594;</td><td> ");
	                for( Split split : ch.getSplitsFor(subgraph)) {
	                    if( first ) {
	                        first = false;
	                    } else {
	                        ret.append(whitespace);
	                    }
	                    
	                    ret.append(corSplit(split, roots) + "</td></tr>");
	                    toVisit.addAll(split.getAllSubgraphs());
	                }
	                
	                for( Set<String> sub : toVisit ) {
	                    ret.append(corSubgraph(sub, ch, roots, visited));
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
	

}
