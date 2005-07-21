/*
 * @(#)StressPattern.java created 18.07.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.poetry.stress;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StressPattern {
    private List<Boolean> pattern;
    
    public StressPattern(String p) {
        String[] syllables = p.split("\\s*,\\s*");
        
        pattern = new ArrayList<Boolean>();
        
        for( int i = 0; i < syllables.length; i++ ) {
            if( syllables[i].equals("s") )
                pattern.add(Boolean.TRUE);
            else
                pattern.add(Boolean.FALSE);
        }
    }
    
    public Set<Integer> wordFitsAtPositions(List<WordPronunciation> prons) {
        Set<Integer> ret = new HashSet<Integer>();
        
        if( prons == null ) {
            return ret;
        } else {
            for( WordPronunciation pron : prons ) {
                List<Integer> wordStressPattern = pron.getStressPattern();
                
                for( int i = 0; i < pattern.size() - wordStressPattern.size() + 1; i++ ) {
                    if( patternsMatch(wordStressPattern, i) ) {
                        ret.add(new Integer(i));
                    }
                }
            }
        }
        
        return ret;
    }

    private boolean patternsMatch(List<Integer> candidate, int startPos) {
        boolean allUnstressed = true;
        
        // 1. Patterns match if all syllables in the target pattern are unstressed.
        for( int i = startPos; i < startPos + candidate.size(); i++ ) {
            if( pattern.get(i) )
                allUnstressed = false;
        }
        
        if( allUnstressed )
            return true;
        
        // 2. Otherwise, patterns match if at least the primary accent of the
        // candidate word is stressed.
        for( int i = 0; i < candidate.size(); i++ ) {
            if( candidate.get(i) == WordPronunciation.PRIMARY ) {
                if( !pattern.get(startPos+i) )
                    return false;
            }
        }
        
        return true;
    }
    
    public String toString() {
        StringBuilder b = new StringBuilder();
        for( Boolean s : pattern ) {
            if( s.booleanValue() ) {
                b.append("/");
            } else {
                b.append(".");
            }
        }
        
        return b.toString();
    }
}
