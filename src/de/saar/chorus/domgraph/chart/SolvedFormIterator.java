package de.saar.chorus.domgraph.chart;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.domgraph.graph.DomGraph;

/**
 * An iterator over the different solved forms represented by
 * a {@link Chart}. The chart is passed to the constructor of
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
public class SolvedFormIterator implements Iterator<List<DomEdge>> {
	private Chart chart;
	private Agenda agenda;
	private Stack<EnumerationStackEntry> stack;
	int num_Fragsets;
	private String nullNode;
    //private Map<Set<String>, String> fragmentTable;
    private Set<String> roots;
    private String rootForThisFragset;
	
	private List<DomEdge> nextSolvedForm;
	
	private List< List<DomEdge> > solvedForms;
	
    // I need the graph in order to determine the fragments: I need to
    // know the roots of singleton fragsets to create the dom edge.
	public SolvedFormIterator(Chart ch, DomGraph graph) {
		chart = ch;
		agenda = new Agenda();
		nullNode = null; 
		stack = new Stack<EnumerationStackEntry>();
        solvedForms = new ArrayList< List<DomEdge> >();
		
        //fragmentTable = graph.getFragments();
        roots = graph.getAllRoots();
        
		
		for( Set<String> fragset : chart.getToplevelSubgraphs() ) {
            if( fragset.size() > 0 ) {
                agenda.add(new AgendaEntry(nullNode, fragset));
            }
        }
		
		//Null-Element on Stack
		stack.push( new EnumerationStackEntry(nullNode, new ArrayList<Split>(), null));
		
		updateNextSolvedForm();
	}
	

    private void updateNextSolvedForm() {
		if( isFinished() ) {
			nextSolvedForm = null;
		} else {
			findNextSolvedForm();
			
			if( representsSolvedForm() ) {
				nextSolvedForm = extractDomEdges();
				solvedForms.add(nextSolvedForm);
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
	
	
	private List<DomEdge> extractDomEdges() {
		List<DomEdge> toReturn = new ArrayList<DomEdge>();
		
		for( EnumerationStackEntry ese : stack) {
			toReturn.addAll(ese.getEdgeAccu());
		}
		
		return toReturn;
	}
	
	
	private void findNextSolvedForm() {
		if( !isFinished() ) {
			do {
				//System.err.println("step()");
				step();
			} while(!agenda.isEmpty());
			
			if( isFinished() ) {
				agenda.clear();
			} else {
//				System.err.println("===== SOLVED FORM ===="); //Debug
			}
		} 
	}
	
	
	private void addSplitToAgendaAndAccu(EnumerationStackEntry ese) {
		Split split = ese.getCurrentSplit();

        // iterate over all dominators
        for( String node : split.getAllDominators() ) {
            List<Set<String>> wccs = split.getWccs(node);
            for( int i = 0; i < wccs.size(); i++ ) {
                Set<String> wcc = wccs.get(i);
                addFragsetToAgendaAndAccu(wcc, node, ese);
			}
		}
	}
	
	private void addFragsetToAgendaAndAccu(Set<String> fragSet, String dominator, EnumerationStackEntry ese) {
        if( isSingleton(fragSet) ) {
            // singleton fragsets: add directly to ese's domedge list
            DomEdge newEdge = new DomEdge(dominator, getSingletonRoot(fragSet));
            ese.addDomEdge(newEdge);
            //System.err.println("Singleton DomEdge : " + newEdge);
        } else {
            // larger fragsets: add to agenda
            AgendaEntry newEntry = new AgendaEntry(dominator,fragSet);
            agenda.add(newEntry);
        }
    }


    private String getSingletonRoot(@SuppressWarnings("unused") Set<String> fragSet) {
        return rootForThisFragset;
        //return fragSet.iterator().next();
        //return fragmentTable.get(fragSet);
    }

    private boolean isSingleton(Set<String> fragSet) {
        int numRoots = 0;
        
        for( String node : fragSet ) {
            if( roots.contains(node) ) {
                numRoots++;
                rootForThisFragset = node;
            }
        }
        
        return numRoots == 1;
    }

    private void step() {
		EnumerationStackEntry top = stack.peek();
		AgendaEntry agTop;
		Set<String> topFragset;
		String topNode;
		
		// 1. Apply (Up) as long as possible
		if ( agenda.isEmpty() ) {
			while( top.isAtLastSplit() ) {
				//System.err.println("(Up)"); //debug
				stack.pop();
				if (stack.isEmpty() )
					return;
				else 
					top = stack.peek();
			}
		}
        
        //System.err.println("agenda: " + agenda);
		
		// 2. Apply (Step) or (Down) as appropriate.
		// Singleton fragments are put directly into the accu, rather
		// than the agenda (i.e. simulation of Singleton).
		if( agenda.isEmpty() ) {
			// (Step)
			//System.err.println("(Step)");
			top.clearAccu();
			top.nextSplit();
			
			if ( top.getDominator() != null ) {
                DomEdge newEdge = 
                    new DomEdge(top.getDominator(), top.getCurrentSplit().getRootFragment());
				top.addDomEdge(newEdge);
				
				//System.err.println("new DomEdge: " + newEdge);
			}
			
			if(! top.getAgendaCopy().isEmpty() ) {
				//System.err.println("Retrieve agenda from stored stack entry."); //debug
				agenda.addAll(top.getAgendaCopy());
			}
			
			addSplitToAgendaAndAccu( top );
		} else {
            // (Down)
            //System.err.println("(Down)");

			agTop = agenda.pop();
            //System.err.println("agTop = " + agTop);
			topNode = agTop.getDominator();
			topFragset = agTop.getFragmentSet();
            
			if (topFragset.size() > 1 ) {
                List<Split> sv = chart.getSplitsFor(topFragset);
                
                EnumerationStackEntry newTop = 
                    new EnumerationStackEntry(topNode, sv, agenda);
                
                //System.err.println("new ese: " + newTop);

				if( topNode != null ) {
                    DomEdge newEdge = 
                        new DomEdge(topNode, newTop.getCurrentSplit().getRootFragment());
					newTop.addDomEdge( newEdge );
				
					//System.err.println("new DomEdge: " + newEdge);
				}
				
                //System.err.println("push: " + newTop);
				stack.push(newTop);
				addSplitToAgendaAndAccu( newTop );
			}
		}
	}
    
    
    
    
    /**** convenience methods for implementing Iterator ****/
    
    public boolean hasNext() {
    	return nextSolvedForm != null;
    }

    public List<DomEdge> next() {
    	List<DomEdge> ret = nextSolvedForm;
    	
    	if( ret != null ) {
    		updateNextSolvedForm();
    		return ret;
    	} else {
    		return null;
    	}
    }

    /**
     * 
     * @param sf index of the solved form to extract
     * @return 
     */
    public List<DomEdge> getSolvedForm(int sf) {
    	if (  chart.countSolvedForms().intValue() <= sf ) {
    		return null;
    	} else {
    		if( sf <= solvedForms.size() ) {
    			return solvedForms.get(sf);
    		} else {
    			for( int i = solvedForms.size(); i <= sf; i++ ) {
    				updateNextSolvedForm();
    			}
    		}
    	}
    	return solvedForms.get(sf);
    }
    
    /**
     * TODO perhaps this is redundand? The entries of an 
     * ArrayList are only accessible by get(int) anyway. 
     * 
     * @param sf index of the solved form to extract
     * @return
     */
    public List<DomEdge> getSolvedForm(BigInteger sf) {
    	if (  chart.countSolvedForms().compareTo(sf) <= 0 ) {
    		return null;
    	} else {
    		if( sf.intValue() <= solvedForms.size() ) {
    			return solvedForms.get(sf.intValue());
    		} else {
    			for( long i = solvedForms.size(); i <= sf.longValue(); i++ ) {
    				updateNextSolvedForm();
    			}
    		}
    	}
    	return solvedForms.get(sf.intValue());
    }
    

    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    public Chart getChart() {
    	return chart;
    }
    
    
    /**** classes for the agenda and the enumeration stack ****/
    
    private static class EnumerationStackEntry {
        private String dominator;
        private List<DomEdge> edgeAccu;
        private Split currentSplit;
        private List<Split> splits; // points into chart
        private Split lastElement;
        private Agenda agendaCopy;
        private Iterator<Split> splitIterator;
        
        EnumerationStackEntry(String dom, List<Split> spl, Agenda agenda) {
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
         * @param dominator The dominator to set.
         */
        public void setDominator(String dominator) {
            this.dominator = dominator;
        }

        /**
         * @return Returns the edgeAccu.
         */
        public List<DomEdge> getEdgeAccu() {
            return edgeAccu;
        }

        /**
         * @param edgeAccu The edgeAccu to set.
         */
        public void setEdgeAccu(List<DomEdge> edgeAccu) {
            this.edgeAccu = edgeAccu;
        }

        /**
         * @return Returns the lastElement.
         */
        public Split getLastElement() {
            return lastElement;
        }

        /**
         * @param lastElement The lastElement to set.
         */
        public void setLastElement(Split lastElement) {
            this.lastElement = lastElement;
        }

        /**
         * @return Returns the splits.
         */
        public List<Split> getSplits() {
            return splits;
        }

        /**
         * @param splits The splits to set.
         */
        public void setSplits(List<Split> splits) {
            this.splits = splits;
        }

        /**
         * @return Returns the currentSplit.
         */
        public Split getCurrentSplit() {
            return currentSplit;
        }

        /**
         * @param currentSplit The currentSplit to set.
         */
        public void setCurrentSplit(Split currentSplit) {
            this.currentSplit = currentSplit;
        }

        /**
         * @return Returns the agendaCopy.
         */
        public Agenda getAgendaCopy() {
            return agendaCopy;
        }

        /**
         * @param agendaCopy The agendaCopy to set.
         */
        public void setAgendaCopy(Agenda agendaCopy) {
            this.agendaCopy = agendaCopy;
        }
        
        public String toString() {
            StringBuilder ret = new StringBuilder();
            
            ret.append("<ESE dom=" + dominator + ", accu=" + edgeAccu);
            ret.append(", agendacopy=" + agendaCopy + ", splits=");
            for( Split split : splits ) {
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
        String key;
        Set<String> value;
        
        AgendaEntry(String source, Set<String> target) {
            key = source;
            value = target;
        }
        
        
        /* (non-Javadoc)
         * @see java.util.Map.Entry#getKey()
         */
        public String getDominator() {
            // TODO Auto-generated method stub
            return key;
        }

        /* (non-Javadoc)
         * @see java.util.Map.Entry#getValue()
         */
        public Set<String> getFragmentSet() {
            // TODO Auto-generated method stub
            return value;
        }

        public String toString() {
            return "<Ag dom="+key+", fs=" + value + ">";
        }
    }

    

    private static class Agenda extends Stack<AgendaEntry> {
    }

}
