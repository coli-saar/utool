/*
 * @(#)Utool.java created 27.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.utool;

import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Set;

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.codec.CodecManager;
import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.codec.OutputCodec;
import de.saar.chorus.domgraph.codec.basic.Chain;
import de.saar.chorus.domgraph.codec.gxl.GxlCodec;
import de.saar.chorus.domgraph.codec.term.OzTermOutputCodec;
import de.saar.chorus.domgraph.codec.term.PrologTermOutputCodec;
import de.saar.chorus.domgraph.graph.DomEdge;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.getopt.ConvenientGetopt;


/*
 * TODO:
 *  - see code below
 *  - exit codes
 */

public class Utool {
    private static final char OPTION_VERSION = (char) 1;
    private static final char OPTION_HELP_OPTIONS = (char) 2;
    private static final char OPTION_DUMP_CHART = (char) 3;
    
    private enum Operation {
        solve,
        convert,
        help,
        solvable,
        classify
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        InputCodec inputCodec = null;
        OutputCodec outputCodec = null;
        Operation op = null;
        String inputsrc = null;
        Writer output = new OutputStreamWriter(System.out);
        String outputname;
        boolean displayStatistics = false;
        boolean noOutput = false;
        DomGraph graph = new DomGraph();
        // TODO compactification
        NodeLabels labels = new NodeLabels();
        boolean dumpChart = false;
        
        // prepare codecs
        CodecManager codecManager = new CodecManager();
        registerAllCodecs(codecManager);

        // parse command line options
        ConvenientGetopt getopt = makeConvenientGetopt();
        getopt.parse(args);
       
        // determine operation and filename
        List<String> rest = getopt.getRemaining();
        
        if( !rest.isEmpty() ) {
            op = resolveOperation(rest.get(0));
        }
        
        if( rest.size() > 1 ) {
            inputsrc = rest.get(1);
        }
        
        
        // handle special commands
        if( getopt.hasOption('d')) {
            codecManager.displayAllCodecs(System.out);
            System.exit(0);
        }
        
        if( getopt.hasOption('h')) {
            displayHelp(op);
            System.exit(0);
        }
        
        if( getopt.hasOption(OPTION_VERSION)) {
            displayVersion();
            System.exit(0);
        }
        
        
        // at this point, we must have an operation
        if( op == null ) {
            System.err.println("You must specify an operation!");
            System.exit(1);
        }
        
        
        // determine input codec
        if( getopt.hasOption('I')) {
            inputCodec = codecManager.getInputCodecForName(getopt.getValue('I'));
            if( inputCodec == null ) {
                System.err.println("Unknown input codec: " + getopt.getValue('I'));
                System.exit(1);
            }
        }
        
        if( inputCodec == null ) {
            if( inputsrc != null ) {
                inputCodec = codecManager.getInputCodecForFilename(inputsrc);
            }
        }
        
        if( inputCodec == null ) {
            System.err.println("You must specify an input codec!");
        }
        
        
        // determine output codec
        if( getopt.hasOption('O')) {
            outputCodec = codecManager.getOutputCodecForName(getopt.getValue('O'));
        }
        
        if( outputCodec == null ) {
            outputCodec = codecManager.getOutputCodecForFilename(getopt.getValue('o'));
        }
        
        if( (outputCodec == null) && (inputCodec != null) ) {
            outputCodec = codecManager.getOutputCodecForFilename(inputCodec.getName());
        }
        
        
        // parse the rest of the flags
        outputname = getopt.getValue('o');

        if( getopt.hasOption('s')) {
            displayStatistics = true;
        }
        
        if( getopt.hasOption('n')) {
            noOutput = true;
        }
        
        if( getopt.hasOption(OPTION_DUMP_CHART)) {
            dumpChart = true;
        }
        
        
        // obtain graph
        try {
            inputCodec.decode(inputsrc, graph, labels);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        if( displayStatistics ) {
            System.err.println("The input graph has " + graph.getAllRoots().size() + " fragments.");
        }
        
        
        // check statistics 
        
        boolean weaklyNormal = graph.isWeaklyNormal();
        boolean normal = graph.isNormal();
        boolean compact = graph.isCompact();
        boolean compactifiable = graph.isCompactifiable();

        DomGraph compactGraph = graph;
        
        if( displayStatistics ) {
            if( normal ) {
                System.err.println("The input graph is normal.");
            } else {
                System.err.print("The input graph is not normal");
                if( weaklyNormal ) {
                    System.err.println (", but it is weakly normal.");
                } else {
                    System.err.println(" (not even weakly normal).");
                }
            }
            
            if( compact ) {
                System.err.println("The input graph is compact.");
            } else {
                System.err.print("The input graph is not compact, ");
                if( compactifiable ) {
                    System.err.println("but I will compactify it for you.");
                } else {
                    System.err.println("and it cannot be compactified.");
                }
            }
        }

        // compactify if necessary
        //System.err.println("original graph:\n" + graph);
        if( !compact && compactifiable ) {
            compactGraph = graph.compactify();
        }
        //System.err.println("\n\ncompact graph:\n" + compactGraph);
        
        
        
        // now do something, depending on the specified operation
        switch(op) {
        case solve:
            if( !noOutput && (outputCodec == null )) {
                System.err.println("No output codec specified!");
                System.exit(1);
            }
            
            // fall-through
            
        case solvable:
            if( displayStatistics ) {
                System.err.println();
            }
            
            if( !weaklyNormal ) {
                System.err.println("Cannot solve graphs that are not weakly normal!");
                System.exit(1);
            }
            
            if( !compact && !compactifiable ) {
                System.err.println("Cannot solve graphs that are not compact and not compactifiable!");
                System.exit(1);
            }

            if( displayStatistics ) {
                System.err.print("Solving graph ... ");
            }

            // compute chart
            long start_solver = System.currentTimeMillis();
            Chart chart = new Chart();
            ChartSolver solver = new ChartSolver(compactGraph, chart);
            boolean solvable = solver.solve();
            long end_solver = System.currentTimeMillis();
            long time_solver = end_solver - start_solver;
            
            if( solvable ) {
                if( displayStatistics ) {
                    System.err.println("it is solvable.");
                    printChartStatistics(chart, time_solver, dumpChart);
                }
                
                // TODO redundancy elimination
                
                // TODO runtime prediction
                
                if( op == Operation.solve ) {
                    try {
                        if( !noOutput ) {
                            if (!"-".equals(outputname)) {
                                output = new FileWriter(outputname);
                            }
                        
                            outputCodec.print_header(output);
                            outputCodec.print_start_list(output);
                        }
                        
                        // extract solved forms
                        long start_extraction = System.currentTimeMillis();
                        long count = 0;
                        SolvedFormIterator it = new SolvedFormIterator(chart,graph);
                        while( it.hasNext() ) {
                            List<DomEdge> domedges = it.next();
                            count++;
                            
                            if( !noOutput ) {
                                if( count > 1 ) {
                                    outputCodec.print_list_separator(output);
                                }
                                outputCodec.encode(graph, domedges, labels, output);
                            }
                        }
                        long end_extraction = System.currentTimeMillis();
                        long time_extraction = end_extraction - start_extraction;
                        
                        if( !noOutput ) {
                            outputCodec.print_end_list(output);
                            outputCodec.print_footer(output);
                        }
                        
                        if( displayStatistics ) {
                            System.err.println("Found " + count + " solved forms.");
                            System.err.println("Time spent on extraction: " + time_extraction + " ms");
                            long total_time = time_extraction + time_solver;
                            System.err.print("Total runtime: " + total_time + " ms (");
                            if( total_time > 0 ) {
                                System.err.print((int) Math.floor(count * 1000.0 / total_time));
                                System.err.print(" sfs/sec; ");
                            }
                            System.err.println(1000 * total_time / count + " microsecs/sf)");
                        }
                        
                        // TODO exit with success (1)
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                
            } else {
                // not solvable
                if( displayStatistics ) {
                    System.err.println("it is unsolvable!");
                }
                
                // TODO exit with failure (0)
            }
            break;
            
        
        case convert:
            if( !noOutput && (outputCodec == null )) {
                System.err.println("No output codec specified!");
                System.exit(1);
            }
            
            if( outputCodec.getType() != OutputCodec.Type.GRAPH ) {
                System.err.println("Output codec must be graph codec!");
                System.exit(1);
            }

            try {
                if (!"-".equals(outputname)) {
                    output = new FileWriter(outputname);
                }
                
                outputCodec.print_header(output);
                outputCodec.encode(graph, null, labels, output);
                outputCodec.print_footer(output);
                
            } catch(Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
            
            break;
            
            
        case classify:
            
            // TODO implement this
            
            break;
            
        case help:
            
            // TODO implement this
            
            break;
            
        
        
        }
        
        
    }
    
    
    
    
    private static void printChartStatistics(Chart chart, long time, boolean dumpChart) {
        System.err.println("Edges in chart: " + chart.size());
        if( dumpChart ) {
            System.err.println(chart);
        }
        System.err.println("Time to build chart: " + time + " ms");
        System.err.println("Number of solved forms: " + chart.countSolvedForms());
        System.err.println("");
    }




    private static void displayHelp(Operation op) {
        System.err.println("Placeholder for help on " + op);
    }

    private static void displayVersion() {
        System.err.println("Utool/Java (The Swiss Army Knife of Underspecification), version 0.1");
        System.err.println("Created by the CHORUS project, SFB 378, Saarland University");
        System.err.println();
    }



    private static Operation resolveOperation(String opstring) {
        for( Operation op : Operation.values() ) {
            if( op.toString().equals(opstring)) {
                return op;
            }
        }
        
        return null;
    }


    private static void registerAllCodecs(CodecManager codecManager) {
        try {
            codecManager.registerCodec(Chain.class);
            codecManager.registerCodec(GxlCodec.class);
        
            codecManager.registerCodec(OzTermOutputCodec.class);
            codecManager.registerCodec(PrologTermOutputCodec.class);
        } catch(Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }




    private static ConvenientGetopt makeConvenientGetopt() {
        ConvenientGetopt getopt = new ConvenientGetopt("Utool/Java", null, null);
        
        getopt.addOption('I', "input-codec", ConvenientGetopt.REQUIRED_ARGUMENT,
                        "Specify the input codec", null);
        getopt.addOption('O', "output-codec", ConvenientGetopt.REQUIRED_ARGUMENT,
                        "Specify the output codec", null);
        getopt.addOption('o', "output", ConvenientGetopt.REQUIRED_ARGUMENT,
                        "Specify an output file", "-");
        getopt.addOption('h', "help", ConvenientGetopt.NO_ARGUMENT,
                        "Display help information", null);
        getopt.addOption('s', "display-statistics", ConvenientGetopt.NO_ARGUMENT,
                        "Display runtime statistics", null);
        getopt.addOption(OPTION_VERSION, "version", ConvenientGetopt.NO_ARGUMENT,
                        "Display version information", null);
        getopt.addOption('d', "display-codecs", ConvenientGetopt.NO_ARGUMENT,
                        "Display installed codecs", null);
        getopt.addOption('n', "no-output", ConvenientGetopt.NO_ARGUMENT,
                        "Suppress the ordinary output", null);
        getopt.addOption(OPTION_HELP_OPTIONS, "help-options", ConvenientGetopt.NO_ARGUMENT,
                        "Display help on options", null);
        getopt.addOption(OPTION_DUMP_CHART, "dump-chart", ConvenientGetopt.NO_ARGUMENT,
                        "Display the chart after solving", null);
        
        return getopt;
    }

}
