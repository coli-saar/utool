import java.util.*;
import java.io.*;

public class Converter {

    //speichert alle vorgekommenen Adressen
    private SortedSet<String> addresses;
    //speichert alle vorgekommenen Labels
    private Set<String> labels;
    //Abbildung von Kategorien auf Adressen
    private Map<String,Set<String>> cats2adds;
    //Nummerierung der Baeume
    private int counter;
    //Abbildung von der Nummerierung auf die entspr. 
    //Baeume
    private Map<Integer, Node> nums2trees;
    //Liste der resultierenden XDG-Eintraege
    private List<XDGEntry> results;
   

    /**
     * Initialize all global variables
     */
    public Converter (){
	addresses = new TreeSet<String>(new AddressComparator());
	labels = new HashSet<String>();
	cats2adds = new HashMap<String, Set<String>>();
	nums2trees = new HashMap<Integer, Node>();
	results = new ArrayList<XDGEntry>();
	counter = 1;
    }

    /**
     * Read the information in the given 
     * set of trees into the global variables
     * @param treeSet the tree set
     */
    public void convert(Set<Node> treeSet){
	for (Node node : treeSet){
	    XDGEntry newEntry = new XDGEntry(counter);
	    nums2trees.put(new Integer(counter), node);
	    String nodeCat = node.getCat();
	    String aux;
	    if (node.isLeftAux()){
		aux = "L";
	    }
	    else{
		if (node.isRightAux()){
		    aux = "R";}
		else {aux = "M";		
		newEntry.passedFoot = true;}
	    }
	    newEntry.rootCat = nodeCat; 
	    newEntry.auxDirection = aux;
	    this.traverseTree(node, newEntry, ".", true);
	    results.add(newEntry);
	    counter++;}
	addresses.add("M.");
	this.updateInLp();
    }
	
    /**
     * collect and store relevant information from a
     * given node, continue in this node's children,
     * if it has some
     * @param node the node
     * @param entry the XDG-entry for the tree to 
     * which the node belongs
     * @param address the address of the node
     * @param isRoot true, if the node is a root
     */ 
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
	

	
	if (node instanceof SubstitutionNode){
		  entry.outId.add(nodeCat+"_!");
		  address = "M"+address;
		  entry.outLp.add(address+"_!");
		  addresses.add(address);
		  labels.add(nodeCat);
		  if (entry.linking.containsKey(address)){
		      entry.linking.get(address).add(nodeCat);}
		  else {
		      HashSet<String> addSet = new HashSet<String>();
		      addSet.add(nodeCat);
		      entry.linking.put(address, addSet);}
	}
	else {
	    if (node instanceof InnerNode){
		String separator = "";
		if (!isRoot){
		    separator = ".";}
		String auxAddress = address;
		if (!entry.passedFoot){
		    auxAddress = entry.auxDirection+address;
		    addresses.add(auxAddress);
		    if (entry.linking.containsKey(auxAddress)){
			entry.linking.get(address).add(nodeCat);}
		    else {
			HashSet<String> addSet = new HashSet<String>();
			addSet.add(nodeCat);
			entry.linking.put(auxAddress, addSet);}
		    if (node.isAdj()){
			entry.outLp.add(auxAddress+"_A_?");
			entry.outId.add(nodeCat+"_A_?");
		    }
		}
		else {
		    if (entry.linking.containsKey("R"+address)){
			entry.linking.get(address).add(nodeCat);}
		    else {
			HashSet<String> addSet = new HashSet<String>();
			addSet.add(nodeCat);
			entry.linking.put("R"+address, addSet);}
		    if (entry.linking.containsKey("L"+address)){
			entry.linking.get(address).add(nodeCat);}
		    else {
			HashSet<String> addSet = new HashSet<String>();
			addSet.add(nodeCat);
			entry.linking.put("L"+address, addSet);}
		    addresses.add("L"+address);
		    addresses.add("R"+address);
		    if (node.isAdj()){
			entry.outId.add(nodeCat+"_A_?");
			entry.outLp.add("L"+address+"_A_?");
			entry.outLp.add("R"+address+"_A_?");
		    }
		}
		int counter = 1;
		
		for (Node child : node.getChildren()){
		    this.traverseTree(child, entry, address
				      +separator+counter, false);
		    counter++;}
	
		labels.add(nodeCat);
	    
	    }
	    else {
		if (node instanceof FootNode){
		    entry.passedFoot = true;
		    addresses.add("M"+address);
		    labels.add(nodeCat);
		  
		}
		else {
		    if (node instanceof TerminalNode){
			addresses.add("M"+address);
		
		       	if (node.isAnchor()){
			    entry.anchor = node.getCat();
			    entry.anchorAddress = "M"+address;}
		    }
		}
	    }
	}
    }
    
    /**
     * update the inLp-lists of all entries in results
     */
    public void updateInLp(){
	HashSet<String> mAds = new HashSet<String>();
	HashSet<String> lAds = new HashSet<String>();
	HashSet<String> rAds = new HashSet<String>();
	for (String ad : addresses){
	    if (ad.charAt(0) == 'M'){
		mAds.add(ad);}
	    else{
		if (ad.charAt(0) == 'L'){
		    lAds.add(ad);}
		else rAds.add(ad);}}
	for (XDGEntry entry : results){
	    if (entry.auxDirection.equals("M")){
		entry.inLp = mAds;}
	    else {
		if (entry.auxDirection.equals("L")){
		    entry.inLp = lAds;}
		else entry.inLp = rAds;}}
    }

    /**
     * print the collected information 
     * and the entries in XDG-Grammar-style
     * @param sb the StringBuffer to print into
     */
    public void printXDG(StringBuffer sb){
	XDGWriter writer = new XDGWriter();
	writer.printHeader(sb, addresses, labels);
	for (XDGEntry entry : results){
	    writer.printEntry(sb,entry, nums2trees.get(entry.number));}
	writer.printEnd(sb);
    }

    /**
     * test-print of the results
     * @param string the StringBuffer to print into
     */
    public void testPrint (StringBuffer string){
	for (XDGEntry entry : results){
	    string.append("<entry num=\""+entry.number+"\">\n");
	    string.append(" <inId cat=\""+entry.rootCat+"\">\n");
	    string.append(" <outId> \n");
	    for (String cat : entry.outId){
		string.append("  <element cat=\""+cat+"\">\n");
	    }
	    string.append(" </outId> \n");
	    string.append(" <inLp>\n");
	    //for (String add : entry.inLp){
	    //string.append("  <element add=\""+add+"\">\n");
	    //}
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
	    string.append("\n");
	}
    }


}
