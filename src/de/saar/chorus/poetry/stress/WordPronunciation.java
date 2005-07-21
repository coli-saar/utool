/*
 * @(#)WordPronunciation.java created 18.07.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.poetry.stress;

import java.util.ArrayList;
import java.util.List;

public class WordPronunciation {
    private String pron;
    private char[] pronArray;
    
    public static final Integer
        UNSTRESSED = new Integer(0),
        PRIMARY = new Integer(1),
        SECONDARY = new Integer(2);
    
    WordPronunciation(String pron) {
        this.pron = pron;
        pronArray = pron.toCharArray();
    }
    
    public List<Integer> getStressPattern() {
        List<Integer> ret = new ArrayList<Integer>();
        
        for( int i = 0; i < pronArray.length; i++ ) {
            switch(pronArray[i]) {
            case '0': ret.add(UNSTRESSED); break;
            case '1': ret.add(PRIMARY); break;
            case '2': ret.add(SECONDARY); break;
            }
        }
        
        return ret;
    }
    
    public List<Boolean> getBooleanStressPattern() {
        List<Boolean> ret = new ArrayList<Boolean>();
        
        for( Integer s : getStressPattern() ) {
            if( s == UNSTRESSED )
                ret.add(Boolean.FALSE);
            else
                ret.add(Boolean.TRUE);
        }
        
        return ret;
    }
    
    public String toString() {
        return pron;
    }
}
