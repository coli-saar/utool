/*
 * @(#)Utool.java created 27.01.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package de.saar.chorus.domgraph.utool;

import java.io.IOException;

import de.saar.chorus.domgraph.GlobalDomgraphProperties;
import de.saar.chorus.domgraph.UserProperties;
import de.saar.chorus.domgraph.chart.Chart;
import de.saar.chorus.domgraph.chart.ChartPresenter;
import de.saar.chorus.domgraph.chart.ChartSolver;
import de.saar.chorus.domgraph.chart.OneSplitSource;
import de.saar.chorus.domgraph.chart.SolvedFormIterator;
import de.saar.chorus.domgraph.chart.SolvedFormSpec;
import de.saar.chorus.domgraph.chart.SolverNotApplicableException;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.MultiOutputCodec;
import de.saar.chorus.domgraph.equivalence.RedundancyEliminationSplitSource;
import de.saar.chorus.domgraph.equivalence.rtg.RtgRedundancyElimination;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.utool.AbstractOptions.Operation;
import de.saar.chorus.domgraph.utool.server.ConnectionManager;
import de.saar.chorus.ubench.MacIntegration;
import de.saar.chorus.ubench.Ubench;

/**
 * The Utool main program for accessing the Domgraph functionality from the
 * command-line. Utool ("Underspecification Tool") is the Swiss Army Knife of
 * Underspecification (Java version).
 * <p>
 *
 * The operation of this class is described in more detail in the end-user
 * documentation of Utool.
 *
 * @author Alexander Koller
 *
 */
public class Utool {
    public static void main(String[] args) {


        CommandLineParser optionsParser = new CommandLineParser();
        AbstractOptions options = null;

        boolean weaklyNormal = false;
        boolean normal = false;
        boolean triviallyUnsolvable = false;

        // parse command-line options and load graph
        try {
            options = optionsParser.parse(args);
        } catch( AbstractOptionsParsingException e ) {
            System.err.print(e.comprehensiveErrorMessage());
            exit(e.getExitcode());
        }

        // if we run on a Mac, set the application name here
        MacIntegration.integrate();

        // check statistics
        if( options.getOperation().requiresInput ) {
            weaklyNormal = options.getGraph().isWeaklyNormal();
            normal = options.getGraph().isNormal();

            if( options.hasOptionStatistics() ) {
                if( normal ) {
                    System.err.println("The input graph is normal.");
                } else {
                    System.err.print("The input graph is not normal");
                    if( weaklyNormal ) {
                        System.err.println(", but it is weakly normal.");
                    } else {
                        System.err.println(" (not even weakly normal).");
                    }
                }

                if( options.hasOptionEliminateEquivalence() ) {
                    System.err.println("I will eliminate equivalences (" + options.getEquations().size()
                            + " equations).");
                }
            }
        }

        // for solving operations (but not convert or display), preprocess the
        // graph
        switch( options.getOperation() ) {
        case solvable:
        case solve:
            triviallyUnsolvable = !options.preprocessGraph();
        }

        // if it turned out that the graph is trivially unsolvable, report that
        // and return
        if( triviallyUnsolvable ) {
            if( options.hasOptionStatistics() ) {
                System.err.println("The graph has trivially unsolvable dominance edges within fragments.");
            }

            if( (options.getOperation() == Operation.solve) ) {
                if( ! (options.getOutputCodec() instanceof MultiOutputCodec) && !options.hasOptionNoOutput() ) {
                    System.err.println("This output codec doesn't support the printing of multiple solved forms!");
                    exit(ExitCodes.OUTPUT_CODEC_NOT_MULTI);
                }

                MultiOutputCodec outputcodec = options.hasOptionNoOutput() ? null
                        : (MultiOutputCodec) options.getOutputCodec();

                try {
                    if( !options.hasOptionNoOutput() ) {
                        outputcodec.print_header(options.getOutput());
                        outputcodec.print_start_list(options.getOutput());
                    }

                    if( !options.hasOptionNoOutput() ) {
                        outputcodec.print_end_list(options.getOutput());
                        outputcodec.print_footer(options.getOutput());
                        options.getOutput().flush();
                    }
                } catch( IOException e ) {
                    System.err.println("An error occurred while trying to print the results.");
                    // e.printStackTrace();
                    exit(ExitCodes.IO_ERROR);
                }
            }

            exit(0);
        }

        // now do something, depending on the specified operation
        switch( options.getOperation() ) {
        case solvable:
            if( options.hasOptionNochart() ) {
                if( options.hasOptionStatistics() ) {
                    System.err.print("Checking graph for solvability (without chart) ... ");
                }

                try {
                    long start_solver = System.currentTimeMillis();
                    boolean solvable = OneSplitSource.isGraphSolvable(options.getGraph());
                    long end_solver = System.currentTimeMillis();
                    long time_solver = end_solver - start_solver;

                    if( solvable ) {
                        if( options.hasOptionStatistics() ) {
                            System.err.println("it is solvable.");
                            System.err.println("Time to determine solvability: " + time_solver + " ms");
                        }

                        exit(1);
                    } else {
                        if( options.hasOptionStatistics() ) {
                            System.err.println("it is unsolvable.");
                            System.err.println("Time to determine unsolvability: " + time_solver + " ms");
                        }

                        exit(0);
                    }
                } catch( SolverNotApplicableException e ) {
                    if( options.hasOptionStatistics() ) {
                        System.err.println("solver not applicable.");
                        System.err.println("Reason: " + e.getMessage());
                    }

                    exit(ExitCodes.SOLVER_NOT_APPLICABLE);
                }
            }

            // intentional fall-through for the non-"nochart" case

        case solve:
            DomGraph graph = options.getGraph();

            if( (options.getOperation() == Operation.solve) && ! (options.getOutputCodec() instanceof MultiOutputCodec)
                    && !options.hasOptionNoOutput() ) {
                System.err.println("This output codec doesn't support the printing of multiple solved forms!");
                exit(ExitCodes.OUTPUT_CODEC_NOT_MULTI);
            }

            if( options.hasOptionStatistics() ) {
                System.err.println();
            }

            if( options.hasOptionStatistics() ) {
                System.err.print("Solving graph ... ");
            }

            // compute chart
            long start_solver = System.currentTimeMillis();
            Chart chart = new Chart(options.getLabels());
            boolean solvable;

            try {
                if( options.hasOptionEliminateEquivalence() ) {
                    solvable = ChartSolver.solve(graph, chart, new RedundancyEliminationSplitSource(
                            new RtgRedundancyElimination(graph, options.getLabels(), options.getEquations()),
                            graph));

                } else {
                    solvable = ChartSolver.solve(graph, chart);
                }

                long end_solver = System.currentTimeMillis();
                long time_solver = end_solver - start_solver;

                if( solvable ) {
                    MultiOutputCodec outputcodec = options.hasOptionNoOutput() ? null
                            : (MultiOutputCodec) options.getOutputCodec();

                    if( options.hasOptionStatistics() ) {
                        System.err.println("it is solvable.");
                        printChartStatistics(chart, time_solver, options.hasOptionDumpChart(), graph);
                    }

                    // TODO runtime prediction (see ticket #11)

                    if( options.getOperation() == Operation.solve ) {
                        try {
                            if( !options.hasOptionNoOutput() ) {
                                outputcodec.print_header(options.getOutput());
                                outputcodec.print_start_list(options.getOutput());
                            }

                            // extract solved forms
                            long start_extraction = System.currentTimeMillis();
                            long count = 0;
                            SolvedFormIterator it = new SolvedFormIterator(chart, options.getGraph());
                            while( it.hasNext() ) {
                                SolvedFormSpec domedges = it.next();
                                count++;

                                if( !options.hasOptionNoOutput() ) {
                                    if( count > 1 ) {
                                        outputcodec.print_list_separator(options.getOutput());
                                    }
                                    outputcodec.encode(options.getGraph().makeSolvedForm(domedges),
                                            options.getLabels().makeSolvedForm(domedges), options.getOutput());
                                }
                            }
                            long end_extraction = System.currentTimeMillis();
                            long time_extraction = end_extraction - start_extraction;

                            if( !options.hasOptionNoOutput() ) {
                                outputcodec.print_end_list(options.getOutput());
                                outputcodec.print_footer(options.getOutput());
                                options.getOutput().flush();
                            }

                            if( options.hasOptionStatistics() ) {
                                System.err.println("Found " + count + " solved forms.");
                                System.err.println("Time spent on extraction: " + time_extraction + " ms");
                                long total_time = time_extraction + time_solver;
                                System.err.print("Total runtime: " + total_time + " ms (");
                                if( total_time > 0 ) {
                                    System.err.print((int) Math.floor(count * 1000.0 / total_time));
                                    System.err.print(" sfs/sec");
                                }

                                if( count > 0 ) {
                                    System.err.print("; " + 1000 * total_time / count + " microsecs/sf)");
                                }

                                System.err.println(")");
                            }
                        } catch( MalformedDomgraphException e ) {
                            System.err.println("Output of the solved forms of this graph is not supported by this output codec.");
                            System.err.println(e);
                            exit(e.getExitcode() + ExitCodes.MALFORMED_DOMGRAPH_BASE_OUTPUT);
                        } catch( IOException e ) {
                            System.err.println("An error occurred while trying to print the results.");
                            // e.printStackTrace();
                            exit(ExitCodes.IO_ERROR);
                        }
                    } // if operation == solve

                    exit(1);
                } else {
                    // not solvable
                    if( options.hasOptionStatistics() ) {
                        System.err.println("it is unsolvable!");
                    }

                    exit(0);
                }
            } catch( SolverNotApplicableException e ) {
                if( options.hasOptionStatistics() ) {
                    System.err.println("solver is not applicable!");
                    System.err.println("Reason: " + e.getMessage());
                }

                exit(ExitCodes.SOLVER_NOT_APPLICABLE);
            }

            break;

        case convert:
            if( !options.hasOptionNoOutput() ) {
                try {
                    options.getOutputCodec().print_header(options.getOutput());
                    options.getOutputCodec().encode(options.getGraph(), options.getLabels(), options.getOutput());
                    options.getOutputCodec().print_footer(options.getOutput());
                } catch( MalformedDomgraphException e ) {
                    System.err.println("This graph is not supported by the specified output codec.");
                    System.err.println(e);
                    exit(ExitCodes.MALFORMED_DOMGRAPH_BASE_OUTPUT + e.getExitcode());
                } catch( IOException e ) {
                    System.err.println("An I/O error occurred while trying to print the results.");
                    System.err.println(e);
                    exit(ExitCodes.IO_ERROR);
                }
            }

            break;

        case classify:
            int programExitCode = 0;

            if( weaklyNormal ) {
                programExitCode |= ExitCodes.CLASSIFY_WEAKLY_NORMAL;
            }

            if( normal ) {
                programExitCode |= ExitCodes.CLASSIFY_NORMAL;
            }

            if( options.getGraph().isCompact() ) {
                if( options.hasOptionStatistics() ) {
                    System.err.println("The input graph is compact.");
                }
                programExitCode |= ExitCodes.CLASSIFY_COMPACT;
            } else {
                if( options.hasOptionStatistics() ) {
                    System.err.println("The input graph is not compact.");
                }
            }

            if( options.getGraph().isHypernormallyConnected() ) {
                if( options.hasOptionStatistics() ) {
                    System.err.println("The graph is hypernormally connected.");
                }
                programExitCode |= ExitCodes.CLASSIFY_HN_CONNECTED;
            } else {
                if( options.hasOptionStatistics() ) {
                    System.err.println("The graph is not hypernormally connected.");
                }
            }

            if( options.getGraph().isLeafLabelled() ) {
                if( options.hasOptionStatistics() ) {
                    System.err.println("The graph is leaf-labelled.");
                }
                programExitCode |= ExitCodes.CLASSIFY_LEAF_LABELLED;
            } else {
                if( options.hasOptionStatistics() ) {
                    System.err.println("The graph is not leaf-labelled.");
                }
            }

            exit(programExitCode);

        case display:
            if( options.getGraph() != null ) {
                Ubench.getInstance().addJDomGraphTab(options.getInputName(), options.getGraph(), options.getLabels());
            } else {
                Ubench.getInstance();
            }

            break;

        case server:
            try {
                ConnectionManager.startServer(options);
            } catch( IOException e ) {
                System.err.println("An I/O error occurred while running the server: " + e);
                exit(ExitCodes.SERVER_IO_ERROR);
            }
            exit(0);

        case help:
            displayHelp(options.getHelpArgument());
            exit(0);

        case _helpOptions:
            displayHelpOptions();
            exit(0);

        case _displayCodecs:
            optionsParser.getCodecManager().displayAllCodecs(System.out);
            exit(0);

        case _version:
            displayVersion();
            exit(0);

        }
    }

    private static void printChartStatistics(Chart chart, long time, boolean dumpChart, DomGraph compactGraph) {
        System.err.println("Splits in chart: " + chart.size());
        if( dumpChart ) {
            // System.err.println(chart);
            System.err.println(ChartPresenter.chartOnlyRoots(chart, compactGraph));
        }

        if( time != -1 ) {
            System.err.println("Time to build chart: " + time + " ms");
        }

        System.err.println("Number of solved forms: " + chart.countSolvedForms());
        System.err.println("");
    }

    private static void displayHelp(Operation op) {
        if( (op == null) || (op.longDescription == null) ) {
            System.err.println("Usage: java -jar Utool.jar <subcommand> [options] [args]");
            System.err.println("Type `utool help <subcommand>' for help on a specific subcommand.");
            System.err.println("Type `utool --help-options' for a list of global options.");
            System.err.println("Type `utool --display-codecs' for a list of supported codecs.\n");

            System.err.println("Available subcommands:");
            for( Operation _op : Operation.values() ) {
                if( _op.shortDescription != null ) {
                    System.err.println(String.format("    %1$-12s %2$s.", _op, _op.shortDescription));
                }
            }

            System.err.println("\nUtool is the Swiss Army Knife of Underspecification (Java version).");
            System.err.println("For more information, see " + GlobalDomgraphProperties.getHomepage());
        } else {
            System.err.println("utool " + op + ": " + op.shortDescription + ".");
            System.err.println(op.longDescription);
        }
    }



    public static void exit(int syscode) {
    	UserProperties.saveProperties();
    	System.exit(syscode);
    }

    private static void displayVersion() {
        System.err.println("Utool (The Swiss Army Knife of Underspecification), " + "version "
                + GlobalDomgraphProperties.getVersion());
        System.err.println("Created by the CHORUS project, SFB 378, Saarland University");
        System.err.println();
    }

    private static void displayHelpOptions() {
        System.err.println("utool global options are:");
        System.err.println("  --help-options                    Displays this information about global options.");
        ;
        System.err.println("  --display-codecs, -d              Displays all input and output filters.");
        System.err.println("  --display-statistics, -s          Displays runtime and other statistics.");
        System.err.println("  --no-output, -n                   Do not display computed output.");
        System.err.println("  --equivalences, -e <filename>     Eliminate equivalent readings.");
        System.err.println("  --version                         Display version and copyright information.");
    }

}
