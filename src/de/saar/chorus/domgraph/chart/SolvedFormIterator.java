package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.domgraph.graph.DomGraph;

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
	
    // I need the graph in order to determine the fragments: I need to
    // know the roots of singleton fragsets to create the dom edge.
	public SolvedFormIterator(Chart ch, DomGraph graph) {
		chart = ch;
		agenda = new Agenda();
		nullNode = null; 
		stack = new Stack<EnumerationStackEntry>();
        
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
			} else {
				nextSolvedForm = null;
			}
		}
	}
	
	public boolean representsSolvedForm(){
		return (agenda.isEmpty() && stack.size() > 0 );
	}
	
	
	boolean isFinished(){	
		return (agenda.isEmpty() && stack.isEmpty() );
	}
	
	
	List<DomEdge> extractDomEdges() {
		List<DomEdge> toReturn = new ArrayList<DomEdge>();
		
		for( EnumerationStackEntry ese : stack) {
			toReturn.addAll(ese.getEdgeAccu());
		}
		
		return toReturn;
	}
	
	
	void findNextSolvedForm() {
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
            for( Set<String> wcc : split.getWccs(node) ) {
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


    private String getSingletonRoot(Set<String> fragSet) {
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

    void step() {
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


    // TODO think about whether hasNext() guarantees that next() will
    // work -- otherwise, we need to precompute the next sf in hasNext()
    public List<DomEdge> next() {
    	List<DomEdge> ret = nextSolvedForm;
    	
    	if( ret != null ) {
    		updateNextSolvedForm();
    		return ret;
    	} else {
    		return null;
    	}
    }


    public void remove() {
        throw new UnsupportedOperationException();
    }
}
