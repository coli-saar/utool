/*
 * @(#)XmlEntities.java created 13.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.basic;

public class XmlEntities {
    public static String encode(String s) {
        if( s == null ) {
            return null;
        } else {
            return s.replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("'", "&apos;")
            .replaceAll("\"", "&quot;");
        }
    }
    
    public static String decode(String s) {
        if( s == null ) {
            return null;
        } else {
            return s.replaceAll("&lt;", "<")
            .replaceAll("&gt;", ">")
            .replaceAll("&apos;", "'")
            .replaceAll("&quot;", "\"")
            .replaceAll("&amp;", "&");
        }
    }
}

