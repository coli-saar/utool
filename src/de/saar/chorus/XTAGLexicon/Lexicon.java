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
	    
	    for (Iterator<MorphInfo> it = morphSet.iterator(); it.hasNext();){
		String entry = it.next().getRoot();
		Set<SyntInfo> syntSet = syntax.get(entry);
		if (syntSet == null){
		    throw new Exception("Not in Syntax : "+word);}
		for (Iterator<SyntInfo> it2 = syntSet.iterator(); it2.hasNext();){
		    SyntInfo nextSynt = it2.next();
		    Set<String> syntTrees = nextSynt.getTrees();
		    Set<String> syntFamilies = nextSynt.getFamilies();
		    List<Anchor> syntAnchors = nextSynt.getAnchors();
		    if (syntTrees != null){
			for (Iterator<String> it3 = syntTrees.iterator(); it3.hasNext();){
			    Node nextNode = trees.get(it3.next());
			    if (nextNode != null){
				Node replacedNode = nextNode.copyAndReplace(syntAnchors, word);
				result.add(replacedNode);}
			    //result.add(nextNode);}
			}
		    }
		    if (syntFamilies != null){
			for (Iterator<String> it3 = syntFamilies.iterator(); it3.hasNext();){
			    Set<String> nextTrees = families.get(it3.next());
			    for (Iterator<String> it4 = nextTrees.iterator(); it4.hasNext();){
				Node nextNode = trees.get(it4.next());
				if (nextNode != null){
				    Node replacedNode = nextNode.copyAndReplace(syntAnchors, word);
				    if (replacedNode != null){
				    result.add(replacedNode);}
				    else {
					result.add(nextNode);}
				}
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
