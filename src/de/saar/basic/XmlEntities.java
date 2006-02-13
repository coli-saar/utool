/*
 * @(#)XmlEntities.java created 13.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.basic;


/**
 * A collection of functions for dealing with XML entities. 
 * 
 * @author Alexander Koller
 *
 */
public class XmlEntities {
    /**
     * Replaces special characters (&lt;, &gt;, quotes, and &amp;)
     * with the XML entities.
     * 
     * @param s a string
     * @return a string with the characters replaced by entities
     */
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
    
    /**
     * Replaces XML entities (&amp;amp; etc.) by the corresponding
     * characters. 
     * 
     * @param s a string
     * @return a string with the entities replaced by characters.
     */
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

