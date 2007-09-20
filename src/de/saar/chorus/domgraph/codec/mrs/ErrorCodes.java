package de.saar.chorus.domgraph.codec.mrs;

public class ErrorCodes {
	// these error codes can be combined with each other by bitwise OR
    public static final int  NOT_NORMAL = 1;
    public static final int  NOT_WEAKLY_NORMAL = 2;
    public static final int  NOT_LEAF_LABELLED = 4;
    public static final int  NOT_HYPERNORMALLY_CONNECTED = 8;
    
    // these indicate separate errors and are never OR'ed with anything
    public static final int  NOT_WELLFORMED = 16;
	public static final int NO_UNIQUE_TOP_FRAGMENT = 17;
}
