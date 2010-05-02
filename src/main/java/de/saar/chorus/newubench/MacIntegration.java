package de.saar.chorus.newubench;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;

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
			AuxiliaryWindows.displayAboutDialog();
			e.setHandled(true);
		}


		public void handlePreferences(ApplicationEvent e) {
			//XX Ubench.getInstance().setPreferenceDialogVisible(true);
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
