/*
 * @(#)MalformedDomgraphException.java created 25.01.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec;

public class MalformedDomgraphException extends Exception {
    private int exitcode = 0;

    
    
    public MalformedDomgraphException(int exitcode) {
        super();
        this.exitcode = exitcode;
    }

    public MalformedDomgraphException(String message, Throwable cause, int exitcode) {
        super(message, cause);
        this.exitcode = exitcode;
    }

    public MalformedDomgraphException(String message, int exitcode) {
        super(message);
        this.exitcode = exitcode;
    }

    public MalformedDomgraphException(Throwable cause, int exitcode) {
        super(cause);
        this.exitcode = exitcode;
    }


    
    public MalformedDomgraphException() {
        this(0);
    }

    public MalformedDomgraphException(String message, Throwable cause) {
        this(message,cause,0);
    }

    public MalformedDomgraphException(String message) {
        this(message,0);
    }

    public MalformedDomgraphException(Throwable cause) {
        this(cause,0);
    }

    
    
    public int getExitcode() { 
        return exitcode; 
    }
}
