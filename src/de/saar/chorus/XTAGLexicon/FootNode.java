import java.io.*;
import java.util.*;

public final class FootNode extends LeafNode {


    public FootNode(String cat, String index, Node mother) {
	super(cat, index, mother);
    }

    public FootNode(String cat, String index) {
	super(cat, index);
    }

    public Node copyAndReplace(List<Anchor> anchors, String lookUp) {
	return new FootNode(cat, index);
    }

    public void printXML() {
	throw new Error("Not implemented");
    }

    public void printXMLInBuffer(StringBuffer result, String distance) {
	result.append(distance+"<foot cat=\""+cat+"\"/>");;
    }

    public void printLisp() {
	System.out.print("*");
	System.out.print(cat);
    }

    public void printLispInBuffer(StringBuffer result) {
	result.append("*");
	result.append(cat);
    }

 public List<Node> getChildren(){
	throw new Error ("oops");
    }
}

