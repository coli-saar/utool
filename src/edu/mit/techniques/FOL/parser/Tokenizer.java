package edu.mit.techniques.FOL.parser;

import java.io.*;

class Tokenizer {

	private StreamTokenizer st;
	private boolean eof = false;
	private Token currentToken = null;

	// Change this to take some kind of character stream as input
	public Tokenizer (String path) throws java.io.IOException {
		// open file
		st = new StreamTokenizer(new FileReader(path));

		// Changed from true;  hope it doesn't break...
		st.eolIsSignificant(false);

	}

	// Tokenize a stream specified by the reader

	public Tokenizer (Reader reader) throws java.io.IOException {
		st = new StreamTokenizer(reader);
		st.eolIsSignificant(true);
	}

	// Consume the next token.  Throw an exception if there's a problem.

	public void getNextToken() throws java.io.IOException {

		st.nextToken();

		switch (st.ttype){

			case StreamTokenizer.TT_EOF:
				eof = true;
				return;
			case StreamTokenizer.TT_EOL:
				currentToken = Token.eol;
				return;
			case StreamTokenizer.TT_WORD:
				if (st.sval.equals("exists")) {
					currentToken = Token.exists;
				}
				else if (st.sval.equals("all")) {
					currentToken = Token.all;
				}
				else if (st.sval.equals("v")) {
					currentToken = Token.vee;
				}
				else if (st.sval.equals("T")) {
					currentToken = Token.t;
				}
				else if (st.sval.equals("F")) {
					currentToken = Token.f;
				}
				else {
					currentToken = new Token (st.sval, true);
				}
				return;
			case '~':
				currentToken = Token.neg;
				return;
			case '<':
				consume('-');
				consume('>');
				currentToken = Token.doubleArrow;
				return;
			case '-':
				consume('>');
				currentToken = Token.singleArrow;
				return;
			case '^':
				currentToken = Token.wedge;
				return;
			case '(':
				currentToken = Token.leftParen;
				return;
			case ')':
				currentToken = Token.rightParen;
				return;
			case ',':
				currentToken = Token.comma;
				return;
			default:
				// Fix this!  Make our own error class.  Catch it somewhere reasonable.
				throw new RuntimeException("Error: Illegal token");
		}

	}

	private void consume(char c) throws java.io.IOException {
		st.nextToken();
		if (st.ttype != c){
			throw new RuntimeException("Expected to read " +
				c + " Actually read " + st.ttype);
		}
	}



	// Look at the current token.  Doesn't change state.

	public Token currentToken(){
		return currentToken;
	}

	// Are we at the end of the stream?

	public boolean hasMoreTokens (){
		return !eof;
	}

	// Check to see that the current token is the desired token.
	// if so, consume it.  If not, raise an exception.

	public void consumeToken (Token desiredToken) throws java.io.IOException {
		if (currentToken == desiredToken){
			getNextToken();
		} else {
			throw new RuntimeException ("Syntax error:  Expected token " +
										desiredToken + "; Saw token " + currentToken);
		}
	}



	// Test!
	public static void main (String[] args) throws java.io.IOException {

		Tokenizer toots = new Tokenizer("test");

		while (toots.hasMoreTokens()){
			toots.getNextToken();
			System.out.println(toots.currentToken());
		}
	}


}
