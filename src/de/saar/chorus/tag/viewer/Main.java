/*
 * @(#)Ubench.java created 25.08.2005
 * 
 * Copyright (c) 2005 Alexander Koller
 *  
 */

package de.saar.chorus.tag.viewer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import de.saar.chorus.XTAGLexicon.Lexicon;
import de.saar.chorus.XTAGLexicon.Tree;
import de.saar.chorus.jgraph.improvedjgraph.GraphScroller;
import de.saar.getopt.ConvenientGetopt;
import de.saar.swing.JStandardFrame;

public class Main {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        ConvenientGetopt getopt = new ConvenientGetopt("XTAG Test", "", "");
        getopt.addOption('d', "grammar-directory", ConvenientGetopt.REQUIRED_ARGUMENT, "Specify directory that contains the XML grammar files.", ".");
        getopt.parse(args);

        Lexicon lexicon = Lexicon.readFromDirectory(getopt.getValue('d'), true);
        List<Tree> trees = new ArrayList<Tree>();
        
        loop:
        for( String word : getopt.getRemaining() ) {
            for( Tree tree : lexicon.lookupNonSplit(word)) {
                trees.add(tree);
            }
        }
        
        JFrame f = new JStandardFrame("XTAG Viewer");
        
        GraphScroller sc = new GraphScroller("Tree", new TagTreeSource(trees), "XTAG Viewer");
        f.add(sc);
        f.pack();
        f.setVisible(true);
        
        sc.selectGraph(0);
        f.pack();
    }
}
