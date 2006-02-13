/*
 * @(#)XmlEncodingWriter.java created 13.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.basic;

import java.io.Writer;

public class XmlEncodingWriter extends ReplacingWriter {

    public XmlEncodingWriter(Writer writer) {
        super(writer);
        
        addReplacementRule("&", "&amp;");
        addReplacementRule("<", "&lt;");
        addReplacementRule(">", "&gt;");
        addReplacementRule("'", "&apos;");
        addReplacementRule("\"", "&quot;");
    }

}
