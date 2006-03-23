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
    public static String getName() {
        return "holesem-comsem";
    }
    
    public static String getExtension() {
        return ".hs.pl";
    }

    
    
    public void encode_graph(DomGraph graph, NodeLabels labels, Writer writer)
            throws IOException, MalformedDomgraphException {
        
        if( !graph.isNormal() || !graph.isHypernormallyConnected() ) {
            throw new MalformedDomgraphException();
        }
        
        int closeBrackets = 0;
        StringBuffer content = new StringBuffer();
        String top = null;
        boolean naked_top_hole = false;
        Set<String> labelledLeaves = new HashSet<String>();
        List<String> conjunction = new ArrayList<String>();
        List<String> hs_labels = new ArrayList<String>();
        Map<String,String> var_nodes = new HashMap<String,String>();
        String topname = "_utool_top_";
        String supertopname = "_utool_top_1_";
        
                
        if( getTopNodes(graph).size() !=  1 ) {
        	content.append("some(" + supertopname + ",and(label(" +
        			supertopname + "),");
        	content.append("some(" + topname + ",and(hole(" +
        			topname + "),");
        	conjunction.add("top(" + supertopname + "," + topname + ")");
        	
        	closeBrackets += 4;
        } else {
        
        	top = getTopNodes(graph).get(0);
        	topname = varifyIfNecessary(top);
        }
        
        for( String node : graph.getAllNodes() ) {
        	NodeData data = graph.getData(node);
        	String nodeName = varifyIfNecessary(node);
        	
        	if( data.getType() == NodeType.UNLABELLED ) {
        		content.append("some(" +  nodeName +
        				",and(hole(" + nodeName + "),");
        		
        		closeBrackets += 2;
        		if(graph.getInEdges(node, null).size() == 0) {
        			naked_top_hole = true;
        		}
        	} else if(graph.getOutEdges(node, null).size() == 0) {
    			labelledLeaves.add(node);
    		} else {
    			content.append("some(" +  nodeName +
        				",and(label(" + nodeName + "),");
    			
    			closeBrackets += 2;
    			hs_labels.add(node);
    		}
        }
        
        for( String node : hs_labels ) {
        	NodeData data = graph.getData(node);
        	String nodeName = varifyIfNecessary(node);
        	String label = labels.getLabel(node);
        	
        	List<String> children = graph.getChildren(node,EdgeType.TREE);
        	
        	
        	if( (top == null) || naked_top_hole ) {
        		conjunction.add("leq(" + nodeName + "," + topname + ")");
        	}
        	        	
        	StringBuffer nextConjunct = new StringBuffer();
        	
        	if( isLogicalLabel(label)) {
        		
        		nextConjunct.append(atomifyIfNecessary(label));
        		
        		nextConjunct.append("(" + nodeName);
        	} else {
        		nextConjunct.append("pred" + children.size() 
        				+ "(" + nodeName + ","
        				+ atomifyIfNecessary(label));
        		
        	}
        	
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
        
        for( String conjunct : conjunction ) {
        	if( i < len -1 ) {
        		content.append("and(" + conjunct + ",");
        		closeBrackets++;
        	} else {
        		content.append(conjunct);
        	}
        	i++;
        }
        
        for( int h = 0; h < closeBrackets; h++ ) {
        	content.append(")");
        }
       content.append("\n");
        writer.write(content.toString());
        writer.flush();
   //     throw new UnsupportedOperationException();
        // TODO Auto-generated method stub

    }
    
    private List<String> getTopNodes(DomGraph graph) {
    	List<String> roots = new ArrayList<String>();
    	
    	for(String node : graph.getAllRoots() ) {
    		if(graph.indeg(node) == 0) {
    			roots.add(node);
    		}
    	}
    	return roots;
    }

    @Override
    public void print_header(Writer writer) throws IOException {
    	 //writer.write("%%  autogenerated by Domgraph/Java (see utool.sourceforge.net for details)\n");
    	
    }

    @Override
    public void print_footer(Writer writer) throws IOException {
    	 writer.flush();

    }

    @Override
    public void print_start_list(Writer writer) throws IOException {
    	  writer.write( "[\n");

    }

    @Override
    public void print_end_list(Writer writer) throws IOException {
    	  writer.write("]\n");

    }

    @Override
    public void print_list_separator(Writer writer) throws IOException {
        writer.write(System.getProperty("line.separator"));

    }
    
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
    					( (-1 < chars[i]) && (chars[i] < 128) ))) ) {
    				return "\'" + var + "\'";
    			}
    		}
    	}
    	return ret;	
    }
    
    private boolean isLogicalLabel(String exp) {
    	return ("some".equals(exp) || "all".equals(exp)
    			|| "and".equals(exp) || "or".equals(exp) 
    			|| "imp".equals(exp) || "not".equals(exp));
    }
    
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
