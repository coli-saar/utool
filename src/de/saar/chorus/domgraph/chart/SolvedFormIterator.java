package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import de.saar.chorus.domgraph.graph.DomEdge;

public class SolvedFormIterator implements Iterator<Set<DomEdge>> {
	//private DomGraph domGraph;
	private Chart chart;
	private Agenda agenda;
	private Stack<EnumerationStackEntry> stack;
	int num_Fragsets;
	private String nullNode;
	
	public SolvedFormIterator(Chart ch) {
		chart = ch;
		agenda = new Agenda();
		nullNode = null; 
		stack = new Stack<EnumerationStackEntry>();
		
		for( Set<String> fragset : chart.getCompleteFragsets() ) {
            if( fragset.size() > 0 ) {
                agenda.add(new AgendaEntry(nullNode, fragset));
            }
        }
		
		//Null-Element on Stack
		stack.push( new EnumerationStackEntry(nullNode, new ArrayList<Split>(), null));
	}
	
	
	boolean representsSolvedForm(){
		return (agenda.isEmpty() && stack.size() > 0 );
	}
	
	
	boolean isFinished(){	
		return (agenda.isEmpty() && stack.isEmpty() );
	}
	
	
	Set<DomEdge> extractDomEdges() {
		Set<DomEdge> toReturn = new HashSet<DomEdge>();
		
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
				System.err.println("===== SOLVED FORM ===="); //Debug
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
        if( fragSet.size() == 1 ) {
            // singleton fragsets: add directly to ese's domedge list
            DomEdge newEdge = new DomEdge(dominator, fragSet.iterator().next());
            ese.addDomEdge(newEdge);
            System.err.println("Singleton DomEdge : " + newEdge);
        } else {
            // larger fragsets: add to agenda
            AgendaEntry newEntry = new AgendaEntry(dominator,fragSet);
            agenda.add(newEntry);
        }
    }


    void step() {
		EnumerationStackEntry top = stack.peek();
		AgendaEntry agTop;
		Set<String> topFragset;
		String topNode;
		
		// 1. Apply (Up) as long as possible
		if ( agenda.isEmpty() ) {
			while( top.isAtLastSplit() ) {
				System.err.println("(Up)"); //debug
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
			System.err.println("(Step)");
			top.clearAccu();
			top.nextSplit();
			
			if ( top.getDominator() != null ) {
                DomEdge newEdge = 
                    new DomEdge(top.getDominator(), top.getCurrentSplit().getRootFragment());
				top.addDomEdge(newEdge);
				
				System.err.println("new DomEdge: " + newEdge);
			}
			
			if(! top.getAgendaCopy().isEmpty() ) {
				System.err.println("Retrieve agenda from stored stack entry."); //debug
				agenda.addAll(top.getAgendaCopy());
			}
			
			addSplitToAgendaAndAccu( top );
		} else {
            // (Down)
            System.err.println("(Down)");

			agTop = agenda.pop();
			topNode = agTop.getDominator();
			topFragset = agTop.getFragmentSet();
            
			if (topFragset.size() > 1 ) {
                List<Split> sv = chart.getSplitsFor(topFragset);
				EnumerationStackEntry newTop = 
                    new EnumerationStackEntry(topNode, sv, agenda);

				if( topNode != null ) {
					System.err.println(newTop.getCurrentSplit());
                    DomEdge newEdge = 
                        new DomEdge(topNode, newTop.getCurrentSplit().getRootFragment());
					newTop.addDomEdge( newEdge );
				
					System.err.println("new DomEdge: " + newEdge);
				}
				
				stack.push(newTop);
				addSplitToAgendaAndAccu( newTop );
			}
		}
	}
    
    
    
    
    /**** convenience methods for implementing Iterator ****/
    
    public boolean hasNext() {
        return !isFinished();
    }


    // TODO think about whether hasNext() guarantees that next() will
    // work -- otherwise, we need to precompute the next sf in hasNext()
    public Set<DomEdge> next() {
        findNextSolvedForm();
        return extractDomEdges();
    }


    public void remove() {
        throw new UnsupportedOperationException();
    }
}
