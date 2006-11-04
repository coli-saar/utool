/*
 * @(#)CommandListener.java created 21.07.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.ubench.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;

import de.saar.basic.ExportUtilities;
import de.saar.basic.GenericFileFilter;
import de.saar.basic.WaitingDialog;
import de.saar.chorus.domgraph.GlobalDomgraphProperties;
import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.codec.CodecManager;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.MultiOutputCodec;
import de.saar.chorus.domgraph.codec.OutputCodec;
import de.saar.chorus.domgraph.equivalence.EquationSystem;
import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.utool.AbstractOptions;
import de.saar.chorus.domgraph.utool.server.ConnectionManager;
import de.saar.chorus.domgraph.utool.server.ConnectionManager.State;
import de.saar.chorus.ubench.DomGraphTConverter;
import de.saar.chorus.ubench.JDomGraph;
import de.saar.chorus.ubench.ServerOptions;

/**
 * The main <code>ActionListener</code> and <code>ItemListener</code> 
 * of Ubench's GUI. 
 * For file choosers, it provides some file filters and stores the 
 * last chosen path. 
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 *
 */
public class CommandListener implements ActionListener, 
ItemListener, ConnectionManager.StateChangeListener {


	
	//private FileFilter ffInNativeGxl = new GenericFileFilter("dc.xml", "Domcon/GXL");
	private List<GenericFileFilter> ffInputCodecs;
	private List<GenericFileFilter> ffOutputCodecs;
	private List<GenericFileFilter> ffMultiOutputCodecs;
	
	
	private Map<Object,String> eventSources;
	
	/**
	 * Creates a new Instance of <code>CommandListener</code>.
	 */
	public CommandListener() {
		CodecManager codecman = Ubench.getInstance().getCodecManager();		
		ConnectionManager.addListener(this);
		
		// to debug the Codec Options.
		try{
			codecman.registerCodec(DummyCodec.class);
		} catch (Exception e) {
			System.err.println("The DummyCodec cannot be registered.");
			System.err.println(e.getMessage());
		}
		// initializing fields
		eventSources = new HashMap<Object,String>();
		
		ffInputCodecs = new ArrayList<GenericFileFilter>();
		ffOutputCodecs = new ArrayList<GenericFileFilter>();
		ffMultiOutputCodecs = new ArrayList<GenericFileFilter>();
		
		for( String codecname : codecman.getAllInputCodecs() ) {
			String extension = codecman.getInputCodecExtension(codecname);
			
			if( (codecname != null) && (extension != null)) {
				ffInputCodecs.add(new GenericFileFilter(extension, codecname));
			}
		}
		
		for( String codecname : codecman.getAllOutputCodecs() ) {
			String extension = codecman.getOutputCodecExtension(codecname);
			
			if( (codecname != null) && (extension != null)) {
				ffOutputCodecs.add(new GenericFileFilter(extension, codecname));
			}
		}
		
		for( String codecname : codecman.getAllMultiOutputCodecs() ) {
			String extension = codecman.getOutputCodecExtension(codecname);
			
			if( (codecname != null) && (extension != null)) {
				ffMultiOutputCodecs.add(new GenericFileFilter(extension, codecname));
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
		
		// picture export
		if( command.equals("preferences") ) {
			Ubench.getInstance().setPreferenceDialogVisible(true);
		} else if(command.equals("loadeqs")){
			loadEQS();
		} else if(command.equals("server")) {
			if(Ubench.getInstance().getMenuBar().isServerButtonPressed())
			{final  AbstractOptions op = new AbstractOptions();
			
			op.setOptionLogging(ServerOptions.isLogging());
			op.setLogWriter(ServerOptions.getLogwriter());
			op.setOptionWarmup(ServerOptions.isWarmup());
			op.setPort(ServerOptions.getPort());
			
			
			new Thread() {
			    public void run() {
			        try {
			            ConnectionManager.startServer(op);
			        }
			        catch( IOException ex ) {
			        	JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
								ex.getMessage(),
								"Server Error",
								JOptionPane.ERROR_MESSAGE);
			        }
			    }
			}.start();
			} else {
				ConnectionManager.stopServer();
			}
		} else if(command.equals("print")) {
			WaitingDialog progress = new WaitingDialog("Printing Graph...", 
					Ubench.getInstance().getWindow());
			progress.beginTask();
			ExportUtilities.printComponent(Ubench.getInstance().getVisibleTab().getGraph());
			progress.endTask();
		} else {
			// loading any graph file
			if( command.equals("loadGXL") ) {
				final JCodecFileChooser fc = new JCodecFileChooser(
						Ubench.getInstance().getLastPath().getAbsolutePath(),
						JCodecFileChooser.Type.OPEN);
				
				fc.addCodecFileFilters(ffInputCodecs);
				
				fc.setCurrentDirectory(Ubench.getInstance().getLastPath());
				Component owner;
				if(Ubench.getInstance().getVisibleTab().hasVisibleChartViewer()) {
					owner = Ubench.getInstance().getVisibleTab().getChartViewer();
				} else {
					owner = Ubench.getInstance().getWindow();
				}
				int fcVal = fc.showOpenDialog(owner);	

				// proceeding the selected file
				if (fcVal == JFileChooser.APPROVE_OPTION) {
					final File file = fc.getSelectedFile();
					
								
					// updating the last chosen path
					Ubench.getInstance().setLastPath(file.getParentFile());
					
					// a new thread for loading and layouting the
					// graph
					new Thread(){
						public void run() {
							
							// loading the graph and converting it to a 
							// JDomGraph
							DomGraph theDomGraph = new DomGraph();
							NodeLabels labels = new NodeLabels();
							JDomGraph graph = Ubench.getInstance().genericLoadGraph(file.getAbsolutePath(), 
									theDomGraph, labels, fc.getCodecOptions());
							
							
							if( graph != null ) {
								
								//	DomGraphTConverter conv = new DomGraphTConverter(graph);
								
								// setting up a new graph tab.
								// the graph is painted and shown at once.
								Ubench.getInstance().addNewTab(graph, file.getName(), theDomGraph, true, true, labels);
							}
						}
					}.start();
					
				}
				
			} else 
				
				// exporting the visible graph.
				if( command.equals("saveUtool")) {
					JDomGraph graph = Ubench.getInstance().getVisibleTab().getGraph();
					
					if( graph != null) {
						JCodecFileChooser fc = new JCodecFileChooser(
								Ubench.getInstance().getLastPath().getAbsolutePath(),
								JCodecFileChooser.Type.EXPORT);
						fc.addCodecFileFilters(ffOutputCodecs);
						
						fc.setCurrentDirectory(Ubench.getInstance().getLastPath());
						fc.setAcceptAllFileFilterUsed(false);
						
						fc.setCurrentDirectory(Ubench.getInstance().getLastPath());
						Component owner;
						if(Ubench.getInstance().getVisibleTab().hasVisibleChartViewer()) {
							owner = Ubench.getInstance().getVisibleTab().getChartViewer();
						} else {
							owner = Ubench.getInstance().getWindow();
						}
						int fcVal = fc.showSaveDialog(owner);	
						if( fcVal == JFileChooser.APPROVE_OPTION ) {
							
							File file = fc.getSelectedFile();
							
							Ubench.getInstance().setLastPath( file.getParentFile() );
							String targetFile = file.getAbsolutePath();
							String defaultExtension = ((GenericFileFilter) 
									fc.getFileFilter()).getExtension();
							
							if(! targetFile.endsWith(defaultExtension) ) {
								targetFile += defaultExtension;
								file = new File(targetFile);
							}
							
												
							
							
							OutputCodec oc = 
								Ubench.getInstance().getCodecManager().getOutputCodecForFilename(file.getName(),fc.getCodecOptions());
							if( oc != null ) {
								try {
									FileWriter writer = new FileWriter(file);
									oc.print_header(writer);
									oc.encode(Ubench.getInstance().getVisibleTab().getDomGraph(),
											Ubench.getInstance().getVisibleTab().getNodeLabels(), 
											writer);
									oc.print_footer(writer);
								} catch (IOException ex) {
									JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
											"The specified file cannot be created.",
											"Error during output",
											JOptionPane.ERROR_MESSAGE);
								} catch (MalformedDomgraphException md) {
									JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
											"The output codec doesn't support output of this graph:\n" + md,
											"Error during output",
											JOptionPane.ERROR_MESSAGE);
								} catch (UnsupportedOperationException uE) {
									JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
											uE.getMessage(),
											"Error during output",
											JOptionPane.ERROR_MESSAGE);
								}
							} else {
								JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
										"The filename extension of this file is not associated with any known output codec.",
										"Error during output",
										JOptionPane.ERROR_MESSAGE);
							}
							
						}
						
					}
					
				} else if ( command.equals("shut")) {
					
					//closing the visible graph.
					Ubench.getInstance().closeCurrentTab();
				} else if ( command.equals("quit") ) {
					
					// closing the window
					Ubench.getInstance().quit();
				} else if ( command.equals("dup")) {
					
					// duplicating the visible graph
					if(Ubench.getInstance().getVisibleTab() != null) {
						Ubench.getInstance().addTab(Ubench.getInstance().getVisibleTab().clone(), true);
						
						
					}
				}  else if ( command.equals("fit")) {
					
					// fitting the visible graph to the window.
					if(Ubench.getInstance().getVisibleTab() != null)
						Ubench.getInstance().getVisibleTab().fitGraph();
				} else if ( command.equals("closeAll")) {
					
					// close all tabs (but not the window)
					Ubench.getInstance().closeAllTabs();
				} else if ( command.equals("resL")) {
					
					// resetting the layout
					Ubench.getInstance().getVisibleTab().resetLayout();
					Ubench.getInstance().resetSlider();
				} else if ( command.equals("cSF")) {
					
					// solve the visible graph
					if( (Ubench.getInstance().getVisibleTab() != null) ) {
						Ubench.getInstance().showProgressBar();
						((JDomGraphTab) Ubench.getInstance().getVisibleTab()).solve();
						Ubench.getInstance().refresh();
					}
				}
			
				else if( command.equals("solvedFormDirectSelection") ) {
					// changed text field and pressed "return"
					
					long no = 1;
					no = Long.parseLong(((JSolvedFormTab) Ubench.getInstance().getVisibleTab()).getSolvedForm().getText());
					
					if(! ((no < 1) || (no > Ubench.getInstance().getVisibleTab().getSolvedForms()) ) ) {
						showSolvedFormWithIndex(no);
					} else {
						((JSolvedFormTab) Ubench.getInstance().getVisibleTab()).resetSolvedFormText();
					}
				} else if(command.equals("plus")) {
					// ">" button in the status bar
					
					
					long no = ((JSolvedFormTab) Ubench.getInstance().getVisibleTab()).getCurrentForm();
					if(no < Ubench.getInstance().getVisibleTab().getSolvedForms()) {
						no++;
						showSolvedFormWithIndex(no);
					}
					
					Ubench.getInstance().refresh();
				} else if (command.equals("minus")) {
					// "<" button in the status bar
					
					long no = ((JSolvedFormTab) Ubench.getInstance().getVisibleTab()).getCurrentForm();
					if(no > 1) {
						no--;
						showSolvedFormWithIndex(no);
					}
					
					Ubench.getInstance().refresh();
					
				} else if (command.equals("solve")) {
					// "solve" button in the status bar
					new Thread() {
						public void run() {
							JSolvedFormTab sFTab = ((JDomGraphTab) Ubench.getInstance().getVisibleTab()).createFirstSolvedForm();
							if( sFTab != null ) {
								Ubench.getInstance().addTab(sFTab, true);
								Ubench.getInstance().getMenuBar().setPlusMinusEnabled(true,false);
								Ubench.getInstance().refresh();
							}
						}
					}.start();
					
				} else if (command.equals("about") ) {
					JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
							"Underspecification Workbench running Domgraph version " + GlobalDomgraphProperties.getVersion() + System.getProperty("line.separator")
							+ "created by the CHORUS project, SFB 378, Saarland University"
							
							+ System.getProperty("line.separator") +System.getProperty("line.separator") +
							
							"JGraph version 1.0.3 & JGraphAddons version 1.0" + System.getProperty("line.separator") + 
							"(c) Gaudenz Alder et al., 2001-2004" + 
							
							System.getProperty("line.separator") + System.getProperty("line.separator") +
							
							"JGraphT version 0.6.0" + System.getProperty("line.separator") +
							"(c) Barak Naveh and Contributors, 2003-2005" +
							
							System.getProperty("line.separator") + System.getProperty("line.separator") +
							
							"iText version 1.3.1" + System.getProperty("line.separator") +
							"(c) Bruno Lowagie, 2005",
							
							"About the Underspecification Workbench", 
							JOptionPane.INFORMATION_MESSAGE);
				} else if (command.equals("loadExample")) {
					try{
						ExampleViewer exview = new ExampleViewer();
						exview.setVisible(true);
					} catch(IOException ex) {
						JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
								ex.getMessage(),
								"Error during example loading",
								JOptionPane.ERROR_MESSAGE);
					}
				} else if (command.equals("saveAll")) {
					//JChooseFrame frame = new JChooseFrame();
					JDomGraph graph = Ubench.getInstance().getVisibleTab().getGraph();
					
					if( graph != null) {
						JCodecFileChooser fc = new JCodecFileChooser(
								Ubench.getInstance().getLastPath().getAbsolutePath(),
								JCodecFileChooser.Type.EXPORT_SOLVED_FORMS);
						fc.addCodecFileFilters(ffMultiOutputCodecs);

						fc.setSelectedFile(new File(Ubench.getInstance().
								getVisibleTab().getDefaultName() + 
						"_solvedForms" + 
						((GenericFileFilter) fc.getFileFilter()).getExtension()));
						
						fc.setCurrentDirectory(Ubench.getInstance().getLastPath());

						Component owner;
						if(Ubench.getInstance().getVisibleTab().hasVisibleChartViewer()) {
							owner = Ubench.getInstance().getVisibleTab().getChartViewer();
						} else {
							owner = Ubench.getInstance().getWindow();
						}
						
						int fcVal = fc.showSaveDialog(owner);
						
						
						if( fcVal == JFileChooser.APPROVE_OPTION ) {
							
							File file = fc.getSelectedFile();
							Ubench.getInstance().setLastPath(file.getParentFile());
							String targetFile = file.getAbsolutePath();
							
							String defaultExtension = ((GenericFileFilter) 
									fc.getFileFilter()).getExtension();
							
							final String targetfileName;
							if(! targetFile.endsWith(defaultExtension) ) {
								targetfileName = targetFile + defaultExtension;
								
							}  else {
								targetfileName = targetFile;
							}
							
							final File outputfile = new File(targetfileName);
							final MultiOutputCodec oc = 
								(MultiOutputCodec) Ubench.getInstance().getCodecManager().
								getOutputCodecForFilename(outputfile.getName(), fc.getCodecOptions());
							new Thread() {
								public void run() {
									
									
									// that's just a guess...
									
									WaitingDialog progress = new WaitingDialog("Printing Solutions",
											Ubench.getInstance().getWindow());
									progress.beginTask();
									
									
									Chart chart = new Chart();
									DomGraph cgraph = Ubench.getInstance().
									getVisibleTab().getDomGraph().compactify();
									
									DomGraph graph = Ubench.getInstance().
									getVisibleTab().getDomGraph();
									long start_solver = System.currentTimeMillis();
									ChartSolver.solve(cgraph, chart);
									long end_solver = System.currentTimeMillis();
									long time_solver = end_solver - start_solver;
									
									
									
									if(oc != null) {
										try {
											FileWriter writer = new FileWriter(outputfile);
											long start_extraction = System.currentTimeMillis();
											long count = 0;
											SolvedFormIterator it = new SolvedFormIterator(chart,graph);
											
											oc.print_header(writer);
											oc.print_start_list(writer);
											while( it.hasNext() ) {
												List<DomEdge> domedges = it.next();
												count++;
												
												
												if( count > 1 ) {
													oc.print_list_separator(writer);
												}
												oc.encode(graph.withDominanceEdges(domedges), 
														Ubench.getInstance().getVisibleTab().getNodeLabels(), 
														writer);
												
											}
											long end_extraction = System.currentTimeMillis();
											long time_extraction = end_extraction - start_extraction;
											oc.print_end_list(writer);
											oc.print_footer(writer);
											
											progress.endTask();
											progress.setVisible(false);
											// new text
											long total_time = time_extraction + time_solver;
											String interTime = null;
											if( total_time > 0 ) {
												interTime = (int) Math.floor(count * 1000.0 / total_time) + " sfs/sec; ";
											}
											JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
													"Found " + count + " solved forms." 
													+ System.getProperty("line.separator") + 
													"Time spent on extraction: " + time_extraction + " ms" + 
													System.getProperty("line.separator") +
													"Total runtime: " + total_time + " ms (" + interTime + 
													1000 * total_time / count + " microsecs/sf)",
													"Solver Statistics",
													JOptionPane.INFORMATION_MESSAGE);
											
											
										} catch (IOException ex) {
											JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
													"The specified file cannot be created.",
													"Error during output",
													JOptionPane.ERROR_MESSAGE);
										} catch (MalformedDomgraphException md) {
											JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
													"The output codec doesn't support output of this graph:\n" + md,
													"Error during output",
													JOptionPane.ERROR_MESSAGE);
										} catch (UnsupportedOperationException uE) {
											JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
													uE.getMessage(),
													"Error during output",
													JOptionPane.ERROR_MESSAGE);
										}
									}
									
								}}.run();
								
								
						}
						
						
						
					}
				} else if(command.equals(("chartView"))) {
					new Thread() {
						public void run() {
							Ubench.getInstance().getVisibleTab().displayChart();
						}
					}.run();
					
				} else if(command.equals("showcodecs")) {
					JFrame cf = new JFrame("Codecs in Utool");
					JLabel cp = new JLabel();
					
					StringBuffer codecList = new StringBuffer();
					codecList.append("<html>Input Codecs:<br><br>" + 
					"<table border=\"0\">");
					
					// TODO avoid doubling here
					for( GenericFileFilter filter : ffInputCodecs ) {
						codecList.append("<tr><td>" + filter.getName() + "</td><td> ("
								+ filter.getExtension() +
						")</td></tr>");
					}
					codecList.append("</table><br><br><br>Output Codecs:<br><br>" + 
					"<table border=\"0\">");
					for( GenericFileFilter filter : ffOutputCodecs ) {
						codecList.append("<tr><td>" + filter.getName() + "</td><td> ("
								+ filter.getExtension() +
						")</td></tr>");
					}
					
					codecList.append("</ul></html>");
					cp.setText(codecList.toString());
					cp.setBorder(
							BorderFactory.createEmptyBorder(
									25,25,25,50));
					cf.add(cp);
					
					cf.setAlwaysOnTop(true);
					cf.pack();
					cf.validate();
					cf.setVisible(true);
				} else if(command.equals("pic")) {
					
					JFileChooser fc = new JFileChooser();
					
					List<GenericFileFilter> pictureFilters = 
						new ArrayList<GenericFileFilter>();
					
					
					GenericFileFilter bmpFilter = 
						new GenericFileFilter("bmp", "*.bmp pictures");
					
					fc.addChoosableFileFilter(bmpFilter);   
					fc.addChoosableFileFilter(new GenericFileFilter("jpeg", "*.jpeg pictures"));
					fc.addChoosableFileFilter(new GenericFileFilter("png", "*.png pictures"));
					
					fc.setFileFilter(bmpFilter);
					
					
						fc.setCurrentDirectory(Ubench.getInstance().getLastPath());
					
						
						Component owner;
						if(Ubench.getInstance().getVisibleTab().hasVisibleChartViewer()) {
							owner = Ubench.getInstance().getVisibleTab().getChartViewer();
						} else {
							owner = Ubench.getInstance().getWindow();
						}
						
					int fcVal =  fc.showDialog(owner, "Export Picture");
					
					
//					proceed with a chosen file
					if (fcVal == JFileChooser.APPROVE_OPTION) {
						
						// resolving the selected file's path
						File file = fc.getSelectedFile();
						final String dir = file.getAbsolutePath();
						
						// updating the last chosen path
						Ubench.getInstance().setLastPath(file.getParentFile());
						
						final String picDesc = ((GenericFileFilter) fc.getFileFilter()).getExtension();
						
						// a new thread for printing the pdf.
						// a progress bar will be visible while this
						// thread runs.
						new Thread() {
							public void run() {
								
								WaitingDialog progress = new WaitingDialog(	
										"Printing Picture...", Ubench.getInstance().getWindow());
								progress.beginTask();
								// that's just a guess...
								
								try {
									JDomGraph toDraw = Ubench.getInstance().getVisibleTab().getGraph();
									ExportUtilities.exportPicture(toDraw,dir, picDesc);
									
									
									
								} catch (IOException exc) {
									JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
											"The output file can't be opened.",
											"Error while creating image",
											JOptionPane.ERROR_MESSAGE);
								}
								progress.endTask();
								
							}
						}.start();
						
					}
					
				} else {
					// PDF-Printing
					if(command.equals("pdf")) {
						
						// file chooser with PDF-filter 
						JFileChooser fc = new JFileChooser();
						fc.setFileFilter(new PDFFilter());
						
						// if there was any path chosen before, the
						// file chooser will start in the related directory
						
							fc.setCurrentDirectory(Ubench.getInstance().getLastPath());
							Component owner;
							if(Ubench.getInstance().getVisibleTab().hasVisibleChartViewer()) {
								owner = Ubench.getInstance().getVisibleTab().getChartViewer();
							} else {
								owner = Ubench.getInstance().getWindow();
							}
							
						// configuring button and window texts
						int fcVal =  fc.showDialog(owner, "Print PDF");
						fc.setApproveButtonText("Print!");
						
						// proceed with a chosen file
						if (fcVal == JFileChooser.APPROVE_OPTION) {
							
							// resolving the selected file's path
							File file = fc.getSelectedFile();
							String dir = file.getAbsolutePath();
							
							// updating the last chosen path
							Ubench.getInstance().setLastPath(file.getParentFile());
							
							final String filepath;
							// if the file was named withoud pdf-extension,
							// the extension is added
							if(dir.indexOf(".pdf") > 0) {
								filepath = dir;
							} else {
								filepath = dir + ".pdf";
							}
							
							
							// a new thread for printing the pdf.
							// a progress bar will be visible while this
							// thread runs.
							new Thread() {
								public void run() {
									WaitingDialog progress = new WaitingDialog(	
											"Printing PDF...", Ubench.getInstance().getWindow());
									progress.beginTask();
									
									
									try {
										// the actual PDF-printing
										ExportUtilities.exportPDF(Ubench.getInstance().getVisibleTab().getGraph(), filepath);
									} catch (IOException io) {
										JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
												"The output file can't be opened.",
												"Error from PDF printer",
												JOptionPane.ERROR_MESSAGE);
									}
									progress.endTask();
									
								}
							}.start();
						}
						
					} 
				}
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
				if(Ubench.getInstance().getVisibleTab() != null) {
					Ubench.getInstance().getVisibleTab().repaintIfNecessary();
				}
			} else 
				
				// checkbox indicating whether graphs are
				// solved right after loading automatically
				if(desc.equals("countAndSolve")) {
					
					// enable/disable menu items and change preferences
					if(e.getStateChange() == ItemEvent.SELECTED) {
						if( Ubench.getInstance().getMenuBar() != null ) {
							Ubench.getInstance().getMenuBar().setCountSfEnabled(false);
						}
						Preferences.setAutoCount(true);
						if( Ubench.getInstance().getVisibleTab() != null  ) {
							((JDomGraphTab) Ubench.getInstance().getVisibleTab()).solve();
							Ubench.getInstance().refresh();
						}
						
					} else {
						if( (Ubench.getInstance().getMenuBar() != null) && 
								Ubench.getInstance().getVisibleTab().getClass() != JSolvedFormTab.class) {
							Ubench.getInstance().getMenuBar().setCountSfEnabled(true);
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
							if( Ubench.getInstance().getVisibleTab() != null ) {
								Ubench.getInstance().getVisibleTab().fitGraph();
							}
						} else {
							Preferences.setFitToWindow(false);
						}
						
					} else if(desc.equals("autoreduce")) {
						if(e.getStateChange() == ItemEvent.SELECTED ) {
							Ubench.getInstance().reduceAutomatically = true;
							if(! Ubench.getInstance().isEquationSystemLoaded() ) {
								JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
										"You have to specify a xml file that contains your equation system" + 
										System.getProperty("line.separator") + 
										" before Utool can eliminate equivalences.",
										"Please load an equation system",
										JOptionPane.INFORMATION_MESSAGE);
								loadEQS();
							}
							
						} else {
							Ubench.getInstance().reduceAutomatically = false;
						}
					}
	}
	
	private void loadEQS() {
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Choose the equation system input file");
		fc.setFileFilter(Ubench.getInstance().getListener().new XMLFilter());
		
		fc.setCurrentDirectory(Ubench.getInstance().getLastPath());
		
		Component owner;
		if(Ubench.getInstance().getVisibleTab().hasVisibleChartViewer()) {
			owner = Ubench.getInstance().getVisibleTab().getChartViewer();
		} else {
			owner = Ubench.getInstance().getWindow();
		}
		int fcVal = fc.showOpenDialog(owner);	
		
		if(fcVal == JFileChooser.APPROVE_OPTION){
			
			File file = fc.getSelectedFile();
			
			try {
				EquationSystem eqs = new EquationSystem();
				eqs.read(new FileReader(file));
				Ubench.getInstance().setEquationSystem(eqs, file.getName());
				for(JGraphTab tab : Ubench.getInstance().getTabs() ) {
					tab.enableGlobalEQS(true);
				}
			
			} catch( Exception ex ) {
				JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
						"The Equation System cannot be parsed." + 
						System.getProperty("line.separator") + 
						"Either the input file is not valid or it contains syntax errors.",
						"Error while loading equation system",
						JOptionPane.ERROR_MESSAGE);
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
	 * A <code>FileFilter</code> that 
	 * accepts files with *.xml-extension.
	 * 
	 * @author Michaela Regneri
	 *
	 */
	public class XMLFilter extends FileFilter {
		
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
	public class PDFFilter extends FileFilter {
		
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
	public class PDFView extends FileView {
		
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
		SolvedFormIterator solver = Ubench.getInstance().getVisibleTab().getSolvedFormIterator();
		NodeLabels labels = Ubench.getInstance().getVisibleTab().getNodeLabels();
		
		de.saar.chorus.domgraph.graph.DomGraph nextForm =   (de.saar.chorus.domgraph.graph.DomGraph) Ubench.getInstance().getVisibleTab().getDomGraph().clone();
		
		List<DomEdge> domEdges = solver.getSolvedForm((int) no-1);
		if( domEdges != null ) {
			nextForm.setDominanceEdges(domEdges);
		}
		int toInsertHere = Ubench.getInstance().getVisibleTabIndex();
		
		// converting the form to a JDomGraph
		//JDomGraph domSolvedForm = new JDomGraph();
		//JDomDataFactory fac = new JDomDataFactory(nextForm, labels, domSolvedForm);
		//ImprovedJGraphAdapter.convert(nextForm, fac, domSolvedForm);
		
		
		DomGraphTConverter conv = new DomGraphTConverter(nextForm, labels);
		JDomGraph domSolvedForm = conv.getJDomGraph();
		
		
		// setting up the new tab
		JSolvedFormTab solvedFormTab = new JSolvedFormTab(domSolvedForm, 
				Ubench.getInstance().getVisibleTab().getGraphName()  + "  SF #" + no, solver,
				nextForm,
				no, Ubench.getInstance().getVisibleTab().getSolvedForms(), 
				Ubench.getInstance().getVisibleTab().getGraphName(), 
				Ubench.getInstance().getListener(), 
				labels);
		// closing the tab with the previous solved form and
		// showing the recent one.
		Ubench.getInstance().closeCurrentTab();
		Ubench.getInstance().addTab(solvedFormTab, true, toInsertHere);
		if(no > 1 && no < Ubench.getInstance().getVisibleTab().getSolvedForms()) {
			Ubench.getInstance().getMenuBar().setPlusMinusEnabled(true,true);
		} else if (no == 1 && no < Ubench.getInstance().getVisibleTab().getSolvedForms()) {
			Ubench.getInstance().getMenuBar().setPlusMinusEnabled(true,false);
		} else if (no > 1 && no == Ubench.getInstance().getVisibleTab().getSolvedForms()) {
			Ubench.getInstance().getMenuBar().setPlusMinusEnabled(false,true);
		} else {
			Ubench.getInstance().getMenuBar().setPlusMinusEnabled(false,false);
		}
		Ubench.getInstance().getStatusBar().showBar(solvedFormTab.getBarCode());
		
	}
	
	/* (non-Javadoc)
	 * @see de.saar.chorus.domgraph.utool.server.ConnectionManager.StateChangeListener#stateChanged(de.saar.chorus.domgraph.utool.server.ConnectionManager.State)
	 */
	public void stateChanged(State newState) {
		
		JDomGraphMenu menu = Ubench.getInstance().getMenuBar();
		
		if(newState == ConnectionManager.State.RUNNING) {
			menu.setServerButtonPressed(true);
		} else if(newState == ConnectionManager.State.STOPPED) {
			menu.setServerButtonPressed(false);
		}
	}
	
	
}