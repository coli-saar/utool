package nl.rug.discomm.udr.chart;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import nl.rug.discomm.udr.chart.IntegerChart.IntSplit;
import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.chart.SolvedFormSpec;
import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.domgraph.graph.DomGraph;

/**
 * An iterator over the different solved forms represented by
 * a {@link IntegerChart}. 
 * This is a subclass of {@link de.saar.chorus.domgraph.chart.SolvedFormIterator} for now
 * to allow integration in Ubench. Most of the code is copied.
 * 
 * TODO either change superclass to Object and implement Iterator<SolvedFormSpec> or
 * 		make some methods in SolvedFormIterator protected s.t. we don't have to duplicate
 * 		them here.
 * 
 * The chart is passed to the constructor of
 * an object of this class. Then you can iterate over the solved
 * forms of this chart using <code>hasNext()</code> and <code>next()</code>
 * as usual.<p>
 * 
 * Each successful call to <code>next()</code> will return an object
 * of class <code>List<{@link DomEdge}></code>, i.e. a list of
 * dominance edge representations. This list can e.g. be passed
 * to the <code>encode</code> method of {@link de.saar.chorus.domgraph.codec.OutputCodec} or
 * one of its subclasses.<p>
 * 
 * This class implements a transition system for states consisting
 * of an agenda of subgraphs that must currently be resolved, and
 * a stack of splits that still need to be processed. This algorithm
 * is dramatically faster than a naive algorithm which simply computes
 * the sets of solved forms of a graph by computing the Cartesian
 * product of the sets of solved forms of its subgraphs, but quite
 * a bit more complicated.
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 *
 */
public class IntSolvedFormIterator extends SolvedFormIterator {
	private IntegerChart chart;
	private Agenda agenda;
	private Stack<EnumerationStackEntry> stack;
    
    private Set<String> roots;
    private String rootForThisFragset;
	
    // the solved form which will be returned by the next call
    // to next()
	private SolvedFormSpec nextSolvedForm;
	
    // a cached list of solved forms for get(int)
	private List<SolvedFormSpec> solvedForms;
    // the iterator used for computing the solved forms
    private IntSolvedFormIterator iteratorForGet;
    
    private boolean chartIsEmpty, returnedSfForEmptyChart;
    
	
    // I need the graph in order to determine the fragments: I need to
    // know the roots of singleton fragsets to create the dom edge.
	public IntSolvedFormIterator(IntegerChart ch) {
        this(ch,true);
	}
    
    private IntSolvedFormIterator(IntegerChart ch, boolean makeIteratorForGet) {
    	super(new Chart(), new DomGraph());
        chart = ch;
        agenda = new Agenda();
        stack = new Stack<EnumerationStackEntry>();
        solvedForms = new ArrayList<SolvedFormSpec>();
        
        if( chart.getToplevelSubgraph().isEmpty() ) {
        	// If the chart has no top-level subgraph, we will generate a single (empty)
        	// solved form.  (If such a chart comes from a solvable graph, the graph must
        	// have been empty.)
        	chartIsEmpty = true;
        	returnedSfForEmptyChart = false;
        } else {

        	if( makeIteratorForGet ) {
        		iteratorForGet = new IntSolvedFormIterator(ch, false);
        	} else {
        		iteratorForGet = null;
        	}

        	roots = new HashSet<String>();

        	roots.add("0y");
        	for(int i = 1; i < chart.getChainlength(); i++) {
        		roots.add( i + "x");
        		roots.add(i + "y");
        	}
        	
        	
        	List<Integer> fragset = chart.getToplevelSubgraph();
        	
        	if( fragset.size() > 0) {
        		agenda.add(new AgendaEntry(null, fragset));
        	}

        	//Null-Element on Stack
        	stack.push( new EnumerationStackEntry(null, new ArrayList<IntSplit>(), null));

        	updateNextSolvedForm();
        }
    }
    
	

    private void updateNextSolvedForm() {
		if( isFinished() ) {
			nextSolvedForm = null;
		} else {
			findNextSolvedForm();
			
			if( representsSolvedForm() ) {
				nextSolvedForm = extractSolvedFormSpec();
			} else {
				nextSolvedForm = null;
			}
		}
	}
	
	private boolean representsSolvedForm(){
		return (agenda.isEmpty() && stack.size() > 0 );
	}
	
	
	private boolean isFinished(){	
		return (agenda.isEmpty() && stack.isEmpty() );
	}
	
	
	private SolvedFormSpec extractSolvedFormSpec() {
		SolvedFormSpec toReturn = new SolvedFormSpec();
		
		for( EnumerationStackEntry ese : stack) {
			toReturn.addAllDomEdges(ese.getEdgeAccu());
		}
		
		return toReturn;
	}
	
	
	private void findNextSolvedForm() {
		if( !isFinished() ) {
			do {
				step();
			} while(!agenda.isEmpty());
			
			if( isFinished() ) {
				agenda.clear();
			}
		} 
	}
	

	private void addSplitToAgendaAndAccu(EnumerationStackEntry ese) {
		IntSplit split = ese.getCurrentSplit();

		
		int root = split.getRoot();
		addFragsetToAgendaAndAccu(split.leftSub, root + "xl", ese);
		addFragsetToAgendaAndAccu(split.rightSub, root + "xr", ese);

	}
	
	private void addFragsetToAgendaAndAccu(List<Integer> fragSet, String dominator, EnumerationStackEntry ese) {
        if( isSingleton(fragSet) ) {
            // singleton fragsets: add directly to ese's domedge list
        	
        	String tgt = getSingletonRoot(fragSet);
        	if(getSingletonRoot(fragSet).equals("0")) {
        		// TODO this is definitely too nasty.
        		int leaf ;
        		if(dominator.endsWith("l")) {
        			leaf = Integer.parseInt(dominator.substring(0, dominator.length() -2)) -1;
        		} else {
        			leaf = Integer.parseInt(dominator.substring(0, dominator.length() -2));
        		}
        		tgt = leaf + "y";
        		
        	} else {
        		
        		// they need their leaves assigned.
        		DomEdge leafleft = new DomEdge(tgt + "xl", (Integer.parseInt(tgt) -1 + "y"));
        		DomEdge leafright = new DomEdge(tgt + "xr", tgt + "y");
        		ese.addDomEdge(leafleft);
        		ese.addDomEdge(leafright);
        		tgt += "x";
        		
        	}
        	
            DomEdge newEdge = new DomEdge(dominator, tgt);
            ese.addDomEdge(newEdge);
        } else {
            // larger fragsets: add to agenda
            AgendaEntry newEntry = new AgendaEntry(dominator,fragSet);
            agenda.add(newEntry);
        }
    }


    private String getSingletonRoot(List<Integer> fragSet) {
    	return rootForThisFragset;
    }

    private boolean isSingleton(List<Integer> fragSet) {
        
    	rootForThisFragset = fragSet.get(0).toString();
    	
        return  fragSet.get(0) == fragSet.get(1);
        
    }

    private void step() {
		EnumerationStackEntry top = stack.peek();
		AgendaEntry agTop;
		List<Integer> topFragset;
		String topNode;
		
		// 1. Apply (Up) as long as possible
		if ( agenda.isEmpty() ) {
			while( top.isAtLastSplit() ) {
				stack.pop();
				if (stack.isEmpty() )
					return;
				else 
					top = stack.peek();
			}
		}
		
		// 2. Apply (Step) or (Down) as appropriate.
		// Singleton fragments are put directly into the accu, rather
		// than the agenda (i.e. simulation of Singleton).
		if( agenda.isEmpty() ) {
			// (Step)
			top.clearAccu();
			top.nextSplit();
			
			if ( top.getDominator() != null ) {
				
				int root = top.getCurrentSplit().getRoot();
				
                DomEdge newEdge = 
                    new DomEdge(top.getDominator(), root + "x");
				top.addDomEdge(newEdge);
			}
			
			if(! top.getAgendaCopy().isEmpty() ) {
				agenda.addAll(top.getAgendaCopy());
			}
			
			addSplitToAgendaAndAccu( top );
		} else {
            // (Down)
			agTop = agenda.pop();
			topNode = agTop.getDominator();
			topFragset = agTop.getFragmentSet();
			
			if( !isSingleton(topFragset) ) {
				// if topFragset is a singleton, then it was a wcc of the entire graph
				// that only contained a single fragment; hence we don't need to do anything here
				List<IntSplit> sv = chart.getSplitsFor(topFragset);

				EnumerationStackEntry newTop = 
					new EnumerationStackEntry(topNode, sv, agenda);

				if( topNode != null ) {
					DomEdge newEdge = 
						new DomEdge(topNode, newTop.getCurrentSplit().getRoot() + "x");
					newTop.addDomEdge( newEdge );
				}

				stack.push(newTop);
				addSplitToAgendaAndAccu( newTop );
			}
		}
	}
    
    
    
    
    /**** convenience methods for implementing Iterator ****/

  
    public boolean hasNext() {
    	if( chartIsEmpty ) {
    		return !returnedSfForEmptyChart;
    	} else {
    		return nextSolvedForm != null;
    	}
    }

    public SolvedFormSpec next() {
    	if( chartIsEmpty ) {
    		if( returnedSfForEmptyChart ) {
    			return null;
    		} else {
    			returnedSfForEmptyChart = true;
    			return new SolvedFormSpec();
    		}
    	}
    	
    	SolvedFormSpec ret = nextSolvedForm;
    	
    	if( ret != null ) {
    		updateNextSolvedForm();
    		return ret;
    	} else {
    		return null;
    	}
    }

    /**
     * This returns a solved form represented by a List of <code>DomEdge</code>
     * objects. The form is accessed via its index.
     * Forms with indices exceeding the range of <code>int</code> have to be
     * extracted manually by calling <code>next()</code> as often as necessary.
     * 
     * @param sf index of the solved form to extract
     * @return the solved form
     */
    public SolvedFormSpec getSolvedForm(int sf) {
        for( int i = solvedForms.size(); i <= sf; i++ ) {
            if( !iteratorForGet.hasNext() ) {
                return null;
            } else {
                solvedForms.add(iteratorForGet.next());
            }
        }
        
        return solvedForms.get(sf);
    }
    
   
    

    public void remove() {
        throw new UnsupportedOperationException();
    }
    
   
    
    
    /**** classes for the agenda and the enumeration stack ****/
    
    private static class EnumerationStackEntry {
        private String dominator;
        private List<DomEdge> edgeAccu;
        private IntSplit currentSplit;
        private List<IntSplit> splits; // points into chart
        private IntSplit lastElement;
        private Agenda agendaCopy;
        private Iterator<IntSplit> splitIterator;
        
        EnumerationStackEntry(String dom, List<IntSplit> spl, Agenda agenda) {
            dominator = dom;
            splits = spl;
            agendaCopy = new Agenda();
            
            if( agenda != null ) {
                agendaCopy.addAll(agenda);
            }
            
            if( (spl != null) && (! spl.isEmpty() )  ) {
                
                splitIterator = splits.iterator();
                currentSplit = splitIterator.next();
                
                lastElement = splits.get(splits.size() - 1);
                
            } 
            
            edgeAccu = new ArrayList<DomEdge>();
        }
            
        public void nextSplit() {
            currentSplit = splitIterator.next();
        }
        
        public boolean isAtLastSplit() {
            if( splitIterator == null ) {
                return true;
            }
            return ( (! splitIterator.hasNext()) || currentSplit.equals(lastElement));
        }
        
        public void addDomEdge(DomEdge edge) {
            edgeAccu.add(edge);
        }
        
        public void clearAccu() {
            edgeAccu.clear();
        }
        
       
        /**
         * @return Returns the dominator.
         */
        public String getDominator() {
            return dominator;
        }

        /**
         * @return Returns the edgeAccu.
         */
        public List<DomEdge> getEdgeAccu() {
            return edgeAccu;
        }


        /**
         * @return Returns the currentSplit.
         */
        public IntSplit getCurrentSplit() {
            return currentSplit;
        }


        /**
         * @return Returns the agendaCopy.
         */
        public Agenda getAgendaCopy() {
            return agendaCopy;
        }

        
        public String toString() {
            StringBuilder ret = new StringBuilder();
            
            ret.append("<ESE dom=" + dominator + ", accu=" + edgeAccu);
            ret.append(", agendacopy=" + agendaCopy + ", splits=");
            for( IntSplit split : splits ) {
                if( split == currentSplit ) {
                    ret.append(split.toString().toUpperCase());
                } else {
                    ret.append(split);
                }
                ret.append(",");
            }
            
            return ret.toString();
        }
    }

    
    
    

    private static class AgendaEntry { 
        String dominator;
        List<Integer> fragmentSet;
        
        AgendaEntry(String source, List<Integer> target) {
            dominator = source;
            fragmentSet = target;
        }
        
        
        public String getDominator() {
            return dominator;
        }

        public List<Integer> getFragmentSet() {
            return fragmentSet;
        }

        public String toString() {
            return "<Ag dom="+dominator+", fs=" + fragmentSet + ">";
        }
    }

    

    private static class Agenda extends Stack<AgendaEntry> {

		/**
		 * 
		 */
		private static final long serialVersionUID = 7426236767126350134L;
    }

}
