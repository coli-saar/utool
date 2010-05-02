/*
 * @(#)CmuDict.java created 18.07.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.poetry.stress;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class CmuDict {
    private Map<String,List<WordPronunciation>> words;
    
    public CmuDict() {
        words = new HashMap<String,List<WordPronunciation>>();
    }
    
    public void read(String filename, boolean verbose) {
        int lineNo = 0;
        
        if( verbose ) {
            System.err.print("Reading pronunciation dictionary: ");
        }
        
        try {
            File f = new File(filename);
            InputStream fr = new FileInputStream(f);
            
            if( filename.endsWith(".gz")) {
            	fr = new GZIPInputStream(fr);
            }
            
            BufferedReader r = new BufferedReader(new InputStreamReader(fr));
            
            Pattern linePattern = Pattern.compile("(\\S+)\\s+(.*)");
            Pattern bracketPattern = Pattern.compile("([^\\(]+).*");
            
            String line;
            
            do {
                line = r.readLine();
                if( line != null ) {
                    if( verbose && (lineNo % 1000 == 0) ) {
                        System.err.print(".");
                    }
                    lineNo ++;
                    
                    Matcher lineM = linePattern.matcher(line);
                    if( lineM.matches() ) {
                        String word = lineM.group(1);
                        String pron = lineM.group(2);
                        
                        Matcher bracketM = bracketPattern.matcher(word);
                        if( bracketM.matches() ) {
                            word = bracketM.group(1);
                        }
                        
                        lookupOrCreate(word.toLowerCase()).add(new WordPronunciation(pron));
                    }
                }
            } while( line != null );
        } catch(IOException e) {
            System.err.println("Error while reading " + filename);
            System.exit(1);
        }
        
        if( verbose ) {
            System.err.println(" done.");
        }
    }
    
    public List<WordPronunciation> lookup(String word) {
        return words.get(word.toLowerCase());
    }

    private List<WordPronunciation> lookupOrCreate(String word) {
        if( words.containsKey(word)) {
            return words.get(word);
        } else {
            List<WordPronunciation> ret = new ArrayList<WordPronunciation>();
            words.put(word, ret);
            return ret;
        }
    }
}
