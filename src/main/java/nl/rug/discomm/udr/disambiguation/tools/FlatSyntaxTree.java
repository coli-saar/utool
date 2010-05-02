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

import nl.rug.discomm.udr.disambiguation.tools.Cues.Relation;
import nl.rug.discomm.udr.graph.Chain;
import danbikel.lisp.Sexp;
import danbikel.lisp.SexpList;
import danbikel.lisp.SexpTokenizer;
import danbikel.parser.util.Util;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class FlatSyntaxTree {
	private Chain graph;
	private NodeLabels graphlabels;
	
	private static final String subClause = "SBAR-";
	
	private	Map<String, List<String>> nodeToTerminals;
	private Map<List<String>, String> terminalsToNode;
	private Map<List<Integer>, List<Integer>> dominances;
	private Map<Sexp, List<Integer>> subtreesToNodes;
	private Map<Map<List<Integer>, List<Integer>>, Relation> fragmentToRelation;
	
	
	public FlatSyntaxTree(Chain dg, NodeLabels labels) {
		graph = dg;
		graphlabels= labels;
		
		nodeToTerminals =  new HashMap<String, List<String>>();
		terminalsToNode = new HashMap<List<String>, String>();
		dominances = new HashMap<List<Integer>, List<Integer>>();
		subtreesToNodes = new HashMap<Sexp, List<Integer>>();
		fragmentToRelation = new HashMap<Map<List<Integer>, List<Integer>>, Relation>();
		
		
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
	
	
	
	
	public void parse(InputStream ptbfile) throws IOException {
		
		SexpTokenizer tok = new SexpTokenizer(new InputStreamReader(ptbfile));
		SexpList current = new SexpList();
		Set<Sexp> visited = new HashSet<Sexp>();
		Sexp read = Sexp.read(tok);
		int currentNodeIndex = 0;
		
	
		List<String> terminals = new ArrayList<String>();
		
		
		
		while(read != null) {
			
		current = read.list();
		
		
		String complete = Util.collectLeaves(read).toString().replaceAll("\\W|_", "");
		String first = Util.collectLeaves(read).list().get(0).toString();
		
		List<Integer> nodesForSentence = new ArrayList<Integer>();
	//	System.err.println(complete);
		int stringIndex = 0;
		while(stringIndex < complete.length() &&
				(currentNodeIndex <= graph.getLength())) {
			terminals = nodeToTerminals.get(currentNodeIndex + "y");
			
			String label = graphlabels.getLabel(currentNodeIndex + "y");
			if(label.endsWith("<P>")) {
				label = label.substring(0,label.length() - 4);
			}
			label = label.replaceAll("\\W|_", "");

			System.err.println(currentNodeIndex + " " + terminals);
			if(complete.contains(label) ) {
				nodesForSentence.add(currentNodeIndex);
				currentNodeIndex++;
			} else {
				break;
			}
			
		}
		System.err.println(nodesForSentence);
	
		extractSubClause(current, nodesForSentence, visited, new HashSet<Sexp>());
		
		System.err.println(" == ");
		read = Sexp.read(tok);
	//	nodesForSentence.clear();
		}
	}
	
	
	
	public Map<Map<List<Integer>, List<Integer>>, Relation> getMarkedSpans() {
		return fragmentToRelation;
	}

	public void setFragmentToRelation(
			Map<Map<List<Integer>, List<Integer>>, Relation> fragmentToRelation) {
		this.fragmentToRelation = fragmentToRelation;
	}

	private void extractSubClause(Sexp startsexp, List<Integer> nodesForSentence, 
			Set<Sexp> visited, Set<Sexp> visitedDFS) {
		
		Sexp subcl = findsubClause(startsexp, visitedDFS);
	
		String complete = Util.collectLeaves(startsexp).toString().replaceAll("\\W|_", "");
		if(subcl != null && (! visited.contains(subcl)) ) {
			visited.add(subcl);
			
			SexpList leaves = Util.collectLeaves(subcl).list();
			
			String completeSub = leaves.toString().replaceAll("\\W|_", "");
			int start = complete.lastIndexOf(completeSub);
			List<Integer> sc = new ArrayList<Integer>();
			List<Integer> mc = new ArrayList<Integer>();

			if(start == 0) {
				sc.add(nodesForSentence.get(0));
				int scright = sc.get(0);

				for(int i = 1; i < nodesForSentence.size(); i++) {
					int node = nodesForSentence.get(i);
					String label = graphlabels.getLabel(node + "y");
					if(label.endsWith("<P>")) {
						label = label.substring(0,label.length() - 4);
					}
					label = label.replaceAll("\\W|_", "");

					if(completeSub.contains(label) ){
						scright = node;

					} else {
						sc.add(scright);
						break;
					}
				}

				if(sc.size() == 1) {
					sc.add(scright);
					mc.add(scright);
				} else {
					mc.add(scright +1);
				}


				mc.add(nodesForSentence.get(nodesForSentence.size()-1));
			} else {
				mc.add(nodesForSentence.get(0)); 
				int mcright = mc.get(0);
				for(int i = 1; i < nodesForSentence.size(); i++) {

					int node = nodesForSentence.get(i);

					String label = graphlabels.getLabel(node + "y");

					if(label.endsWith("<P>")) {
						label = label.substring(0,label.length() - 4);
					}
					label = label.replaceAll("\\W|_", "");

					if(! completeSub.contains(label) ) {
						mcright = node;

					} else {
						mc.add(mcright);
						break;
					}
				}

				if(mc.size() == 1) {
					mc.add(mcright);
					sc.add(mcright);
				} else {
					sc.add(mcright +1);
				}

				sc.add(nodesForSentence.get(nodesForSentence.size()-1));
			}
			
			
			dominances.put(mc, sc);
			for(Relation rel : Cues.getRelations() ) {
				for(String marker : rel.getMarkers()) {
				if(leaves.toString().contains(marker) ) {
					HashMap<List<Integer>, List<Integer>> dom = new HashMap<List<Integer>, List<Integer>>();
					dom.put(mc, sc);
					fragmentToRelation.put(dom, rel);
				} else {
				//	System.err.println(leaves + " doesn't contain " + marker);
				}
				}
			}
			
			if(sc.size() == 2) {
			List<Integer> newNodes = new ArrayList<Integer>();
			for(int i = sc.get(0); i <= sc.get(1); i++) {
				newNodes.add(i);
			}
			System.err.println("RECURSION");
			extractSubClause(subcl, newNodes, visited, visitedDFS);
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
					System.err.println(list.get(0));
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
