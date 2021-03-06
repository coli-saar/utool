package de.saar.chorus.newubench;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import de.saar.basic.GenericFileFilter;
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
	
	private List<GenericFileFilter> ffInputCodecs;
	private List<GenericFileFilter> ffOutputCodecs;
	private List<GenericFileFilter> ffMultiOutputCodecs;





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
			new Ubench();
		}

		return instance;
	}

	private Ubench() {
		instance = this;
		
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
		setupCodecFileFilters();
	}
	
	private void setupCodecFileFilters() {
		ffInputCodecs = new ArrayList<GenericFileFilter>();
		ffOutputCodecs = new ArrayList<GenericFileFilter>();
		ffMultiOutputCodecs = new ArrayList<GenericFileFilter>();
		
		for( String codecname : codecManager.getAllInputCodecs() ) {
			String extension = codecManager.getInputCodecExtension(codecname);
			
			if( (codecname != null) && (extension != null)) {
				ffInputCodecs.add(new GenericFileFilter(extension, codecname));
			}
		}
		
		for( String codecname : codecManager.getAllOutputCodecs() ) {
			String extension = codecManager.getOutputCodecExtension(codecname);
			
			if( (codecname != null) && (extension != null)) {
				ffOutputCodecs.add(new GenericFileFilter(extension, codecname));
			}
		}
		
		for( String codecname : codecManager.getAllMultiOutputCodecs() ) {
			String extension = codecManager.getOutputCodecExtension(codecname);
			
			if( (codecname != null) && (extension != null)) {
				ffMultiOutputCodecs.add(new GenericFileFilter(extension, codecname));
			}
		}
	}

	private void setupSwing() {
		window = new JFrame("Underspecification Workbench");
		window.addWindowListener(commandListener);
		window.setMaximizedBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
		
		window.add(tabManager.getTabbedPane());
		
		window.setJMenuBar(new UbenchMenu());
		
		window.pack();
		window.setSize(300, 200);
		window.setLocationByPlatform(true);
		
		window.setVisible(true);
	}

	synchronized public void refresh() {
		if( fitWindowToNextGraph ) {
			window.pack();
			
			GraphicsEnvironment env =
	        	GraphicsEnvironment.getLocalGraphicsEnvironment();
			Rectangle bounds = env.getMaximumWindowBounds();
			Dimension oldWindowSize = window.getSize();
			Dimension windowsize = new Dimension(Math.min(bounds.width, oldWindowSize.width), Math.min(bounds.height, oldWindowSize.height));
			window.setSize(windowsize);
			
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
	
	public CodecManager getCodecManager() {
		return codecManager;
	}
	
	public ExampleManager getExampleManager() {
		return exampleManager;
	}
	
	public File getLastPath() {
		return lastPath;
	}
	
	public void setLastPath(File lastPath) {
		this.lastPath = lastPath;
	}
	
	public List<GenericFileFilter> getInputCodecFileFilters() {
		return ffInputCodecs;
	}
	
	public List<GenericFileFilter> getOutputCodecFileFilters() {
		return ffOutputCodecs;
	}
	
	public List<GenericFileFilter> getMultiOutputCodecFileFilters() {
		return ffMultiOutputCodecs;
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
		MacIntegration.integrate();
		Ubench.getInstance();
		
		//Ubench.getInstance().loadDemoGraph();
	}
}
