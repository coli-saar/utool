/*
 * @(#)XmlDecodingException.java created 15.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.basic;

public class XmlDecodingException extends Exception {

    public XmlDecodingException() {
        super();
    }

    public XmlDecodingException(String message) {
        super(message);
    }

    public XmlDecodingException(String message, Throwable cause) {
        super(message, cause);
    }

    public XmlDecodingException(Throwable cause) {
        super(cause);
    }

}
