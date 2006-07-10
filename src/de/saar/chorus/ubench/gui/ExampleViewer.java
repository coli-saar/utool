package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.JDomGraph;

public class ExampleViewer extends JFrame implements ListSelectionListener, ActionListener {
	

	private BorderLayout layout = new BorderLayout();
	private	File exampleFolder;
	private JPanel listContents;
	private JLabel desc;
	private JLabel prev;
	private JList files;
	private File[] exampleFiles;
	private String[] exampleNames;
	public ExampleViewer() throws IOException {
		super();
		setLayout(layout);
		
		
		exampleFolder = new File("projects/Domgraph/examples/");
		if(! exampleFolder.isDirectory()) {
			exampleFolder = new File("examples/");
			if(! exampleFolder.isDirectory()) {
				throw new IOException("Couldn't find an example files."); 
			}
		}
		
		FileFilter exFilter = new ExampleFilter();
		
		
		exampleFiles = exampleFolder.listFiles(exFilter);
		exampleNames = new String[exampleFiles.length];
		
		for( int i = 0; i < exampleNames.length; i++ ) {
			exampleNames[i] = exampleFiles[i].getName();
		}
		
		files = new JList(exampleNames);
		files.addListSelectionListener(this);
		JScrollPane listPane = new JScrollPane(files);
		
		listContents = new JPanel();
		listContents.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		listContents.add(listPane, BorderLayout.WEST);
		
		JPanel preview = new JPanel();
		//preview.setPreferredSize(listPane.getSize());
		prev  = new JLabel();
		preview.add(prev);
		
		JButton load = new JButton("Load!");
		load.addActionListener(this);
		load.setActionCommand("loEx");
		preview.add(load, BorderLayout.SOUTH);
		
		listContents.add(preview, BorderLayout.EAST);
		listContents.validate();
		add(listPane,BorderLayout.WEST);
		add(preview, BorderLayout.EAST);
		JPanel description = new JPanel();
		desc = new JLabel("No example selected.");
		description.add(desc);
		
		add(description, BorderLayout.SOUTH); 
		
		pack();
		validate();
		//setVisible(true);
	}
	
	private class ExampleFilter implements FileFilter {

		/* (non-Javadoc)
		 * @see java.io.FileFilter#accept(java.io.File)
		 */
		public boolean accept(File pathname) {
			
		
				String name = pathname.getName();
				
				for(String ext : Ubench.getInstance().getCodecManager().getAllInputCodecExtensions() ) {
					if(name.endsWith(ext)) {
						return true;
					}
				}
				
			
			return false;
		}
		
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent e) {
	
		File selectedFile = exampleFiles[files.getSelectedIndex()];
		String selected = selectedFile.getName();
			
			pack();
			validate();
			desc.setText("Example " + selected);
        }

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		
		File selectedFile = exampleFiles[files.getSelectedIndex()];
		final String selected = selectedFile.getAbsolutePath();
		
		
		
		if(e.getActionCommand().equals("loEx")) {
		setVisible(false);
		Ubench.getInstance().getStatusBar().showProgressBar();
			new Thread(){
                public void run() {
                    
                    // loading the graph and converting it to a 
                    // JDomGraph
                    DomGraph theDomGraph = new DomGraph();
                    NodeLabels labels = new NodeLabels();
                    JDomGraph graph = Ubench.getInstance().genericLoadGraph(selected, theDomGraph, labels);
                    
                    
                    if( graph != null ) {
                        
                        //	DomGraphTConverter conv = new DomGraphTConverter(graph);
                        
                        // setting up a new graph tab.
                        // the graph is painted and shown at once.
                        Ubench.getInstance().addNewTab(graph, selected, theDomGraph, true, true, labels);
                    }
                }
            }.start();
		}
		
	}
    
		
	
}
