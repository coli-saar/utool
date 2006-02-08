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
        // TODO Auto-generated constructor stub
    }

    public MalformedDomgraphException() {
        super();
        // TODO Auto-generated constructor stub
    }

    public MalformedDomgraphException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public MalformedDomgraphException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public MalformedDomgraphException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public int getExitcode() { return exitcode; }

}
