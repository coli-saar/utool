package de.saar.chorus.ubench;


import de.saar.chorus.domgraph.layout.LayoutAlgorithm;
import de.saar.chorus.domgraph.layout.LayoutOptions.LabelType;
import de.saar.chorus.domgraph.layout.chartlayout.DomGraphChartLayout;
import de.saar.chorus.domgraph.layout.domgraphlayout.DomGraphLayout;
import de.saar.chorus.domgraph.layout.solvedformlayout.SFGecodeTreeLayout;

/**
 * This contains several preferences for layouting and solving 
 * graphs and a master object containing the general used
 * preferences.
 * Every <code>JDomGraph</code> has a <code>Preferences</code>
 * object of its own and has to be repaintet, if its preferences
 * are not aligned to the global master preferences.
 * 
 * The global preferences concerning layout and solving can be
 * changed by using the menu checkboxes of the <code>JDomGraphMenu</code>.
 * The default values are <code>true</code> for showing labels and
 * automatical solving and <code>false</code> for automatical 
 * window fitting.
 * 
 * @author Alexander Koller
 *
 */
public class Preferences implements Cloneable {
    
    // static fields: per-application preferences 
    private static boolean autoCount = true;
    private static boolean fitToWindow = false;
    private static boolean fitWindowToGraph = true;
    private static boolean removeRedundandEdges = true;
    
    // non-static fields: specific to each graph
    private LayoutType layouttype;
    private LabelType labeltype;
  
    
    public enum LayoutType {
    	JDOMGRAPH {
    		public LayoutAlgorithm getLayout() {
    			return new DomGraphLayout();
    		}

    		@Override
    		public boolean isApplicable(JDomGraphTab tab) {
    			return true;
    		}
    	},
    	
    	TREELAYOUT {
    		public LayoutAlgorithm getLayout () {
    			return new SFGecodeTreeLayout();
    		}
    		

    		@Override
    		public boolean isApplicable(JDomGraphTab tab) {
    			return tab.getDomGraph().isSolvedForm();
    		}
    	} ,
    	
    	CHARTLAYOUT {
    		public LayoutAlgorithm getLayout() {
    					return new DomGraphChartLayout();
    			
    		}

    		@Override
    		public boolean isApplicable(JDomGraphTab tab) {
    			return tab.isSolvedYet() && tab.isSolvable();
    		}
    	};
    	
    	public abstract LayoutAlgorithm getLayout();
    	public abstract boolean isApplicable(JDomGraphTab tab);
    }
    
    
    // the global preferences
    private static Preferences master;

    /**
     * Creating a new Preferences object
     * with the default values.
     */
	public Preferences() {
		labeltype = LabelType.LABEL;
		//layouttype = LayoutType.JDOMGRAPH;
		layouttype = LayoutType.CHARTLAYOUT;
		
	}
	
    
    /******** accessor methods **************/
	
	public void setLabelType(LabelType lt) {
		labeltype = lt;
	}
	
	public LabelType getLabelType() {
		return labeltype;
	}
    
	
	
    public static boolean isFitWindowToGraph() {
		return fitWindowToGraph;
	}


	public static void setFitWindowToGraph(boolean findWindowToGraph) {
		Preferences.fitWindowToGraph = findWindowToGraph;
	}


	public void setLayoutType(LayoutType lt) {
    	layouttype = lt;
    }
    
    public LayoutType getLayoutType() {
    	return layouttype;
    }
    
    
  
    
	
  
    
    public static boolean isRemoveRedundantEdges() {
		return removeRedundandEdges;
	}


	public static void setRemoveRedundandEdges(boolean removeRedundandEdges) {
		Preferences.removeRedundandEdges = removeRedundandEdges;
	}


	/**
     * Set to true, fitWindow will cause the visible and
     * all further opened graphs to be zoomed out if they 
     * oversize their tab. 
     * Default: <code>false</code>.
     * 
     * @param fit
     */
    public static void setFitToWindow(boolean fit) {
        fitToWindow = fit;
    }
    
    /**
     * 
     * @return true if all graphs are automatically solved
     */
    public static boolean isAutoCount() {
        return autoCount;
    }

    /**
     * This can enable or disable the automatical solving /
     * counting of solved forms. 
     * If disabled, a menu item for "manual" solving / 
     * solved form counting should get enabled!     
     * Default: <code>true</code> (automatical counting)
     * 
     * @param xautoCount 
     */
    public static void setAutoCount(boolean xautoCount) {
        autoCount = xautoCount;
    }

    /**
     * 
     * @return true if the graphs shall be fitted into their tab
     */
    public static boolean isFitToWindow() {
        return fitToWindow;
    }
    
  




    /*********** instance management methods **************/
    
    /**
     * Returns the master <code>Preferences</code> object
     * which is a new one if there is no master yet.
     * 
     * @return the master Preferences object
     */
    public static Preferences getInstance() {
        if( master == null ) {
            master = new Preferences();
        }
        
        return master;
    }
    
    /**
     * Clones this.
     * 
     * @return a clone of this Preferences
     */
    public Preferences clone() {
        try {
            return (Preferences) super.clone();
        } catch (CloneNotSupportedException e) {
            // This shouldn't happen because we are Cloneable.
            throw new InternalError();
        }
    }
    
    /**
     * Compares the given Preferences object with this one.
     * 
     * @param previousLayoutPreferences the preferences to compare to
     * @return true if the previous preferences are out of date
     */
    static boolean mustUpdateLayout(Preferences previousLayoutPreferences) {
        return (
        		(previousLayoutPreferences.layouttype != getInstance().layouttype) ||
        		(previousLayoutPreferences.labeltype != getInstance().labeltype));
    }

    
    /**
     * Copies these preferences to a second given
     * <code>Preferences</code> object.
     * 
     * @param second the Preferences to copy the values to
     */
	public void copyTo(Preferences second) {
        second.layouttype = layouttype;
        second.labeltype = labeltype;
	}


}
