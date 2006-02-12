/*
 * @(#)ExitCodes.java created 03.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.utool;

public class ExitCodes {
    public static final int MALFORMED_DOMGRAPH_BASE_INPUT = 192;   // 11000000
    public static final int MALFORMED_DOMGRAPH_BASE_OUTPUT = 224;  // 11100000
    
    // range of 10000000 ... 10111111 reserved for Utool main program
    public static final int ILLFORMED_GRAPH = 128;
    public static final int NO_INPUT_CODEC_SPECIFIED = 129;
    public static final int NO_SUCH_INPUT_CODEC = 130;
    public static final int NO_OUTPUT_CODEC_SPECIFIED = 131;
    public static final int NO_SUCH_OUTPUT_CODEC = 132;
    public static final int NO_OUTPUT_FILE = 133;
    public static final int NO_SUCH_COMMAND = 134;
    public static final int CHAIN_WITHOUT_LENGTH = 135;
    public static final int IO_ERROR = 136;
    public static final int OUTPUT_CODEC_NOT_APPLICABLE = 137;
    public static final int CODEC_REGISTRATION_ERROR = 138;
    public static final int EQUIVALENCE_READING_ERROR = 139;
    public static final int PARSING_ERROR = 140;
    public static final int PARSER_CONFIGURATION_ERROR = 141;
    
    
    // exit codes for "utool classify"
    public static final int  CLASSIFY_WEAKLY_NORMAL = 1;
    public static final int  CLASSIFY_NORMAL = 2;
    public static final int  CLASSIFY_COMPACT = 4;
    public static final int  CLASSIFY_COMPACTIFIABLE = 8;
    public static final int  CLASSIFY_HN_CONNECTED = 16;
    public static final int  CLASSIFY_LEAF_LABELLED = 32;
    
}
