/*
 * @(#)CommandListener.java created 21.07.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;

import de.saar.basic.SwingComponentPDFWriter;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.codec.CodecManager;
import de.saar.chorus.domgraph.codec.OutputCodec;
import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.DomGraphTConverter;
import de.saar.chorus.ubench.JDomGraph;

/**
 * The main <code>ActionListener</code> and <code>ItemListener</code> 
 * of Leonardo's GUI. 
 * For file choosers, it provides some file filters and stores the 
 * last chosen path. 
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 *
 */
public class CommandListener implements ActionListener, ItemListener {
	private File lastPath = null;
	private String recentPath = ".", recentFile="";
	
	//private FileFilter ffInNativeGxl = new GenericFileFilter("dc.xml", "Domcon/GXL");
	private List<GenericFileFilter> ffInputCodecs;
	private List<GenericFileFilter> ffOutputCodecs;
	private OverallFileFilter showAll;
	
	
	private Map<Object,String> eventSources;
	
	/**
	 * Creates a new Instance of <code>CommandListener</code>.
	 */
	public CommandListener() {
		CodecManager codecman = Main.getCodecManager();
		
		// initializing fields
		eventSources = new HashMap<Object,String>();
		
		ffInputCodecs = new ArrayList<GenericFileFilter>();
		ffOutputCodecs = new ArrayList<GenericFileFilter>();
		showAll = new OverallFileFilter();
		
		for( Class codec : codecman.getAllInputCodecs() ) {
			String name = CodecManager.getCodecName(codec);
			String extension = CodecManager.getCodecExtension(codec);
			
			if( (name != null) && (extension != null)) {
				ffInputCodecs.add(new GenericFileFilter(extension, name));
				showAll.addExtension(extension);
			}
		}
		
		for( Class codec : codecman.getAllOutputCodecs() ) {
			String name = CodecManager.getCodecName(codec);
			String extension = CodecManager.getCodecExtension(codec);
			
			if( (name != null) && (extension != null)) {
				ffOutputCodecs.add(new GenericFileFilter(CodecManager.getCodecExtension(codec), CodecManager.getCodecName(codec)));
			}
		}
	}
	
	/**
	 * Overwrites the <code>actionPerformed</code> method of 
	 * <code>ActionListener</code>.
	 */
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		
		// no action command defined
		if( command == null ) {
			
			// looking up the source of the action
			command = lookupEventSource(e.getSource());
		}
		
		// no command and no source
		if( command == null ) {
			System.err.println("Undefined action command!");
			return;
		}
		
		
		/* Handling the known actions by identifying their command */
		
		
		// PDF-Printing
		if(command.equals("pdf")) {
			
			// file chooser with PDF-filter 
			JFileChooser fc = new JFileChooser();
			fc.setFileFilter(new pdfFilter());
			
			// if there was any path chosen before, the
			// file chooser will start in the related directory
			if(! (lastPath == null) ) {
				fc.setCurrentDirectory(lastPath);
			}
			
			// configuring button and window texts
			int fcVal =  fc.showDialog(Main.getWindow(), "Print PDF");
			fc.setApproveButtonText("Print!");
			
			// proceed with a chosen file
			if (fcVal == JFileChooser.APPROVE_OPTION) {
				
				// resolving the selected file's path
				File file = fc.getSelectedFile();
				String dir = file.getAbsolutePath();
				
				// updating the last chosen path
				lastPath = file.getParentFile();
				
				// if the file was named withoud pdf-extension,
				// the extension is added
				if(dir.indexOf(".pdf") > 0) {
					recentPath = dir;
				} else {
					recentPath = dir + ".pdf";
				}
				
				
				// a new thread for printing the pdf.
				// a progress bar will be visible while this
				// thread runs.
				new Thread() {
					public void run() {
						
						// that's just a guess...
						int taskLength = Main.getVisibleTab().numGraphNodes();
						
						// the progress bar and the panel containing it.
						JDialog progress = new JDialog(Main.getWindow(), false);
						JPanel dialogPane = new JPanel();
						JProgressBar progressBar = new JProgressBar(0, taskLength);
						
						// the OK-Button to press after printing is done
						// (it will close the dialog)
						JButton ok = new JButton("OK");
						ok.setActionCommand("ok");
						
						// text visible while printing
						JLabel export = new JLabel("Printing PDF...",SwingConstants.CENTER);
						export.setHorizontalAlignment(SwingConstants.CENTER);
						
						// listener for the button 
						ok.addActionListener(new JDialogListener(progress));
						
						// first configuration: indeterminate bar, OK is
						// disabeld
						progressBar.setStringPainted(true); 
						progressBar.setString(""); 
						progressBar.setIndeterminate(true);
						ok.setEnabled(false);
						
						// layouting the panel with the progress bar
						dialogPane.add(export, BorderLayout.NORTH);
						dialogPane.add(progressBar,BorderLayout.CENTER);
						dialogPane.add(ok,BorderLayout.SOUTH);
						dialogPane.doLayout();
						progress.add(dialogPane);
						progress.pack();
						progress.validate();
						
						// locating the panel centered
						progress.setLocation((Main.getWindow().getWidth() - progress.getWidth())/2,
								(Main.getWindow().getHeight() - progress.getHeight())/2); 
						
						progress.setVisible(true);
						
						try {
						// the actual PDF-printing
						SwingComponentPDFWriter.printToPDF(Main.getVisibleTab().getGraph(), recentPath);
						} catch (IOException io) {
							JOptionPane.showMessageDialog(Main.getWindow(),
									"The output file can't be opened.",
									"Error from PDF printer",
									JOptionPane.ERROR_MESSAGE);
						}
						// after finishing, the progress bar becomes
						// determined and fixated at maximum value (100%)
						progressBar.setMaximum(100);
						progressBar.setIndeterminate(false);
						progressBar.setValue(100);
						progressBar.setString("100%");
						
						// new text
						export.setText("Done!");
						export.setHorizontalTextPosition(SwingConstants.CENTER);
						
						dialogPane.validate();
						
						// enabling the button that closes
						// the dialog pane.
						ok.setEnabled(true);
						
					}
				}.start();
			}
			
		} else 
			// loading any graph file
			if( command.equals("loadGXL") ) {
				JFileChooser fc = new JFileChooser(recentPath);
				
				
				
				//    fc.addChoosableFileFilter(ffInNativeGxl);
				for( FileFilter ff : ffInputCodecs ) {
					fc.addChoosableFileFilter(ff);
				}
				fc.addChoosableFileFilter(showAll);
				
				if(! (lastPath == null) ) {
					fc.setCurrentDirectory(lastPath);
				}
				
				int fcVal = fc.showOpenDialog(Main.getWindow());
				
				// proceeding the selected file
				if (fcVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					
					// resolving the file's path
					recentPath = file.getAbsolutePath();
					recentFile = file.getName();
					
					// updating the last chosen path
					lastPath = file.getParentFile();
					
					// a new thread for loading and layouting the
					// graph
					new Thread(){
						public void run() {
							
							// loading the graph and converting it to a 
							// JDomGraph
							DomGraph theDomGraph = new DomGraph();
							NodeLabels labels = new NodeLabels();
							JDomGraph graph = Main.genericLoadGraph(recentPath, theDomGraph, labels);
							
							
							if( graph != null ) {
								
								//	DomGraphTConverter conv = new DomGraphTConverter(graph);
								
								// setting up a new graph tab.
								// the graph is painted and shown at once.
								Main.addNewTab(graph, recentFile, theDomGraph, true, true, labels);
							}
						}
					}.start();
					
				}
				
			} else 
				
				// exporting the visible graph.
				if( command.equals("saveUtool")) {
					JDomGraph graph = Main.getVisibleTab().getGraph();
					
					if( graph != null) {
						JFileChooser fc = new JFileChooser(recentPath);
						
						// show plugging codecs just for solved forms.
						// TODO perhaps find a more aesthetic solution here.
						if( Main.getVisibleTab() instanceof JSolvedFormTab) {
							for( FileFilter ff : ffOutputCodecs ) {
								fc.addChoosableFileFilter(ff);
							}
						} else {
							for( FileFilter ff : ffOutputCodecs ) {
								if(! ff.getDescription().contains("plugging") ) {
									fc.addChoosableFileFilter(ff);
								}
							}
						}
						
						
						if(! (lastPath == null) ) {
							fc.setCurrentDirectory(lastPath);
						}
						
						int fcVal = fc.showSaveDialog(Main.getWindow());
						if( fcVal == JFileChooser.APPROVE_OPTION ) {
							
							File file = fc.getSelectedFile();
							lastPath = file.getParentFile();
							String targetFile = file.getAbsolutePath();
                            String defaultExtension = ((GenericFileFilter) 
                            			fc.getFileFilter()).getExtension();
                            
                            if(! targetFile.endsWith(defaultExtension) ) {
                            	targetFile += defaultExtension;
                            	file = new File(targetFile);
                            }
							
							recentPath = file.getAbsolutePath();
							
							
							
							OutputCodec oc = 
								Main.getCodecManager().getOutputCodecForFilename(file.getName(),null);
							
							try {
								FileWriter writer = new FileWriter(file);
								oc.print_header(writer);
								oc.encode(Main.getVisibleTab().getDomGraph(),
										null, 
										Main.getVisibleTab().getNodeLabels(), 
										writer);
								oc.print_footer(writer);
							} catch (Exception ex) {
								JOptionPane.showMessageDialog(Main.getWindow(),
										"The output codec couldn't be determined, or it doesn't support"
										+ " output of this graph.",
										"Error from output codec",
										JOptionPane.ERROR_MESSAGE);
							}
							
						}
						
					}
					
				} else if ( command.equals("shut")) {
					
					//closing the visible graph.
					Main.closeCurrentTab();
				} else if ( command.equals("quit") ) {
					
					// closing the window
					Main.quit();
				} else if ( command.equals("dup")) {
					
					// duplicating the visible graph
					if(Main.getVisibleTab() != null) {
						Main.addTab(Main.getVisibleTab().clone(), true);
						
						
					}
				}  else if ( command.equals("fit")) {
					
					// fitting the visible graph to the window.
					if(Main.getVisibleTab() != null)
						Main.getVisibleTab().fitGraph();
				} else if ( command.equals("closeAll")) {
					
					// close all tabs (but not the window)
					Main.closeAllTabs();
				} else if ( command.equals("resL")) {
					
					// resetting the layout
					Main.getVisibleTab().resetLayout();
					Main.resetSlider();
				} else if ( command.equals("cSF")) {
					
					// solve the visible graph
					if( (Main.getVisibleTab() != null) ) {
						Main.showProgressBar();
						((JDomGraphTab) Main.getVisibleTab()).solve();
						Main.refresh();
					}
				}
		
				else if( command.equals("solvedFormDirectSelection") ) {
					// changed text field and pressed "return"
					
					long no = 1;
					no = Long.parseLong(((JSolvedFormTab) Main.getVisibleTab()).getSolvedForm().getText());
					
					if(! ((no < 1) || (no > Main.getVisibleTab().getSolvedForms()) ) ) {
						showSolvedFormWithIndex(no);
					} else {
						((JSolvedFormTab) Main.getVisibleTab()).resetSolvedFormText();
					}
				} else if(command.equals("plus")) {
					// ">" button in the status bar
					
					
					long no = ((JSolvedFormTab) Main.getVisibleTab()).getCurrentForm();
					if(no < Main.getVisibleTab().getSolvedForms()) {
						no++;
						showSolvedFormWithIndex(no);
				}
					
					Main.refresh();
				} else if (command.equals("minus")) {
					// "<" button in the status bar
					
					long no = ((JSolvedFormTab) Main.getVisibleTab()).getCurrentForm();
					if(no > 1) {
						no--;
						showSolvedFormWithIndex(no);
					}
					
					Main.refresh();
					
				} else if (command.equals("solve")) {
					// "solve" button in the status bar
					new Thread() {
						public void run() {
							JSolvedFormTab sFTab = ((JDomGraphTab) Main.getVisibleTab()).createFirstSolvedForm();
                            if( sFTab != null ) {
                                Main.addTab(sFTab, true);
                                Main.refresh();
                            }
						}
					}.start();
					
				} else if( command.equals("utool-about")) {
					//DomSolver solv = new DomSolver();
					
					JOptionPane.showMessageDialog(Main.getWindow(),
							"Utool version " + /*solv.getLibdomgraphVersion() +*/ " loaded.",
							"About utool",
							JOptionPane.INFORMATION_MESSAGE);
				} else if (command.equals("about") ) {
					JOptionPane.showMessageDialog(Main.getWindow(),
							"Ubench version 1.0" + System.getProperty("line.separator")
							+ "created by the CHORUS project, SFB 378, Saarland University"
							+ System.getProperty("line.separator") +System.getProperty("line.separator") +
							"libdomgraph version 2.0pre" + System.getProperty("line.separator") +
							"created by the CHORUS project, SFB 378, Saarland University"
							+ System.getProperty("line.separator") +System.getProperty("line.separator") +
							"JGraph version 1.0.3 & JGraphAddons version 1.0" + System.getProperty("line.separator") + 
							"(c) by Gaudenz Alder et al. 2001-2004" + 
							System.getProperty("line.separator") + System.getProperty("line.separator")
							+ "iText version 1.3.1" + System.getProperty("line.separator") +
							"(c) by Bruno Lowagie 2005",
							"About Ubench", 
							JOptionPane.INFORMATION_MESSAGE);
				}
	}
	
	
	/**
	 * Overwrites the <code>itemStateChanged</code> method of
	 * <code>ItemListener</code>.
	 * This handles the events occuring when one of the 
	 * preference checkboxes is (de-)selected.
	 */
	public void itemStateChanged(ItemEvent e) {
		String desc = lookupEventSource(e.getSource());
		
		// unknown event
		if( desc == null ) {
			System.err.println("Unknown item state change event!");
		} else 
			// layout change on displaying labels
			if(desc.equals("showLabels")) {
				
				// align preferences to selection state.
				if(e.getStateChange() == ItemEvent.SELECTED) {
					Preferences.getInstance().setShowLabels(true);
				} else {
					Preferences.getInstance().setShowLabels(false);
				}
				
				// refresh the visible graph if necessary.
				if(Main.getVisibleTab() != null) {
					Main.getVisibleTab().repaintIfNecessary();
				}
			} else 
				
				// checkbox indicating whether graphs are
				// solved right after loading automatically
				if(desc.equals("countAndSolve")) {
					
					// enable/disable menu items and change preferences
					if(e.getStateChange() == ItemEvent.SELECTED) {
						if( Main.getMenuBar() != null ) {
							Main.getMenuBar().setCountSfEnabled(false);
						}
						Preferences.setAutoCount(true);
						if( Main.getVisibleTab() != null  ) {
							((JDomGraphTab) Main.getVisibleTab()).solve();
							Main.refresh();
						}
						
					} else {
						if( Main.getMenuBar() != null ) {
							Main.getMenuBar().setCountSfEnabled(true);
						}
						
						Preferences.setAutoCount(false);
					}
				} else 
					
					// layout preferences concerning graph scaling
					if (desc.equals("fitAll")) {
						
						// change preferences and refresh the visible
						// graph 
						if(e.getStateChange() == ItemEvent.SELECTED) {
							Preferences.setFitToWindow(true);
							if( Main.getVisibleTab() != null ) {
								Main.getVisibleTab().fitGraph();
							}
						} else {
							Preferences.setFitToWindow(false);
						}
						
					}
	}
	
	/**
	 * TODO comment me!
	 * 
	 * @param source
	 * @param desc
	 */
	public void registerEventSource(Object source, String desc) {
		eventSources.put(source,desc);
	}
	
	/**
	 * TODO comment me!
	 * 
	 * @param source
	 * @return
	 */
	private String lookupEventSource(Object source) {
		return eventSources.get(source);
	}
	
	/**
	 * A <code>FileFilter</code> designed to 
	 * accept a succesively added collection of
	 * extensions.
	 * 
	 * @author Michaela Regneri
	 *
	 */
	private class OverallFileFilter extends FileFilter {

		Set<String> extensions;
		
		/**
		 *  Empty constructor (just for initialising).
		 *
		 */
		OverallFileFilter() {
			extensions = new HashSet<String>();
		}
		
		/**
		 * Initialise the Filter with a list of 
		 * extensions to accept.
		 * Please make sure to have a Collection of
		 * extension strings starting with "." !
		 * 
		 * @param ext
		 */
		OverallFileFilter(Collection<String> ext) {
			extensions = new HashSet<String>(ext);
		}
		
		public void addExtension(String extension) {
			if( extension.startsWith(".") ) {
				extensions.add(extension);
			} else {
				extensions.add("."+ extension);
			}
		}
		
		/**
		 * 
		 */
		public boolean accept(File f) {
			
			String fileName = f.getName();
			
			if( f.isDirectory() ) {
				return true;
			} 
			
			for(String extension : extensions ) {
				if(fileName.endsWith(extension) ) {
					return true;
				}
				
			}
			return false;
		}

		/**
		 * 
		 */
		public String getDescription() {
			return "All known file types";
		}
		
	}
	
	
	/**
	 * TODO comment me!
	 * 
	 * @author Alexander Koller
	 *
	 */
	private class GenericFileFilter extends FileFilter {
		private String extension;
		private String desc;
		
		/**
		 * 
		 * @param extension
		 * @param desc
		 */
		GenericFileFilter(String extension, String desc) {
			if( extension.startsWith(".") )
				this.extension = extension;
			else
				this.extension = "." + extension;
			
			this.desc = desc;
		}
		
		/**
		 * 
		 * @return
		 */
		public boolean accept(File f) {
			String fileName = f.getName();
			
			if( f.isDirectory() ) {
				return true;
			} 
			
			if(fileName.endsWith(extension) ) {
				return true;
			}
			
			return false;
		}
		
		/**
		 * 
		 * @return
		 */
		public String getDescription() {
			return "*" + extension + " (" + desc + ")";
		}
		
		public String getExtension() {
			return extension;
		}
		
	}
	
	/**
	 * A <code>FileFilter</code> that 
	 * accepts files with *.xml-extension.
	 * 
	 * @author Michaela Regneri
	 *
	 */
	public class xmlFilter extends FileFilter {
		
		/**
		 * Overwrites the <code>accept</code> method
		 * of <code>Filefilter</code>.
		 * 
		 * @return true if the file has an xml extension
		 */
		public boolean accept(File f) {
			String fileName = f.getName();
			if( f.isDirectory() ) {
				return true;
			} 
			if(fileName.indexOf(".xml") > 0) {
				return true;
			}
			return false;
		}
		
		/**
		 * Overwrites the <code>getDescription</code> 
		 * method of <code>FileFilter</code>.
		 * 
		 * @return just "XML"
		 */
		public String getDescription() {
			
			return "XML";
		}
	}
	
	/**
	 * A <code>FileFilter</code> that 
	 * accepts files with *.pdf-extension.
	 * 
	 * @author Michaela Regneri
	 *
	 */
	public class pdfFilter extends FileFilter {
		
		/**
		 * Overwrites the <code>accept</code> method
		 * of <code>Filefilter</code>.
		 * 
		 * @return true if the file has a pdf extension
		 */
		public boolean accept(File f) {
			String fileName = f.getName();
			if( f.isDirectory() ) {
				return true;
			} 
			if(fileName.indexOf(".pdf") > 0) {
				return true;
			}
			return false;
		}
		
		/**
		 * Overwrites the <code>getDescription</code>
		 * method of <code>FileFilter</code>.
		 * 
		 * @return just "PDF"
		 */
		public String getDescription() {
			return "PDF";
		}
		
	}
	
	/**
	 * A <code>FileView</code> that shows 
	 * PDF-Files.
	 * 
	 * @author Michaela Regneri
	 *
	 */
	public class pdfView extends FileView {
		
		/**
		 * Overwrites the <code>getDescription</code>
		 * method of <code>FileView</code>.
		 * 
		 * @return "PDF-file" if there is a pdf-file,
		 * 			otherwise the standard-description
		 */
		public String getDescription(File f) {
			if(f.getName().indexOf(".pdf") > 0) {
				return "PDF-file";
			} 
			return super.getDescription(f);
		}
		
	}
	
	/**
	 * Picks the solved form with the given number and 
	 * displays it. Should not be used to show the first form!
	 * (It would close the related graph then.)
	 *  
	 * @param no the number of the form to show.
	 */
	void showSolvedFormWithIndex(long no) {
		
		// extracting the wanted solved form
		SolvedFormIterator solver = Main.getVisibleTab().getSolvedFormIterator();
		NodeLabels labels = Main.getVisibleTab().getNodeLabels();
		
		de.saar.chorus.domgraph.graph.DomGraph nextForm =   (de.saar.chorus.domgraph.graph.DomGraph) Main.getVisibleTab().getDomGraph().clone();
		
		List<DomEdge> domEdges = solver.getSolvedForm((int) no-1);
		if( domEdges != null ) {
			nextForm.setDominanceEdges(domEdges);
		}
		int toInsertHere = Main.getVisibleTabIndex();
		
		// converting the form to a JDomGraph
		//JDomGraph domSolvedForm = new JDomGraph();
		//JDomDataFactory fac = new JDomDataFactory(nextForm, labels, domSolvedForm);
		//ImprovedJGraphAdapter.convert(nextForm, fac, domSolvedForm);
		
		
		DomGraphTConverter conv = new DomGraphTConverter(nextForm, labels);
		JDomGraph domSolvedForm = conv.getJDomGraph();
		
		
		// setting up the new tab
		JSolvedFormTab solvedFormTab = new JSolvedFormTab(domSolvedForm, 
				Main.getVisibleTab().getGraphName()  + "  SF #" + no, solver,
				nextForm,
				no, Main.getVisibleTab().getSolvedForms(), 
				Main.getVisibleTab().getGraphName(), 
				Main.getListener(), 
				labels);
		// closing the tab with the previous solved form and
		// showing the recent one.
		Main.closeCurrentTab();
		Main.addTab(solvedFormTab, true, toInsertHere);
		Main.getStatusBar().showBar(solvedFormTab.getBarCode());
		
	}
	
}
