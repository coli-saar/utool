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

    public Set<Node> lookup(String word) 
	throws Exception
    {
	Set<Node> result = new HashSet<Node>();
	
	try{
	    Set<MorphInfo> morphSet = morph.get(word);
	    if (morphSet == null){
		throw new Exception("Not in Lexicon : "+word);}
	    
	    for (MorphInfo it : morphSet){
		String entry = it.getRoot();
		Set<SyntInfo> syntSet = syntax.get(entry);
		if (syntSet == null){
		    throw new Exception("Not in Syntax : "+word);}
		for (SyntInfo nextSynt : syntSet){
		    Set<String> syntTrees = nextSynt.getTrees();
		    Set<String> syntFamilies = nextSynt.getFamilies();
		    List<Anchor> syntAnchors = nextSynt.getAnchors();
		    if (syntTrees != null){
			for (String it3 : syntTrees){
			    Node nextNode = trees.get(it3);
			    if (nextNode != null){
				Node replacedNode = nextNode.copyAndReplace(syntAnchors, word);
				result.add(replacedNode);
				List<Node> nodes = new ArrayList<Node>();
				replacedNode.lexicalize(nodes, word);
				result.addAll(nodes);}
			}
		    }
		    if (syntFamilies != null){
			for (String it3 : syntFamilies){
			    Set<String> nextTrees = families.get(it3);
			    for (String it4 : nextTrees){
				Node nextNode = trees.get(it4);
				if (nextNode != null){
				    Node replacedNode = 
					nextNode.copyAndReplace(syntAnchors, word);
				    result.add(replacedNode);
				    List<Node> nodes = new ArrayList<Node>();
				    replacedNode.lexicalize(nodes, word);
				    result.addAll(nodes);}
			    }
			}
			
		    }
		}
	    } 
	}
    
    
	catch (NullPointerException e){
	    System.out.println("NullPointerException in Lexikon");}
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
