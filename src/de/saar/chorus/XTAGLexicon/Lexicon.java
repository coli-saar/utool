/*
 * Lexicon.java
 */

import java.util.*;

public class Lexicon {

    private POSScaler scaler;

    private Map<String, Set<MorphInfo>> morph;
    private Map<String, Set<SyntInfo>> syntax;
    private Map<String, Node> trees;
    private Map<String, Set<String>> families;

    public Lexicon(POSScaler scaler) {
	this.scaler = scaler;
	this.morph = new HashMap<String, Set<MorphInfo>>();
	this.syntax = new HashMap<String, Set<SyntInfo>>();
	this.trees = new HashMap<String, Node>();
	this.families = new HashMap<String, Set<String>>();
    }

    /**
     * look up a word 
     * @param word the word
     * @return a set of trees for that word
     */
    public Set<Node> lookup(String word) 
	throws Exception
    {
	//collect the trees in this set
	Set<Node> result = new HashSet<Node>();
	
	try{
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
		    //get the Trees, Families and Anchors
		    Set<String> syntTrees = nextSynt.getTrees();
		    Set<String> syntFamilies = nextSynt.getFamilies();
		    List<Anchor> syntAnchors = nextSynt.getAnchors();
		    if (syntTrees != null){
			//for all trees
			for (String it3 : syntTrees){
			    //get the root Node
			    Node nextNode = trees.get(it3);
			    if (nextNode != null){
				//copy the tree and replace the anchors
				Node replacedNode = nextNode.copyAndReplace(syntAnchors, word);
				//add this tree to the result
				result.add(replacedNode);
				List<Node> nodes = new ArrayList<Node>();
				//lexicalize this tree, put the resulting new
				//trees in nodes
				replacedNode.lexicalize(nodes, word);
				//add all trees in nodes to the result
				result.addAll(nodes);}
			}
		    }
		    if (syntFamilies != null){
			//for all families
			for (String it3 : syntFamilies){
			    //get the trees of the family
			    Set<String> nextTrees = families.get(it3);
			    //for all of these trees
			    for (String it4 : nextTrees){
				//get the root Node
				Node nextNode = trees.get(it4);
				if (nextNode != null){
				    //copy the tree and replace the anchors
				    Node replacedNode = 
					nextNode.copyAndReplace(syntAnchors, word);
				    //add this tree to the result
				    result.add(replacedNode);
				    List<Node> nodes = new ArrayList<Node>();
				    //lexicalize this tree, put the resulting 
				    //new trees in nodes
				    replacedNode.lexicalize(nodes, word);
				    //add all trees in nodes to the result
				    result.addAll(nodes);}
			    }
			}
			
		    }
		}
	    } 
	}
    
    
	catch (NullPointerException e){
	    System.out.println("NullPointerException in Lexikon");}
	//return the result
    	return result;
    }

    public void addMorph(String word, String root, String pos) {
	Set<MorphInfo> infos = morph.get(word);
	
	if (infos == null) {
	    infos = new HashSet<MorphInfo>();
	    morph.put(word, infos);
	}
	infos.add(new MorphInfo(root, pos));
    }

    public void addSyntax(String root,
			  List<Anchor> anchors, 
			  Set<String> trees,
			  Set<String> families)
    {
	Set<SyntInfo> infos = syntax.get(root);

	if (infos == null)
	    syntax.put(root, infos = new HashSet<SyntInfo>());

	infos.add(new SyntInfo(root, anchors, trees, families));
    }
    
    public void addFamily(String name, Set<String> trees) {
	families.put(name, trees);
    }

    public void addTree(String name, Node root) {
	//System.out.print(name);
	//System.out.print("\t");
	//root.printLisp();
	//System.out.print("\n");

	trees.put(name, root);
    }

}
