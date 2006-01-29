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

import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.codec.CodecManager;
import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.codec.OutputCodec;
import de.saar.chorus.domgraph.codec.basic.Chain;
import de.saar.chorus.domgraph.codec.domconOz.DomconOzInputCodec;
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
        solve      
        ("Solve an underspecified description",
                "Usage: utool solve [options] [input-source]\n\n" +
                "By default, reads an underspecified description from standard input\n" +
                "and writes the solutions to standard output. An alternative input source\n" +
                "can be specified on the command line; an alternative filename for the\n" +
                "output can be specified with the -o option. The input and output codecs\n" +
                "can be specified with the -I and -O options. If only an input codec\n" +
                "is specified, and an output codec of the same name exists, this codec\n" +
                "is used for output. `utool --display-codecs' will display a list of\n" +
                "input and output codecs.\n\n" +
                "Valid options:\n" +
                "  --output, -o filename           Write solutions to a file.\n" +
                "  --input-codec, -I codecname     Specify the input codec.\n" +
                "  --output-codec, -O codecname    Specify the output codec (default: same as input).\n"),
                
        solvable   
        ("Check solvability without enumerating solutions",
                "Usage: utool solvable [options] [input-source]\n\n" +
                "This command checks whether an underspecified description is solvable.\n" +
                "If it is, utool terminates with an exit code of 0; if it isn't, it terminates\n" +
                "with an exit code of 1.\n\n" +
                "The \'solvable\' command computes the total number of solved forms (= readings),\n" +
                "but not the solved forms themselves (use \'solve\' if you want them). This makes\n" +
                "\'solvable\' run much, much faster than \'solve\'. utool will display the total\n" +
                "number of solved forms if you run \'utool solvable -s\'.\n\n" +
                "By default, reads an underspecified description from standard input\n" +
                "and writes the solutions to standard output. An alternative input source\n" +
                "can be specified on the command line; an alternative filename for the\n" +
                "output can be specified with the -o option. The input codec\n" +
                "can be specified with the -I option. `utool --display-codecs'\n" +
                "will display a list of valid input codecs.\n\n" +
                "Valid options:\n" +
                "  --input-codec, -I codecname     Specify the input codec.\n"),
                
                
        convert    
        ("Convert underspecified description from one format to another",
                "Usage: utool convert -I inputcodec -O outputcodec [options] [input-source]\n\n" +
                "By default, reads an underspecified description from standard input\n" +
                "and writes it (in a different format) to standard output. An alternative\n" +
                "input source can be specified on the command line; an alternative filename\n" +
                "for the output can be specified with the -o option. The input and output\n" +
                "codecs can be specifieid with the -I and -O options. `utool --display-codecs'\n" +
                "will display a list of input and output codecs.\n\n" +
                "Valid options:\n" +
                "  --output, -o filename           Write solutions to a file.\n" +
                "  --input-codec, -I codecname     Specify the input codec.\n" +
                "  --output-codec, -O codecname    Specify the output codec (default: same as input).\n"),

                
        classify   
        ("Check whether a description belongs to special classes",
                "Usage: utool classify [options] [input-source]\n\n" +
                "This command checks whether an underspecified description belongs to a\n" +
                "class with special properties. A call to \'utool classify\' returns an\n" +
                "exit code that is the OR combination of some of the following values:\n\n" +
                "    1   the description is a weakly normal dominance graph\n" +
                "    2   the description is a normal dominance graph\n" +
                "    4   the description is compact\n" +
                "    8   the description can be compactified (or is already compact)\n" +
                "   16   the description is hypernormally connected\n" +
                "   32   the description is leaf-labelled\n\n" +
                "For example, the exit code for a graph that is hypernormally connected\n" +
                "and normal (and hence compactifiable), but not compact, would be 27.\n\n" +
                "Note that the notion of hypernormal connectedness only makes sense\n" +
                "for normal graphs (although utool will test for it anyway).\n\n" +
                "Valid options:\n" +
                "  --input-codec, -I codecname     Specify the input codec.\n"),                
                
        help       
        ("Display help on a command",
                "Usage: utool help [command]\n\n" +
                "Without any further parameters, \'utool help\' displays a list of available\n" +
                "commands. Alternatively, pass one of the command names to \'utool help\' as the\n" +
                "second parameter to get command-specific help for this command.\n")

                ;

        
        public String shortDescription, longDescription;
        
        Operation(String shortDescription, String longDescription) {
            this.shortDescription = shortDescription;
            this.longDescription = longDescription;
        }
    }
    
    
    // exit codes for "utool classify"
    public static final int  CLASSIFY_WEAKLY_NORMAL = 1;
    public static final int  CLASSIFY_NORMAL = 2;
    public static final int  CLASSIFY_COMPACT = 4;
    public static final int  CLASSIFY_COMPACTIFIABLE = 8;
    public static final int  CLASSIFY_HN_CONNECTED = 16;
    public static final int  CLASSIFY_LEAF_LABELLED = 32;


    
    

    /**
     * @param args
     */
    public static void main(String[] args) {
        InputCodec inputCodec = null;
        OutputCodec outputCodec = null;
        Operation op = null;
        String argument = null;
        Writer output = new OutputStreamWriter(System.out);
        String outputname;
        boolean displayStatistics = false;
        boolean noOutput = false;
        DomGraph graph = new DomGraph();
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
            argument = rest.get(1);
        }
        
        
        // handle special commands
        if( getopt.hasOption('d')) {
            codecManager.displayAllCodecs(System.out);
            System.exit(0);
        }
        
        if( getopt.hasOption('h') ) {
            displayHelp(op);
            System.exit(0);
        }
        
        if( op == Operation.help ) {
            displayHelp(resolveOperation(argument));
            System.exit(0);
        }
        
        if( getopt.hasOption(OPTION_HELP_OPTIONS)) {
            displayHelpOptions();
            System.exit(0);
        }

        if( getopt.hasOption(OPTION_VERSION)) {
            displayVersion();
            System.exit(0);
        }
        
        
        
        // at this point, we must have an operation
        if( op == null ) {
            displayHelp(null);
            System.exit(0);
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
            if( argument != null ) {
                inputCodec = codecManager.getInputCodecForFilename(argument);
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
        
        
        // parse the global options
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
            if( argument == null ) {
                argument = "-"; // stdin
            }
            
            inputCodec.decode(argument, graph, labels);
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
            int programExitCode = 0;
            
            if( graph.isWeaklyNormal() ) {
                programExitCode |= CLASSIFY_WEAKLY_NORMAL;
            }

            if( graph.isNormal() ) {
                programExitCode |= CLASSIFY_NORMAL;
            }
            
            if( graph.isCompact() ) {
                programExitCode |= CLASSIFY_COMPACT;
            }
            
            if( graph.isCompactifiable() ) {
                programExitCode |= CLASSIFY_COMPACTIFIABLE;
            }
            
            if( graph.isHypernormallyConnected() ) {
                if( displayStatistics ) {
                    System.err.println("The graph is hypernormally connected.");
                }
                programExitCode |= CLASSIFY_HN_CONNECTED;
            } else {
                if( displayStatistics ) {
                    System.err.println("The graph is not hypernormally connected.");
                }
            }
            
            if( graph.isLeafLabelled() ) {
                if( displayStatistics ) {
                    System.err.println("The graph is leaf-labelled.");
                }
                programExitCode |= CLASSIFY_LEAF_LABELLED;
            } else {
                if( displayStatistics ) {
                    System.err.println("The graph is not leaf-labelled.");
                }
            }

            System.exit(programExitCode);
            
        case help:
            
            // This was handled above.
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
        if( op == null ) {
            System.err.println("Usage: java -jar Utool.jar <subcommand> [options] [args]");
            System.err.println("Type `utool help <subcommand>' for help on a specific subcommand.");
            System.err.println("Type `utool --help-options' for a list of global options.");
            System.err.println("Type `utool --display-codecs' for a list of supported codecs.\n");
            
            System.err.println("Available subcommands:");
            for( Operation _op : Operation.values() ) {
                System.err.println(String.format("    %1$-12s %2$s.",
                        _op, _op.shortDescription));
            }

            System.err.println("\nutool is the Swiss Army Knife of Underspecification.");
            System.err.println("For additional information, see http://utool.sourceforge.net/");
        } else {
            System.err.println("utool " + op + ": " + op.shortDescription + ".");
            System.err.println(op.longDescription);
        }
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
            codecManager.registerCodec(DomconOzInputCodec.class);
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

    

    private static void displayHelpOptions() {
        System.err.println("utool global options are:");
        System.err.println("  --help-options                    Displays this information about global options.");;
        System.err.println("  --display-codecs, -d              Displays all input and output filters.");
        System.err.println("  --display-statistics, -s          Displays runtime and other statistics.");
        System.err.println("  --no-output, -n                   Do not display computed output.");
        System.err.println("  --equivalences, -e <filename>     Eliminate equivalent readings.");
        System.err.println("  --version                         Display version and copyright information.");
    }

}
