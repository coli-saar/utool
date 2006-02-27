/*
 * @(#)RootNotFreeException.java created 03.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.chart;

/**
 * This exception will be thrown by the <code>computeSplit</code> method
 * of the class <code>SplitSource.SplitComputer</code> if the root
 * that it gets as an argument is not free. 
 * 
 * @author Alexander Koller
 *
 */
public class RootNotFreeException extends Exception {

    public RootNotFreeException() {
        super();
    }

    public RootNotFreeException(String message) {
        super(message);
    }

    public RootNotFreeException(String message, Throwable cause) {
        super(message, cause);
    }

    public RootNotFreeException(Throwable cause) {
        super(cause);
    }

}
