package de.saar.chorus.libdomgraph.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import de.saar.chorus.libdomgraph.Chart;
import de.saar.chorus.libdomgraph.FragmentSet;
import de.saar.chorus.libdomgraph.SWIGTYPE_p_Node;
import de.saar.chorus.libdomgraph.Split;

public class EnumerationState {
	private Chart chart;
	private Agenda agenda;
	private Stack<EnumerationStackEntry> stack;
	private List<FragmentSet> allocatedFragsets;
	int num_allocatedFragsets;
	boolean lastAppliedRuleWasSingleton;
	private SWIGTYPE_p_Node nullNode;
	
	EnumerationState(Chart ch, List<FragmentSet> fsets) {
		
		nullNode = null; 
		allocatedFragsets = new ArrayList<FragmentSet>(fsets);
		num_allocatedFragsets = fsets.size();
		
		for( FragmentSet fragSet : allocatedFragsets ) {
			
			if(! (fragSet.size() == 0 ))
				agenda.addEntry(new AgendaEntry(nullNode, fragSet));
		}
		
		stack.push( new EnumerationStackEntry(nullNode, new ArrayList<Split>(), null));
		
		lastAppliedRuleWasSingleton = false; 
	}
	
	
	boolean representsSolvedForm(){
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
				step();
			} while(!agenda.isEmpty());
			
			if( isFinished() ) {
				Agenda.deleteAgendaEntries();
				Agenda.clearDomEdges();
			}
		}
	}
	
	
	void addSplitToAgenda(Split sp) {
		
		List<SWIGTYPE_p_Node> roots = 
			WrapperTools.vectorToList(sp.getChildRoots());
		
		for( SWIGTYPE_p_Node node : roots ) {
			List<FragmentSet> childFrags =
				WrapperTools.vectorToList(sp.getChildFor(node));
			
			for(FragmentSet fragSet : childFrags) {
				AgendaEntry newEntry = new AgendaEntry(node,fragSet);
				agenda.addEntry(newEntry);
			}
		}
		
	}
	
	void addSplitToAgendaAndAccu(EnumerationStackEntry ese) {
		Split sp = ese.getCurrentSplit();
		
		List<SWIGTYPE_p_Node> roots = 
			WrapperTools.vectorToList(sp.getChildRoots());
		
		for( SWIGTYPE_p_Node node : roots ) {
			List<FragmentSet> childFrags =
				WrapperTools.vectorToList(sp.getChildFor(node));
			
			for(FragmentSet fragSet : childFrags) {
				
				if( fragSet.size() == 1 ) {
					ese.addDomEdge(new DomEdge(node, fragSet.getFirstNode()));
				} else {
					
					AgendaEntry newEntry = new AgendaEntry(node,fragSet);
					agenda.addEntry(newEntry);
				}
			}
		}
	}
	
	void step() {
		EnumerationStackEntry top = stack.peek();
		AgendaEntry agTop;
		FragmentSet topFragset;
		SWIGTYPE_p_Node topNode;
		
		// 1. Apply (Up) as long as possible
		if ( agenda.isEmpty() ) {
			while( top.isLastSplit() ) {
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
				top.addDomEdge(Agenda.makeDomEdge(top.getDominator(), 
						top.getCurrentSplit().getRoot()));
			}
			
			if(! top.getAgendaCopy().isEmpty() ) {
				agenda.addAll(top.getAgendaCopy());
			}
			
			addSplitToAgendaAndAccu( top );
		} else {
			
			// (Down)
			agTop = agenda.getAndRemoveNext();
			topNode = agTop.getKey();
			topFragset = agTop.getValue();
			
			
			
			if (topFragset.size() > 1 ) {
				EnumerationStackEntry newTop = new EnumerationStackEntry(topNode, 
						WrapperTools.vectorToList(chart.getEdgesFor(topFragset)),
						agenda);
				
				if( topNode != null ) {
					newTop.addDomEdge( Agenda.makeDomEdge(topNode, 
							newTop.getCurrentSplit().getRoot() ) );
				}
				
				stack.push(newTop);
				
				addSplitToAgendaAndAccu( newTop );
				
			}
			
		}
		
		
	}
	
	
}
