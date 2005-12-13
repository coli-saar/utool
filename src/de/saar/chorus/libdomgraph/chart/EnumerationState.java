package de.saar.chorus.libdomgraph.chart;

import java.util.List;
import java.util.Stack;

import de.saar.chorus.libdomgraph.Chart;
import de.saar.chorus.libdomgraph.FragmentSet;
import de.saar.chorus.libdomgraph.SWIGTYPE_p_Node;
import de.saar.chorus.libdomgraph.Split;

public class EnumerationState {
	 private Chart chart;
	 private List<AgendaEntry> agenda;
	 private Stack<EnumerationStackEntry> stack;
	 //FragmentSet** allocatedFragsets;
	 int num_allocatedFragsets;
	 boolean lastAppliedRuleWasSingleton;

	 EnumerationState(Chart ch, List<FragmentSet> fsets) {
		 
	 }
	  

	  boolean representsSolvedForm(){
		  return false;
	  }
	  boolean isFinished(){
		  return false;
	  }

	  void extractDomEdges(List<DomEdge> dEdges){
		  
	  }
	  void findNextSolvedForm() {
		  
	  }
	 
	 
	  void addSplitToAgenda(Split sp) {
		  /*
		   * TODO implement me;
		   * 
		   * crate apropriate map-templates
		   * in SWIG-interface
		   */
	  }
	  void addSplitToAgendaAndAccu(EnumerationStackEntry ese) {
		  Split sp = ese.getCurrentSplit();
		  
		  /*
		   * TODO implement me;
		   *  
		   * crate apropriate map-templates
		   * in SWIG-interface
		   */
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
			  agTop = agenda.get(agenda.size() - 1);
			  topNode = agTop.getKey();
			  topFragset = agTop.getValue();
			  
			  agenda.remove(agenda.size() - 1);
			  
			/*
			 * TODO 
			 * - implement FragmentSet.size() in SWIG
			 */
		  }
	  
	  
	  }
}
