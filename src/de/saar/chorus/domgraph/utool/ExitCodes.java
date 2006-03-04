/*
 * @(#)ExitCodes.java created 03.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.utool;

/**
 * Exit codes for Utool. Utool signals errors with an exit code with
 * bit 7 set (i.e. values of 128 or more). The exit codes are split up
 * as follows:
 * <ul>
 * <li> range 128 .. 191: errors triggered in the main programme
 * <li> range 192 .. 223: errors triggered in an input codec
 * <li> range 224 .. 255: errors triggered in an output codec
 * </ul>
 * 
 * The exit codes for the main program are documented here. The
 * exit codes for the codecs are documented together with the
 * respective codec: Each codec will return an error value between
 * 0 and 31, which is then OR'ed with 192 or 224, respectively.<p>
 * 
 * This class also defines the exit codes for the "utool classify"
 * command, which are all values between 0 and 127.
 * 
 * @author Alexander Koller
 *
 */
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
    public static final int NO_INPUT = 142;
    public static final int SERVER_IO_ERROR = 143;
    
    
    // exit codes for "utool classify"
    public static final int  CLASSIFY_WEAKLY_NORMAL = 1;
    public static final int  CLASSIFY_NORMAL = 2;
    public static final int  CLASSIFY_COMPACT = 4;
    public static final int  CLASSIFY_COMPACTIFIABLE = 8;
    public static final int  CLASSIFY_HN_CONNECTED = 16;
    public static final int  CLASSIFY_LEAF_LABELLED = 32;
    
}
