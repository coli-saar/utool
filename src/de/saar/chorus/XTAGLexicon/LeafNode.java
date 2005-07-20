package de.saar.chorus.XTAGLexicon;

import java.io.*;
import java.util.*;

public abstract class LeafNode extends Node {

  

    public LeafNode(String cat, String index, Node mother) {
	super(cat, index, mother);
    }

    public LeafNode(String cat, String index) {
	super(cat, index);
    }

    //senseless in LeafNodes
    public void addChild(Node node) {
	throw new Error("oops");
    } 

    //senseless in LeafNodes
    public List<Node> getChildren(){
	throw new Error ("oops");
    }

    public int hashCode(){
	return cat.hashCode();
    }

}
