package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.JDomGraph;

public class ExampleViewer extends JFrame implements 
ListSelectionListener, ActionListener {
	
	
	private BorderLayout layout = new BorderLayout();
	private	File exampleFolder;
	private JPanel listContents;
	private JLabel desc;
	private JLabel prev;
	private JList files;
	private String[] exampleNames;
	private List<File> exampleFiles;
	private JButton load;
	private JButton cancel;
	public ExampleViewer() throws IOException {
		super("Open Example");
		setLayout(layout);
		setAlwaysOnTop(true);
		
		exampleFiles = Ubench.getInstance().getCodecManager().getExampleFiles();
		
		if( ! exampleFiles.isEmpty() ) {
			exampleNames = new String[exampleFiles.size()];
			
			for( int i = 0; i < exampleNames.length; i++) {
				exampleNames[i] = exampleFiles.get(i).getName();
			}
			
			
			
			files = new JList(exampleNames);
			files.addListSelectionListener(this);
			files.addMouseListener(new DoubleClickAdapter());
			files.addKeyListener(new EnterAdapter());
			
			JScrollPane listPane = new JScrollPane(files);
			
			listContents = new JPanel();
			listContents.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
			listContents.add(listPane, BorderLayout.WEST);
			
			JPanel preview = new JPanel();
			preview.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

			
			load = new JButton("Load!");
			load.addActionListener(this);
			load.setActionCommand("loEx");
			preview.add(load);
			
			cancel = new JButton ("Cancel");
			cancel.addActionListener(this);
			cancel.setActionCommand("cancel");
			preview.add(cancel);
			
			
			
			listContents.add(preview, BorderLayout.SOUTH);
			listContents.validate();
			add(listPane,BorderLayout.WEST);
			add(preview, BorderLayout.SOUTH);
			JPanel description = new JPanel();
			desc = new JLabel("No example selected.");
			description.add(desc);
			
			add(description, BorderLayout.EAST); 
			
			pack();
			validate();
		} else {
			throw new IOException("There are no examples in your Utool directory.");
			
		}
		//setVisible(true);
	}
	
	
	
	/* (non-Javadoc)
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent e) {
		
		
		String selected = exampleNames[files.getSelectedIndex()];
		
	/*	pack(); */
		validate(); 
		desc.setText("Example " + selected);
		
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		
		
		
		
		
		
		if(e.getActionCommand().equals("loEx")) {
			final String selected = exampleFiles.get(
					files.getSelectedIndex()).getAbsolutePath();
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
		} else if (e.getActionCommand().equals("cancel")) {
			setVisible(false);
			dispose();
		}
		
	}
	
	private class DoubleClickAdapter extends MouseAdapter {
		
		@Override
		public void mouseClicked(MouseEvent e) {
			if(e.getClickCount() == 2) {
				actionPerformed(new ActionEvent
						(load, ActionEvent.ACTION_PERFORMED, "loEx"));
			}
		}
		
	}
	
	private class EnterAdapter extends KeyAdapter {
		public void keyReleased(KeyEvent ke) {
			if( ke.getKeyCode() == KeyEvent.VK_ENTER) {
				actionPerformed(new ActionEvent
						(load, ActionEvent.ACTION_PERFORMED, "loEx"));
			}
			
			
		}
		
	}
	
}



