package de.saar.basic;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.RepaintManager;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;



public class ExportUtilities implements Printable {

	private Component componentToBePrinted;
	/* (non-Javadoc)
	 * @see java.awt.print.Printable#print(java.awt.Graphics, java.awt.print.PageFormat, int)
	 */
	public ExportUtilities(Component componentToBePrinted) {
		this.componentToBePrinted = componentToBePrinted;
	}
	
	public static void printComponent(Component c) {
		new ExportUtilities(c).print();
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
	
	
	public static void exportPDF(JComponent component, String filename)
			throws IOException {

		float scale;
		Rectangle boundingBox = component.getBounds();

		// the document to write in
		Document document;

		// to choose between portrait and landscape
		double boxRatio = (double) boundingBox.height / boundingBox.width;

		if (boxRatio >= 1.0) {
			// a piture that is longer than wide.
			// the width of the picture has to fit in the page 
			// (considering the margins with 40)
			scale = Math.min(1, Math.min((PageSize.A4.width() - 40)
					/ (boundingBox.width + 50), (PageSize.A4.height() - 40)
					/ (boundingBox.height + 50)));

		} else {
			// a picture that is square or wider than long.

			//width of the picture has to fit in the page 
			// (considering the margins with 40)
			// landscape-width = portrait-height !
			scale = Math.min(1, Math.min((PageSize.A4.height() - 40)
					/ (boundingBox.width + 50), (PageSize.A4.width() - 40)
					/ (boundingBox.height + 50)));

		}
		document = new Document(new com.lowagie.text.Rectangle(
				boundingBox.width, boundingBox.height));

		try {
			//the writer
			PdfWriter writer = PdfWriter.getInstance(document,
					new FileOutputStream(filename));

			// opening the document
			document.open();

			// getting the context to write in
			PdfContentByte cb = writer.getDirectContent();
			Graphics2D g2 = cb.createGraphicsShapes(boundingBox.width,
					boundingBox.height);

			// painting myself 
			component.paint(g2);

			cb.transform(AffineTransform.getScaleInstance(scale, scale));

			// updating
			g2.dispose();
			document.close();

		} catch (DocumentException de) {
			System.err.println(de.getMessage());
		}
	}

	public static void exportPicture(JComponent comp, String name, String ext)
			throws IOException {

		BufferedImage bi = new BufferedImage(comp.getMaximumSize().width, comp
				.getMaximumSize().height, BufferedImage.TYPE_INT_RGB);

		Graphics2D graphCont = bi.createGraphics();

		RepaintManager currentManager = RepaintManager.currentManager(comp);
		currentManager.setDoubleBufferingEnabled(false);
		comp.paint(graphCont);

		String pointedExtension = ext;
		if (!ext.startsWith(".")) {
			pointedExtension = "." + ext;
		}

		String filename;
		if (!name.endsWith(pointedExtension)) {
			filename = name + pointedExtension;
		} else {
			filename = name;
		}
		File file = new File(filename);

		String picExt = pointedExtension.substring(1, ext.length());

		ImageIO.write(bi, picExt, file);
		graphCont.dispose();
		currentManager.setDoubleBufferingEnabled(true);
		//		System.out.println("File: " + filename + " Ext: " + pointedExtension +" picext:" + picExt);
	}
}
