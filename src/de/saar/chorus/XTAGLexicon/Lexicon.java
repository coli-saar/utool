/*
 * Lexicon.java
 */
package de.saar.chorus.XTAGLexicon;

import java.io.File;
import java.util.*;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.DefaultHandler;

public class Lexicon {
    
    private POSScaler scaler;
    
    private Map<String, Set<MorphInfo>> morph;
    private Map<String, Set<SyntInfo>> syntax;
    private Map<String, Tree> trees;
    private Map<String, Set<String>> families;
    
    public Lexicon(POSScaler scaler) {
        this.scaler = scaler;
        this.morph = new HashMap<String, Set<MorphInfo>>();
        this.syntax = new HashMap<String, Set<SyntInfo>>();
        this.trees = new HashMap<String, Tree>();
        this.families = new HashMap<String, Set<String>>();
    }
    
    /**
     * look up a word 
     * @param word the word
     * @return a set of trees for that word
     */
    public Set<Tree> lookup(String word) throws Exception
    {
        //collect the trees in this set
        Set<Tree> result = new HashSet<Tree>();
        
        //try to find the word in morphSet
        Set<MorphInfo> morphSet = morph.get(word);
        //if word not in morphSet, return
        if (morphSet == null){
            throw new Exception("Not in Lexicon : "+word);}
        //for all the MorphInfos of the word
        for (MorphInfo it : morphSet){
            //get the root and try to find it in syntSet
            String entry = it.getRoot();
            Set<SyntInfo> syntSet = syntax.get(entry);
            //if root not in syntSet, return
            if (syntSet == null){
                throw new Exception("Not in Syntax : "+word);}
            //for all SyntInfos of the word
            for (SyntInfo nextSynt : syntSet){
                Set<String> syntTrees = nextSynt.getTrees();
                Set<String> syntFamilies = nextSynt.getFamilies();
                List<Anchor> syntAnchors = nextSynt.getAnchors();
                
                // add the trees listed in the SyntInfo
                if (syntTrees != null) {
                    for (String treename : syntTrees){
                        Tree tree = trees.get(treename);
                        if (tree != null) {
                            addSplitTrees(tree, syntAnchors, word, result);
                        }
                    }
                }
                
                // add the trees from all families listed in the SyntInfo
                if (syntFamilies != null){
                    for (String familyname : syntFamilies) {
                        for (String treename : families.get(familyname) ) {
                            Tree tree = trees.get(treename);
                            addSplitTrees(tree, syntAnchors, word, result);
                        }
                    }
                }
            }
        } 
        
        
        //return the result
        return result;
    }
    

    /**
     * look up a word 
     * @param word the word
     * @return a set of trees for that word
     */
    public Set<Tree> lookupNonSplit(String word) throws Exception
    {
        //collect the trees in this set
        Set<Tree> result = new HashSet<Tree>();
        
        //try to find the word in morphSet
        Set<MorphInfo> morphSet = morph.get(word);
        //if word not in morphSet, return
        if (morphSet == null){
            throw new Exception("Not in Lexicon : "+word);}
        //for all the MorphInfos of the word
        for (MorphInfo it : morphSet){
            //get the root and try to find it in syntSet
            String entry = it.getRoot();
            Set<SyntInfo> syntSet = syntax.get(entry);
            //if root not in syntSet, return
            if (syntSet == null){
                throw new Exception("Not in Syntax : "+word);}
            //for all SyntInfos of the word
            for (SyntInfo nextSynt : syntSet){
                Set<String> syntTrees = nextSynt.getTrees();
                Set<String> syntFamilies = nextSynt.getFamilies();
                List<Anchor> syntAnchors = nextSynt.getAnchors();
                
                // add the trees listed in the SyntInfo
                if (syntTrees != null) {
                    for (String treename : syntTrees){
                        Tree tree = trees.get(treename);
                        if (tree != null) {
                            addInstantiatedTrees(tree, syntAnchors, word, result);
                        }
                    }
                }
                
                // add the trees from all families listed in the SyntInfo
                if (syntFamilies != null){
                    for (String familyname : syntFamilies) {
                        for (String treename : families.get(familyname) ) {
                            Tree tree = trees.get(treename);
                            addInstantiatedTrees(tree, syntAnchors, word, result);
                        }
                    }
                }
            }
        } 
        
        
        //return the result
        return result;
    }
    
    private void addSplitTrees(Tree tree, List<Anchor> syntAnchors, String word, Collection<Tree> result) {
        // copy the tree and replace the anchors
        Tree instance = tree.instantiate(syntAnchors, word);
        //result.add(instance);         // add this tree to the result -- why do we want this?? AK
        
        // split into strictly lexicalised parts
        for( Tree part : instance.splitIntoStrictlyLexicalisedParts(word) ) {
            result.add(part);
        }
    }

    
    private void addInstantiatedTrees(Tree tree, List<Anchor> syntAnchors, String word, Collection<Tree> result) {
        // copy the tree and replace the anchors
        Tree instance = tree.instantiate(syntAnchors, word);
        result.add(instance);  
    }

    public void addMorph(String word, String root, String pos, String agr) {
        Set<MorphInfo> infos = morph.get(word);
        
        if (infos == null) {
            infos = new HashSet<MorphInfo>();
            morph.put(word, infos);
        }
        infos.add(new MorphInfo(root, pos, agr));
    }
    
    public void addSyntax(String root,
            List<Anchor> anchors, 
            Set<String> trees,
            Set<String> families,
            List<UnificationEquation> equations)
    {
        Set<SyntInfo> infos = syntax.get(root);
        
        if (infos == null)
            syntax.put(root, infos = new HashSet<SyntInfo>());
        
        infos.add(new SyntInfo(root, anchors, trees, families, equations));
    }
    
    public void addFamily(String name, Set<String> trees) {
        families.put(name, trees);
    }
    
    public void addTree(Tree tree) {
        //System.out.print(name);
        //System.out.print("\t");
        //root.printLisp();
        //System.out.print("\n");
        
        trees.put(tree.getName(), tree);
    }
    
    
    public static Lexicon readFromDirectory(String directory, boolean verbose) {
        Lexicon lexicon = new Lexicon(new POSScaler());
        
        if( verbose )  System.err.println("reading trees.xml ...");
        parse(directory + "/trees.xml", new TreeHandler(lexicon));
        
        if( verbose )  System.err.println("reading families.xml ...");
        parse(directory + "/families.xml", new FamilyHandler(lexicon));
        
        if( verbose )  System.err.println("reading morphology.xml ...");
        parse(directory + "/morphology.xml", new MorphHandler(lexicon));
        
        if( verbose )  System.err.println("reading syntax.xml ...");
        parse(directory + "/syntax.xml", new SyntHandler(lexicon));
        
        return lexicon;
    }
    
    private static void parse(String filename, DefaultHandler handler) {
        try {   
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            
            parser.parse(new File(filename), handler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
