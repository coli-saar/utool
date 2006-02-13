/*
 * @(#)StringTools.java created 13.02.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.basic;

import java.util.List;

public class StringTools {
    public static String join(List<String> strings, String separator) {
        boolean first = true;
        StringBuffer sb = new StringBuffer();
        
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

}
