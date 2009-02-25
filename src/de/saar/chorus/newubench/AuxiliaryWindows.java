package de.saar.chorus.newubench;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;

import de.saar.basic.GenericFileFilter;
import de.saar.chorus.domgraph.GlobalDomgraphProperties;
import de.saar.chorus.domgraph.codec.CodecManager;

public class AuxiliaryWindows {
	public static void showCodecWindow() {
		// show a list of all codecs installed
		Set<String> seen = new HashSet<String>();
		CodecManager manager = Ubench.getInstance().getCodecManager();
		
		StringBuffer codecList = new StringBuffer();
		
		// initialising a big HTML table
		codecList.append("<html><table border=\"0\">" +
		"<tr><th colspan=\"4\" align=\"left\">Input Codecs:</th></td>");
		
		// insert the input codecs first
		for( GenericFileFilter filter : Ubench.getInstance().getInputCodecFileFilters() ) {
			String codecname = 
				filter.getName();
			
			if(! seen.contains(codecname)) {
				seen.add(codecname);
				
				// if a codec it's experimental,
				// this is displayed
				String exp = 
					manager.isExperimentalInputCodec(codecname) ? "(EXPERIMENTAL!)" : "";
				
				codecList.append("<tr><td>" + filter.getName() + "</td><td> ("
						+ filter.getExtension() +
						")</td><td></td><td>" + exp +
				"</td></tr>");
			}
		}
		seen.clear();
		codecList.append("<tr><td colspan=\"4\"></td></tr>");
		codecList.append("<tr><th colspan=\"4\" align=\"left\">Output Codecs:</th></tr>");
		
		// the output codecs
		for( GenericFileFilter filter : Ubench.getInstance().getOutputCodecFileFilters() ) {
			String codecname = 
				filter.getName();
			
			if(! seen.contains(codecname)) {
				seen.add(codecname);
				
				// experimental codec?
				String exp = 
					manager.isExperimentalOutputCodec(codecname) ? "  (EXPERIMENTAL!)" : "";
				
				// multi-output-codec?
				String multi = 
					manager.isMultiOutputCodec(codecname) ? 
							"[M]" : "";
				
				codecList.append("<tr><td>" + filter.getName() + "</td><td> ("
						+ filter.getExtension() +
						")</td><td>" + multi +
						"</td><td>" + exp +
				"</td></tr>");
			}
		}
		
		codecList.append("</table><br><br>[M]: Allows output of " +
		"multiple graphs (applicable for solved form export)</html>");
		
		
		JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
				codecList, "Codecs in Utool", JOptionPane.INFORMATION_MESSAGE);
		
	}
	
	public static void displayAboutDialog() {
		JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
				"Underspecification Workbench running Utool version " + GlobalDomgraphProperties.getVersion() + System.getProperty("line.separator")
				+ "created by the CHORUS project, SFB 378, Saarland University"
				+ System.getProperty("line.separator") + System.getProperty("line.separator") 
				+ "http://" + GlobalDomgraphProperties.getHomepage()
				+ System.getProperty("line.separator") +System.getProperty("line.separator") +
				
				"JGraph version 1.0.3 & JGraphAddons version 1.0" + System.getProperty("line.separator") + 
				"(c) Gaudenz Alder et al., 2001-2004" + 
				
				System.getProperty("line.separator") + System.getProperty("line.separator") +
				
				"JGraphT version 0.6.0" + System.getProperty("line.separator") +
				"(c) Barak Naveh and Contributors, 2003-2005" +
				
				System.getProperty("line.separator") + System.getProperty("line.separator") +
				
				"iText version 1.3.1" + System.getProperty("line.separator") +
				"(c) Bruno Lowagie, 2005",
				
				"About the Underspecification Workbench", 
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	public static void showErrorMessage(String message, String title) {
		JOptionPane
		.showMessageDialog(
				Ubench.getInstance().getWindow(), message, title, JOptionPane.ERROR_MESSAGE);
	}
	
	public static void showInformationMessage(String message, String title) {
		JOptionPane
		.showMessageDialog(
				Ubench.getInstance().getWindow(), message, title, JOptionPane.INFORMATION_MESSAGE);
	}
}
