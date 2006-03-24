/*
 * @(#)HolesemComsemOutputCodec.java created 03.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec.holesem;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org._3pq.jgrapht.Edge;

import de.saar.chorus.domgraph.codec.GraphOutputCodec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeData;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;
import de.saar.chorus.domgraph.graph.EdgeType;

/*
 * TODO: Implement this class.
 * It is currently just a placeholder because the implementation
 * of the holesem output codec is so ugly.
 * 
 * see holesem-comsem-parser.yy in utool
 */

public class HolesemComsemOutputCodec extends GraphOutputCodec {
    
	/**
	 * 
	 * @return
	 */
	public static String getName() {
        return "holesem-comsem";
    }
    
    /**
     * 
     * @return
     */
    public static String getExtension() {
        return ".hs.pl";
    }

    
  
    public void encode_graph(DomGraph graph, NodeLabels labels, Writer writer)
            throws IOException, MalformedDomgraphException {
        
    	// check whether the graph is applicable for holesemantics conversion
        if( !graph.isNormal() || !graph.isHypernormallyConnected() ||
        		! encodeGraphApplicable(graph, labels)) {
            throw new MalformedDomgraphException();
        }
        
        int closeBrackets = 0;	
        StringBuffer content = new StringBuffer();
        String top = null;
        boolean naked_top_hole = false;
        Set<String> labelledLeaves = new HashSet<String>();
        List<String> conjunction = new ArrayList<String>();
        List<String> hs_labels = new ArrayList<String>();
       
        // artificial top-fragment (for the case that it's needed) 
        String topname = "_utool_top_";
        String supertopname = "_utool_top_1_";
        
                
        /*
         * TODO check for cycles here
         * Is there a top-node dominating all other nodes (and
         * thus having no incoming edges)?
         */
        if( getTopNodes(graph).size() !=  1 ) {
        	// if there are no cycles, there can't be more
        	// than one root of that kind.
        	// so, if there is none, we create one.
        	content.append("some(" + supertopname + ",and(label(" +
        			supertopname + "),");
        	content.append("some(" + topname + ",and(hole(" +
        			topname + "),");
        	conjunction.add("top(" + supertopname + "," + topname + ")");
        	
        	closeBrackets += 4;
        } else {
        	// if there is a root, we don't need to create one.
        	top = getTopNodes(graph).get(0);
        	topname = varifyIfNecessary(top);
        }
        
        // node conversion
        for( String node : graph.getAllNodes() ) {
        	NodeData data = graph.getData(node);
        	String nodeName = varifyIfNecessary(node);
        	
        	// labelled nodes
        	if( data.getType() == NodeType.UNLABELLED ) {
        		// unlabelled nodes are holes
        		content.append("some(" +  nodeName +
        				",and(hole(" + nodeName + "),");
        		
        		closeBrackets += 2;
        		if(graph.getInEdges(node, null).size() == 0) {
        			naked_top_hole = true;
        		}
        	} else if(graph.getOutEdges(node, null).size() == 0) {
    			// storage for labelled leaves
        		labelledLeaves.add(node);
    		} else {
    			// labelled nodes
    			content.append("some(" +  nodeName +
        				",and(label(" + nodeName + "),");
    			
    			closeBrackets += 2;
    			hs_labels.add(node);
    		}
        }
        
        // processing labelled nodes that are no leaves
        for( String node : hs_labels ) {
        	NodeData data = graph.getData(node);
        	String nodeName = varifyIfNecessary(node);
        	String label = labels.getLabel(node);
        	
        	List<String> children = graph.getChildren(node,EdgeType.TREE);
        	
        	// if there was no root originally and we
        	// created an artificial one, we draw an edge
        	// between this top fragment and all labelled nodes.
        	if( (top == null) || naked_top_hole ) {
        		conjunction.add("leq(" + nodeName + "," + topname + ")");
        	}
        	
        	// resolving the predicate denoted by the node
        	StringBuffer nextConjunct = new StringBuffer();
        	
        	if( isLogicalLabel(label)) {
        		nextConjunct.append(atomifyIfNecessary(label));
        		
        		nextConjunct.append("(" + nodeName);
        	} else {
        		nextConjunct.append("pred" + children.size() 
        				+ "(" + nodeName + ","
        				+ atomifyIfNecessary(label));
        		
        	}
        	
        	// adding (tree-)edges
        	for( String child : children ) {
        		NodeData holeData = graph.getData(child);
        		nextConjunct.append(",");
        		if(! labelledLeaves.contains(child)) {
        			nextConjunct.append(varifyIfNecessary(child));
        		} else {
        			nextConjunct.append(atomifyIfNecessary(labels.getLabel(child)));
        		}
        	}
        	
        	nextConjunct.append(")");
        	conjunction.add(nextConjunct.toString());
        	
        	// adding domincance edges (out of the node's
        	// children)
        	for(String child : children) {
        		for( String domChild : graph.getChildren(child, 
        				EdgeType.DOMINANCE)) {
        			 conjunction.add("leq("
        					 + varifyIfNecessary(domChild)
        					 + ","
        					 + varifyIfNecessary(child) + 
        					 ")");
        		
        		}
        	}
        }
        
        int i = 0, len = conjunction.size();
        
        // add the conjunction consisting of 
        // the conjunctions stored
        for( String conjunct : conjunction ) {
        	if( i < len -1 ) {
        		content.append("and(" + conjunct + ",");
        		closeBrackets++;
        	} else {
        		content.append(conjunct);
        	}
        	i++;
        }
        
        // closing the brackets
        for( int h = 0; h < closeBrackets; h++ ) {
        	content.append(")");
        }
        
        
        content.append("\n");
        
        // let the writer write everything stored
        writer.write(content.toString());
        writer.flush();
    }
    
    /**
     * Resolving the roots of the graph, i.e.
     * the fragment roots with no incoming domincance edges.
     * 
     * @param graph the graph
     * @return a List of roots
     */
    private List<String> getTopNodes(DomGraph graph) {
    	List<String> roots = new ArrayList<String>();
    	
    	for(String node : graph.getAllRoots() ) {
    		if(graph.indeg(node) == 0) {
    			roots.add(node);
    		}
    	}
    	return roots;
    }

    /**
     * 
     */
    public void print_header(Writer writer) throws IOException {
    	 writer.write("%%  autogenerated by Domgraph/Java (see utool.sourceforge.net for details)\n");
    	
    }

    /**
     * 
     */
    public void print_footer(Writer writer) throws IOException {
    	 writer.flush();

    }

    /**
     * 
     */
    public void print_start_list(Writer writer) throws IOException {
    	  writer.write( "[\n");

    }

    /**
     * 
     */
    public void print_end_list(Writer writer) throws IOException {
    	  writer.write("]\n");

    }

    /**
     * 
     */
    public void print_list_separator(Writer writer) throws IOException {
        writer.write(System.getProperty("line.separator"));

    }
    
    /**
     * Checks whether a String has to be preceeded by "_"
     * to form a valid PROLOG variable name; if so, it returns
     * the modified string, otherwise the string itself.
     * 
     * @param var the original string
     * @return the string as valid prolog variable
     */
    private String varifyIfNecessary(String var) {
    	String ret = "";
    	if( (var != null) && var.length() > 0 ) {
    		if( Character.isUpperCase(var.charAt(0)) ||
    				var.startsWith("_")) {
    			ret = var;
    		} else {
    			ret = "_" + var;
    		}
    	}
    	return ret;
    }
    
    /**
     * Checks whether a String has to be enclosed by "\'"
     * to form a valid PROLOG atom; if so, it returns
     * the modified string, otherwise the string itself.
     *  
     *  TODO correct the test for valid ASCII characters
     *  
     * @param var the original String
     * @return the String as valid prolog atom
     */
    private String atomifyIfNecessary(String var) {
    	
    	String ret = var;
    	
    	if( var != null ) {
    		char[] chars = var.toCharArray();
    		for( int i = 0; i < chars.length; i++ ) {
    			if( i == 0 ) {
    				if(Character.isUpperCase(chars[i]) ||
    						Character.isDigit((chars[i]))) {
    					
    					return "\'" + var + "\'";
    				}
    			}
    			if(! ((Character.isLetterOrDigit(chars[i]) && 
    					( (-1 < (int) chars[i]) && ((int) chars[i] < 128) ))) ) {
    				// TODO correct this! obviously this does not accept "_",
    				// at least not each time it should do so.
    				return "\'" + var + "\'";
    			}
    		}
    	}
    	return ret;	
    }
    
    /**
     * Checks whether a String represents a logical
     * label.
     * @param exp the expression
     * @return true if it is a logical label.
     */
    private boolean isLogicalLabel(String exp) {
    	return ("some".equals(exp) || "all".equals(exp)
    			|| "and".equals(exp) || "or".equals(exp) 
    			|| "imp".equals(exp) || "not".equals(exp));
    }
    
    /**
     * Checks whether a graph is applicable for holesem-comsem
     * conversion.
     * 
     * TODO revise me!!!
     * 
     * @param graph the graph
     * @param labels the node labels 
     * @return true if the graph can be converted
     */
    private boolean encodeGraphApplicable(DomGraph graph, 
    		NodeLabels labels) {
    	
    	for( String node : graph.getAllNodes() ) {
    		if( graph.getChildren(node, null).size() > 2 ) 
    			return false;
    		
    		if( graph.getData(node).getType() == NodeType.LABELLED ) {
    			
    			if( graph.getAdjacentEdges(node).size() == 0 )
    			return false;
    		
    		}	
    	}
    	
    	return graph.isHypernormallyConnected();
    }

}
