package de.saar.chorus.ubench.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.RepaintManager;

import de.saar.chorus.ubench.JDomGraph;

/**
 * This provides a method to save <code>JDomGraph</code>s to a 
 * picture.
 * This probably won't work with "invisible" graphs 
 * that are in no visible context at the moment.
 * 
 * 
 * @author Michaela Regneri
 *
 */
public class GraphPicture {
	
	public static void makePicture(JDomGraph graph, String name, String ext)
			throws IOException {
	
		BufferedImage bi = new BufferedImage(graph.getMaximumSize().width, graph.getMaximumSize().height, 
				BufferedImage.TYPE_INT_RGB);
		
		Graphics2D graphCont = bi.createGraphics();
		
		RepaintManager currentManager = RepaintManager.currentManager(graph);
		currentManager.setDoubleBufferingEnabled(false);
		graph.paint(graphCont);
		
		
		String pointedExtension = ext;
		if(! ext.startsWith(".")) {
			pointedExtension = "." + ext;
		}
		
		String filename;
		if(! name.endsWith(pointedExtension)) {
			filename = name + pointedExtension;
		} else {
			filename = name;
		}
		File file = new File(filename);
		
		String picExt= pointedExtension.substring(1,ext.length());
		
		
		ImageIO.write(bi, picExt, file);
		graphCont.dispose();
		currentManager.setDoubleBufferingEnabled(true);
		//System.out.println("File: " + filename + " Ext: " + pointedExtension +" picext:" + picExt);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] names = ImageIO.getWriterFormatNames();
		for(int i = 0; i< names.length; i++ ) {
			System.err.println(names[i]);
		}

	}

}
