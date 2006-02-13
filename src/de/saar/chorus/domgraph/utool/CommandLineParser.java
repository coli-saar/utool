/*
 * @(#)CommandLineOptionsParser.java created 10.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.utool;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import de.saar.chorus.domgraph.codec.CodecManager;
import de.saar.chorus.domgraph.codec.InputCodec;
import de.saar.chorus.domgraph.codec.MalformedDomgraphException;
import de.saar.chorus.domgraph.codec.OutputCodec;
import de.saar.chorus.domgraph.codec.ParserException;
import de.saar.chorus.domgraph.codec.basic.Chain;
import de.saar.chorus.domgraph.codec.domcon.DomconGxlInputCodec;
import de.saar.chorus.domgraph.codec.domcon.DomconGxlOutputCodec;
import de.saar.chorus.domgraph.codec.domcon.DomconOzInputCodec;
import de.saar.chorus.domgraph.codec.domcon.DomconOzOutputCodec;
import de.saar.chorus.domgraph.codec.holesem.HolesemComsemInputCodec;
import de.saar.chorus.domgraph.codec.mrs.MrsPrologInputCodec;
import de.saar.chorus.domgraph.codec.plugging.DomconOzPluggingOutputCodec;
import de.saar.chorus.domgraph.codec.plugging.LkbPluggingOutputCodec;
import de.saar.chorus.domgraph.codec.term.OzTermOutputCodec;
import de.saar.chorus.domgraph.codec.term.PrologTermOutputCodec;
import de.saar.chorus.domgraph.equivalence.EquationSystem;
import de.saar.chorus.domgraph.graph.DomGraph;
import de.saar.chorus.domgraph.graph.NodeLabels;
import de.saar.chorus.domgraph.utool.AbstractOptions.Operation;
import de.saar.getopt.ConvenientGetopt;

public class CommandLineParser {
    private static final char OPTION_VERSION = (char) 1;
    private static final char OPTION_HELP_OPTIONS = (char) 2;
    private static final char OPTION_DUMP_CHART = (char) 3;

    private CodecManager codecManager;
    
    
    public CommandLineParser() {
        codecManager = new CodecManager();
        registerAllCodecs(codecManager);
    }

    public AbstractOptions parse(String[] args)
    throws AbstractOptionsParsingException
    {
        AbstractOptions ret = new AbstractOptions();

        String argument = null;
        Operation op = null;
        InputCodec inputCodec = null;
        OutputCodec outputCodec = null;
        
        // prepare codecs
        codecManager = new CodecManager();
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
            ret.setOperation(Operation._displayCodecs);
            return ret;
        }
        
        if( getopt.hasOption('h') ) {
            ret.setOperation(Operation.help);
            ret.setHelpArgument(op);
            return ret;
        }
        
        if( op == Operation.help ) {
            ret.setOperation(Operation.help);
            ret.setHelpArgument(resolveOperation(argument));
            return ret;
        }
        
        if( getopt.hasOption(OPTION_HELP_OPTIONS)) {
            ret.setOperation(Operation._helpOptions);
            return ret;
        }

        if( getopt.hasOption(OPTION_VERSION)) {
            ret.setOperation(Operation._version);
            return ret;
        }
        
        
        
        // at this point, we must have an operation; otherwise, we 
        // silently interpret this as a "help" command.
        if( op == null ) {
            ret.setOperation(Operation.help);
            ret.setHelpArgument(null);
            return ret;
        } else {
            ret.setOperation(op);
        }
        
        

        // obtain graph
        inputCodec = determineInputCodec(getopt, argument);
        if( inputCodec == null ) {
            throw new AbstractOptionsParsingException("You must specify an input codec!",
                    ExitCodes.NO_INPUT_CODEC_SPECIFIED);
        } else {
            DomGraph graph = new DomGraph();
            NodeLabels labels = new NodeLabels();
            
            ret.setInputCodec(inputCodec);
            
            if( argument == null ) {
                argument = "-"; // stdin
            }
            
            try {
                inputCodec.decodeFile(argument, graph, labels);
                ret.setGraph(graph);
                ret.setLabels(labels);
            } catch(MalformedDomgraphException e) {
                throw new AbstractOptionsParsingException("A semantic error occurred while decoding the graph.", 
                        e, ExitCodes.MALFORMED_DOMGRAPH_BASE_INPUT + e.getExitcode());
            } catch (IOException e) {
                throw new AbstractOptionsParsingException("An I/O error occurred while reading the input.",
                        e, ExitCodes.IO_ERROR);
            } catch (ParserException e) {
                throw new AbstractOptionsParsingException("A parsing error occurred while reading the input.",
                        e, ExitCodes.PARSING_ERROR);
            } 
        }
        
        // output
        if( getopt.hasOption('n') ) {
            ret.setOptionNoOutput(true);
        } else {
            if( op.requiresOutput ) {
                outputCodec = determineOutputCodec(getopt, inputCodec);
                
                if( outputCodec == null ) {
                    throw new AbstractOptionsParsingException("You must specify an output codec for this operation!", ExitCodes.NO_OUTPUT_CODEC_SPECIFIED);
                }
                
                ret.setOutputCodec(outputCodec);
                if( getopt.hasOption('o')) {
                    try {
                        ret.setOutput(new FileWriter(getopt.getValue('o')));
                    } catch(IOException e) {
                        throw new AbstractOptionsParsingException("An I/O error occurred while opening the output file!", e, ExitCodes.IO_ERROR);
                    }
                } else {
                    ret.setOutput(new OutputStreamWriter(System.out));
                }
            }
        }
        
        // some global options
        
        if( getopt.hasOption('s')) {
            ret.setOptionStatistics(true);
        }
        
        if( getopt.hasOption(OPTION_DUMP_CHART)) {
            ret.setOptionDumpChart(true);
        }

        
        if( getopt.hasOption('e') ) {
            try {
                EquationSystem eqs = new EquationSystem();
                eqs.read(new FileReader(getopt.getValue('e')));
                ret.setOptionEliminateEquivalence(true);
                ret.setEquations(eqs);
            } catch(Exception e) {
                throw new AbstractOptionsParsingException("An error occurred while reading the equivalences file!", e, ExitCodes.EQUIVALENCE_READING_ERROR);
            }
        }
        

        return ret;
    }
    




    private static ConvenientGetopt makeConvenientGetopt() {
        ConvenientGetopt getopt = new ConvenientGetopt("utool", null, null);
        
        getopt.addOption('I', "input-codec", ConvenientGetopt.REQUIRED_ARGUMENT,
                        "Specify the input codec", null);
        getopt.addOption('O', "output-codec", ConvenientGetopt.REQUIRED_ARGUMENT,
                        "Specify the output codec", null);
        getopt.addOption('o', "output", ConvenientGetopt.REQUIRED_ARGUMENT,
                        "Specify an output file", "-");
        getopt.addOption('e', "equivalences", ConvenientGetopt.REQUIRED_ARGUMENT,
                        "Eliminate equivalence readings", null);
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



    private static Operation resolveOperation(String opstring) {
        if( opstring == null ) {
            return null;
        }
        
        for( Operation op : Operation.values() ) {
            String name = op.toString();
            
            if( !name.startsWith("_") && name.equals(opstring)) {
                return op;
            }
        }
        
        return null;
    }
    


    
    /*** codec management ***/
    
    private void registerAllCodecs(CodecManager codecManager) {
        try {
            codecManager.registerCodec(Chain.class);
            codecManager.registerCodec(DomconOzInputCodec.class);
            codecManager.registerCodec(DomconGxlInputCodec.class);
            codecManager.registerCodec(HolesemComsemInputCodec.class);
            codecManager.registerCodec(MrsPrologInputCodec.class);
        
            codecManager.registerCodec(DomconOzOutputCodec.class);
            codecManager.registerCodec(DomconGxlOutputCodec.class);
            // TBD // codecManager.registerCodec(DomconUdrawOutputCodec.class);
            // TBD // codecManager.registerCodec(HolesemComsemOutputCodec.class);
            codecManager.registerCodec(DomconOzPluggingOutputCodec.class);
            codecManager.registerCodec(LkbPluggingOutputCodec.class);
            codecManager.registerCodec(OzTermOutputCodec.class);
            codecManager.registerCodec(PrologTermOutputCodec.class);
        } catch(Exception e) {
            System.err.println("An error occurred trying to register a codec.");
            e.printStackTrace(System.err);
            System.exit(ExitCodes.CODEC_REGISTRATION_ERROR);
        }
    }




    private  OutputCodec determineOutputCodec(ConvenientGetopt getopt, InputCodec inputCodec) 
    throws AbstractOptionsParsingException {
        OutputCodec outputCodec = null;

        if( getopt.hasOption('O')) {
            outputCodec = codecManager.getOutputCodecForName(getopt.getValue('O'));
            if( outputCodec == null ) {
                throw new AbstractOptionsParsingException("Unknown output codec: " + getopt.getValue('O'),
                        ExitCodes.NO_SUCH_OUTPUT_CODEC);
            }
        }
        
        if( outputCodec == null ) {
            outputCodec = codecManager.getOutputCodecForFilename(getopt.getValue('o'));
        }
        
        if( (outputCodec == null) && (inputCodec != null) ) {
            outputCodec = codecManager.getOutputCodecForName(inputCodec.getName());
        }
        
        return outputCodec;
    }







    private  InputCodec determineInputCodec(ConvenientGetopt getopt, String argument)
    throws AbstractOptionsParsingException {
        InputCodec inputCodec = null;
        
        if( getopt.hasOption('I')) {
            inputCodec = codecManager.getInputCodecForName(getopt.getValue('I'));
            if( inputCodec == null ) {
                throw new AbstractOptionsParsingException("Unknown input codec: " + getopt.getValue('I'),
                        ExitCodes.NO_SUCH_INPUT_CODEC);
            }
        }
        
        if( inputCodec == null ) {
            if( argument != null ) {
                inputCodec = codecManager.getInputCodecForFilename(argument);
            }
        }
        
        return inputCodec;
    }


    public CodecManager getCodecManager() {
        return codecManager;
    }


}