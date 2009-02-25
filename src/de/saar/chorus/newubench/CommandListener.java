package de.saar.chorus.newubench;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.OutputCodec;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

@SuppressWarnings("unused")
public class CommandListener extends WindowAdapter implements ActionListener, ItemListener {
	private Map<Object,String> eventSources;

	/******** actions *********/

	public static final String QUIT="quit";
	@CommandAnnotation(command=QUIT)
	private void quit(String command) {
		Ubench.getInstance().quit();
	}

	public static final String SOLVE="solve";
	@CommandAnnotation(command=SOLVE)
	private void solve(String command) {
		UbenchTab tab = Ubench.getInstance().getCurrentTab();

		if( tab instanceof GraphTab ) {
			((GraphTab) tab).showFirstSolvedForm();
		}
	}

	public static final String NEXT="next";
	@CommandAnnotation(command=NEXT)
	private void next(String command) {
		UbenchTab tab = Ubench.getInstance().getCurrentTab();

		if( tab instanceof SolvedFormTab ) {
			((SolvedFormTab) tab).showNextSolvedForm();
		}
	}

	public static final String PREVIOUS="prev";
	@CommandAnnotation(command=PREVIOUS)
	private void previous(String command) {
		UbenchTab tab = Ubench.getInstance().getCurrentTab();

		if( tab instanceof SolvedFormTab ) {
			((SolvedFormTab) tab).showPreviousSolvedForm();
		}
	}

	public static final String JUMP_TO_SF="jumpToSf";
	@CommandAnnotation(command=JUMP_TO_SF)
	private void jumpToSf(String command) {
		UbenchTab tab = Ubench.getInstance().getCurrentTab();

		if( tab instanceof SolvedFormTab ) {
			((SolvedFormTab) tab).showSelectedSolvedForm();
		}
	}

	public static final String FILE_OPEN="fileOpen";
	@CommandAnnotation(command=FILE_OPEN)
	private void fileOpen(String command) {
		DomGraph graph = new DomGraph();
		NodeLabels labels = new NodeLabels();

		String filename = FileUtilities.loadGraphFromFilechooser(graph, labels);
		if( filename != null ) {
			Ubench.getInstance().getTabManager().addDomGraphTab(filename, graph, labels);
		}
	}

	public static final String FILE_OPEN_EXAMPLE="fileExample";
	@CommandAnnotation(command=FILE_OPEN_EXAMPLE)
	private void fileOpenExample(String command) {
		new ExampleViewer().setVisible(true);
	}

	public static final String EXPORT_CLIPBOARD="export-clipboard-";
	@CommandAnnotation(command=EXPORT_CLIPBOARD)
	private void exportClipboard(String command) {
		String codecname = command.substring(EXPORT_CLIPBOARD.length());
		OutputCodec codec = Ubench.getInstance().getCodecManager().getOutputCodecForName(codecname, "");

		StringWriter buf = new StringWriter();

		try {
			Ubench.getInstance().getCurrentTab().printGraph(buf, codec);
			new MyClipboardOwner().setClipboardContents(buf.toString());
		} catch (IOException e1) {
			// highly unlikely unless the StringWriter ran out of memory or something
			JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
					"An error occurred while writing into an internal buffer.",
					"Error during output",
					JOptionPane.ERROR_MESSAGE);
		} catch (MalformedDomgraphException e1) {
			JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
					"The output codec doesn't support output of this graph:\n" + e1,
					"Error during output",
					JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public static final String FILE_SAVE="fileSave";
	@CommandAnnotation(command=FILE_SAVE)
	private void fileSave(String command) {
		FileUtilities.saveGraphToFilechooser();
	}
	
	public static final String FILE_SAVE_SOLVED_FORMS="fileSavAllSolvedForms";
	@CommandAnnotation(command=FILE_SAVE_SOLVED_FORMS)
	private void fileSaveSolvedForms(String command) {
		UbenchTab tab = Ubench.getInstance().getCurrentTab();
		
		if( tab instanceof GraphTab ) {
			((GraphTab) tab).saveAllSolvedFormsToFilechooser();
		}
	}

	public static final String DISPLAY_CODECS="fileDisplayCodecs";
	@CommandAnnotation(command=DISPLAY_CODECS)
	private void fileDisplayCodecs(String command) {
		AuxiliaryWindows.showCodecWindow();
	}

	public static final String IMPORT_CLIPBOARD="import-clipboard-";
	@CommandAnnotation(command=IMPORT_CLIPBOARD)
	private void importClipboard(String command) {
		String codecname = command.substring(IMPORT_CLIPBOARD.length());
		InputCodec codec = Ubench.getInstance().getCodecManager().getInputCodecForName(codecname, "");
		String clip = new MyClipboardOwner().getClipboardContents();
		
		Ubench.getInstance().getTabManager().addDomGraphTab("(from clipboard)", new StringReader(clip), codec);
	}




	/********* event handling ***********/

	public void actionPerformed(ActionEvent ev) {
		// obtain the command
		String command = ev.getActionCommand();

		if( command == null ) {
			command = lookupEventSource(ev.getSource());
		}

		if( command == null ) {
			System.err.println("Undefined action command!");
			return;
		}

		// call the appropriate method
		call(command);
	}

	public void itemStateChanged(ItemEvent ev) {
		String command = lookupEventSource(ev.getSource());

		if( command == null ) {
			System.err.println("Undefined item state change command!");
			return;
		}

		call(command);
	}

	@Override
	public void windowClosing(WindowEvent e) {
		quit(null);
	}

	private void call(String command) {
		System.err.println("Call: " + command);

		try {
			for( Method m : getClass().getDeclaredMethods() ) {
				if( m.isAnnotationPresent(CommandAnnotation.class) ) {
					if( command.startsWith(m.getAnnotation(CommandAnnotation.class).command()) ) {
						m.invoke(this, command);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD) 
	private static @interface CommandAnnotation {
		String command();
	}




	/********* event source handling ***********/

	public void registerEventSource(Object source, String desc) {
		eventSources.put(source,desc);
	}

	private String lookupEventSource(Object source) {
		return eventSources.get(source);
	}


	public CommandListener() {
		eventSources = new HashMap<Object, String>();
	}



	/******* clipboard ***********/
	// Code adapted from http://www.javapractices.com/Topic82.cjp
	private static class MyClipboardOwner implements ClipboardOwner {

		public void lostOwnership(Clipboard arg0, Transferable arg1) {
			// do nothing
		}

		/**
		 * Place a String on the clipboard, and make this class the
		 * owner of the Clipboard's contents.
		 */
		public void setClipboardContents( String aString ){
			StringSelection stringSelection = new StringSelection( aString );
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents( stringSelection, this );
		}

		/**
		 * Get the String residing on the clipboard.
		 *
		 * @return any text found on the Clipboard; if none found, return an
		 * empty String.
		 */
		public String getClipboardContents() {
			String result = "";
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			//odd: the Object param of getContents is not currently used
			Transferable contents = clipboard.getContents(null);
			boolean hasTransferableText =
				(contents != null) &&
				contents.isDataFlavorSupported(DataFlavor.stringFlavor)
				;
			if ( hasTransferableText ) {
				try {
					result = (String)contents.getTransferData(DataFlavor.stringFlavor);
				}
				catch (UnsupportedFlavorException ex){
					//highly unlikely since we are using a standard DataFlavor
					System.out.println(ex);
					ex.printStackTrace();
				}
				catch (IOException ex) {
					System.out.println(ex);
					ex.printStackTrace();
				}
			}
			return result;
		}
	} 
}
