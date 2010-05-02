package de.saar.chorus.domgraph.layout;

public class LayoutOptions {

	private LabelType labeltype;
	private boolean removeRdundandEdges;
	
	public LayoutOptions(LabelType lt, boolean rre) {
		labeltype = lt;
		removeRdundandEdges = rre;
	}
	
	public enum LabelType {
		NAME, LABEL, BOTH;
	}

	public LabelType getLabeltype() {
		return labeltype;
	}

	public void setLabeltype(LabelType labeltype) {
		this.labeltype = labeltype;
	}

	public boolean isRemoveRdundandEdges() {
		return removeRdundandEdges;
	}

	public void setRemoveRdundandEdges(boolean removeRdundandEdges) {
		this.removeRdundandEdges = removeRdundandEdges;
	}
	
	
	
}


