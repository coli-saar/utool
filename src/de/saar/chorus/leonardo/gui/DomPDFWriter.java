package de.saar.chorus.leonardo.gui;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.FileOutputStream;
import java.io.IOException;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import de.saar.chorus.leonardo.JDomGraph;
/**
 * 
 * Provides a static method to print a <code>JDomGraph</code> 
 * to a PDF.
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 *
 */
public class DomPDFWriter  {

	
	/**
	 * Method to print the given JDomGraph to a pdf-file with the
	 * given name.
	 * 
	 * @param graph the JDomGraph to print
	 * @param filename the file to print in (has to be *.pdf)
	 */
	public static void printToPDF(JDomGraph graph, String filename) {
		
	    /*JDomGraph graph = gr.clone();
	    graph.computeFragments();
		
	    JFrame f = new JFrame("JGraph Test");
		   f.add(graph);
		   f.pack();
		   
		   graph.repaintIfNecessary();
		    
	
	    */
		// to resize the graph
		double originalScale = graph.getScale(); 
		float scale;
		Rectangle boundingBox = graph.getBoundingBox();
		
		
		// a4Paper - size
		float a4Width = PageSize.A4.width();
		float a4Height = PageSize.A4.height();
		
		
		// the document to write in
		Document document;
		
		// to choose between portrait and landscape
		double boxRatio = boundingBox.height/boundingBox.width;
		
		// a graph that is longer than wide.
		if( boxRatio >= 1.0 ) {
			// a graph that is longer than wide.
			// width of the graph has to fit in the page 
			// (considering the margins with 40)
			scale = Math.min(1, 
					Math.min((PageSize.A4.width()-40)/(boundingBox.width + 50),
							(PageSize.A4.height()-40)/(boundingBox.height + 50)
					)
			);
			
		
			
		} else {
			// a graph that is square or wider than long.
			
			//width of the graph has to fit in the page 
			// (considering the margins with 40)
			// landscape-width = portrait-height !
			scale = Math.min(1, 
					Math.min((PageSize.A4.height()-40)/(boundingBox.width + 50),
							(PageSize.A4.width()-40)/(boundingBox.height + 50)
					)
			);
			
			
        
        }
        document= new Document(new com.lowagie.text.Rectangle(boundingBox.width, boundingBox.height));
		
		try {
		
			//boundingBox.setBounds(0,0, (int) (boundingBox.width*scale), (int) (boundingBox.height*scale));
			//the writer
			PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
			
			// opening the document
			document.open();
			
			// getting the context to write in
			PdfContentByte cb = writer.getDirectContent();
			Graphics2D g2 = cb.createGraphicsShapes(boundingBox.width 
					, boundingBox.height);
		

			// painting myself 
			graph.paint(g2);

            cb.transform(AffineTransform.getScaleInstance(scale,scale));

            
            // updating
			g2.dispose();
			document.close();
			
		
			
		}
		catch(DocumentException de) {
			System.err.println(de.getMessage());
		}
		catch(IOException ioe) {
			System.err.println(ioe.getMessage());
		}
	}
	
	
}
