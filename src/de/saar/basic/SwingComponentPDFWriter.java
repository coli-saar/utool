package de.saar.basic;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JComponent;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
/**
 * 
 * Provides a static method to print a <code>JComponent</code> 
 * to a PDF.
 * 
 * @author Alexander Koller
 * @author Michaela Regneri
 *
 */
public class SwingComponentPDFWriter  {

	
	/**
	 * Method to print the given Swing component to a pdf-file with the
	 * given name.
	 * 
	 * @param component the <code>JComponent</code> to print
	 * @param filename the file to print in (has to be *.pdf)
	 * @throws <code>IOException</code> if there is anything wrong with the file
	 */
	public static void printToPDF(JComponent component, String filename)
			throws IOException {
		
	
	
		float scale;
		Rectangle boundingBox = component.getBounds();
		
		
		// a4Paper - size
		float a4Width = PageSize.A4.width();
		float a4Height = PageSize.A4.height();
		
		
		// the document to write in
		Document document;
		
		// to choose between portrait and landscape
		double boxRatio = (double) boundingBox.height/boundingBox.width;
		
		
		if( boxRatio >= 1.0 ) {
			// a piture that is longer than wide.
			// the width of the picture has to fit in the page 
			// (considering the margins with 40)
			scale = Math.min(1, 
					Math.min((PageSize.A4.width()-40)/(boundingBox.width + 50),
							(PageSize.A4.height()-40)/(boundingBox.height + 50)
					)
			);
			
		
			
		} else {
			// a picture that is square or wider than long.
			
			//width of the picture has to fit in the page 
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
			//the writer
			PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
			
			// opening the document
			document.open();
			
			// getting the context to write in
			PdfContentByte cb = writer.getDirectContent();
			Graphics2D g2 = cb.createGraphicsShapes(boundingBox.width 
					, boundingBox.height);
		

			// painting myself 
			component.paint(g2);

            cb.transform(AffineTransform.getScaleInstance(scale,scale));

            
            // updating
			g2.dispose();
			document.close();
			
		
			
		}
		catch(DocumentException de) {
			System.err.println(de.getMessage());
		}
	}
	
	
}
