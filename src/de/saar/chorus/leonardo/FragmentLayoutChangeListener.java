/*
 * Created on 02.02.2005
 *
 
 */
package de.saar.chorus.leonardo;


import org.jgraph.graph.DefaultGraphCell;

import org.jgraph.layout.*;
import org.jgraph.util.JGraphUtilities;

/**
 * @author Michaela
 *
 * Handles the menu added by the <code> addTestMenu </code> method
 * defined in <code> JDomGraph </code>.
 * Nice toy for a graph construction kit. 
 */
public class FragmentLayoutChangeListener implements DomGraphPopupListener {
	
	public void popupSelected(DefaultGraphCell source, Fragment fragment,
			String menuItem) 
	{
		// menu that prints positions and dimensions of the
		// fragments and nodes. 
		if(menuItem.equals("prInf")) {
			fragment.getParent().printPositions();
		} else {
			/*
			 * should apply some layout changes to a fragment.
			 * Doesn't work everytime (e.g. works not if there
			 * was just applied Sugiyama to the Graph before.)
			 * "repaint()" does not change anything.
			 * 
			 */
			if(menuItem.equals("tree")) {
				fragment.getParent().computeFragmentLayout(fragment, 
													new TreeLayoutAlgorithm());
			} else {
				if(menuItem.equals("sug")) {
					fragment.getParent().computeFragmentLayout(fragment, 
							new SugiyamaLayoutAlgorithm());
				} else {
					
					/*
					 * The "all"-Layout-menus doesn't wort at 
					 * all. Don't know why yet.
					 * "repaint()" does not change anything.
					 */
					if(menuItem.equals("allSug")) {
						JGraphUtilities.applyLayout(fragment.getParent(), 
								new SugiyamaLayoutAlgorithm());
						
					} else {
						if(menuItem.equals("allTree")) {
							JGraphUtilities.applyLayout(fragment.getParent(), 
									new TreeLayoutAlgorithm());
						}
					}
				}
			}
		}
	}
}
