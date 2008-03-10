package nl.rug.discomm.udr.structurecheck;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.rug.discomm.udr.chart.IntegerChart;
import nl.rug.discomm.udr.chart.IntegerCheapestSolvedFormComputer;
import nl.rug.discomm.udr.chart.IntegerChart.IntSplit;
import nl.rug.discomm.udr.codec.urml.URMLInputCodec;
import nl.rug.discomm.udr.graph.Chain;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.CompleteSplitSource;
import de.saar.chorus.domgraph.chart.GraphBasedNonterminal;
import de.saar.chorus.domgraph.chart.Split;
import de.saar.chorus.domgraph.chart.SubgraphNonterminal;
import de.saar.chorus.domgraph.chart.wrtg.CheapestSolvedFormComputer;
import de.saar.chorus.domgraph.chart.wrtg.RealSemiring;
import de.saar.chorus.domgraph.chart.wrtg.TropicalSemiring;
import de.saar.chorus.domgraph.chart.wrtg.WeightedRegularTreeGrammar;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.EdgeType;
import de.saar.chorus.domgraph.graph.NodeLabels;

/**
 * TODO
 * * read everything with chain codec
 * * add weights for constraints
 * * compute best conf.
 * * model check best conf.
 * * recall / precision
 * 
 * @author Michaela Regneri
 *
 */
public class ConstraintEvaluation {
	private String testDirectory;
	
	private  File folder;
	private StringBuffer out;
	private static List<String> orderedRoots;
	private static List<String> orderedLeaves;
	private double averageProportion, baselineRandom, baselineRightbranch;
	private int filecounter;
	
	public ConstraintEvaluation(String dir) {
		testDirectory = dir;
		averageProportion = 0.0;
		baselineRandom = 0.0;
		baselineRightbranch = 0.0;
		filecounter = 0;
		folder = new File(dir);
		orderedRoots = new ArrayList<String>();
		orderedLeaves = new ArrayList<String>();
		out = new StringBuffer();
	}
	
	private static boolean cachedReachable(String upper, String lower, DomGraph graph,
			Set<String> visited,Map<String, Set<String>> cache) {
		   if( upper.equals(lower) ) {
	            return true;
	        }
	        if( !visited.add(lower) ) {
	            return false;
	        }
	        
	        if(cache.containsKey(upper) ) {
	        	if(cache.get(upper).contains(lower)) {
	        		return true;
	        	} 
	        } else {
	        	cache.put(upper, new HashSet<String>());
	        }


	        for( String node : graph.getParents(lower, null) ) {
	            if( cachedReachable(upper, node,graph, visited, cache) ) {
	            	cache.get(upper).add(node);
	                return true;
	            }
	        }

	        return false;
	}
	
	public static int countCommonDominances(DomGraph gold, DomGraph checked, int length,
			List<String> rootOrdering) {
		Map<String, Set<String>> goldCache = new HashMap<String, Set<String>>();
		Map<String, Set<String>> chainCache = new HashMap<String, Set<String>>();
		int ret = 0;
		
		
		for(int i = 0; i < length; i++) {
			String goldRoot = rootOrdering.get(i);
			String checkRoot = (i+1) + "x";
			
			for(int h = 0; h < length; h++) {
				if(h != i) {
					String dominator, chaindom;
					if(h < i) {
						 dominator = gold.getHoles(goldRoot).get(0);
						 chaindom = checkRoot + "l";
					} else {
						 dominator = gold.getHoles(goldRoot).get(1);
						 chaindom = checkRoot + "r";
					}
					
					if(gold.getChildren(dominator, EdgeType.DOMINANCE).contains(rootOrdering.get(h)) &&
							checked.getChildren(chaindom, EdgeType.DOMINANCE).contains((h+1) + "x")) {
								ret++;
							}
					
				/*	if(cachedReachable(goldRoot, rootOrdering.get(h), gold, 
							new HashSet<String>(), goldCache) && 
							cachedReachable(checkRoot, (h+1) + "x", checked, 
									new HashSet<String>(), chainCache) ) {
						ret++;
					} */
				}
			}
		}
		return ret;
	}
	
	public static int maxSubtreeSize(DomGraph gold, DomGraph checked, int length,
			List<String> rootOrdering, Set<String> constnodes, Set<String> leaves, NodeLabels nodeLabels) {
		Map<String,Set<String>> maxSubNodecounts  = new HashMap<String, Set<String>>();
		int ret = 0, maxRoot = 1;
		for(int i = 0; i < length; i++) {
			int nexttree = recMaxSubtree(gold, rootOrdering.get(i), checked,
					(i+1) + "x",maxSubNodecounts,rootOrdering);
			if(nexttree > ret) {
				maxRoot = i+1;
				ret = nexttree;
			}
			
			
			
		}
		if(maxSubNodecounts.containsKey(maxRoot + "x")) {
		for(String stn : maxSubNodecounts.get(maxRoot + "x")) {
			System.err.println(nodeLabels.getLabel(stn));
			if(stn.endsWith("y")) {
				leaves.add(stn);
				continue;
			} 
			for(InterdependencyConstraint cons : InterdependencyConstraint.values()) {
				if(nodeLabels.getLabel(stn).equals(cons.getRelationName())) {
					constnodes.add(stn);
					break;
				}
			}
		}
		}
		return ret;
	}
	
	private static int recMaxSubtree(DomGraph gold, String currentGoldRoot, 
			DomGraph checked, String currentChainRoot, 
			Map<String,Set<String>> maxSubNodecounts,List<String> rootOrdering) {
		
		if(maxSubNodecounts.containsKey(currentChainRoot) ) {
			return maxSubNodecounts.get(currentChainRoot).size();
		}
		if(currentChainRoot.endsWith("y")) {
			Set<String> leafset = new HashSet<String>();
		    leafset.add(currentChainRoot);
			maxSubNodecounts.put(currentChainRoot, leafset);
			return 1;
		}
		int count = 1;
	
		List<String> holes = checked.getHoles(currentChainRoot);
		List<Integer> children = new ArrayList<Integer>();
		List<Boolean> leaf = new ArrayList<Boolean>();
		for(String hole : holes) {
		
			String child = checked.getChildren(hole, EdgeType.DOMINANCE).get(0);
			if(child.endsWith("y")) {
				leaf.add(true);
			} else {
				leaf.add(false);
			}
			children.add(Integer.parseInt(child.substring(0, child.length() -1) ));
		}

		List<String> goldholes = gold.getHoles(currentGoldRoot);
		List<String> goldchildren = new ArrayList<String>();
		for(String hole : goldholes) {
			goldchildren.add(gold.getChildren(hole, EdgeType.DOMINANCE).get(0));
		}
		Set<String> iteration = new HashSet<String>();
		iteration.add(currentChainRoot);
		for(int i = 0; i<=1;i++) {
			if(leaf.get(i)) {
				//if(orderedLeaves.get(children.get(i)).equals(goldchildren.get(i))) {
					iteration.add(children.get(i) + "y");
					count += 1;
				//rootOrdering}
			} else {
				if(rootOrdering.get(children.get(i) -1).equals(goldchildren.get(i))) {
					
					count += recMaxSubtree(gold, goldchildren.get(i), checked, 
							children.get(i) + "x",maxSubNodecounts,rootOrdering);
					iteration.addAll(maxSubNodecounts.get(children.get(i) + "x"));
				}
			}
		}
		maxSubNodecounts.put(currentChainRoot, iteration);
		
		return count;
	}
	
	private boolean evaluateUtoolFile(File file) throws Exception {
		DomGraph graph = new DomGraph();
		NodeLabels labels = new NodeLabels();
		
		URMLInputCodec codec = new URMLInputCodec(false);
		 codec.decode(new FileReader(file), graph, labels);
		 
		 Chain chain = createChain(graph, labels);
		 NodeLabels chainlabels = chain.getStandardLabels();
		 //Chart chart = new Chart(chainlabels);
		 WeightedRegularTreeGrammar<SubgraphNonterminal, Double> chart =
				new WeightedRegularTreeGrammar<SubgraphNonterminal, Double> (new RealSemiring());
		 
		 long time = System.currentTimeMillis();
		 try {
		 ChartSolver.solve(chain.preprocess(), chart,new CompleteSplitSource(chain));
		 long solve = System.currentTimeMillis() - time;
		 
		 time = System.currentTimeMillis();
		 for(Split<SubgraphNonterminal> split : chart.getAllSplits()) {
			 for(InterdependencyConstraint constraint : InterdependencyConstraint.values()) {
				 if(constraint.addToSplit(split, chart, chainlabels)) {
					 continue;
				 }
				
			 }
		 }
		 long cadd = System.currentTimeMillis() - time;
		 
		 time = System.currentTimeMillis();
		 CheapestSolvedFormComputer<SubgraphNonterminal, Double> comp = 
				new CheapestSolvedFormComputer<SubgraphNonterminal, Double>(chart,chain);
		 
		 comp.getCheapestSolvedForm();
		 long extr = System.currentTimeMillis() - time;
		 
	//	 out.append(chain.getLength() + "\t" + solve);
		 out.append(chain.getLength() +  "\t" + extr + "\t" + cadd);
		 out.append(System.getProperty("line.separator"));
		 return true;
		 } catch( OutOfMemoryError e ) {
				return false;
		}
	}
	
	private class FileComparator implements Comparator<File> {

		public int compare(File arg0, File arg1) {
			// TODO Auto-generated method stub
			return Long.signum(arg0.length() - arg1.length());
		}
		
	}
	
	public void evaluate() throws Exception {
		 File[] files = folder.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if( name.endsWith(".urml.xml") ) {
						return true;
					} else {
						return false;
					}
					
				}});
		 
		 Arrays.sort(files, new FileComparator());
		 for( int i = 0; i< files.length; i++ ) {
			 
			 String fn = files[i].getName();
		//	 System.err.println();
		//	 System.err.println("Evaluating  " + fn + "...");
			 // for( int i = 0; i<2; i++ ) {	
	//		if(files[i].length() < 40000) {
			
			 clear();
			 try {
			 if(! evaluateUtoolFile(files[i])) {
				 System.err.println("Last file: " + files[i].getName());
				 break;
			 }
			 } catch(Exception e) {
				 continue;
			 }
			 
			 /*try {
				 evaluateFile(files[i]);
				 
			 } catch(OutOfMemoryError e) {
				 System.err.println("Last file: " + files[i].getName());
				 break;
			 } catch(Exception e) {
				 continue;
			 }*/
			 
	/*		} else {
				System.err.println("too big!");
			} */

		 }
		 
		/* averageProportion = averageProportion / (double) filecounter;
		 baselineRandom = baselineRandom / (double) filecounter;
		 baselineRightbranch = baselineRightbranch / (double) filecounter;
		 System.err.println();
		 System.err.println("Average max. tree contains " + averageProportion + " of the roots.");
		 System.err.println("Baseline random: " + baselineRandom );
		 System.err.println("Baseline right-branching: " + baselineRightbranch);*/
		 
		 FileWriter write = new FileWriter(new File(folder.getAbsolutePath() + File.separator + "UtoolChartRuntimeComplete.stats"));
		 write.write(out.toString());
		 write.close();
				
	}
	
	public void clear() {
		orderedLeaves.clear();
		orderedRoots.clear();
	}
	
	/**
	 * Quick and Dirty; the graph has to be a tree
	 * TODO maintain the mapping!!
	 * @param graph
	 * @return
	 */
	private static Chain createChain(DomGraph graph, NodeLabels labels) {
		
		
		String root = "";
		Map<String, String> multis =
			new HashMap<String,String>();
		for(String node : graph.getAllRoots()) {
			if(graph.indeg(node) == 0) {
				root = node;
				break;
			}
		}
		
		Chain ret = new Chain();
		NodeLabels chainLabels = new NodeLabels();
		recCreateChain(graph, labels, ret, root, chainLabels, -1, multis);
		ret.setStandardLabels(chainLabels);
		
		
		return ret;
	}
	
	/**
	 * returns the maximal index of successor EDUs of "current".
	 * @param graph
	 * @param graphlabels
	 * @param chain
	 * @param current
	 * @param chainLabels
	 * @param lengthIndex
	 * @return
	 */
	private static int recCreateChain(DomGraph graph, NodeLabels graphlabels, Chain chain, 
			String current, NodeLabels chainLabels, int lengthIndex, Map<String,String> multis) {
		int li = lengthIndex;
		if(graph.isLeaf(current) && (! graph.isHole(current))) {
			// an EDU
			if(li > -1) {
				chain.addFragment();
			}
			li++;
			chainLabels.addLabel(li + "y", graphlabels.getLabel(current));
			orderedLeaves.add(current);
			return li;
			
		} else {
			List<String> holes = new ArrayList<String>(graph.getHoles(current) );
			List<String> children = new ArrayList<String>();
			String rootLabel = graphlabels.getLabel(current);
			for(String hole : holes) {
				children.add(graph.getChildren(hole, EdgeType.DOMINANCE).get(0));
			}
			li = recCreateChain(graph, graphlabels, chain, children.get(0), chainLabels, li,multis);
			chainLabels.addLabel( (li +1) + "x", rootLabel );
			orderedRoots.add(current);
			int linext = recCreateChain(graph, graphlabels, chain, children.get(1), chainLabels, li, multis);
			if(rootLabel.endsWith("(1)(2)")) {
				multis.put(rootLabel, (li +1) + "x");
			} else if(rootLabel.endsWith("(2)(2)") ){
				
				chain.addDominanceEdge((li +1) + "xl", multis.remove(rootLabel.substring(0, rootLabel.length() - 3)));
				multis.put(rootLabel, (li +1) + "x");
			}
			return linext;
		}
	}
	
	/**
	 * TODO determine the max. common subtree with the gold tree!
	 * quickly!!!
	 * @param file
	 * @throws Exception
	 */
	private void evaluateFile(File file) throws Exception{
	    StringBuffer line = new StringBuffer();
	    DecimalFormat df = new DecimalFormat("0.0000");
		long time = System.currentTimeMillis();
		filecounter++;
		
		DomGraph graph = new DomGraph();
		NodeLabels labels = new NodeLabels();
		
		URMLInputCodec codec = new URMLInputCodec(false);
		 codec.decode(new FileReader(file), graph, labels);
		 
	//	System.err.println("Loaded tree: " + (System.currentTimeMillis() - time) );
		time = System.currentTimeMillis();
		
		Chain chain = createChain(graph, labels);
		
	//	System.err.println("Created chain: (length " + chain.getLength() + "): " 
	//			+ (System.currentTimeMillis() - time) );
		
		
		NodeLabels chainlabels = chain.getStandardLabels();
		

	//	u.addJDomGraphTab("chain", chain, chainlabels);
		
		IntegerChart iChart = new IntegerChart(chain.getLength(), chain.getAdditionalEdges());
		
		time = System.currentTimeMillis();
		iChart.solve();
	//	System.err.println("solved Chart: " + (System.currentTimeMillis() - time) );
		
		long solve = System.currentTimeMillis() - time;
		time = System.currentTimeMillis();

		
		for(IntSplit split : iChart.getAllSplits()) {
			
			for(InterdependencyConstraint constraint : InterdependencyConstraint.values()) {
				if(chainlabels.getLabel(split.getRoot() + "x").equals(constraint.getRelationName())) {
					constraint.add(split, chainlabels);
				}
			}
		}
		long constraintAddition = System.currentTimeMillis() - time;
		
		line.append(file.getName().substring(
				0, file.getName().length() - 17) + "\t\t" + chain.getLength() + "\t" );
	
		
	//	System.err.println("Added all constraints: " + (System.currentTimeMillis() - time) );
		

		time = System.currentTimeMillis();
		IntegerCheapestSolvedFormComputer iComp = new IntegerCheapestSolvedFormComputer(iChart,
				 new TropicalSemiring());
		
		DomGraph sf = chain.makeSolvedForm(iComp.getCheapestSolvedForm());
		long extract = System.currentTimeMillis() - time;
	//	System.err.println("Extracted best solved form: " + (System.currentTimeMillis() - time) );
		
		
	/*	int roots = graph.getAllRoots().size();
		
		time = System.currentTimeMillis();
		Set<String> constraintNodes = new HashSet<String>();
		Set<String> stLeaves = new HashSet<String>();
		int max = maxSubtreeSize(graph, sf, chain.getLength(), orderedRoots, constraintNodes, stLeaves,chainlabels);
		int cNodes = constraintNodes.size();
		
		System.err.println("Max Subtree: " + max +"(roots: " + roots + "); = " + ((double) max / (double) roots)   );
		
		
		
		int domsum = countCommonDominances(graph, sf, chain.getLength(), orderedRoots);
		System.err.println("Dominances found: " + domsum + "(needed " + (System.currentTimeMillis() - time) + "ms)");
		
		line.append(domsum + "\t");
		//averageProportion += (double) domsum / (double) roots;
		IntegerChart randomBaseline = new IntegerChart(chain.getLength(), chain.getAdditionalEdges());
		randomBaseline.solve();
		for(IntSplit split : randomBaseline.getAllSplits()) {
			split.setLikelihood(Math.random());
		}
		IntegerCheapestSolvedFormComputer brComp = new IntegerCheapestSolvedFormComputer(randomBaseline,
				 new TropicalSemiring());
		
		DomGraph baselineSfRandom = chain.makeSolvedForm(brComp.getCheapestSolvedForm());
		
		int b1 = maxSubtreeSize(graph, baselineSfRandom, 
				chain.getLength(), orderedRoots, new HashSet<String>(),new HashSet<String>(), chainlabels );
		System.err.println("Max Subtree with random baseline: "+ b1 + "; = " + ((double) b1 / (double) roots));
		/*baselineRandom +=  (double) b1 / (double) roots;*/
		
	/*	int dombase1 = countCommonDominances(graph, baselineSfRandom, chain.getLength(), orderedRoots);
		System.err.println("Dominances with random baseline: "+ dombase1 );
		if(dombase1 > 0) {
		baselineRandom +=   (double) dombase1 / (double) domsum ;
		}
		line.append(dombase1 + "\t");
		IntegerChart rightRecursionBaseline = new IntegerChart(chain.getLength(), chain.getAdditionalEdges());
		rightRecursionBaseline.solve();
		
		IntegerCheapestSolvedFormComputer rightComp = new IntegerCheapestSolvedFormComputer(rightRecursionBaseline,
				 new TropicalSemiring());
		DomGraph baselineSfRight = chain.makeSolvedForm(rightComp.getCheapestSolvedForm());
		
		int b2 = maxSubtreeSize(graph, baselineSfRight, 
				chain.getLength(), orderedRoots, new HashSet<String>(), new HashSet<String>(),chainlabels );
		System.err.println("Max Subtree with right-branching baseline: "+ b2 + "; = " + ((double) b2 / (double) roots));
	/*	baselineRightbranch += (double) b2 / (double) roots;*/
		
	/*	int dombase2 = countCommonDominances(graph, baselineSfRight, chain.getLength(), orderedRoots);
		System.err.println("Dominances with right-branch baseline: "+ dombase2 );
		baselineRightbranch +=  (double) dombase2 / (double) domsum ;
		line.append(dombase2 + "\t");
		System.err.println("=====================================");
	/*	for(IntSplit split : iChart.getAllSplits() ) {
			if(split.getLikelihood() < 0.0) {
				System.err.println(split + " " + split.getLikelihood());
			}
		} */
		/*
		 * debug
		 */
	//	line.append(max + "\t" + cNodes + "\t" +stLeaves.size() + "\t" + b1 + "\t" + b2);*/
		
		line.append(solve + "\t" + extract + "\t" + constraintAddition);
		out.append(line);
		out.append(System.getProperty("line.separator"));
		
	
	}
	
	
	enum InterdependencyConstraint {
		ATTR1 {
			String getRelationName() {
				return "attribution(1)";
			}
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
					
				if(split.getRightSubgraph().get(0) == 0 &&
						split.getRightSubgraph().get(1) == 0) {
					split.setLikelihood(  - 0.8966 );
				//	System.err.println("Found: " + split);
					return true;
				}
				
				}
				return false;
			}
			
			void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
			wrtg) {
				addRightLeafUtool(split, wrtg, -0.8966);
			}
			
		}, 
		
		ATTR2 {
			String getRelationName() {
				return "attribution(2)";
			}
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
				if(split.getLeftSubgraph().get(0) == 0 &&
						split.getLeftSubgraph().get(1) == 0) {
					split.setLikelihood( - 0.9369 );
					return true;
				}
				
				}
				return false;
			}
			
			void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
			wrtg) {
				addLeftLeafUtool(split, wrtg, -0.9369);
			}
		},

		CONDITION1 {
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
					
					
					if(split.getLeftSubgraph().get(0) == 0 &&
							split.getLeftSubgraph().get(1) == 0) {
						split.setLikelihood(- 0.7765);
						
						
					} 
					
					if(split.getRightSubgraph().get(0) == 0 &&
							split.getRightSubgraph().get(1) == 0) {
						split.setLikelihood( (split.getLikelihood() - (- 0.8118) ) / 2.0);
					} else if(split.getRightSubgraph().get(0) == 
						split.getRightSubgraph().get(1)) {
						split.setLikelihood( (split.getLikelihood() - 0.75 ) / 2.0);
					}
					return true;
				}
				return false;
			}
			
			void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
			wrtg) {
				addLeftLeafUtool(split, wrtg, -0.7765);
				if(! addRightLeafUtool(split, wrtg, -0.8118)) {
					List<String> holes = split.getAllDominators();
					GraphBasedNonterminal right = split.getWccs(holes.get(1)).get(0);
					// just close your eyes for 3 lines of code.
					if(right.getNodes().size() == 5) {
						wrtg.setWeightForSplit(split, (wrtg.getWeightForSplit(split) - 0.75) / 2.0);
					}
				}
			}
			
			String getRelationName() {
				return "condition(1)";
			}
		},
		CONDITION2 {
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
					if(split.getLeftSubgraph().get(0) == 0 &&
							split.getLeftSubgraph().get(1) == 0) {
						split.setLikelihood( - 0.7097);
						return true;
					} 
				}
				return false;
			}
			
			 void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
				wrtg) {
				 addLeftLeafUtool(split, wrtg, -0.7097);
			 }
			
			String getRelationName() {
				return "condition(2)";
			}
		},
		PURPOSE1 {
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
					if(split.getLeftSubgraph().get(0) == 0 &&
							split.getLeftSubgraph().get(1) == 0) {
						split.setLikelihood(  - 0.9289);
						
					} 
					if(split.getRightSubgraph().get(0) == 0 &&
							split.getRightSubgraph().get(1) == 0) {
						split.setLikelihood( (split.getLikelihood() - 0.7843 ) / 2.0);
					}
					return true;
				}
				return false;
			}
			
			 void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
				wrtg) {
				 addRightLeafUtool(split, wrtg, 0.7843);
			 }
			
			String getRelationName() {
				return "purpose(1)";
			}
		},
		PURPOSE2 {
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
					if(split.getLeftSubgraph().get(0) == 0 &&
							split.getLeftSubgraph().get(1) == 0) {
						split.setLikelihood( - 0.76);
						return true;
					} 
				}
				return false;
			}
			
			
			 void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
				wrtg) {
				 addLeftLeafUtool(split, wrtg, -0.76);
			 }
			String getRelationName() {
				return "purpose(2)";
			}
		},
	/*	CONSEQUENCEN1 {
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
					
				}
			}
			String getRelationName() {
				return "consequence-n(1)";
			}
		}, */
		ELABOA1 {
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
					if(split.getLeftSubgraph().get(0) == 0 &&
							split.getLeftSubgraph().get(1) == 0) {
						split.setLikelihood( - 0.9821);
						
					} 
					
					if(split.getRightSubgraph().get(0) == 0 &&
							split.getRightSubgraph().get(1) == 0) {
						split.setLikelihood( (split.getLikelihood() - 0.8036 ) / 2.0);
					}
					return true;
				}
				return false;
			}
			
			 void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
				wrtg) {
				 addLeftLeafUtool(split, wrtg, -0.9821);
				 addRightLeafUtool(split, wrtg, -0.8036);
			 }
			
			String getRelationName() {
				return "elaboration-object-attribute(1)";
			}
			
		}, 
		
		INTERPRETATIONS1 {
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
					addLeftLeafWeight(split, 1 - 0.1519);
					addRightLeafWeight(split, 1 - 0.1772);
					return true;
				}
				return false;
			}
			
			 void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
				wrtg) {
				 addLeftLeafUtool(split, wrtg, 1-0.1519);
				 addRightLeafUtool(split, wrtg, 1-0.1772);
			 }
			
			String getRelationName() {
				return "interpretation-s(1)";
			}
		},
		
		
		EVALUATIONS1 {
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
					addLeftLeafWeight(split, 1 - 0.1203);
					addRightLeafWeight(split, 1 - 0.1805);
					return true;
				}
				return false;
			}
			
			
			void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
			wrtg) {
			 addLeftLeafUtool(split, wrtg, 1-0.1203);
			 addRightLeafUtool(split, wrtg, 1-0.1805);
		 }
			String getRelationName() {
				return "evaluation-s(1)";
			}
		},
		
		EVIDENCE1 {
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
					
					addRightLeafWeight(split, 1 - 0.0927);
					return true;
				}
				return false;
			}
			
			void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
			wrtg) {
	
			 addRightLeafUtool(split, wrtg, 1-0.0927);
		 }
			
			String getRelationName() {
				return "evidence(1)";
			}
		},
		EXAMPLE1 {
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
					
					addRightLeafWeight(split, 1 - 0.067);
					return true;
				}
				return false;
			}
			
			void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
			wrtg) {
			
			 addRightLeafUtool(split, wrtg, 1-0.067);
		 }
			
			String getRelationName() {
				return "example(1)";
			}
		},
		
		
		
		MANNER1 {
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
					if(split.getLeftSubgraph().get(0) == 0 &&
							split.getLeftSubgraph().get(1) == 0) {
						split.setLikelihood(- 0.7838  );
						return true;
					} 
				}
				return false;
			}
			
			void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
			wrtg) {
			 addLeftLeafUtool(split, wrtg, -0.7838);
			
		 }
			
			String getRelationName() {
				return "manner(1)";
			}
		}, 
		EXPLARG {
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
					
					addRightLeafWeight(split, 1 - 0.1134);
					return true;
				}
				return false;
			}
			
			void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
			wrtg) {
		
			 addRightLeafUtool(split, wrtg, 1-0.1134);
		 }
			
			String getRelationName() {
				return "explanation-argumentative(1)";
			}
		},
		
		MEANS1 {
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
					if(split.getLeftSubgraph().get(0) == 0 &&
							split.getLeftSubgraph().get(1) == 0) {
						split.setLikelihood( - 0.7872 );
						return true;
					} 
				}
				return false;
			}
			
			void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
			wrtg) {
			 addLeftLeafUtool(split, wrtg, -0.7872);
		 }
			
			String getRelationName() {
				return "means(1)";
			}
		},
	/*	RESTATEMENT {
			void add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
					
				}
			}
			String getRelationName() {
				return "restatement(1)";
			}
		}, */

		AFTER {
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
				
					
					if(split.getLeftSubgraph().get(0) == 0 &&
							split.getLeftSubgraph().get(1) == 0) {
						split.setLikelihood( (split.getLikelihood() - 0.8475 ) / 2.0);
						return true;
					}
				}
				return false;
			}
			
			void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
			wrtg) {
			 addLeftLeafUtool(split, wrtg, -0.8475);
		 }
			
			String getRelationName() {
				return "temporal-after(1)";
			}
		},
		ELABADD1 {
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
					addLeftLeafWeight(split, 1 - 0.1707);
					addRightLeafWeight(split, 1 -0.2035 );
					return true;
				}
				return false;
			}
			
			void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
			wrtg) {
			 addLeftLeafUtool(split, wrtg, 1-0.1707);
			 addRightLeafUtool(split, wrtg, 1-0.2035);
		 }
			
			String getRelationName() {
				return "elaboration-additional(1)";
			}
		},
		
		ELABASETMEM1 {
			boolean add(IntSplit split, NodeLabels chainLabels) {
				int root = split.getRoot();
				if(chainLabels.getLabel(root + "x").equals(getRelationName())) {
					
					addRightLeafWeight(split, 1 -0.125 );
					return true;
				}
				return false;
			}
			
			void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
			wrtg) {
			
			 addRightLeafUtool(split, wrtg, 1-0.125);
		 }
			
			String getRelationName() {
				return "elaboration-set-member(1)";
			}
		},

		SAMEUNIT12 {
			String getRelationName() {
				return "Same-Unit(1)(2)";
			}
			boolean add(IntSplit split, NodeLabels chainLabels) {
				if(chainLabels.getLabel(split.getRoot() + "x").equals(getRelationName())) {
					addLeftLeafWeight(split, 1-0.1029);
					return true;
				}
				return false;
			}
			void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
			wrtg) {
			 addLeftLeafUtool(split, wrtg, 1-0.1029);
			
		 }
		} ,
		
		
		TO12 {
			String getRelationName() {
				return "TextualOrganization(1)(2)";
			}
			boolean add(IntSplit split, NodeLabels chainLabels) {
				if(chainLabels.getLabel(split.getRoot() + "x").equals(getRelationName())) {
					addRightLeafWeight(split,1- 0.1639);
					return true;
				}
				return false;
			}
			
			void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
			wrtg) {
			 
			 addRightLeafUtool(split, wrtg, 1-0.1639);
		 }
		}, 
		
		BACKGROUND1 {
			String getRelationName() {
				return "background(1)";
			}
			boolean add(IntSplit split, NodeLabels chainLabels) {
				if(chainLabels.getLabel(split.getRoot() + "x").equals(getRelationName())) {
					addLeftLeafWeight(split, 1- 0.1217);
					addRightLeafWeight(split, 1 - 0.2348);
					return true;
				}
				return false;
			}
			
			void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
			wrtg) {
			 addLeftLeafUtool(split, wrtg, 1-0.1217);
			 addRightLeafUtool(split, wrtg, 1-0.2348);
		 }
			
		} ,
		
		BACKGROUND2 {
			String getRelationName() {
				return "background(2)";
			}
			boolean add(IntSplit split, NodeLabels chainLabels) {
				if(chainLabels.getLabel(split.getRoot() + "x").equals(getRelationName())) {
					addLeftLeafWeight(split, 1- 0.2025);
					addRightLeafWeight(split, 1 - 0.1013);
					return true;
				}
				return false;
			}
			void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
			wrtg) {
			 addLeftLeafUtool(split, wrtg, 1-0.2025);
			 addRightLeafUtool(split, wrtg, 1-0.1013);
		 }
			
		} ,
		
		COMMENT1 {
			String getRelationName() {
				return "comment(1)";
			}
			boolean add(IntSplit split, NodeLabels chainLabels) {
				if(chainLabels.getLabel(split.getRoot() + "x").equals(getRelationName())) {
					addLeftLeafWeight(split, 1- 0.1048);
					addRightLeafWeight(split, 1- 0.2016);
					return true;
				}
				return false;
			}
			void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
			wrtg) {
			 addLeftLeafUtool(split, wrtg, 1-0.1048);
			 addRightLeafUtool(split, wrtg, 1-0.2016);
		 }
		} ,
		
		
		
		;
		abstract boolean add(IntSplit split, NodeLabels chainLabels);
		abstract String getRelationName();
		abstract void add(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
		wrtg);
		boolean addToSplit(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
				wrtg, NodeLabels chainLabels) {
			if(chainLabels.getLabel(split.getRootFragment()).equals(getRelationName())) {
				add(split, wrtg);
				return true;
			}
			return false;
		}
		
		boolean addRightLeafUtool(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
				wrtg, double weight) {
			List<String> holes = split.getAllDominators();
			if(split.getWccs(holes.get(1)).get(0).getNodes().size() == 1) {
				wrtg.setWeightForSplit(split, (wrtg.getWeightForSplit(split) + weight) / 2.0);
				return true;
			}
			return false;
		}

		boolean addLeftLeafUtool(Split<SubgraphNonterminal> split, WeightedRegularTreeGrammar<SubgraphNonterminal, Double>
		wrtg, double weight) {
			List<String> holes = split.getAllDominators();
			if(split.getWccs(holes.get(0)).get(0).getNodes().size() == 1) {
				wrtg.setWeightForSplit(split, (wrtg.getWeightForSplit(split) + weight) / 2.0);
				return true;
			}
			return false;
		}
		
		boolean addRightLeafWeight(IntSplit split, double weight) {
			if(split.getRightSubgraph().get(0) == 0 &&
					split.getRightSubgraph().get(1) == 0) {
				split.setLikelihood( (split.getLikelihood() + weight) / 2.0 );
				return true;
			} 
			return false;
		}
		
		void addLeftLeafWeight(IntSplit split, double weight) {
			if(split.getLeftSubgraph().get(0) == 0 &&
					split.getLeftSubgraph().get(1) == 0) {
				split.setLikelihood( (split.getLikelihood() + weight) / 2.0 );
				
			} 
		}
	}
	
	public static void main(String[] args) {
		String testfile = 
			"/Users/Michaela/Studium/" +
			"MSc/Thesis/Pre-Experiments/RSTDT/" +
			"rst_discourse_treebank/data/RSTtrees-WSJ-main-1.0/ALL" ;
		
		try {
			ConstraintEvaluation ev = new ConstraintEvaluation(testfile);
		//	ev.evaluateFile(new File(testfile + "/wsj_1346.out.dis.urml.xml"));
			ev.evaluate();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	/*	DomGraph graph = new DomGraph();
		NodeLabels labels = new NodeLabels();
		try {
		URMLInputCodec codec = new URMLInputCodec(false);
		 codec.decode(new FileReader(testfile), graph, labels);
		 Chain chain = createChain(graph, labels);
		 Ubench u = Ubench.getInstance();
		 u.addJDomGraphTab("the tree", graph, labels);
		 u.addJDomGraphTab("the chain", chain, chain.getStandardLabels());
		} catch(Exception e) {
			e.printStackTrace();
		}*/
	}

}
