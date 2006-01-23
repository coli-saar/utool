package de.saar.chorus.libdomgraph.chart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import junit.framework.Assert;

import de.saar.chorus.libdomgraph.Chart;
import de.saar.chorus.libdomgraph.DomGraph;
import de.saar.chorus.libdomgraph.FragmentSet;
import de.saar.chorus.libdomgraph.SWIGTYPE_p_Node;
import de.saar.chorus.libdomgraph.Split;
import de.saar.chorus.libdomgraph.SplitVector;
import de.saar.chorus.ubench.JDomGraph;

public class SolvedFormEnumerator {
	private DomGraph domGraph;
	private JDomGraph jGraph;
	private Chart chart;
	private Agenda agenda;
	private Stack<EnumerationStackEntry> stack;
	int num_Fragsets;
	//boolean lastAppliedRuleWasSingleton;
	private SWIGTYPE_p_Node nullNode;
	
	SolvedFormEnumerator(Chart ch, List<FragmentSet> fsets, JDomGraph graph) {
		chart = ch;
		domGraph = chart.getGraph();
		jGraph = graph;
		agenda = new Agenda();
		nullNode = null; 
		num_Fragsets = fsets.size();
		stack = new Stack<EnumerationStackEntry>();
		
		
		for( FragmentSet fragSet : fsets ) {
			if( fragSet.size() > 0 ) {
				agenda.add(new AgendaEntry(nullNode, fragSet));
				//System.out.println("Start-Agenda-Entry (constructor)");
			}
		}
		
		//Null-Element on Stack
		stack.push( new EnumerationStackEntry(nullNode, new ArrayList<Split>(), null));
		
		//lastAppliedRuleWasSingleton = false; 
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
				//System.out.println("step()");
				step();
			} while(!agenda.isEmpty());
			
			if( isFinished() ) {
				agenda.clear();
			} else {
				System.out.println("===== SOLVED FORM ===="); //Debug
			}
		} 
	}
	
	
	private void addSplitToAgendaAndAccu(EnumerationStackEntry ese) {
		Split split = ese.getCurrentSplit();

        // iterate over all holes
		for( SWIGTYPE_p_Node node : WrapperTools.vectorToList(split.getChildRoots()) ) {
			// for each hole, iterate over fragsets assigned to this hole
			for(FragmentSet fragSet : WrapperTools.vectorToList(split.getChildFor(node)) ) {
                addFragsetToAgendaAndAccu(fragSet, node, ese);
			}
		}
        
        // iterate over weakly dominated fragsets
		for(FragmentSet fragSet : WrapperTools.vectorToList(split.getOtherFragmentSets()) ) {
            addFragsetToAgendaAndAccu(fragSet, split.getRoot(), ese);
		}
		
	}
	
	private void addFragsetToAgendaAndAccu(FragmentSet fragSet, SWIGTYPE_p_Node dominator, EnumerationStackEntry ese) {
        if( fragSet.size() == 1 ) {
            // singleton fragsets: add directly to ese's domedge list
            DomEdge newEdge = new DomEdge(dominator, fragSet.getFirstNode(),
                    domGraph);
            ese.addDomEdge(newEdge);
            System.out.println("Singleton DomEdge : " + newEdge);
        } else {
            // larger fragsets: add to agenda
            AgendaEntry newEntry = new AgendaEntry(dominator,fragSet);
            agenda.add(newEntry);
        }
    }


    void step() {
		EnumerationStackEntry top = stack.peek();
		AgendaEntry agTop;
		FragmentSet topFragset;
		SWIGTYPE_p_Node topNode;
		
		// 1. Apply (Up) as long as possible
		if ( agenda.isEmpty() ) {
			while( top.isAtLastSplit() ) {
				System.out.println("(Up)"); //debug
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
			System.out.println("(Step)");
			top.clearAccu();
			top.nextSplit();
			
			if ( top.getDominator() != null ) {
                DomEdge newEdge = new DomEdge(top.getDominator(), 
                        top.getCurrentSplit().getRoot(),
                        domGraph); 
				top.addDomEdge(newEdge);
				
				System.out.println("new DomEdge: " + newEdge);
			}
			
			if(! top.getAgendaCopy().isEmpty() ) {
				System.out.println("Retrieve agenda from stored stack entry."); //debug
				agenda.addAll(top.getAgendaCopy());
			}
			
			addSplitToAgendaAndAccu( top );
		} else {
            // (Down)
            System.out.println("(Down)");

			agTop = agenda.pop();
			topNode = agTop.getKey();
			topFragset = agTop.getValue();
            
			if (topFragset.size() > 1 ) {
                SplitVector sv = chart.getEdgesFor(topFragset);
                
                Assert.assertNotNull("null topFragset", topFragset);
                
				EnumerationStackEntry newTop = new EnumerationStackEntry(topNode, 
						WrapperTools.vectorToList(sv), agenda);

                Assert.assertNotNull("null edges", sv);
                Assert.assertTrue("empty edges", !sv.isEmpty());
				
				if( topNode != null ) {
					System.err.println(newTop.getCurrentSplit());
                    DomEdge newEdge = new DomEdge(topNode, 
                            newTop.getCurrentSplit().getRoot(),
                            domGraph);
					newTop.addDomEdge( newEdge );
				
					System.out.println("new DomEdge: " + newEdge);
				}
				
				stack.push(newTop);
				addSplitToAgendaAndAccu( newTop );
			}
		}
	}
}
