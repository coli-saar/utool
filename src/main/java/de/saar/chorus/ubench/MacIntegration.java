package de.saar.chorus.ubench;

import org.madlonkay.desktopsupport.DesktopSupport;

public class MacIntegration {
	public static boolean isMac() {
		return "Mac OS X".equals(System.getProperty("os.name"));
	}
	
	public static void integrate() {
		if( isMac() ) {
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Utool");
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			
			DesktopSupport.getSupport().setAboutHandler(e -> {
				Ubench.getInstance().displayAboutDialog();
			});

			DesktopSupport.getSupport().setPreferencesHandler(e -> {
				Ubench.getInstance().setPreferenceDialogVisible(true);
			});

			DesktopSupport.getSupport().setQuitHandler((e, response) -> {
				Ubench.getInstance().quit();
			});
		}
	}
}
