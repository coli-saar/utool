/*
 * Ubench.java
 */

package de.saar.chorus.XTAGLexicon;

import de.saar.getopt.ConvenientGetopt;

public class Main {
    public static String xmlpath;
    
    public static void main (String args[]){
        ConvenientGetopt getopt = new ConvenientGetopt("XTAG Test", "", "");
        getopt.addOption('d', "grammar-directory", ConvenientGetopt.REQUIRED_ARGUMENT, "Specify directory that contains the XML grammar files.", ".");
        getopt.parse(args);

        Lexicon lexicon = Lexicon.readFromDirectory(getopt.getValue('d'), true);
        
        for (String word : getopt.getRemaining() ) {
            System.out.println(word+":");
            try{
                for (Tree tree : lexicon.lookup(word)) {
                    System.out.println(tree + "\n");
                }
            }
            catch (Exception e){
                System.out.println(e.getMessage());}
        }
    }
    
    
}
