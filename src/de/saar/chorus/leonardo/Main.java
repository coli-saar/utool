/*
 * Created on 27.07.2004
 *
 */
package de.saar.coli.chorus.leonardo;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;

import de.saar.coli.getopt.ConvenientGetopt;

/**
 * The main program for Leonardo.
 * 
 * At the moment, this program can be run in three modes:
 * <ol>
 * <li><strong>Standalone mode </strong>: Expects the name of a file containing
 * a GXL description of a dominance graph on the command line. Parses the file
 * and displays the dominance graph.
 * <li><strong>Dummy mode </strong>: Without any command-line arguments, the
 * program will display a small sample graph.
 * <li><strong>Server mode </strong>: Accepts socket connections on the
 * specified port. If the client sends a GXL description of a graph (on a single
 * line), it will draw this graph. Clicks on popup menus are translated into
 * messages of the form
 * 
 * <blockquote><code>
 * &lt;popupClicked clickedOn="..." type="..." menulabel="..." /&gt;,
 * </code>
 * </blockquote>
 * 
 * which are sent back to the client. "clickedOn" is the name of the node or
 * edge that triggered the popup menu. "type" is either "node" or "edge",
 * depending on the type of the popup trigger. "menulabel" is the menu label
 * that was specified in the GXL description.
 * <p>
 * 
 * The client can also send a message <code>&lt;close /&gt;</code>, which
 * will instruct Leonardo to close the current socket (and window), and accept a
 * new socket connection.
 * </ol>
 * 
 * 
 * TODO Distinguish holes and roots, and draw holes differently (as circles?).
 * <p>
 * 
 * TODO Extend GXL format so nodes and perhaps edges can be given visual
 * attributes, e.g. colours. This is used quite a bit in the CHORUS demo, and we
 * should support it.
 *  
 */
public class Main {
    public static void main(String[] args)  {
        // parse command-line arguments
        ConvenientGetopt getopt = 
            new ConvenientGetopt("Leonardo",
                "java -jar Leonardo.jar [options] [filename]",
            	"If Leonardo doesn't run in server mode, specify a filename on the command line"
                + "\nto display the graph.");
        
        getopt.addOption('s', "server", ConvenientGetopt.NO_ARGUMENT,
        		"Run in server mode", null);
        getopt.addOption('p', "port", ConvenientGetopt.REQUIRED_ARGUMENT,
                		"Use port <arg> for server mode", "4300");
        
        getopt.parse(args);
        
        // extract arguments
        boolean serverMode = getopt.hasOption('s');
        int port = Integer.parseInt(getopt.getValue('p'));
        
        String filename = null;
        if( !getopt.getRemaining().isEmpty() )
            filename = getopt.getRemaining().get(0);
        
        // run main program (server or standalone)
        if (serverMode) {
            runServer(port);
        } else {
            runStandalone(filename);
        }
    }
    
    
    
    
    
    
    /**
     * Run Leonardo as server.
     * 
     * TODO The window drawing is very clumsy: A new frame is created for
     * each new graph that is to be drawn. This seems to be necessary because
     * the JGraph component doesn't seem to redraw itself properly if the
     * graph is changed or the window is resized (need to clarify exact
     * source of problems). It works for now, but should be cleaned up 
     * in the future. 
     * 
     * @param port the port on which Leonardo accepts socket connections
     */
    private static void runServer(int port) {
        JFrame f = null;
        
        while( true ) {
            // accept connection
            final Server serv = new Server(port);
            
            // set up basic (empty) graph object
            final JDomGraph graph = new JDomGraph();
            graph.addPopupListener(new DomGraphPopupListener() {
                public void popupSelected(DefaultGraphCell source,
                        Fragment fragment, String menuItem) {
                    StringBuilder msg = new StringBuilder(
                        	"<popupClicked");

                    if (source instanceof DefaultEdge) {
                        EdgeData data = graph
                            	.getEdgeData((DefaultEdge) source);
                        msg.append(" clickedOn=\"" + data.getName()
                                + "\" type=\"edge\"");
                    } else {
                        NodeData data = graph.getNodeData(source);
                        msg.append(" clickedOn=\"" + data.getName()
                                + "\" type=\"node\"");
                    }

                    msg.append(" menulabel=\"" + menuItem + "\"");

                    msg.append(" />");

                    serv.write(msg.toString());
                }
            });
            
            String messageStr;
            
            try {
                do {
                    // read a line from the socket
                    messageStr = serv.read();

                    if (messageStr != null) {
                        // "close" command: close the connection
                        if( messageStr.equals("<close/>") || messageStr.equals("<close />") )
                            break;
                    
                        // otherwise interpret message as GXL description of a graph
                        DomGraphGXLCodec.decode(new StringReader(messageStr),
                                graph);
 
                        graph.computeFragments();
                        graph.computeLayout();
                    
                    
                        // open the window
                        if( f != null )
                            f.setVisible(false);
                    
                        f = makeWindow();
                        if( graph.getName() != null ) {
                            f.setTitle(graph.getName() + " ["
                                    + serv.getClientAddress() + ":" +
                                    serv.getClientPort() + "] - Leonardo");
                        } else {
                            f.setTitle("Server mode [" + 
                                    serv.getClientAddress() + ":"
                                    + serv.getClientPort() + "] - Leonardo");
                        }
                        JScrollPane scrollpane = new JScrollPane(graph);
                        f.add(scrollpane);
                        f.pack();
                        f.setVisible(true);
                        f.toFront();
                    }
                } while (messageStr != null);
            } catch(Exception e) {
                System.err.println("Error while parsing GXL description!");
                e.printStackTrace(System.err);
            }
            
            // hide old frame if we had one.
            if( f != null ) 
                f.setVisible(false);
        }
    }
    
    
    
    
    
    

    /**
     * Run Leonardo in standalone or dummy mode.
     * 
     * If the "filename" parameter is not null, read a GXL description from
     * the specified file and display it (standalone mode).
     * 
     * Otherwise, just draw a small sample graph (dummy mode).
     * 
     * @param filename see above
     */
    private static void runStandalone(String filename) {
        // set up window
        JFrame f = makeWindow();
        final JDomGraph graph = new JDomGraph();
        graph.addPopupListener(new DomGraphPopupListener() {
            public void popupSelected(DefaultGraphCell source,
                    Fragment fragment, String menuId) {

                if( source instanceof DefaultEdge ) {
                    System.err.println("Click (edge): " + graph.getEdgeData((DefaultEdge) source).getName()
                            	+ ", fragment = " + fragment.getMenuLabel() 
                            	+ ", menuId = " + menuId);
                } else {
                    System.err.println("Click (node): " + graph.getNodeData(source).getName()
                            + ", fragment = " + fragment.getMenuLabel()
                            + ", menuId = " + menuId);
                }
            }
        });

        
        // acquire graph to draw
        if (filename != null) {
            File gxl = new File(filename);
            f.setTitle(gxl.getName() + " - Leonardo");
            
            try {
                DomGraphGXLCodec.decode(new FileReader(gxl), graph);
            } catch(Exception e) {
                System.err.println("Error while parsing " + gxl + ":");
                e.printStackTrace(System.err);
                System.exit(1);
            }
        } else {
            f.setTitle("Sample graph - Leonardo");
            graph.addSampleData();
        }
        
        if( graph.getName() != null )
            f.setTitle(graph.getName() + " - Leonardo");

        
        // compute fragments & layout
        graph.computeFragments();
        graph.computeLayout();
        
        if( filename == null )
            graph.addSampleFragmentMenus();
            

        // all the rest of the Swing stuff
        JScrollPane scrollpane = new JScrollPane(graph);
        f.add(scrollpane);
        f.pack();
        f.setVisible(true);
    }

    /**
     * Create a new JFrame window. The application will be terminated
     * once this window is closed.
     * 
     * @return the new window
     */
    private static JFrame makeWindow() {
        JFrame f = new JFrame("JGraph Test");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        return f;
    }
}






/**

test inputs to send over the socket:



        <gxl xmlns:xlink="http://www.w3.org/1999/xlink"><graph id="chain2" edgeids="true" hypergraph="false" edgemode="directed"><!-- OF 1 --><node id="X"><type xlink:href="root" /><attr name="label"><string>f</string></attr><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><node id="X1"><type xlink:href="hole" /><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><edge from="X" to="X1" id="x-x1"><type xlink:href="solid" /></edge><!-- OF 2 --><node id="Y"><type xlink:href="root" /><attr name="label"><string>g</string></attr><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><node id="Y1"><type xlink:href="hole" /><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><edge from="Y" to="Y1" id="Y-Y1"><type xlink:href="solid" /></edge><!-- UF 1 --><node id="Z"><type xlink:href="root" /><attr name="label"><string>a</string></attr><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><!-- dominances --><edge from="X1" to="Z" id="x1-z"><type xlink:href="dominance" /></edge><edge from="Y1" to="Z" id="y1-z"><type xlink:href="dominance" /></edge></graph></gxl>
        <gxl xmlns:xlink="http://www.w3.org/1999/xlink"><graph id="chain2b" edgeids="true" hypergraph="false" edgemode="directed"><!-- OF 1 --><node id="X"><type xlink:href="root" /><attr name="label"><string>g</string></attr><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><node id="X1"><type xlink:href="hole" /><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><edge from="X" to="X1" id="x-x1"><type xlink:href="solid" /></edge><!-- OF 2 --><node id="Y"><type xlink:href="root" /><attr name="label"><string>h</string></attr></node><node id="Y1"><type xlink:href="hole" /><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><edge from="Y" to="Y1" id="Y-Y1"><type xlink:href="solid" /></edge><!-- UF 1 --><node id="Z"><type xlink:href="root" /><attr name="label"><string>b</string></attr><popup id="foo" label="Foo Foo" /><popup id="bar" label="Bar Bar" /><popup id="baz" label="Baz Baz" /></node><!-- dominances --><edge from="X1" to="Z" id="x1-z"><type xlink:href="dominance" /></edge><edge from="Y1" to="Z" id="y1-z"><type xlink:href="dominance" /></edge></graph></gxl>


**/