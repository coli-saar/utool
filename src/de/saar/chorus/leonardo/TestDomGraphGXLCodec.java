/*
 * Created on 28.07.2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package de.saar.chorus.leonardo;

import java.io.InputStream;
import java.io.StringReader;

import junit.framework.TestCase;

public class TestDomGraphGXLCodec extends TestCase {
    /*
     * TODO Testcases:
     *  - the decoded graph has correct (number of) nodes/edges
     */ 	
	private String chain2;
	private InputStream is;
	private JDomGraph graph;

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		chain2 = "<?xml version=\"1.0\"?>\n\n<!DOCTYPE gxl SYSTEM \"http://www.gupro.de/GXL/gxl-1.0.dtd\">\n\n"
			+ "<gxl xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n<graph id=\"chain2\" edgeids=\"true\" hypergraph=\"false\" edgemode=\"directed\">\n"
			+ "<!-- OF 1 -->\n<node id=\"X\">\n<type xlink:href=\"root\" />\n<attr name=\"label\"><string>f</string></attr>\n</node>\n"
			+ "<node id=\"X1\">\n<type xlink:href=\"hole\" />\n</node>\n<edge from=\"X\" to=\"X1\" id=\"x-x1\">\n<type xlink:href=\"solid\" />\n</edge>\n"
			+ "\n\n<!-- OF 2 -->\n<node id=\"Y\">\n<type xlink:href=\"root\" />\n<attr name=\"label\"><string>g</string></attr>\n</node>\n<node id=\"Y1\">\n"
			+ "<type xlink:href=\"hole\" />\n</node>\n<edge from=\"Y\" to=\"Y1\" id=\"Y-Y1\">\n<type xlink:href=\"solid\" />\n</edge>\n\n"
			+ "<!-- UF 1 -->\n<node id=\"Z\">\n<type xlink:href=\"root\" />\n<attr name=\"label\"><string>a</string></attr>\n</node>\n\n\n"
			+ "<!-- dominances -->\n<edge from=\"X1\" to=\"Z\" id=\"x1-z\">\n<type xlink:href=\"dominance\" />\n</edge>\n"
			+ "<edge from=\"Y1\" to=\"Z\" id=\"y1-z\">\n<type xlink:href=\"dominance\" />\n</edge>\n</graph>\n</gxl>";

		graph = new JDomGraph();
		DomGraphGXLCodec.decode(new StringReader(chain2), graph);
}
	
	public void testDomGraphNotNull() {
		assertTrue(graph != null);
	}
	
	public void testFoo() {
		assertTrue(true);
	}

}
