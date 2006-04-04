/*
 * @(#)Ubench.java created 18.07.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.poetry.stress;

import java.util.List;

public class Main {

    /**
     * @param args
     */
    public static void main(String[] args) {
        String filename = args[0];
        String word = args[1];
        
        System.err.println("Filename: " + filename);
        System.err.println("Word: " + word);
        
        CmuDict dict = new CmuDict();
        dict.read(filename, true);
        
        System.out.println("Lookup for " + word + ":");
        List<WordPronunciation> prons = dict.lookup(word);
        
        // output dictionary entries
        if( prons == null ) {
            System.out.println(" - none -");
        } else {
            for( WordPronunciation pron : prons ) {
                System.out.print (pron + "   // stress pattern: ");
                for( Integer x : pron.getStressPattern() ) {
                    System.out.print(x);
                }
                System.out.println("");
            }
        }
        
        // find matches
        StressPattern pat = new StressPattern("w,s,w,w,s,w,w,w,s,w");
        System.out.println("Pattern: " + pat);
        
        System.out.println("possible start positions: " + pat.wordFitsAtPositions(prons));
    }

}
