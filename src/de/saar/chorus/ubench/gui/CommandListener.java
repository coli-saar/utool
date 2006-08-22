/*
 * @(#)CommandListener.java created 21.07.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.ubench.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.RepaintManager;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;

import de.saar.basic.SwingComponentPDFWriter;
import de.saar.chorus.domgraph.GlobalDomgraphProperties;
import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.codec.CodecManager;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.OutputCodec;
import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.ubench.DomGraphTConverter;
import de.saar.chorus.ubench.JDomGraph;

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
public class CommandListener implements ActionListener, ItemListener {
	private File lastPath = new File(System.getProperty("user.dir"));
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
		CodecManager codecman = Ubench.getInstance().getCodecManager();
		
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
		
		Collections.sort(ffInputCodecs);
		Collections.reverse(ffInputCodecs);
		Collections.sort(ffOutputCodecs);
		Collections.reverse(ffInputCodecs);
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
		if(command.equals("pic")) {
			
			JFileChooser fc = new JFileChooser();
			
			List<GenericFileFilter> pictureFilters = 
				new ArrayList<GenericFileFilter>();
			
			
			GenericFileFilter bmpFilter = 
				new GenericFileFilter("bmp", "*.bmp pictures");
			
			fc.addChoosableFileFilter(bmpFilter);   
			fc.addChoosableFileFilter(new GenericFileFilter("jpeg", "*.jpeg pictures"));
			fc.addChoosableFileFilter(new GenericFileFilter("png", "*.png pictures"));
			
			fc.setFileFilter(bmpFilter);
			
			
			if(! (lastPath == null) ) {
				fc.setCurrentDirectory(lastPath);
			}
			
			int fcVal =  fc.showDialog(Ubench.getInstance().getWindow(), "Export Picture");
			
			
//			proceed with a chosen file
			if (fcVal == JFileChooser.APPROVE_OPTION) {
				
				// resolving the selected file's path
				File file = fc.getSelectedFile();
				final String dir = file.getAbsolutePath();
				
				// updating the last chosen path
				lastPath = file.getParentFile();
				
				final String picDesc = ((GenericFileFilter) fc.getFileFilter()).getExtension();
				
				// a new thread for printing the pdf.
				// a progress bar will be visible while this
				// thread runs.
				new Thread() {
					public void run() {
						
						// that's just a guess...
						int taskLength = Ubench.getInstance().getVisibleTab().numGraphNodes();
						
						// the progress bar and the panel containing it.
						JDialog progress = new JDialog(Ubench.getInstance().getWindow(), false);
						JPanel dialogPane = new JPanel();
						JProgressBar progressBar = new JProgressBar(0, taskLength);
						
						// the OK-Button to press after printing is done
						// (it will close the dialog)
						JButton ok = new JButton("OK");
						ok.setActionCommand("ok");
						
						// text visible while printing
						JLabel export = new JLabel("Printing Picture...",SwingConstants.CENTER);
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
						progress.setLocation((Ubench.getInstance().getWindow().getWidth() - progress.getWidth())/2,
								(Ubench.getInstance().getWindow().getHeight() - progress.getHeight())/2); 
						
						progress.setVisible(true);
						try {
							JDomGraph toDraw = Ubench.getInstance().getVisibleTab().getGraph();
							
							
							GraphPicture.makePicture(
									toDraw,
									dir, picDesc);
							
							
						} catch (IOException exc) {
							JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
									"The output file can't be opened.",
									"Error while creating image",
									JOptionPane.ERROR_MESSAGE);
						}
//						after finishing, the progress bar becomes
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
			
		} else {
			// PDF-Printing
			if(command.equals("pdf")) {
				
				// file chooser with PDF-filter 
				JFileChooser fc = new JFileChooser();
				fc.setFileFilter(new PDFFilter());
				
				// if there was any path chosen before, the
				// file chooser will start in the related directory
				if(! (lastPath == null) ) {
					fc.setCurrentDirectory(lastPath);
				}
				
				// configuring button and window texts
				int fcVal =  fc.showDialog(Ubench.getInstance().getWindow(), "Print PDF");
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
							int taskLength = Ubench.getInstance().getVisibleTab().numGraphNodes();
							
							// the progress bar and the panel containing it.
							JDialog progress = new JDialog(Ubench.getInstance().getWindow(), false);
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
							progress.setLocation((Ubench.getInstance().getWindow().getWidth() - progress.getWidth())/2,
									(Ubench.getInstance().getWindow().getHeight() - progress.getHeight())/2); 
							
							progress.setVisible(true);
							
							try {
								// the actual PDF-printing
								SwingComponentPDFWriter.printToPDF(Ubench.getInstance().getVisibleTab().getGraph(), recentPath);
							} catch (IOException io) {
								JOptionPane.showMessageDialog(Ubench.getInstance().getWindow(),
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
				
			} else if(command.equals("print")) {
				new PrintUtilities(Ubench.getInstance().getVisibleTab().getGraph()).print();
			} else {
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
					
					int fcVal = fc.showOpenDialog(Ubench.getInstance().getWindow());
					
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
								JDomGraph graph = Ubench.getInstance().genericLoadGraph(recentPath, theDomGraph, labels);
								
								
								if( graph != null ) {
									
									//	DomGraphTConverter conv = new DomGraphTConverter(graph);
									
									// setting up a new graph tab.
									// the graph is painted and shown at once.
									Ubench.getInstance().addNewTab(graph, recentFile, theDomGraph, true, true, labels);
								}
							}
						}.start();
						
					}
					
				} else 
					
					// exporting the visible graph.
					if( command.equals("saveUtool")) {
						JDomGraph graph = Ubench.getInstance().getVisibleTab().getGraph();
						
						if( graph != null) {
							JFileChooser fc = new JFileChooser(recentPath);
							
							// show plugging codecs just for solved forms.
							// TODO perhaps find a more aesthetic solution here.
							if( Ubench.getInstance().getVisibleTab() instanceof JSolvedFormTab) {
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
							
							int fcVal = fc.showSaveDialog(Ubench.getInstance().getWindow());
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
									Ubench.getInstance().getCodecManager().getOutputCodecForFilename(file.getName(),null);
								if( oc != null ) {
									try {
										FileWriter writer = new FileWriter(file);
										oc.print_header(writer);
										oc.encode(Ubench.getInstance().getVisibleTab().getDomGraph(),
												null, 
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
							JFileChooser fc = new JFileChooser(recentPath);
							for( FileFilter ff : ffOutputCodecs ) {
								fc.addChoosableFileFilter(ff);
							}
							fc.setAcceptAllFileFilterUsed(false);
							fc.setSelectedFile(new File(Ubench.getInstance().
									getVisibleTab().getDefaultName() + 
							"_solvedForms"));
							
							int fcVal = fc.showSaveDialog(Ubench.getInstance().getWindow());
							
							
							if( fcVal == JFileChooser.APPROVE_OPTION ) {
								
								File file = fc.getSelectedFile();
								lastPath = file.getParentFile();
								String targetFile = file.getAbsolutePath();
								
								String defaultExtension = ((GenericFileFilter) 
										fc.getFileFilter()).getExtension();
								
								final String targetfileName;
								if(! targetFile.endsWith(defaultExtension) ) {
									targetfileName = targetFile + defaultExtension;
									
								}  else {
									targetfileName = targetFile;
								}
								
								
								
								new Thread() {
									public void run() {
										File outputfile = new File(targetfileName);
										recentPath = outputfile.getAbsolutePath();
										// that's just a guess...
										int taskLength = Ubench.getInstance().getVisibleTab().numGraphNodes();
										
										// the progress bar and the panel containing it.
										JDialog progress = new JDialog(Ubench.getInstance().getWindow(), false);
										JPanel dialogPane = new JPanel();
										JProgressBar progressBar = new JProgressBar(0, taskLength);
										
										// the OK-Button to press after printing is done
										// (it will close the dialog)
										JButton ok = new JButton("OK");
										ok.setActionCommand("ok");
										
										// text visible while printing
										JLabel export = new JLabel("Printing Solutions...",SwingConstants.CENTER);
										export.setHorizontalAlignment(SwingConstants.CENTER);
										
										// listener for the button 
										ok.addActionListener(new JDialogListener(progress));
										
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
										progress.setLocation((Ubench.getInstance().getWindow().getWidth() - progress.getWidth())/2,
												(Ubench.getInstance().getWindow().getHeight() - progress.getHeight())/2); 
										progress.setVisible(true);
										
										
										OutputCodec oc= Ubench.getInstance().getCodecManager().
										getOutputCodecForFilename(outputfile.getName(), null);
										
										
										
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
													oc.encode(graph, domedges, 
															Ubench.getInstance().getVisibleTab().getNodeLabels(), 
															writer);
													
												}
												long end_extraction = System.currentTimeMillis();
												long time_extraction = end_extraction - start_extraction;
												oc.print_end_list(writer);
												oc.print_footer(writer);
												
												
												
												
												
												
												// after finishing, the progress bar becomes
												// determined and fixated at maximum value (100%)
												progressBar.setMaximum(100);
												progressBar.setIndeterminate(false);
												progressBar.setValue(100);
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
						Ubench.getInstance().getVisibleTab().displayChart();
					} else if(command.equals("showcodecs")) {
						JFrame cf = new JFrame("Codecs in Utool");
						JLabel cp = new JLabel();
						
						StringBuffer codecList = new StringBuffer();
						codecList.append("<html>Input Codecs:<br><br>" + 
								"<table border=\"0\">");
						for( GenericFileFilter filter : ffInputCodecs ) {
							codecList.append("<tr><td>" + filter.desc+ "</td><td> ("
									+ filter.getExtension() +
									")</td></tr>");
						}
						codecList.append("</table><br><br><br>Output Codecs:<br><br>" + 
								"<table border=\"0\">");
						for( GenericFileFilter filter : ffOutputCodecs ) {
							codecList.append("<tr><td>" + filter.desc+ "</td><td> ("
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
		
		/**
		 * Add a file extension that shall be accepted
		 * by the filter
		 * 
		 * @param extension the new extension
		 */
		public void addExtension(String extension) {
			if( extension.startsWith(".") ) {
				extensions.add(extension);
			} else {
				extensions.add("."+ extension);
			}
		}
		
		/**
		 * 
		 * @return true if the file has an extension
		 *        contained here or is a folder
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
	private class GenericFileFilter extends FileFilter implements Comparable {
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
		
		public int compareTo(Object o) {
			return desc.compareTo( 
					((GenericFileFilter)o).desc);
		}
		
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
	
	private static class PrintUtilities implements Printable {
		private Component componentToBePrinted;
		/* (non-Javadoc)
		 * @see java.awt.print.Printable#print(java.awt.Graphics, java.awt.print.PageFormat, int)
		 */
		public PrintUtilities(Component componentToBePrinted) {
			this.componentToBePrinted = componentToBePrinted;
		}
		
		public static void printComponent(Component c) {
			new PrintUtilities(c).print();
		}
		
		public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {
			if (pageIndex > 0) {
				return(NO_SUCH_PAGE);
			} else {
				Graphics2D g2d = (Graphics2D)g;
				
				disableDoubleBuffering(componentToBePrinted);
				
				
				double scale = Math.min(pageFormat.getImageableWidth()/(double) componentToBePrinted.getWidth(), 
						pageFormat.getImageableHeight()/ (double) componentToBePrinted.getHeight());
				
				g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
				g2d.scale(scale,scale);
				componentToBePrinted.paint(g2d);
				g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY() );
				enableDoubleBuffering(componentToBePrinted);
				return(PAGE_EXISTS);
			}
		}
		
		public void print() {
			PrinterJob printJob = PrinterJob.getPrinterJob();
			printJob.setPrintable(this);
			if (printJob.printDialog())
				try {
					printJob.print();
				} catch(PrinterException pe) {
					System.out.println("Error printing: " + pe);
				}
		}
		
		public static void disableDoubleBuffering(Component c) {
			RepaintManager currentManager = RepaintManager.currentManager(c);
			currentManager.setDoubleBufferingEnabled(false);
		}
		
		public static void enableDoubleBuffering(Component c) {
			RepaintManager currentManager = RepaintManager.currentManager(c);
			currentManager.setDoubleBufferingEnabled(true);
		}
		
	}
	
}