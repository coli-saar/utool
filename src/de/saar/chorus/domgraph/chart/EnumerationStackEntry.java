package de.saar.chorus.domgraph.chart;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.saar.chorus.domgraph.graph.DomEdge;

public class EnumerationStackEntry {
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
