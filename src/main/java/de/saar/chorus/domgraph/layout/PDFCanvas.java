package de.saar.chorus.domgraph.layout;

import java.awt.Color;
import java.awt.Point;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import de.saar.chorus.domgraph.graph.NodeData;

public class PDFCanvas implements Canvas {

	Document document;
	BaseFont basefont;
	PdfContentByte cb;
	PdfWriter writer;
	Font font;
	Map<String, Integer> xPos, yPos;
	Map<String, String> labels;
	Map<Point, Set<Point>> treeEdges, dominanceEdges, lightDominanceEdges,
	tempDom, tempTree, tempLDom;
	
	private float height;
	private float maxX;
	private float maxY;
	
	public PDFCanvas(String filename) throws IOException, DocumentException {

		document = new Document();
		yPos = new HashMap<String,Integer>();
		xPos = new HashMap<String,Integer>();
		
		
		labels = new HashMap<String ,String>();
		treeEdges = new HashMap<Point, Set<Point>>();
		dominanceEdges = new HashMap<Point, Set<Point>>();
		lightDominanceEdges = new HashMap<Point, Set<Point>>();
		
		
		tempDom = new HashMap<Point, Set<Point>>();
		tempTree = new HashMap<Point, Set<Point>>();
		tempLDom = new HashMap<Point, Set<Point>>();
		
		
		maxX = 0;
		maxY = 0;
			basefont =
				BaseFont.createFont(
						BaseFont.HELVETICA,
						BaseFont.CP1252,
						BaseFont.NOT_EMBEDDED);
			font = new Font(basefont, 17);

			writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
			
			
	
	}

	
	public void drawDominanceEdge(String src, String tgt) {
		drawEdge(src,tgt,tempDom);
	
	}
	
	private void drawEdge(String src, String tgt, Map<Point, Set<Point>> edges) {
		Point source = new Point(xPos.get(src) + 
				(getNodeWidth(labels.get(src))/2) ,yPos.get(src) + 10);
		
		Point target = new Point(xPos.get(tgt) + 
				(getNodeWidth(labels.get(tgt))/2), yPos.get(tgt) - 15);
		
		Set<Point> targets;
		if(edges.containsKey(source)) {
			targets = edges.get(source);
		} else {
			targets = new HashSet<Point>();
			edges.put(source,targets);
		}
		targets.add(target);
	}


	public void drawLightDominanceEdge(String src, String tgt) {
		
		drawEdge(src,tgt,tempLDom);
	
	}

	public void drawNodeAt(int x, int y, String nodename, String label, NodeData data, String show) {
		
		int realy = y + 30;
		
		labels.put(nodename, show);
		
		maxX = Math.max(x + getNodeWidth(show),maxX);
		maxY = Math.max(realy,maxY);
		xPos.put(nodename,x);
		yPos.put(nodename,realy - 5);
	}

	public void drawTreeEdge(String src, String tgt) {
		
		drawEdge(src,tgt,tempTree);
	}
	
	private void drawNodes() {
		
		for(String node : xPos.keySet()) {
			cb.beginText();
			int x = xPos.get(node);
			int realy = yPos.get(node);
			cb.moveText(x, realy);
			cb.showText(labels.get(node));
			cb.endText();
		}
		
		
		
	}
	
	private void drawEdges() {
		cb.setLineDash(0);
		cb.setColorStroke(Color.black);
		for(Point src : treeEdges.keySet() ) {
			for(Point tgt : treeEdges.get(src)) {
				
				cb.moveTo(src.x, src.y);
				cb.lineTo(tgt.x, tgt.y);
			}
		}
		cb.stroke();


		cb.setLineDash(3,3);
		cb.setColorStroke(Color.red);
		for(Point src : dominanceEdges.keySet() ) {
			for(Point tgt : dominanceEdges.get(src) ) {
				cb.moveTo(src.x, src.y);
				cb.lineTo(tgt.x, tgt.y);
			}
		}
		cb.stroke();

		cb.setColorStroke(new Color(255, 204, 230));
		for(Point src : lightDominanceEdges.keySet() ) {
			for(Point tgt : lightDominanceEdges.get(src) ) {
				cb.moveTo(src.x, src.y);
				cb.lineTo(tgt.x, tgt.y);
			}
		}
		cb.stroke();

	}

	public int getNodeHeight(String label) {
		// TODO Auto-generated method stub
		return 30;
	}

	public int getNodeWidth(String label) {
		
		return (int) basefont.getWidthPoint(label,17);
	}
	
	private void transformY(Map<String, Integer> y, Set<String> keys,int height) {
		for(String word : keys ) {
			
			int realy =  height - y.get(word);
			y.remove(word);
			y.put(word, realy);
		}
	}
	
	private void transformPoints(Map<Point, Set<Point>> old, Map<Point, Set<Point>> newpoints,  int height) {
		
	
		
		
		for(Point src : old.keySet() ) {
			Set<Point> nexttargets = new HashSet<Point>();
			newpoints.put(new Point(src.x, height - src.y), nexttargets);
			for(Point tgt: old.get(src)) {
				nexttargets.add(new Point(tgt.x, height - tgt.y));
			}
			
		}
	}
	
	public void finish() {

		
		
		document.setPageSize(new Rectangle(maxX + 10, maxY + 10));
		height = document.getPageSize().height();
		
		
		
		int h = (int) height;
		
		transformY(yPos, xPos.keySet(), h);
		transformPoints(tempDom, dominanceEdges, h);
		
		transformPoints(tempTree, treeEdges, h);
	
		transformPoints(tempLDom, lightDominanceEdges, h);
		
		document.open();
		cb = writer.getDirectContent();
		
		cb.setFontAndSize(basefont, 17);
		cb.setColorStroke(Color.BLACK);

		
		drawNodes();
		drawEdges();
		
		
		document.close();
	}

}
