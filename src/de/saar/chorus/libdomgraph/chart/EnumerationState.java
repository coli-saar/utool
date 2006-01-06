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
		
		chart = ch;
		agenda = new Agenda();
		nullNode = null; 
		allocatedFragsets = new ArrayList<FragmentSet>(fsets);
		num_allocatedFragsets = fsets.size();
		stack = new Stack<EnumerationStackEntry>();
		
		
		for( FragmentSet fragSet : fsets ) {
			
			if(! (fragSet.size() == 0 )) {
				agenda.addEntry(new AgendaEntry(nullNode, fragSet));
				System.out.println("Start-Agenda-Entry (constructor)");
			}
		}
		
		//Null-Element on Stack
		stack.push( new EnumerationStackEntry(nullNode, new ArrayList<Split>(), null));
		
		lastAppliedRuleWasSingleton = false; 
	}
	
	
	boolean representsSolvedForm(){
		return (agenda.isEmpty() && stack.size() > 0 );
	}
	
	
	boolean isFinished(){	
		return (agenda.isEmpty() && stack.isEmpty() );
	}
	
	
	List<JDomEdge> extractDomEdges() {
		
		List<JDomEdge> toReturn = new ArrayList<JDomEdge>();
		
		for( EnumerationStackEntry ese : stack) {
			toReturn.addAll(ese.getEdgeAccu());
		}
		
		return toReturn;
		
	}
	
	
	void findNextSolvedForm() {
		if( !isFinished() ) {
			do {
				System.out.println("step()");
				step();
				
			} while(!agenda.isEmpty());
			
			if( isFinished() ) {
				agenda.clear();
				
			} else {
				
				System.out.println("===== SOLVED FORM ===="); //Debug
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
					ese.addDomEdge(new JDomEdge(node, fragSet.getFirstNode()));
				System.out.println("Singelton JDomEdge : "
					//	+ chart.getGraph().getData(node).getName()
						+ " ---> "); //+ 
					//	chart.getGraph().getData(fragSet.getFirstNode()).getName()); //debug
				} else {
					//System.out.println("New AgendaEntry -- Root: "+ chart.getGraph().getData(node).getName());
					AgendaEntry newEntry = new AgendaEntry(node,fragSet);
					agenda.addEntry(newEntry);
				}
			}
		}
		
		List<FragmentSet> otherFragments = WrapperTools.vectorToList(sp.getOtherFragmentSets());
		
		for(FragmentSet fragSet : otherFragments) {
			
			if( fragSet.size() == 1 ) {
				ese.addDomEdge(new JDomEdge(sp.getRoot(), fragSet.getFirstNode()));
			System.out.println("Singelton JDomEdge : "
				//	+ chart.getGraph().getData(node).getName()
					+ " ---> "); //+ 
				//	chart.getGraph().getData(fragSet.getFirstNode()).getName()); //debug
			} else {
				//System.out.println("New AgendaEntry -- Root: "+ chart.getGraph().getData(node).getName());
				AgendaEntry newEntry = new AgendaEntry(sp.getRoot(),fragSet);
				agenda.addEntry(newEntry);
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
				System.out.println("pop-up"); //debug
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
			System.out.println("step-side");
			top.clearAccu();
			top.nextSplit();
			
			if ( top.getDominator() != null ) {
				top.addDomEdge(new JDomEdge(top.getDominator(), 
						top.getCurrentSplit().getRoot()));
				
				System.out.println("new JDomEdge: " + 
						//chart.getGraph().getData(top.getDominator()).getName() + 
						" ---> " );//+ 
						//chart.getGraph().getData(top.getCurrentSplit().getRoot()).getName()) ;
				
			}
			
			if(! top.getAgendaCopy().isEmpty() ) {
				System.out.println("COPY AGENDA"); //debug
				agenda.addAll(top.getAgendaCopy());
			}
			
			addSplitToAgendaAndAccu( top );
		} else {
			System.out.println("down");
			// (Down)
			agTop = agenda.getAndRemoveNext();
			topNode = agTop.getKey();
			topFragset = agTop.getValue();
			
	
			
			if (topFragset.size() > 1 ) {
				
				EnumerationStackEntry newTop = new EnumerationStackEntry(topNode, 
						WrapperTools.vectorToList(chart.getEdgesFor(topFragset)),
						agenda);

				
				if( topNode != null ) {
					
					newTop.addDomEdge( new JDomEdge(topNode, 
							newTop.getCurrentSplit().getRoot() ) );
				
					System.out.println("new JDomEdge: " + 
						//	chart.getGraph().getData(topNode).getName() + 
							" ---> ");// + 
						//	chart.getGraph().getData(newTop.getCurrentSplit().getRoot()).getName()) ;
				}
				
				stack.push(newTop);
				
				addSplitToAgendaAndAccu( newTop );
				
			}
			
		}
		
		
	}
	
	
}
