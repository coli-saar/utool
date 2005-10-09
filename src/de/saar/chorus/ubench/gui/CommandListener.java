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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import de.saar.chorus.libdomgraph.DomGraph;
import de.saar.chorus.libdomgraph.DomSolver;
import de.saar.chorus.ubench.JDomGraph;
import de.saar.chorus.ubench.utool.DomGraphConverter;
import de.saar.chorus.ubench.utool.JDomGraphConverter;

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
    private String recentPath = "", recentFile="";
    
    private FileFilter ffInNativeGxl = new GenericFileFilter("dc.xml", "Domcon/GXL");
    private List<FileFilter> ffInputCodecs;
    private List<FileFilter> ffOutputCodecs;
    
    private Map<Object,String> eventSources;

    /**
     * Creates a new Instance of <code>CommandListener</code>.
     */
    public CommandListener() {
    	
    	// initializing fields
        eventSources = new HashMap<Object,String>();
        
        ffInputCodecs = new ArrayList<FileFilter>();
        ffOutputCodecs = new ArrayList<FileFilter>();
        
        
        // deteting utool and configure the filters for file 
        // choosers
        if(Preferences.utoolPresent()) {
            DomSolver s = new DomSolver();
            for( int i = 0; i < s.num_input_codecs(); i++ ) {
                if( s.is_file_input_codec(i)) {
                    ffInputCodecs.add(new GenericFileFilter(s.input_codec_extension(i), "utool: " + s.input_codec_name(i)));
                }
            }
            
            for( int i = 0; i < s.num_output_codecs(); i++ ) {
                ffOutputCodecs.add(new GenericFileFilter(s.output_codec_extension(i), "utool: " + s.output_codec_name(i)));
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
                        
                        
                        // the actual PDF-printing
                        DomPDFWriter.printToPDF(Main.getVisibleTab().getGraph(), recentPath);
                        
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
            JFileChooser fc = new JFileChooser();
            
            fc.addChoosableFileFilter(ffInNativeGxl);
            for( FileFilter ff : ffInputCodecs )
                fc.addChoosableFileFilter(ff);
            
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
                        JDomGraph graph = Main.genericLoadGraph(recentPath);
                        
                        
                        if( graph != null ) {
                        	
                        	// setting up a new graph tab.
                        	// the graph is painted and shown at once.
                            Main.addNewTab(graph, recentFile, true, true);
                        }
                    }
                }.start();
                
            }
            
        } else 
        	
        	// exporting the visible graph.
        	if( command.equals("saveUtool")) {
            JDomGraph graph = Main.getVisibleTab().getGraph();
            
            if( graph != null) {
                JFileChooser fc = new JFileChooser();
                
                for( FileFilter ff : ffOutputCodecs ) {
                    fc.addChoosableFileFilter(ff);
                }
                
                if(! (lastPath == null) ) {
                    fc.setCurrentDirectory(lastPath);
                }
                
                int fcVal = fc.showSaveDialog(Main.getWindow());
                if( fcVal == JFileChooser.APPROVE_OPTION ) {
                   final File file = fc.getSelectedFile();
                   
                   lastPath = file.getParentFile();
                   
                   final DomSolver solver = new DomSolver();
                   JDomGraphConverter conv = new JDomGraphConverter(solver);
                   conv.toDomGraph(graph);
                   
                   if( !solver.saveGraph(file.getAbsolutePath()) ) {
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
            	if(! Main.getVisibleTab().isSolvedForm ) {
            		Main.addNewTab(Main.getVisibleTab().getCloneOfGraph(), Main.getVisibleTab().getDefaultName(), true, true);
            	} else {
            		JDomGraphTab sFormCopy = new JDomGraphTab(Main.getVisibleTab().getCloneOfGraph(), 
            				Main.getVisibleTab().getGraphName() + " SF#" +Main.getVisibleTab().currentForm, 
            				Main.getVisibleTab().getSolver(), Main.getVisibleTab().currentForm, 
            				Main.getVisibleTab().getSolvedForms(),
            				Main.getVisibleTab().getGraphName(),
            				Main.getListener() );
            		sFormCopy.setGraphName(Main.getVisibleTab().getGraphName());
            		Main.addTab(sFormCopy, true);
            	
            	}
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
            if( (Main.getVisibleTab() != null) && Preferences.utoolPresent() ) {
                Main.showProgressBar();
                Main.getVisibleTab().solve();
                Main.refresh();
            }
        }
        
        else if( command.equals("solvedFormDirectSelection") ) {
            // changed text field and pressed "return"
                       
            long no = 1;
            no = Long.parseLong(Main.getVisibleTab().getSolvedForm().getText());
            
            if(! ((no < 1) || (no > Main.getVisibleTab().getSolvedForms()) ) ) {
                showSolvedFormWithIndex(no);
            } else {
            	Main.getVisibleTab().resetSolvedFormText();
            }
        } else if(command.equals("plus")) {
            // ">" button in the status bar
          
            
            long no = Main.getVisibleTab().currentForm;
            if(no < Main.getVisibleTab().getSolvedForms()) {
                no++;
               // bar.setSolvedFormText(String.valueOf(no));
                showSolvedFormWithIndex(no);
                //Main.getVisibleTab().setCurrentForm(no);
            }
            
            Main.refresh();
        } else if (command.equals("minus")) {
            // "<" button in the status bar
            
            long no = Main.getVisibleTab().currentForm;
            if(no > 1) {
                no--;
                //bar.setSolvedFormText(String.valueOf(no));
                showSolvedFormWithIndex(no);
                //Main.getVisibleTab().setCurrentForm(no);
            }
            
            Main.refresh();
            
        } else if (command.equals("solve")) {
            // "solve" button in the status bar
            new Thread() {
                public void run() {
                    DomSolver solver = Main.getVisibleTab().getSolver();
                    
                    if(! Main.getVisibleTab().isSolvedYet) {
                        solver.solve();
                        Main.getVisibleTab().setSolvedForms(solver.countSolvedForms());
                        
                    }
                    DomGraph firstForm = solver.extractNextSolvedForm();
                    DomGraphConverter conv = new DomGraphConverter(solver, firstForm);
                    JDomGraph domSolvedForm = conv.toJDomGraph();
                    
                    JDomGraphTab sFTab = new JDomGraphTab(domSolvedForm, Main.getVisibleTab().getDefaultName()  + "  SF #1", 
                    		solver, 1, Main.getVisibleTab().getSolvedForms(), Main.getVisibleTab().getGraphName(), 
                    		Main.getListener());
                    
               
                    Main.addTab(sFTab, true);
                    Main.refresh();
                }
            }.start();
            
        } else if( command.equals("utool-about")) {
        	DomSolver solv = new DomSolver();
        	
        	JOptionPane.showMessageDialog(Main.getWindow(),
        				"Libdomgraph version " + solv.getLibdomgraphVersion() + " loaded.",
        				"About libdomgraph",
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
                if( Main.getVisibleTab() != null && Preferences.utoolPresent() ) {
                    Main.getVisibleTab().solve();
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
    	DomSolver solver = Main.getVisibleTab().getSolver();
        DomGraph nextForm = solver.extractSolvedForm(no-1);
        int toInsertHere = Main.getVisibleTabIndex();
        
        // converting the form to a JDomGraph
        DomGraphConverter conv = new DomGraphConverter(solver, nextForm);
        JDomGraph domSolvedForm = conv.toJDomGraph();
        
        
        // setting up the new tab
        JDomGraphTab solvedFormTab = new JDomGraphTab(domSolvedForm, 
        		Main.getVisibleTab().getGraphName()  + "  SF #" + no, solver, 
        		 no, Main.getVisibleTab().getSolvedForms(), Main.getVisibleTab().getGraphName(), 
        		Main.getListener());
        // closing the tab with the previous solved form and
        // showing the recent one.
        Main.closeCurrentTab();
        Main.addTab(solvedFormTab, true, toInsertHere);
        Main.getStatusBar().showBar(solvedFormTab.getBarcode());
        
    }

}