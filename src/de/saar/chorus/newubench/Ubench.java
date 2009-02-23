package de.saar.chorus.newubench;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.StringReader;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import de.saar.chorus.domgraph.ExampleManager;
import de.saar.chorus.domgraph.UserProperties;
import de.saar.chorus.domgraph.codec.CodecManager;
import de.saar.chorus.domgraph.codec.CodecRegistrationException;
import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.utool.ExitCodes;
import de.saar.chorus.domgraph.utool.Utool;

public class Ubench {
	private static Ubench instance = null;
	private JFrame window;
	private CommandListener commandListener;
	private CodecManager codecManager;
	private ExampleManager exampleManager;
	private TabManager tabManager;
	private File lastPath;
	private boolean fitWindowToNextGraph;





	/**
	 * Returns the (only) instance of the <code>Ubench</code>
	 * class. Creates a new <code>Ubench</code> object if there
	 * was none created yet, returns the already created instance
	 * otherwise.
	 * 
	 * @return the single <code>Ubench</code> instance
	 */
	public static Ubench getInstance() {
		if( instance == null ) {
			instance = new Ubench();
		}

		return instance;
	}

	private Ubench() {
		// register codecs
		codecManager = new CodecManager();
		try {
			codecManager.registerAllDeclaredCodecs();
		} catch (CodecRegistrationException e1) {
			System.err.println("An error occurred while trying to register a codec: " + e1);
			System.exit(ExitCodes.CODEC_REGISTRATION_ERROR);
		}
		codecManager.setAllowExperimentalCodecs(UserProperties.allowExperimentalCodecs());

		lastPath = new File(".");
		fitWindowToNextGraph = true;

		try {
			exampleManager = new ExampleManager();
			for( String dir : UserProperties.getExampleDirectories() ) {
				exampleManager.addAllExamples(dir);
			}
		} catch (de.saar.chorus.domgraph.ExampleManager.ParserException e) {
			JOptionPane.showMessageDialog(window,
					"A parsing error occurred " +
					"while reading an examples declaration." + 
					System.getProperty("line.separator") + 
					e + " (cause: " + e.getCause() + ")");

			Utool.exit(ExitCodes.EXAMPLE_PARSING_ERROR);
		}
		
		tabManager = new TabManager();
		
		commandListener = new CommandListener();

		// Set look and feel. Currently we are only setting the Windows L&F, as
		// the GTK L&F (for Linux) looks ugly even on Java 6.0, and on MacOS even
		// the standard L&F looks good.
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception e) {
		}


		Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler());
		/*
		eqs = null;
		eqsname = null;
		reduceAutomatically = false;
*/
		
		setupSwing();
	}
	
	private void setupSwing() {
		window = new JFrame("Underspecification Workbench");
		window.addWindowListener(commandListener);
		window.setMaximizedBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
		
		window.add(tabManager.getTabbedPane());
		
		window.pack();
		window.setVisible(true);
	}

	synchronized public void refresh() {
		if( fitWindowToNextGraph ) {
			window.pack();
			fitWindowToNextGraph = false;
		}
	}
	
	public JFrame getWindow() {
        return window;
    }
	
	public void quit() {
    	Utool.exit(0);
    }
	
	public CommandListener getCommandListener() {
		return commandListener;
	}
	
	public UbenchTab getCurrentTab() {
		return tabManager.getCurrentTab();
	}
	
	public TabManager getTabManager() {
		return tabManager;
	}
	
	
	
	public void loadDemoGraph() {
		InputCodec chainCodec = codecManager.getInputCodecForName("chain", "");
		
		DomGraph graph = new DomGraph();
		NodeLabels labels = new NodeLabels();
		FileUtilities.genericLoadGraph(new StringReader("3"), chainCodec, graph, labels);
		tabManager.addDomGraphTab("chain 3", graph, labels);

		DomGraph graph2 = new DomGraph();
		NodeLabels labels2 = new NodeLabels();
		FileUtilities.genericLoadGraph(new StringReader("4"), chainCodec, graph2, labels2);
		tabManager.addDomGraphTab("chain 4", graph2, labels2);

		window.pack();
	}
	
	
	public static void main(String[] args) {
		Ubench.getInstance();
		
		Ubench.getInstance().loadDemoGraph();
	}
}
