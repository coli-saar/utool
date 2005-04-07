import java.util.*;
import java.io.*;

public class Converter {

    //speichert alle vorgekommenen Adressen
    private Set<String> addresses;
    //Abbildung von Kategorien auf Adressen
    private Map<String,Set<String>> cats2adds;
    //Nummerierung der Baeume
    private int counter;
    //Abbildung von der Nummerierung auf die entspr. 
    //Baeume
    private Map<Integer, Node> nums2trees;
    //Liste der resultierenden XDG-Eintraege
    private List<XDGEntry> results;
   


    public Converter (){
	addresses = new HashSet<String>();
	cats2adds = new HashMap<String, Set<String>>();
	nums2trees = new HashMap<Integer, Node>();
	results = new ArrayList<XDGEntry>();
	counter = 1;
    }



    public void convert(Set<Node> treeSet){
	for (Node node : treeSet){
	    XDGEntry newEntry = new XDGEntry(counter);
	    nums2trees.put(new Integer(counter), node);
	    String nodeCat = node.getCat();
	    String aux;
	    if (node.isLeftAux()){
		aux = "_L";}
	    else{
		if (node.isRightAux()){
		    aux = "_R";}
		else {aux = "";}
	    }
	    newEntry.rootCat = nodeCat; 
	    newEntry.auxDirection = aux;
	    this.traverseTree(node, newEntry, "", true);
	    results.add(newEntry);
	    counter++;}
	this.updateInLp();
    }
	


    public void traverseTree(Node node, 
			     XDGEntry entry, 
			     String address,
			     boolean isRoot){
	node.setAddress(address);
	String nodeCat = node.getCat();
	if (cats2adds.containsKey(nodeCat)){
	    cats2adds.get(nodeCat).add(address);}
	else {
	    HashSet<String> addSet = new HashSet<String>();
	    addSet.add(address);
	    cats2adds.put(nodeCat, addSet);}
	addresses.add(address);
	if (entry.linking.containsKey(address)){
		      entry.linking.get(address).add(nodeCat);}
		  else {
		      HashSet<String> addSet = new HashSet<String>();
		      addSet.add(nodeCat);
		      cats2adds.put(address, addSet);}
	if (node instanceof SubstitutionNode){
		  entry.outId.add(nodeCat+"_!");
		  entry.outLp.add(address+"_!");
	}
	else {
	    if (node instanceof InnerNode){
		if (node.isAdj()){
		    entry.outId.add(nodeCat+"_A_?");
		    if (isRoot){
			entry.outLp.add(address+"_A"+entry.auxDirection+"_?");}
		    else {entry.outLp.add(address+"_A_?");}
		}
		int counter = 1;
		for (Node child : node.getChildren()){
		    this.traverseTree(child, entry, address+"."+counter, false);
		    counter++;}
	    }
	}
    }

    public void updateInLp(){
	for (XDGEntry entry : results){
	    entry.inLp = cats2adds.get(entry.rootCat);
	}
    }



    

    public void printInBuffer (StringBuffer string){
	for (XDGEntry entry : results){
	    string.append("<entry num=\""+entry.number+"\">\n");
	    string.append(" <inId cat=\""+entry.rootCat+"\">\n");
	    string.append(" <outId> \n");
	    for (String cat : entry.outId){
		string.append("  <element cat=\""+cat+"\">\n");
	    }
	    string.append(" </outId> \n");
	    string.append(" <inLp>\n");
	    for (String add : entry.inLp){
		string.append("  <element add=\""+add+"\">\n");
	    }
	    string.append(" </inLp>\n");
	    string.append(" <outLp>\n");
	    for (String add : entry.outLp){
		string.append("  <element add=\""+add+"\">\n");
	    }
	    string.append(" </outLp>\n");
	    string.append(" <linking>\n");
	    for (String add : entry.linking.keySet()){
		string.append("  <link add=\""+add+"\" ");
		for (String cat : entry.linking.get(add)){
		    string.append("cat=\""+cat+"\">\n");}
	    }
	    string.append(" </linking>\n");
	    nums2trees.get(entry.number).printXMLInBuffer(string," ");
	}
    }


}
