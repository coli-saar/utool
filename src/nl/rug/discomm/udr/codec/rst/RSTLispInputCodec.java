package nl.rug.discomm.udr.codec.rst;

/**
 * TODO 
 * - index holes with depth; figure out, how their pluggings and the depth relate
 * - make sure that...
 * 		- everything is plugged in the right hole (at the right depth)
 * 		- not too many fragments are created (this might happen because of unnecessary "sandwich fragments")
 * - strip off Marcu's textmarkers ("_!")
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import org._3pq.jgrapht.Edge;

import de.saar.chorus.domgraph.codec.CodecMetadata;
import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.ParserException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeData;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeData;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.graph.NodeType;

@CodecMetadata(name="rst-lisp-input", extension=".dis", experimental=true)
public class RSTLispInputCodec extends InputCodec {

	private Stack<String> holesToPlug; 
	private Stack<RelationTag> brackets;
	private List<Map<String, String>> multinucs;
	private Map<String, Integer> rootsToDepth;
	private Set<String> relationIndicators;
	private final String TEXT = "text", LEAF = "leaf", NUCLEUS = "Nucleus", SATELLITE ="Satellite",
						SPAN="span", ROOT="Root";
	private boolean nextIsAttr, inNuc, forceText;
	private List<String> namesFromTheLeft, rootsToName; //TODO rename me.
	
	private int relindex, leafindex, depth;
	public RSTLispInputCodec() {
		holesToPlug = new Stack<String>();
		brackets = new Stack<RelationTag>();
		rootsToName = new ArrayList<String>();
		namesFromTheLeft = new ArrayList<String>();
		namesFromTheLeft.add(null);
		rootsToName.add(null);
		relationIndicators = new HashSet<String>();
		relationIndicators.add(NUCLEUS);
		relationIndicators.add(ROOT);
		rootsToDepth = new HashMap<String, Integer>();
		nextIsAttr = false;
		inNuc = false;
		forceText = false;
		relindex = 1;
		leafindex = 0;
		multinucs = new ArrayList<Map<String, String>>();
		depth = 0;
	}
	
	@Override
	public void decode(Reader reader, DomGraph graph, NodeLabels labels)
			throws IOException, ParserException, MalformedDomgraphException {
		
		
		
		BufferedReader r = new BufferedReader(reader);
		StringBuffer content = new StringBuffer();
		String line = r.readLine();
		while(line != null) {
			content.append(line);
			line = r.readLine();
		}
		

		StringTokenizer tok = new StringTokenizer(content.toString(), "( )\n\t",true);
		String current = "";
		while(tok.hasMoreTokens()) {
			current = tok.nextToken();
			
			if( current.equals("(") && (! forceText) ) {
				brackets.push(new RelationTag(depth));
				nextIsAttr = true;
			} else if( current.equals(")") ) {
				RelationTag gone = brackets.pop();
				if(gone.attribute.equals(NUCLEUS)) {
					depth--;
					
				} else if(gone.attribute.equals(SATELLITE)) {
					depth--;
				}  else if(gone.attribute.equals("Root")) {
				
					System.err.println(rootsToName);
				}
				System.err.println("popping " + gone.attribute);
			} else if( current.equals(" ") ) {
				continue;
			} else {
				
				if(nextIsAttr) {
					brackets.peek().setAttribute(current);
					if( current.equals(NUCLEUS) ) {
						depth++;
						if( namesFromTheLeft.size() <= depth ) {
							namesFromTheLeft.add(depth, null);
						}
						if(rootsToName.size() <= depth ) {
							rootsToName.add(depth,null);
						}
			
						brackets.peek().de = depth;
						inNuc = true;
					} else if( current.equals("text") ) {
						forceText = true;
					} else if( current.equals(SATELLITE) ) {
						depth++;

						System.err.println("Sat at depth " + depth);
						if( namesFromTheLeft.size() <= depth ) {
							namesFromTheLeft.add(depth, null);
						}
						if(rootsToName.size() <= depth ) {
							rootsToName.add(depth,null);
						}
						inNuc = false;
					} else if( current.equals(ROOT)) {
					}
					nextIsAttr = false;
				} else {
					String attr = brackets.peek().getAttribute();
					if(attr.equals(SPAN)) {
						//inLeaf = false;
						if( inNuc ) {
							// create a new relation fragment;
							// push its holes (in reverse order) on the stack
						//	createRelationFragment(graph);
						}
						
					} else if( attr.equals(ROOT) ) {
						
					} else if( attr.equals("rel2par") ) {
							if( current.equals("span") ) {
								createRelationFragment(graph);
								System.err.println("Span --> Fragment: " + rootsToName.get(depth));
								if(namesFromTheLeft.get(depth) != null) {
									String root = rootsToName.set(depth,null);
									System.err.println(root + " ---> " + current);
									System.err.println(rootsToName);
									labels.addLabel(root, namesFromTheLeft.remove(depth));
									
								}
								
								
							} else {
								if(inNuc) {
									RelationTag tag = brackets.peek();
									tag.setMultinucRelation(current);
									int de = tag.de;
									// multinuclear relation
									if(multinucs.size() > de &&
											multinucs.get(de) != null &&
											multinucs.get(de).containsKey(current)) {
										String root = multinucs.get(de).get(current);	

											if(graph.getOpenHoles(root).isEmpty()) {
												makeSandwichFragment(root, graph, labels);
											}
									} else {
										System.err.println("No key for " + tag.multinucRelation +
												" of depth " + tag.de);
										createRelationFragment(graph);
										System.err.println("nuc --> Fragment: " + rootsToName.get(depth));
										String root = rootsToName.set(depth,null);
										labels.addLabel(root, current);
										
										
										Map<String,String> rootToRel;
										
										if(multinucs.size() <= de ) {
											for(int i = multinucs.size(); i <= de; i++) {
												multinucs.add(i,new HashMap<String,String>());
											}
									
										} 
										
										rootToRel = multinucs.get(de);
										rootToRel.put(current, root);
										
										
										
										
										
										System.err.println(root + " ---> " + current);
										
									}
								} else {
									if(rootsToName.get(depth) == null) {
										namesFromTheLeft.add(depth, current);
									} else {
										String root = rootsToName.set(depth,null);
										System.err.println(root + " ---> " + current);
										System.err.println(rootsToName);
										labels.addLabel(root, current);
									}
								}
								
							}
							// the check avoids adding two labels in case
							// we have a multinuclear relation
						
							//System.out.println(currentRoot + " --> " + current);
							//labels.addLabel(currentRoot, current);
						
					} else if( attr.equals(TEXT) ) {
						// create the label of the current leaf
						
						String leaf = "y" + leafindex;
						graph.addNode(leaf, new NodeData(NodeType.LABELLED));
						graph.addEdge(holesToPlug.pop(), leaf, new EdgeData(EdgeType.DOMINANCE));
						leafindex++;
						
						StringBuffer label = new StringBuffer();
						
						int br = 0;
						while(! (current.equals(")") && br == 0) ) {
							if(current.equals("(")) {
								System.err.println("Klammer auf.");
								br++;
							} else if(current.equals(")")) {
								System.err.println("Klammer zu.");
								br--;
							} 
							label.append(current);
							current = tok.nextToken();
						}
				
						labels.addLabel(leaf, label.toString());
						forceText = false;
						RelationTag match = brackets.pop();
						System.err.println("After \"" + label + "\": popping " + match.attribute);
						if(match.attribute.equals(NUCLEUS) ) {
							inNuc = false;
							depth--;
						} else {
							
						}
					} else if( attr.equals(LEAF) ) {
						// create a fragment consisting of a single node
					
					//	inLeaf = true;
					}
				}
			}
			
			
		}
		
	}
	
	
	private void makeSandwichFragment(String root, DomGraph graph, NodeLabels labels) {
		System.out.println("Hallo.");
		List<String> children = graph.getChildren(root, EdgeType.TREE);
		List<Edge> edges = graph.getOutEdges(root, EdgeType.TREE);
		for(Edge edge : edges) {
			graph.remove(edge);
		}
		int  d = rootsToDepth.get(root);
		String sandwich = root + "mc" + d ;
		
		d++;
		rootsToDepth.put(root, d);
		graph.addNode(sandwich, new NodeData(NodeType.LABELLED));
		labels.addLabel(sandwich, labels.getLabel(root));
		System.err.println(sandwich + " -s-> " + labels.getLabel(root));
		
		for(String child : children) {
			graph.addEdge(sandwich,child, new EdgeData(EdgeType.TREE));
		}
		String plughole = root + "l" + d;
		graph.addNode(plughole, new NodeData(NodeType.UNLABELLED));
		graph.addEdge(root, plughole, new EdgeData(EdgeType.TREE));
		graph.addEdge(plughole,sandwich, new EdgeData(EdgeType.DOMINANCE));
		
		String hole = root + "r" + d;
		graph.addNode(hole, new NodeData(NodeType.UNLABELLED));
		graph.addEdge(root, hole, new EdgeData(EdgeType.TREE));
		holesToPlug.push(hole);
	}
	
	private void createRelationFragment(DomGraph graph) {
		String upper_root = "x" + relindex;
		String upper_lefthole = "xl" + relindex;
		String upper_righthole = "xr" + relindex;
		
		rootsToName.set(depth,upper_root);
		rootsToDepth.put(upper_root, 1);
		graph.addNode(upper_root, new NodeData(NodeType.LABELLED));
		
		graph.addNode(upper_lefthole, new NodeData(NodeType.UNLABELLED));
		graph.addNode(upper_righthole, new NodeData(NodeType.UNLABELLED));
		
		graph.addEdge(upper_root, upper_lefthole, new EdgeData(EdgeType.TREE));
		graph.addEdge(upper_root, upper_righthole, new EdgeData(EdgeType.TREE));
		
		if(! holesToPlug.isEmpty() ) {
			graph.addEdge(holesToPlug.pop(), upper_root, new EdgeData(EdgeType.DOMINANCE));
		}
		
		holesToPlug.push(upper_righthole);
		holesToPlug.push(upper_lefthole);
		relindex++;
	}
	
	private class RelationTag implements Comparable<RelationTag>{
		
		
		

		String multinucRelation;
		String attribute;
		int de;
		
		RelationTag(int d) {
			de = d;
			attribute = "";
			multinucRelation = "";
		}
		
	
		
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof RelationTag) {
				
				System.err.println("Depth: " + de + "   <->   " + ((RelationTag)obj).de);
				return ((RelationTag)obj).multinucRelation.equals(multinucRelation) &&
				((RelationTag)obj).de == de;
			} else {
				return false;
			}
		}





		@Override
		public int hashCode() {
			// TODO Auto-generated method stub
			return super.hashCode();
		}
		
		public int compareTo(RelationTag o) {
			if(de == o.de 
					&& multinucRelation.equals(o.multinucRelation)) {
				return 0;
			} else if(de < o.de) {
				return -1;
			} else {
				return 1;
			}
		}

		public String getAttribute() {
			return attribute;
		}

		public void setAttribute(String attribute) {
			this.attribute = attribute;
		}

		
		void setMultinucRelation(String s) {
			multinucRelation = s;
		}
		
		String getMultinucRelation() {
			return multinucRelation;
		}
	}

}
