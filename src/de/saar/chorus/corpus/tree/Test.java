/**
 * @file   Test.java
 * @author Alexander Koller
 * @date   Wed May 21 15:08:35 2003
 * 
 * @brief  A test program for this package.
 * 
 * 
 */

package de.saar.coli.chorus.corpus.tree;

import java.io.*;


/**
 * A class containing a main program to test the package.
 * 
 */
class Test {
    public static void main(String args[]) {
	try {
	    Document d = new Document(args[0]);
	    System.err.println(d);

	    MultiTrees mts = d.getSentences();

	    while( mts.hasMoreElements() ) {
		Tree syn = mts.next().getSyntax();
		
		System.out.println(syn.surface());

		//		System.err.println("reconstruct: " + syn.surface());
	    }

	} catch(Exception e) {
	    e.printStackTrace();
	}
	    
    }
}


/*
	    


	try {
	    Document d = new
	    Element firstSentence = d.getRoot().getElement(new XPath("/corpus/body/s"));

	    MultiTree mt = new MultiTree(firstSentence);
	    Syntax syn = mt.getSyntax();

	    System.err.println(mt.getTopology());
	    System.err.println(mt.getSyntax());
	    System.err.println("reconstruct: " + mt.getSyntax().surface());

	    Node a = syn.nodeForID("s1921_11");
	    Node b = syn.nodeForID("s1921_501");
	    System.err.println("a: " + a);
	    System.err.println("b: " + b);
	    
	    System.err.println("lub: " + 
			       syn.leastUpperBound(new Node[] {
				   a, b
			       }));

	}
*/
