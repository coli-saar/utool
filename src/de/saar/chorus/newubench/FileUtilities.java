package de.saar.chorus.newubench;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import de.saar.basic.GUIUtilities;
import de.saar.basic.GenericFileFilter;
import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.ParserException;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;

public class FileUtilities {

	/**
	 * Loads a labelled dominance graph from a reader.
	 * 
	 * @param reader the Reader from which the graph is read
	 * @param codec the name of the input codec that should be used
	 * to decode the graph 
	 * @param graph a <code>DomGraph</code> which this method sets
	 * to the dominance graph part of the labelled graph
	 * @param nl a <code>NodeLabels</code> object which this method
	 * fills with the node labelling part of the labelled graph
	 * @return a new <code>JDomGraph</code> representation for the
	 * labelled graph
	 */
	static public boolean genericLoadGraph(Reader reader, InputCodec inputCodec, DomGraph graph, NodeLabels nl) {
		if(inputCodec != null ) {
			try {
				inputCodec.decode(reader, graph, nl);

			} catch (IOException e) {
				JOptionPane
				.showMessageDialog(
						Ubench.getInstance().getWindow(),
						"The specified file doesn't exist or cannot be opened.",
						"Error during import", JOptionPane.ERROR_MESSAGE);
				//e.printStackTrace();
				return false;
			} catch (ParserException pe) {
				JOptionPane
				.showMessageDialog(
						Ubench.getInstance().getWindow(),
						"A parsing error occurred while reading the input file:\n"
						+ pe,
						"Error during import", JOptionPane.ERROR_MESSAGE);

				return false;
			} catch (MalformedDomgraphException me) {
				JOptionPane
				.showMessageDialog(
						Ubench.getInstance().getWindow(),
						"A semantic error occurred while decoding the graph:\n" +
						me,
						"Error during import", JOptionPane.ERROR_MESSAGE);

				return false;
			}
		} else {
			JOptionPane
			.showMessageDialog(
					Ubench.getInstance().getWindow(),
					"The filename extension of this file is not associated with any known input codec.",
					"Error during import", JOptionPane.ERROR_MESSAGE);

			return false;
		}


		return true;
	}

	public static File getFileFromExportFileChooser(List<GenericFileFilter> fileFilters, Map<String,String> options) {
		final CodecFileChooser fc = new CodecFileChooser(
				Ubench.getInstance().getLastPath().getAbsolutePath(),
				CodecFileChooser.Type.EXPORT);

		fc.addCodecFileFilters(fileFilters);
		fc.setCurrentDirectory(Ubench.getInstance().getLastPath());
		fc.setAcceptAllFileFilterUsed(false);		

		int fcVal = GUIUtilities.confirmFileOverwriting(fc, Ubench.getInstance().getWindow());

		if( fcVal == JFileChooser.APPROVE_OPTION ) {
			File file = fc.getSelectedFile();
			Ubench.getInstance().setLastPath( file.getParentFile() );

			String defaultExtension = ((GenericFileFilter) fc.getFileFilter()).getExtension();
			if( !file.getName().endsWith(defaultExtension) ) {
				file = new File(file.getAbsolutePath() + defaultExtension);
			}
			
			options.clear();
			options.putAll(fc.getCodecOptions());
			
			return file;
		} else {
			return null;
		}
	}
	
	public static File getFileFromOpenFileChooser(List<GenericFileFilter> fileFilters, Map<String,String> options) {
		CodecFileChooser fc = new CodecFileChooser(
				Ubench.getInstance().getLastPath().getAbsolutePath(),
				CodecFileChooser.Type.OPEN);
		
		fc.addCodecFileFilters(fileFilters);
		fc.setCurrentDirectory(Ubench.getInstance().getLastPath());

		int fcVal = fc.showOpenDialog(Ubench.getInstance().getWindow());	

		if (fcVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			Ubench.getInstance().setLastPath(file.getParentFile());
			
			options.clear();
			options.putAll(fc.getCodecOptions());
			
			return file;
		} else {
			return null;
		}
	}
	
	
}
