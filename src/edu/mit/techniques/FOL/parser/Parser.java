package edu.mit.techniques.FOL.parser;

import java.io.*;
import edu.mit.techniques.FOL.*;

// Takes a tokenizer, returns a sentence

// Doesn't have any state;  so we'll just use a bunch of static methods

// A recursive-descent parser.  One method per non-terminal.
// Each method assumes that the first token has already been read.

// The only public method is parse, which assumes no tokens have been read.


public class Parser {

  public static Sentence parse(String input){
	try{
	  return startSentence(new Tokenizer(new StringReader(input)));
	}
	catch(IOException e){
	    return null;
	}
  }

    public static Sentence parseFile(String path){
	try {
	    return startSentence(new Tokenizer(path));
	}
	catch (IOException e){
	    System.out.println("File read failed:" + e);
	    return null;
	}
    }

	private static boolean debug = true;

	// Start parsing a sentence
	static Sentence startSentence (Tokenizer tz) throws IOException {
		tz.getNextToken();
		return parseSentence (tz);
		// Check to be sure there are no more tokens?
	}

	// Sentence -> Sentence1 doubleArrow Sentence | Sentence1
	
	private static Sentence parseSentence (Tokenizer tz) throws java.io.IOException {
		Sentence s1 = parseSentence1 (tz);
	
		if (tz.currentToken() == Token.doubleArrow){
			// Parse another sentence and return an equivalence
			tz.getNextToken();
			Sentence s = parseSentence(tz);
			return new Equivalence(s1,s);
		} else {
			return s1;
		}
	}
	
	// Sentence1 -> Sentence2 singleArrow Sentence1 | Sentence2
	
	private static Sentence parseSentence1 (Tokenizer tz) throws java.io.IOException {
		Sentence s2 = parseSentence2 (tz);

		if (tz.currentToken() == Token.singleArrow){
			// Parse another sentence and return an implication
			tz.getNextToken();
			Sentence s = parseSentence1(tz);
			return new Implication(s2,s);
		} else {
			return s2;
		}
	}
	
	// Sentence2 -> Sentence3 vee Sentence2 | Sentence3
	
	private static Sentence parseSentence2 (Tokenizer tz) throws java.io.IOException {
		Sentence s3 = parseSentence3 (tz);
	
		if (tz.currentToken() == Token.vee){
			// Parse another sentence and return a disjunction
			tz.getNextToken();
			Sentence s = parseSentence2(tz);
			return new Disjunction(s3,s);
		} else {
			return s3;
		}
	}
	
	// Sentence3 -> Sentence4 wedge Sentence3 | Sentence4
	
	private static Sentence parseSentence3 (Tokenizer tz) throws java.io.IOException {
		Sentence s4 = parseSentence4 (tz);

		if (tz.currentToken() == Token.wedge){
			// Parse another sentence and return a conjunction
			tz.getNextToken();
			Sentence s = parseSentence3(tz);
			return new Conjunction(s4,s);
		} else {
			return s4;
		}
	}
	
	// Sentence4 -> neg Sentence4 | Exists Symbol Sentence | All Symbol Sentence |
	//				( Sentence ) | Proposition
	
	private static Sentence parseSentence4 (Tokenizer tz) throws java.io.IOException {
	
		// The current token tells us what to do
		Token ct = tz.currentToken();
			
		if (ct == Token.neg){
			// Parse another sentence and return a negation
			tz.getNextToken();
			return new Negation (parseSentence4 (tz));
		}
		
		else if (ct == Token.exists){	
			// Parse a symbol and a sentence, and return an existential
			tz.getNextToken();
			Variable v = parseVariable(tz);
			Sentence s = parseSentence(tz);
			return new Existential(v, s);
		}
		
		else if (ct == Token.all){
			// Parse a symbol and a sentence, and return a universal			
			tz.getNextToken();
			Variable v = parseVariable(tz);
			Sentence s = parseSentence(tz);
			return new Universal(v, s);
		}
		
		else if (ct == Token.leftParen){
			// Parse a sentence and check for the matching paren
			tz.getNextToken();
			Sentence s = parseSentence(tz);
			tz.consumeToken(Token.rightParen);
			return s;
		}
		
		else if (ct == Token.singleArrow || ct == Token.doubleArrow || 
				 ct == Token.vee || ct == Token.wedge){ 
			// Internal error:  we should have dealt with these above
			throw new RuntimeException("Internal error: shouldn't see token " +
										tz.currentToken() + " here");
		}
		
		else if (ct == Token.rightParen || ct == Token.comma){
			//  Syntax error
			throw new RuntimeException("Syntax error: shouldn't see token " +
										tz.currentToken() + " here");
		}
		
		else {
			// Proposition
			return parseProposition(tz);
		}
	}
	
	// Proposition -> AtomicProposition | leftParen termList rightParen | T | F
	
	// Start by parsing a symbol.  If the next token is leftParen, then its a 
	// compound, otherwise it's one of the atoms
	
	private static Proposition parseProposition (Tokenizer tz)  throws java.io.IOException {
	
		if (tz.currentToken() == Token.t){
			tz.getNextToken();
			return new TrueProposition();
		} 
		else if (tz.currentToken() == Token.f){
			tz.getNextToken();
			return new FalseProposition();
		}
		else {
	
			Symbol sym = parseSymbol(tz);
		
			if (tz.currentToken() == Token.leftParen){
				// This is going to be a compound proposition			
				tz.getNextToken();
				TermList termList = parseTermList(tz);
				return new CompoundProposition (sym,termList);
			} else {
				// Regular atomic proposition
				return new AtomicProposition(sym.name());
			}
		}
	}
	
	// TermList -> epsilon | Term | Term comma TermList
	
	// The only legal token after a TermList is a rightParen, so that's how we can
	// tell whether to take the epsilon production
	
	private static TermList parseTermList (Tokenizer tz) throws java.io.IOException  {
		if (tz.currentToken() == Token.rightParen){
			tz.getNextToken();
			return null;
		} else {
			Term term = parseTerm(tz);
			// Next token has to be either comma or rightParen
			if (tz.currentToken() == Token.rightParen){
				// We're done.
				tz.getNextToken();
				return new TermList(term, null);
			} else {
				// Keep going
				tz.consumeToken(Token.comma);
				TermList restList = parseTermList(tz);
				return new TermList(term, restList);
			}
			
		}
	}
	
	// Term -> Symbol | Symbol leftParen TermList rightParen
	
	// We'll read a symbol, then keep going if the next token is a left paren
	
	private static Term parseTerm (Tokenizer tz) throws java.io.IOException  {
		
		Symbol sym = parseSymbol(tz);
		
		if (tz.currentToken() == Token.leftParen){
			// This is going to be function application			
			tz.getNextToken();
			TermList termList = parseTermList(tz);
			return new Function (sym, termList);
		} else {
		    // This is either a variable or a constant
		    // By convention, we assume that it's a constant if the name
		    // starts with x,y,z. - ak
		    char firstChar = sym.name().charAt(0);

		    if( (firstChar == 'x') || (firstChar == 'y') || (firstChar == 'z') )
			return new Variable(sym.name());
		    else
			return new ConstantTerm(sym.name());
		}
	}
	
	private static Symbol parseSymbol (Tokenizer tz) throws java.io.IOException  {
	
		Token ct = tz.currentToken();
		
		// Be sure it isn't one of our reserved tokens
		if (!ct.isSymbol()) {		
			throw new RuntimeException("Syntax error: Expected symbol, got " +
											tz.currentToken());
		} else {
			Symbol sym = new Symbol (tz.currentToken().name());
			tz.getNextToken();
			return sym;
		}
	}
	
	private static Variable parseVariable (Tokenizer tz) throws java.io.IOException  {
	
		Token ct = tz.currentToken();
		
		// Be sure it isn't one of our reserved tokens
		if (!ct.isSymbol()) {		
			throw new RuntimeException("Syntax error: Expected symbol, got " +
											tz.currentToken());
		} else {
			Variable var = new Variable (tz.currentToken().name());
			tz.getNextToken();
			return var;
		}
	}
	
	
	// Test!
	
	public static void main (String[] args) throws java.io.IOException {
	
		Tokenizer toots = new Tokenizer("test");
		
		System.out.println("**************   New Test   ****************");
		
		while (toots.hasMoreTokens()){
			Sentence s = startSentence(toots);
			System.out.println(s.clausalForm());
		}
	}
		

}
