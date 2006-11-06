package de.saar.chorus.ubench.gui;

import java.io.IOException;
import java.io.Writer;

import de.saar.chorus.domgraph.codec.CodecMetadata;
import de.saar.chorus.domgraph.codec.CodecOption;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.OutputCodec;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;


@CodecMetadata(name="dummy-codec", extension=".du.co")
public class DummyCodec extends OutputCodec {

	public enum Shape {SQARE, CIRCLE, SCHNAPPI, SNOOPY};
	private Shape shape = Shape.CIRCLE;
	private boolean dressed = false;
	private String color;
	
	public DummyCodec (@CodecOption
			(name="shape", defaultValue="SCHNAPPI")
			Shape shape,
			@CodecOption(name="color", defaultValue="purple") String color,
			@CodecOption(name="dressed", defaultValue="true")
			boolean dressed,
			@CodecOption(name="dressed2", defaultValue="true")
			boolean dressed2,
			@CodecOption(name="dressed3", defaultValue="true")
			boolean dressed3,
			@CodecOption(name="dressed4", defaultValue="true")
			boolean dressed4,
			@CodecOption(name="dressed5", defaultValue="true")
			boolean dressed5,
			@CodecOption(name="dressed6", defaultValue="true")
			boolean dressed6,
			@CodecOption(name="dressed7", defaultValue="true")
			boolean dressed7,
			@CodecOption(name="dressed8", defaultValue="true")
			boolean dressed8,
			@CodecOption(name="dressed9", defaultValue="true")
			boolean dressed9,
			@CodecOption(name="dressed10", defaultValue="true")
			boolean dressed10,
			@CodecOption(name="dressed11", defaultValue="true")
			boolean dressed11,
			@CodecOption(name="dressed12", defaultValue="true")
			boolean dressed12) {
        this.shape = shape;
        this.dressed = dressed;
        this.color = color;
	}
	

	/* (non-Javadoc)
	 * @see de.saar.chorus.domgraph.codec.OutputCodec#encode(de.saar.chorus.domgraph.graph.DomGraph, de.saar.chorus.domgraph.graph.NodeLabels, java.io.Writer)
	 */
	@Override
	public void encode(DomGraph graph, NodeLabels labels, Writer writer) throws IOException, MalformedDomgraphException {
		System.err.println("This is simply not made for encoding graphs, OK?");
		System.err.println("Option Shape: " + shape);
		System.err.println("Option dressed: " + dressed);
		System.err.println("Option color: " + color);
	}


	/* (non-Javadoc)
	 * @see de.saar.chorus.domgraph.codec.OutputCodec#print_footer(java.io.Writer)
	 */
	@Override
	public void print_footer(Writer writer) throws IOException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see de.saar.chorus.domgraph.codec.OutputCodec#print_header(java.io.Writer)
	 */
	@Override
	public void print_header(Writer writer) throws IOException {
		// TODO Auto-generated method stub
		
	}


}
