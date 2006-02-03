/*
 * @(#)CodecTools.java created 03.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.domgraph.codec;

import java.util.List;

public class CodecTools {
    public static String join(List<String> strings, String separator) {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        
        for( String s : strings ) {
            if( first ) {
                first = false;
            } else {
                sb.append(separator);
            } 
            
            sb.append(s);
        }
        
        return sb.toString();
        
    }
    
    public static String atomify(String label) {
        boolean must_atomify = false;
        
        for( int i = 0; i < label.length(); i++ ) {
            if( !Character.isLetterOrDigit(label.charAt(i)) && (label.charAt(i) != '_') ) {
                must_atomify = true;
            }
        }
        
        if( Character.isUpperCase(label.charAt(0)) ||
                Character.isDigit(label.charAt(0)) ||
                (label.charAt(0) == '_') ) {
            must_atomify = true;
        }
        
        return must_atomify ? ("\'" + label + "'") : label;
    }
    
    public static String varify(String label) {
        if( !label.startsWith("_") && !Character.isUpperCase(label.charAt(0)) ) {
            return "_" + label;
        } else {
            return label;
        }
    }
    
    public static String encodeXmlEntities(String str) {
        return
            str.replaceAll("&", "&amp;").replaceAll("'", "&apos;").replaceAll("\"", "&quot;")
                .replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }


}
