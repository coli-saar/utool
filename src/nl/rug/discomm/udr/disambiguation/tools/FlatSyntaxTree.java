package nl.rug.discomm.udr.disambiguation.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.rug.discomm.udr.graph.Chain;
import danbikel.lisp.Sexp;
import danbikel.lisp.SexpList;
import danbikel.lisp.SexpTokenizer;
import danbikel.parser.util.Util;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class FlatSyntaxTree {
	private Chain graph;
	private NodeLabels graphlabels;
	
	private static final String subClause = "SBAR";
	
	Map<String, List<String>> nodeToTerminals;
	Map<List<String>, String> terminalsToNode;
	Map<List<Integer>, List<Integer>> dominances;
	Map<Sexp, List<Integer>> subtreesToNodes;
	
	public FlatSyntaxTree(Chain dg, NodeLabels labels) {
		graph = dg;
		graphlabels = labels;
		
		nodeToTerminals =  new HashMap<String, List<String>>();
		terminalsToNode = new HashMap<List<String>, String>();
		dominances = new HashMap<List<Integer>, List<Integer>>();
		subtreesToNodes = new HashMap<Sexp, List<Integer>>();
		
		for(int i = 0; i <= dg.getLength(); i++) {
			String node = i + "y";
			String label = graphlabels.getLabel(node);
			
			List<String> wordsAsList = new ArrayList<String>(Arrays.asList(label.split(" |_")));
			if(wordsAsList.get(wordsAsList.size()-1).endsWith(".<P>")) {
				String finalWord = wordsAsList.get(wordsAsList.size() - 1);
				finalWord = finalWord.substring(0, finalWord.length() - 4);
				
				wordsAsList.remove(wordsAsList.size() - 1);
				wordsAsList.add(finalWord);
			}
			
			nodeToTerminals.put(node, wordsAsList);
			terminalsToNode.put(wordsAsList, node);
		}
	}
	
	public Map<List<Integer>, List<Integer>> getDominances() {
		return dominances;
	}
	
	
	private int dfs(Sexp current, Set<Sexp> visited, List<Integer> collected, 
			 int currentNode) {
		if(! visited.contains(current)) {
			visited.add(current);
			if(current.isList()) {
				SexpList list = current.list();
				if( list.size() == 1) {
					return dfs(list.get(0), visited, collected, currentNode);
				} else {
					String node = currentNode + "y";
					String label = graphlabels.getLabel(node);
					if(label.endsWith("<P>")) {
						label = label.substring(0,label.length() - 4);
					}
				//	System.err.println(label);
				//	System.err.println(Util.collectLeaves(current).toString());
					if(label.replaceAll("\\W|_", "").matches(
							Util.collectLeaves(current).toString().replaceAll("\\W|_", ""))) {
						List<Integer> base = new ArrayList<Integer>();
						base.add(currentNode);
						subtreesToNodes.put(current, base);
						
						collected.add(currentNode);
						return currentNode +1;
					} else {
						List<Integer> myChildren = new ArrayList<Integer>();
						int nextNode = currentNode;
						Children : for(int i = 0; i< list.length(); i++) {
							Sexp nextChild = list.get(i);
							int tmp = currentNode;
							while(! nodeToTerminals.get(tmp + "y").get(0).replaceAll("\\W|_", "")
									.equals(nextChild.toString().replaceAll("\\W|_", ""))) {
								tmp++;
								if(tmp > graph.getLength()) {
									continue Children;
								}
							}
							nextNode = dfs(list.get(i), visited, myChildren,
									tmp);
						}
						collected.addAll(myChildren);
						subtreesToNodes.put(current, new ArrayList<Integer>(myChildren));
						return nextNode;
					}
				}
			}
			
		}
		return currentNode;
	}
	
	public void parse(InputStream ptbfile) throws IOException {
		
		SexpTokenizer tok = new SexpTokenizer(new InputStreamReader(ptbfile));
		SexpList current = new SexpList();
		Sexp read = Sexp.read(tok);
		int currentNodeIndex = 0;
		
	
		List<String> terminals = new ArrayList<String>();
		
		
		
		while(read != null) {
			
		current = read.list();
		
		
		String complete = Util.collectLeaves(read).toString().replaceAll("\\W|_", "");
		String first = Util.collectLeaves(read).list().get(0).toString();
		
		List<Integer> nodesForSentence = new ArrayList<Integer>();
		
		int stringIndex = 0;
		while(stringIndex < complete.length() &&
				(currentNodeIndex <= graph.getLength())) {
			terminals = nodeToTerminals.get(currentNodeIndex + "y");
			System.err.println(currentNodeIndex + " " + terminals);
			stringIndex = complete.lastIndexOf(terminals.get(terminals.size() -1).replaceAll("\\W|_", ""));
			if( stringIndex > -1) {
				nodesForSentence.add(currentNodeIndex);
				currentNodeIndex++;
			} else {
				break;
			}
			
		}
		System.err.println(nodesForSentence);
		System.err.println(" == ");
		
		Sexp subcl = findsubClause(current, new HashSet<Sexp>());
		
		if(subcl != null ) {
		String completeSub = Util.collectLeaves(subcl).toString().replaceAll("\\W|_", "");
		int start = complete.lastIndexOf(completeSub);
		List<Integer> sc = new ArrayList<Integer>();
		List<Integer> mc = new ArrayList<Integer>();
		
		if(start == 0) {
			sc.add(nodesForSentence.get(0));
			for(Integer node : nodesForSentence) {
				if(completeSub.endsWith(nodeToTerminals.get(node.toString() + "y").get(
						nodeToTerminals.get(node.toString() + "y").size() -1))) {
					sc.add(node);
					if(node == nodesForSentence.get(nodesForSentence.size() -1)) {
						mc.add(node);
					} else {
						mc.add(node +1);
					}
					
					break;
				}
			}
			mc.add(nodesForSentence.get(nodesForSentence.size()-1));
		} else {
			mc.add(nodesForSentence.get(0)); 
			for(Integer node : nodesForSentence) {
				if(completeSub.startsWith(nodeToTerminals.get(node.toString() + "y").get(
						0))) {
					mc.add(node -1);
					sc.add(node);
					
					break;
				}
			}
			sc.add(nodesForSentence.get(nodesForSentence.size()-1));
		}
		
		dominances.put(mc, sc);
		}
	/*	while(! nodeToTerminals.get(currentNodeIndex + "y").get(0).replaceAll("\\W|_", "")
				.equals(first.replaceAll("\\W|_", ""))) {
			currentNodeIndex++;
		}
		
		currentNodeIndex = 
			dfs(current,new HashSet<Sexp>(),new ArrayList<Integer>(), currentNodeIndex);
		
		
		System.err.println(subtreesToNodes);
		if(current.size() == 1) {
			current = current.get(0).list();
		}
		List<Integer> nodesForSentence = subtreesToNodes.get(current);
		
		if(! nodesForSentence.isEmpty()) {
		int lastStart = nodesForSentence.get(0);
		int lastEnd = nodesForSentence.get(nodesForSentence.size() -1);
		findSubClauses(current, new HashSet<Sexp>(), lastStart, lastEnd);
	
		
		} */
		read = Sexp.read(tok);
	//	nodesForSentence.clear();
		}
	}
	
	
	private void findSubClauses(Sexp current, Set<Sexp> visited, int currentSentenceStart,
			int currentSentenceEnd) {
		if(! visited.contains(current) ) {
			visited.add(current);
			if(current.isList()) {
				SexpList list = current.list();
				if(list.size() == 1) {
					findSubClauses(list.get(0), visited, currentSentenceStart, currentSentenceEnd);
				} else {
					if(list.get(0).isSymbol() &&
							list.get(0).toString().startsWith(subClause)) {
						List<Integer> nodes = subtreesToNodes.get(current);

						if(! nodes.isEmpty() ) {
							int scStart = nodes.get(0);
							int scEnd = nodes.get(nodes.size() -1);
							List<Integer> sc = new ArrayList<Integer>();
							sc.add(scStart);
							sc.add(scEnd);

							List<Integer> mc = new ArrayList<Integer>();
							if(scStart == currentSentenceStart) {
								mc.add(scEnd +1);
								mc.add(currentSentenceEnd);
							} else {
								mc.add(currentSentenceStart);
								mc.add(scStart -1);
							}

							dominances.put(mc, sc);
							for(int i = 1; i < list.length(); i++) {
								if(subtreesToNodes.containsKey(list.get(i))) {
									findSubClauses(list.get(i), visited, scStart, scEnd);
								}
							}
						}
					}
				}
			}
		}
	}
	
	private Sexp findsubClause(Sexp current, Set<Sexp> visited) {
		if(! visited.contains(current)) {
			visited.add(current);
			if( ! current.isList() ) {
				return null;
			} else {
				
				SexpList list = current.list();
				if(list.size() == 1) {
					list = list.get(0).list();
					visited.add(list);
				}
			//	System.err.println("sub claus saearch: current = " + list);

				if(list.get(0).isSymbol() &&
						list.get(0).toString().startsWith(subClause)) {
					return current;
				} else {
					Sexp child =  null;
					for(int i = 1; i < list.length(); i++) {
						child = 
							findsubClause(list.get(i),visited);
						if(child != null) {
							return child;
						}
					}
					return child;
				}
			}
		} else {
			if(current.isList()) {


				SexpList list = current.list();
				Sexp child =  null;
				for(int i = 1; i < list.length(); i++) {
					child = 
						findsubClause(list.get(i),visited);
					if(child != null) {
						return child;
					}
				}
			//	System.err.println("Child in subcl. search: " + child);
				return child;
			} else {
				return null;
			}
		}
	}

}
