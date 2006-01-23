package de.saar.chorus.libdomgraph.chart;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.saar.chorus.libdomgraph.SWIGTYPE_p_Node;
import de.saar.chorus.libdomgraph.Split;
import de.saar.chorus.libdomgraph.SplitVector;

public class EnumerationStackEntry {
	
	private SWIGTYPE_p_Node dominator;
	private List<DomEdge> edgeAccu;
	private Split currentSplit;
	private List<Split> splits; // points into chart
	private Split lastElement;
	private Agenda agendaCopy;
	private Iterator<Split> splitIterator;
	
	EnumerationStackEntry(SWIGTYPE_p_Node dom, List<Split> spl,
			Agenda agend) {
		dominator = dom;
		splits = spl;
		agendaCopy = new Agenda();
		
		if( agend != null ) {
			agendaCopy.addAll(agend);
		}
		
		if( (spl != null) && (! spl.isEmpty() )  ) {
			
			splitIterator = splits.iterator();
			currentSplit = splitIterator.next();
			
			lastElement = splits.get(splits.size() - 1);
			
		} 
		
		edgeAccu = new ArrayList<DomEdge>();
	}
	
	EnumerationStackEntry(SWIGTYPE_p_Node dom, SplitVector spl,
			Agenda agend) {
		dominator = dom;
		splits = new ArrayList<Split>();
		
		
		
		agendaCopy = new Agenda();
		
		if( agend != null ) {
			agendaCopy.addAll(agend);
		}
		
		//if( (spl != null) && (! spl.isEmpty() )  ) {
			if( spl != null ) {	
			splits = WrapperTools.vectorToList(spl);

			splitIterator = splits.iterator();
			currentSplit = splitIterator.next();
			lastElement = splits.get((int) splits.size() - 1);
			
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
	public SWIGTYPE_p_Node getDominator() {
		return dominator;
	}

	/**
	 * @param dominator The dominator to set.
	 */
	public void setDominator(SWIGTYPE_p_Node dominator) {
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
	
}
