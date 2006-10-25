package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.saar.chorus.domgraph.ExampleManager;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.JDomGraph;

public class ExampleViewer extends JFrame implements 
ListSelectionListener, ActionListener {
	
	
	private BorderLayout layout = new BorderLayout();
	private	File exampleFolder;
	private JPanel listContents;
	private JTextArea desc;
	private JLabel prev;
	private JList files;
	private String[] exampleNames;
	private JButton load;
	private JButton cancel;
	private JScrollPane descriptionPane;
	private ExampleManager manager;
	public ExampleViewer() throws IOException {
		super("Open Example");
		setLayout(layout);
		setAlwaysOnTop(true);
		manager = Ubench.getInstance().getExampleManager();
		exampleNames = manager.getExampleNames().toArray(new String[] { });
		
		//exampleNames = Ubench.getInstance().getExampleManager().getExampleNames().toArray();
		
		if( exampleNames.length > 0 ) {
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
			
			JPanel description = new JPanel();
			desc = new JTextArea("No example selected.");
			desc.setEditable(false);
			desc.setAutoscrolls(true);
			desc.setColumns(30);
			desc.setBackground(Color.LIGHT_GRAY);
			desc.setOpaque(false);
			desc.setLineWrap(true);
			desc.setWrapStyleWord(true);
			
			TitledBorder border = new TitledBorder(
					new LineBorder(Color.GRAY, 1, true), 
					"Description",
					TitledBorder.CENTER,
					TitledBorder.ABOVE_TOP);
			
			
			description.add(desc, BorderLayout.CENTER);
			description.setBorder(border);
			descriptionPane = new JScrollPane(description);
			files.setAutoscrolls(true);
			
			add(files,BorderLayout.WEST);
			add(preview, BorderLayout.SOUTH);
			add(descriptionPane, BorderLayout.EAST); 
			
			pack();
			validate();
		} else {
			throw new IOException("There are no examples in your Utool directory.");
			
		}
		//setVisible(true);
	}
	
	private String killWhitespaces(String str) {
		return str.replaceAll("\\s+", " ");
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent e) {
		
		
		String selected = exampleNames[files.getSelectedIndex()];
		
	/*	pack(); */
		
		desc.setText(
				killWhitespaces(
						manager.getDescriptionForExample(selected)));
		
		
		//descriptionPane.validate();
		//files.validate();
		validate();
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		
		
		
		
		
		
		if(e.getActionCommand().equals("loEx")) {
			final String selected = exampleNames[files.getSelectedIndex()];
			setVisible(false);
			Ubench.getInstance().getStatusBar().showProgressBar();
			new Thread(){
				public void run() {
					
					// loading the graph and converting it to a 
					// JDomGraph
					DomGraph theDomGraph = new DomGraph();
					NodeLabels labels = new NodeLabels();
                    
                    Ubench u = Ubench.getInstance();
                    
					JDomGraph graph = 
                        u.genericLoadGraph(u.getExampleManager().getExampleReader(selected), 
                                u.getCodecManager().getInputCodecNameForFilename(selected),
                                theDomGraph, labels, null);
					
					
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



