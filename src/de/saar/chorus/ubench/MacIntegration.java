package de.saar.chorus.ubench;

import javax.swing.JOptionPane;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;

import de.saar.chorus.domgraph.GlobalDomgraphProperties;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class MacIntegration {
	public static boolean isMac() {
		return System.getProperty("mrj.version") != null;
	}
	
	public static void integrate() {
		if( isMac() ) {
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Utool");
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			
			Application app = Application.getApplication();

			app.addAboutMenuItem();
			app.setEnabledAboutMenu(true);
			
			app.addPreferencesMenuItem();
			app.setEnabledPreferencesMenu(true);
			
			app.addApplicationListener(new UtoolApplicationListener());
			
		}
	}
	
	private static class UtoolApplicationListener implements ApplicationListener {
		
		public void handleAbout(ApplicationEvent e) {
			Ubench.getInstance().displayAboutDialog();
			e.setHandled(true);
		}


		public void handlePreferences(ApplicationEvent e) {
			Ubench.getInstance().setPreferenceDialogVisible(true);
			e.setHandled(true);
		}
		

		public void handleQuit(ApplicationEvent e) {
			Ubench.getInstance().quit();
			e.setHandled(true);
		}

		public void handleOpenApplication(ApplicationEvent event) {
			// TODO Auto-generated method stub
			
		}

		public void handlePrintFile(ApplicationEvent event) {
			// TODO Auto-generated method stub
			
		}

		public void handleReOpenApplication(ApplicationEvent event) {
			// TODO Auto-generated method stub
			
		}

		public void handleOpenFile(ApplicationEvent event) {
			// TODO Auto-generated method stub
			
		}
	}
	
}
