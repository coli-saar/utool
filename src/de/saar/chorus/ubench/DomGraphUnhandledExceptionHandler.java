package de.saar.chorus.ubench;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import de.saar.chorus.domgraph.GlobalDomgraphProperties;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.domcon.DomconOzOutputCodec;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;


/**
 * This is a handler for all Exceptions which are thrown in threads besides the
 * main Event Dispatch Thread of Ubench.
 * As we use separate threads for every new graph we load and every "expensive" 
 * oparation on graphs (like exporting them, solving them etc.), we have to handle 
 * different types of exceptions here and show the user a meaningful error message. 
 * 
 * 
 * @author Michaela Regneri
 * @see Thread.UncaughtExceptionHandler
 *
 */
class DomGraphUnhandledExceptionHandler implements Thread.UncaughtExceptionHandler {

	private static Calendar cal = Calendar.getInstance();
	private static String date = cal.get(Calendar.YEAR) + "-" +
		cal.get(Calendar.MONTH) + "-" +
		cal.get(Calendar.DAY_OF_MONTH) + "-" +
		cal.get(Calendar.HOUR_OF_DAY) + "-" +
		cal.get(Calendar.MINUTE) + "-" +
		cal.get(Calendar.SECOND);
	
	private static String n = System.getProperty("line.separator");
	private static String w = "     ";
	private static String line = "-------------------------------------------------------------------------" + n;
	
	
	private static DomGraph getGraph() {
		return Ubench.getInstance().getVisibleDomGraph();
	}
	
	private static NodeLabels getLabels() {
		return Ubench.getInstance().getVisibleNodeLabels();
	}
	
	
	public static void showErrorDialog(Throwable arg1) {
		
		arg1.printStackTrace();
		//showErrorDialog(arg1, getGraph(), getLabels());
	}
	
	public static void showErrorDialog(Throwable arg1, DomGraph graph, NodeLabels labels) {
		String logfile = 
			writeLogFile(arg1, graph, labels);
		
		JOptionPane pane = 
			new JOptionPane("An unexpected error occured while running Utool." +n +
					"A logfile containing details of this error was saved under " + n +
					logfile + "." + n +
					"Please email this file to us!"	//TODO insert email option here
					, JOptionPane.ERROR_MESSAGE);
		//pane.setOptionType(JOptionPane.YES_NO_CANCEL_OPTION);
		JDialog dialog = 
			pane.createDialog(Ubench.getInstance().getWindow(), "Error");
		dialog.setModal(false);
		dialog.setVisible(true);
		Ubench.getInstance().refresh();

		
		/*
		 * TODO MR: if we implement automatic mail set-up, we will have 
		 * react to different user selections here.
		 */
		Object selected = pane.getValue();
	/*	if( selected == null ) {
			// user has simply closed the dialog
			return;
		} else if( selected instanceof Integer ) {
			int ret = ((Integer) selected).intValue();
			switch (ret) {
			case JOptionPane.YES_OPTION: writeLogFile(arg1); break;
			case JOptionPane.CANCEL_OPTION: break;
			case JOptionPane.NO_OPTION: break;
			};
 		}*/
	}
	/**
	 * 
	 */
	public void uncaughtException(Thread arg0, Throwable arg1) {
		
			showErrorDialog(arg1);
			//DEBUG
	
		
	}
	
	public static String writeLogFile(Throwable e, DomGraph graph, NodeLabels labels) {
		try {
			
			
			File file = new File(System.getProperty("user.dir") + 
					System.getProperty("file.separator") + "utool_error" + date + ".log");
					
			StringBuffer log = new StringBuffer();
			log.append("utool - The Swiss Army Knife of Underspecification, v. " + GlobalDomgraphProperties.getVersion() + System.getProperty("line.separator")
					+ "created by the CHORUS project, SFB 378, Saarland University" +n + line + n);
			log.append("Utool System Error, encountered on " + date + ":" + n + n);
			log.append("Utool version " + GlobalDomgraphProperties.getVersion() + n);
			log.append("JVM: " + System.getProperty("java.vm.name")  + w +"version: "
					    + System.getProperty("java.vm.version") + n);
			log.append("OS: " + System.getProperty("os.name") + w
					   + System.getProperty("os.version") + w + 
					   System.getProperty("os.arch") + n );
			
			log.append(n + line + n);
			
			log.append(e.getMessage());
			log.append(n +n );
			
			
			
			
			
			PrintWriter writer = new PrintWriter(new FileWriter(file));
			writer.append(log);
			e.printStackTrace(writer);

			if( graph != null ) {
				writer.append(n + line + n);
				writer.println("Offending graph:");
				new DomconOzOutputCodec().encode(graph, labels, writer);
			}
			
			writer.close();
			
			return file.getAbsolutePath();
		} catch(IOException ex) {
			
		} catch (MalformedDomgraphException ex) {
			// This shouldn't happen in domcon-oz output codec
		}
		
		return null;
	}

}
