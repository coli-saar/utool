import java.io.*;
import java.util.*;

public final class SubstitutionNode extends LeafNode {

    
    public SubstitutionNode(String cat, String index, Node node) {
	super(cat, index, node);
    }

    public SubstitutionNode(String cat, String index) {
	super(cat, index);
    }

    public Node copyAndReplace(List<Anchor> anchors, String lookUp) {
	return new SubstitutionNode(cat, index);
    }

    
    public void printXML() {
	throw new Error("Not implemented");
    }

    public void printXMLInBuffer(StringBuffer result, String distance) {
	result.append(distance+"<substitution cat=\""+cat+"\"/>");
    }


    public void printLisp() {
	System.out.print("$");
	System.out.print(cat);
    }

    public void printLispInBuffer(StringBuffer result) {
	result.append("$");
	result.append(cat);
    }
 public List<Node> getChildren(){
	throw new Error ("oops");
    }
}
